# Decisão Técnica — A "hora atual" da sessão: regra no domínio, relógio do banco

**Data:** setembro de 2026
**Escopo:** definição do estado aberta/fechada da sessão de votação.
**Referência:** resolução do ponto fraco [PF9 — Fechamento da sessão depende do relógio do
servidor](pontos-fracos-e-falhas.md).

---

## 1. O problema real

A sessão não guarda estado "aberta/fechada": ele é **derivado da hora atual** comparada com a
expiração. Para aceitar um voto, o sistema decide **"o agora ainda está antes de `expires_at`?"** —
e o que conta como "agora" depende de qual relógio responde.

### O cenário em um exemplo concreto

Uma sessão é criada pela instância A com `startedAt = 12:00` e `expiresAt = 12:10` (duração de
10 minutos, correta). A instância B tem o relógio **2 minutos adiantado** em relação à hora real.

| Hora real | Relógio que a instância B "vê" | B compara `expiresAt (12:10)` com o agora | Sessão para B |
| --------- | ------------------------------ | ----------------------------------------- | ------------- |
| 12:07     | 12:09                          | `12:09 < 12:10` → sim                      | **Aberta**    |
| 12:09     | 12:11                          | `12:11 > 12:10` → não                      | **Fechada**   |

No mesmo instante real (12:09), a instância A ainda considera a sessão aberta — o fim real é 12:10.
Resultado: um votante atendido por A tem o voto **aceito**; outro, atendido por B, tem o voto
**recusado** — mesmo estando dentro da janela real de votação.

Cada instância está "certa" dentro do próprio relógio. O problema é que a sessão passa a terminar
**em momentos distintos no mundo real**, dependendo de quem atende a requisição.

### Por que os relógios divergem

`expires_at` fica **no banco de dados**, mas pode ser gravado e julgado usando o relógio da
**máquina onde a aplicação roda** (`LocalDateTime.now()`). São dois relógios físicos diferentes:

- o relógio do host da **JVM** — usado por `LocalDateTime.now()`;
- o relógio do host do **banco de dados** — usado por `CURRENT_TIMESTAMP`.

Relógios físicos "derivam" (osciladores imperfeitos) e divergem principalmente quando o serviço de
sincronização de hora (NTP) não está configurado ou quando máquinas virtuais são migradas/pausadas.
Divergências de **milissegundos a segundos** são comuns.

| Relógio da aplicação frente ao banco | Efeito observado                                     |
| ------------------------------------ | ---------------------------------------------------- |
| Adiantado                            | Sessão "fecha" cedo — votos legítimos recusados.      |
| Atrasado                             | Sessão "fica aberta" além da expiração — votos aceitos após o fim. |

### O papel da duração

A **duração** nunca é o problema: ela é estática e sempre correta. O que varia é o **referencial**:
`expiresAt` só tem significado no quadro de relógio de quem o emitiu; um relógio desalinhado, ao
comparar o próprio "agora" com esse número, desloca a borda pelo offset entre os dois quadros — ou
seja, muda **em que momento real a duração termina** para cada relógio.

Isso independe do número de instâncias: mesmo com uma única aplicação, o relógio dela pode divergir
do banco; com múltiplas instâncias, a divergência ocorre também **entre** os pods.

## 2. Quando isso acontece de verdade?

O cenário só se manifesta quando **aplicação e banco rodam em hosts (máquinas) diferentes**:

| Ambiente                | Relógio da JVM vs relógio do banco                    | Ocorre?                           |
| ----------------------- | ----------------------------------------------------- | --------------------------------- |
| Desenvolvimento (H2)    | H2 embarcado no mesmo processo da JVM — **único relógio**. | Nunca.                        |
| Docker em um host       | App e Postgres no mesmo host — **mesmo relógio do kernel**. | Não.                        |
| Produção / cloud        | App e banco em nós distintos, cada um com seu relógio. | **Sim** — drift de ms a segundos. |

Na prática, para uma votação de cooperativa, o **impacto** da divergência é mínimo: uma janela de
poucos milissegundos a segundos na fronteira da expiração, imperceptível para votantes humanos e
sem peso financeiro/legal. Mesmo que um voto-limite entre ou saia, a apuração usa a mesma regra
(ao apurar, a sessão já está fechada), portanto o resultado não fica corrompido — ele apenas
pode incluir ou excluir votos muito próximos do limite.

Trata-se, portanto, de uma **correção de consistência** (uma única fonte de decisão), não de uma
mudança de comportamento visível ao usuário.

## 3. A decisão

**Opção escolhida:** a regra "a sessão está aberta?" vive como **método puro do domínio**, e o
"agora" usado para avaliá-la vem **sempre do relógio do banco de dados**.

```java
// domínio — a regra de negócio (pura, testável sem banco)
public boolean isOpen(LocalDateTime now) {
    return expiresAt != null && now.isBefore(expiresAt);
}

// serviço — o "agora" vem do banco, não da JVM
if (!sessao.isOpen(sessaoRepository.now())) {
    throw new SessaoIsClosedException(sessaoId);
}
```

A regra permanece **única e visível no domínio** (não há regra de negócio dentro de queries SQL),
e a fonte de hora é **única** (o banco, compartilhada por todas as instâncias).

## 4. Alternativas consideradas

| Alternativa                                   | O que faz                                                  | Vantagem                                      | Custo / desvantagem                                                        | Veredito           |
| --------------------------------------------- | ---------------------------------------------------------- | --------------------------------------------- | -------------------------------------------------------------------------- | ------------------ |
| **Original (antes do PF9)** — regra no domínio, `LocalDateTime.now()` | `Sessao.isOpen(LocalDateTime.now())` | Simples, zero chamadas extras                 | Relógio do host da JVM; instâncias distintas podem decidir diferente.       | Descartada         |
| **PF9 (SQL)** — decisão no banco (`isOpenById`, `findOpenIds`) | borda decidida por queries no banco | Consenso total e atômico                      | Regra de negócio escondida em SQL, fora do domínio; difícil de testar sem banco. | Descartada         |
| **Clock bean** — `java.time.Clock` injetado   | regra no domínio, hora da JVM (bean) | Testável com relógio fixo                     | **É o relógio da JVM**: devolve a divergência entre pods/instâncias (o problema original). | Descartada         |
| **ClockRepository** dedicado                  | interface própria só para o `now()`                          | Semântica limpa (hora ≠ sessão)              | 3+ arquivos novos para um único `SELECT CURRENT_TIMESTAMP` da JVM — over-engineering. | Descartada         |
| **Escrita atômica no banco** (check + insert em uma única instrução) | elimina a janela check→insert | Elimina todo resíduo de corrida              | Complexidade alta (funcões/locks/transações especiais) para um risco desprezível. | Descartada         |
| **Escolhida** — regra no domínio + `SessaoRepository.now()` | `sessao.isOpen(sessaoRepository.now())` | Regra única e visível; hora única; custo mínimo | Uma consulta "extra" por operação (ver §6). Uma janela residual de ms entre ler a hora e gravar o voto (ver §7). | **Adotada**        |

**Por que não o Clock bean?** ele atacaria o *sintoma errado*: injetar um `Clock` melhora apenas a
testabilidade, mas continua sendo o relógio da JVM — ou seja, traz de volta exatamente a
divergência entre instâncias que motivou o PF9. A testabilidade já é obtida sem bean, pois os
testes de serviço congelam o tempo através do stub de `SessaoRepository.now()`.

## 5. Trade-offs assumidos

- **+ Clareza:** a regra de negócio mais importante da sessão está centralizada no domínio
  (`Sessao.isOpen`), visível para quem lê o código, e não dispersa em queries.
- **+ Consistência:** a fonte de hora é única (banco); todas as instâncias enxergam a mesma borda
  de expiração.
- **− Uma ida a mais ao banco** em cada operação de sessão (ler a hora). Custo desprezível
  (ver §6).
- **− Janela residual:** a decisão de aceitar o voto e a gravação do voto não são atômicas.
  Persiste uma janela de poucos milissegundos entre a leitura do "agora" e o `INSERT` do voto
  (ver Observação 8 em [traceabilidade.md](traceabilidade.md)).

## 6. Custo da consulta à hora (`SELECT CURRENT_TIMESTAMP`)

A consulta é deliberadamente barata:

- **Nenhuma tabela envolvida:** não varre linhas, não faz I/O em disco, não usa índices. É um
  escalar que o servidor de banco responde em **microssegundos**.
- **Sem rede extra em transação:** dentro de uma transação o `CURRENT_TIMESTAMP` é, na prática,
  o horário de início da própria transação — chamadas repetidas retornam o mesmo valor, sem custo
  adicional de calibração.
- **Um round-trip a mais por operação:** o custo dominante é a ida e volta na rede
  (≈ 50–200 µs em rede local; < 1 ms entre hosts), comparável à própria consulta da sessão
  (`findById`). Operações de sessão passam a custar, no máximo, **duas** consultas curtas
  (sessão + hora) em vez de uma — ordem de grandeza irrelevante frente a qualquer operação de
  escrita/leitura real.

Resumindo: o "peso" da query é **desprezível** — trata-se de trade "uma chamada trivial por
operação" por "uma única autoridade de hora".

## 7. Quando essa preocupação importa?

- **Sempre importou como modelo mental**, mas **só se manifesta** com aplicação e banco em hosts
  distintos.
- Em um sistema de votação de cooperativa (votantes humanos, janela de minutos, borda de
  expiração com folga), a divergência de relógio raramente muda algum resultado e nunca corrompe
  a apuração.
- A opção escolhida mantém o sistema simples hoje e elimina a classe inteira de "relógio certo no
  lugar errado" **sem custo perceptível** — por isso foi preferida à alternativa original.

## 8. Conclusão

Adotamos: **regra de negócio no domínio + relógio do banco como fonte do "agora"**. É a opção com
melhor relação simplicidade/consistência: pouco mais que uma chamada a mais por operação, em troca
de uma única fonte de decisão e de uma regra centralizada e testável — sem aceitar o risco (ainda
que pequeno) de relógios divergentes entre instâncias, e sem o over-engineering de um componente
de relógio dedicado.