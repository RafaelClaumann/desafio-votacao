# Pontos Fracos e Modos de Falha do Sistema

Este documento complementa à [Documentação de Negócio](DOCUMENTATION.md) listando os pontos
fracos, modos de falha e ambiguidades identificados no código-fonte atual. Para cada item são
descritos o **gatilho** (o que precisa acontecer), a **consequência** observada e o **impacto**.

Legenda de impacto: **Alta** (regra de negócio burlável ou falha comum com efeito grave) ·
**Média** (erro de API/robustez com efeito relevante) · **Baixa** (limitação/ambiguidade).

---

## PF1 — (RESOLVIDO) Duração de votação sem validação de positividade ("sessão com tempo negativo")

**Status: corrigido** — `PautaRequestDTO.tempoVotacaoMinutos` agora exige valor **maior que zero**
(`@Positive`, junto do `@NotNull`).

**Comportamento anterior**

Uma pauta era criada com `tempo_votacao_minutos` igual a `0` ou negativo, pois a entrada tinha
apenas `@NotNull` — não existia `@Min`/`@Positive`. As consequências eram:

- A sessão era criada com `expiração = agora + duração`. Com `0`, a expiração é igual ao início;
  com valor negativo, é anterior ao início.
- Como a sessão só está aberta enquanto `agora < expiração` (strictamente), uma sessão com
  duração 0 ou negativa **nascia fechada** e **nunca aceitava votos**.
- Consequência agravada: como a pauta só pode ter **uma única sessão** (regra R5) e não existe
  edição/cancelamento de pauta ou sessão, a pauta ficava **permanentemente inutilizável** —
  nenhum voto podia ser coletado e o "resultado" apurado era sempre EMPATE (0 × 0).

**Comportamento atual**

Valores `0` ou negativos são **recusados na criação da pauta** (HTTP 400) com a mensagem
"O tempo de votação deve ser maior que zero". Como toda duração persistida é ≥ 1 minuto, não
há como criar sessão que já nasça fechada por duração inválida.

**Exemplo (comportamento atual)**

```json
POST /pautas
{
  "titulo": "Reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": -5
}
→ 400 Bad Request — "O tempo de votação deve ser maior que zero"
```

**Impacto**: originalmente Alta — corrigido; sem impacto residual (validação na entrada).

**Evidência**: `PautaRequestDTO` (`@Positive tempoVotacaoMinutos`, `@NotNull`);
teste `PautaRequestDTOValidationTest`; `SessaoService.saveSessao()`
(`now.plusMinutes(dur)`), `Sessao.isOpen()` (`now.isBefore(expiresAt)`), R5.

---

## PF2 — Ausência de limite superior de duração (risco de overflow)

**Gatilho**

`tempo_votacao_minutos` recebe um valor extremamente grande (ex.: próximo de
`Long.MAX_VALUE`).

**O que acontece**

Sem `@Max`, o cálculo `now.plusMinutes(dur)` pode exceder a capacidade de representação
interna do `LocalDateTime`, resultando em erro de aritmética/`DateTimeException` → erro
genérico **HTTP 500** ao abrir a sessão. Não há mensagem de negócio. (O limite inferior —
`@Positive` — já existe; o problema aqui é a ausência de limite superior.)

**Impacto**: Média — cenário extremo, mas a API responde 500 sem orientação.

**Evidência**: tipo `Long` de `tempoVotacaoMinutos`, ausência de `@Max`, uso de
`plusMinutes(long)`.

---

## PF3 — Criação de sessão sem validação da entrada (pautaId nulo)

**Gatilho**

O cliente envia `POST /sessoes` com corpo `{}` (ou sem `pauta_id`). Diferente de `/pautas` e
`/votos`, o controller de sessões **não usa `@Valid`** e `SessaoDTO.pautaId` **não tem
constraints**.

**O que acontece**

`pautaId = null` chega a `SessaoService.saveSessao(null)` → `findById(null)`. O Spring Data
lança `IllegalArgumentException` ("id must not be null"), que não tem handler específico →
**HTTP 500** no lugar de um 400 de validação.

**Impacto**: Média — falha comum de uso mapeada como erro interno.

**Evidência**: `SessaoController.save()` (sem `@Valid`), `SessaoDTO`,
`PautaRepositoryAdapter.findById(null)`.

---

## PF4 — Abrir a 2ª sessão da mesma pauta resulta em HTTP 500

**Gatilho**

O usuário tenta abrir (por engano) uma sessão para uma pauta que já tem sessão — violação da
regra R5.

**O que acontece**

`SessaoService.saveSessao()` lança `IllegalArgumentException("Sessão already exists for this
Pauta")`. Essa exceção **não possui handler** no `GlobalExceptionHandler`, então cai no caso
genérico → **HTTP 500 "Erro interno do servidor"**, embora se trate de uma regra de negócio
esperada e previsível (deveria ser 409/400).

**Impacto**: Alta — erro comum de usuário (duplo clique / clique duplo em submit) respondido
como falha interna, dificultando diagnóstico.

**Evidência**: `SessaoService.saveSessao()` (linha `existsByPautaId`), `GlobalExceptionHandler`
(sem handler para `IllegalArgumentException`).

---

## PF5 — Concorrência ao criar sessão não é traduzida (salvaguarda parcial)

**Gatilho**

Duas requisições simultâneas tentam abrir sessão para a mesma pauta. A verificação
`existsByPautaId` é **check-then-act** sem trava: as duas podem passar pela checagem.

**O que acontece**

O índice único `uk_sessao_pauta` bloqueia a duplicidade no banco, **mas**
`SessaoRepositoryAdapter.save()` **não captura** `DataIntegrityViolationException` (diferente
dos adapters de Pauta e Voto) → a violação propaga como **HTTP 500**.

**Impacto**: Média — cenário de corrida, resposta 500 e falta de mensagem de conflito.

**Evidência**: `SessaoRepositoryAdapter.save()`, `schema.sql` (índice `uk_sessao_pauta`),
comparação com `PautaRepositoryAdapter`/`VotoRepositoryAdapter`.

---

## PF6 — Regra de título único aplicada de forma inconsistente entre aplicação e banco

**Gatilho**

Duas pautas com títulos que diferem **apenas em maiúsculas/minúsculas** (ex.: "Reforma X" e
"reforma x") são submetidas, em especial de forma concorrente.

**O que acontece**

- A aplicação verifica duplicidade **ignorando caixa** (`existsByTituloIgnoreCase`) → regra
  R2 sequencialmente bloqueia outercase.
- Porém a constraint do banco (`UNIQUE(titulo)`) é **sensível à caixa** (H2 padrão). Em uma
  corrida em que ambas passem da checagem em memória, o banco **não** bloqueia os dois títulos
  com variação de caixa → pautas "duplicadas" podem ser persistidas.

**Impacto**: Média — a salvaguarda do banco não reproduz a regra de negócio (divergência entre
camadas).

**Evidência**: `SpringDataPautaRepository.existsByTituloIgnoreCase()` vs
`schema.sql` `UNIQUE(titulo)`.

---

## PF7 — Unicidade do CPF declarada de forma divergente entre entidade e schema

**Gatilho**

Leitura/manutenção do código ou mudança de geração de DDL da entidade.

**O que acontece**

- `VotoEntity` declara `@Column(unique = true)` em `documento` (sugeriria **um único voto do CPF
  em todo o sistema**).
- O `schema.sql` aplica unicidade por par `(sessao_id, documento)` — e a regra de negócio
  (R10) é **um voto por CPF por sessão**.
- Com `ddl-auto=validate` + `schema.sql`, a regra efetiva é a do schema (por sessão). A
  anotação da entidade é contraditória e, se algum dia a DDL for gerada automaticamente,
  poderia impor uma regra mais restritiva do que a documentada.

**Impacto**: Baixa — ambiguidade que não altera o comportamento atual, mas é armadilha futura.

**Evidência**: `VotoEntity.documento` (`unique = true`), `schema.sql`
(`uk_voto_sessao_documento`), `VotoService.votar()` (check por sessão).

---

## PF8 — CPF sem normalização permite votar 2 vezes com o mesmo documento

**Gatilho**

Um associado vota na sessão usando o CPF com pontuação ("123.456.789-09") e depois com o CPF
somente números ("12345678909").

**O que acontece**

A validação `@CPF` aceita **ambos os formatos**, e o sistema **não normaliza** o documento
antes de comparar/armazenar. Como a unicidade (R10) compara a string literal, os dois registros
são considerados **documentos diferentes** → o mesmo titular consegue registrar **dois votos na
mesma sessão**, burlando a regra R10 pela formatação.

**Impacto**: **Alta** — regra de negócio contornável por um valor trivial e legal de CPF.

**Evidência**: `VotoDTO` (`@CPF`), `VotoService.votar()` (`existsBySessaoIdAndDocumento` com a
string bruta), ausência de normalização entre a entrada e o repositório.

---

## PF9 — Fechamento da sessão depende do relógio do servidor

**Gatilho**

Execução distribuída (mais de uma instância) ou relógio com desvio.

**O que acontece**

O estado aberta/fechada é **derivado** de `LocalDateTime.now()` (relógio local de quem
processa a requisição), sempre. Não há relógio centralizado nem job de fechamento: cada
instância/servidor pode "ver" uma borda de expiração diferente. Um relógio adiantado fecha a
sessão cedo; um atrasado deixa aceitando votos além da expiração.

**Impacto**: Média — em single-node o efeito é desprezível; em cluster, pode haver divergência
entre votação e apuração.

**Evidência**: `Sessao.isOpen(LocalDateTime.now())`, `SessaoMapper.toDTO` (`LocalDateTime.now()`
na resposta), ausência de UUID/timestamp distribuído.

---

## PF10 — Sessão fechada sem nenhum voto resulta em EMPATE silencioso

**Gatilho**

Uma sessão chega ao fim sem nenhum voto registrado (ex.: ninguém participou).

**O que acontece**

O resultado reporta `votos_sim = 0`, `votos_nao = 0`, `total = 0` e status **EMPATE** — a mesma
classificação de uma disputa empatada de verdade. Não há distinção entre "empate real" e
"votação sem nenhuma participação" (nem quórum mínimo).

**Impacto**: Baixa — ambiguidade de negócio; o resultado pode ser lido como "empate válido".

**Evidência**: `ResultadoVotacao.status()` (SIM == NÃO → EMPATE, incl. 0 × 0).

---

## PF11 — Indicador "pauta já possui sessão" calculado mas não exposto

**Gatilho**

Consumidor da API quer saber se uma pauta já tem sessão sem consultar a lista de sessões.

**O que acontece**

`PautaService.pautaComStatuses()` calcula `hasSessao` para cada pauta, mas **nenhum controller
o utiliza** — `GET /pautas` retorna apenas id/título/duração. A informação precisa ser
inferida cruzando `GET /pautas` com `GET /sessoes`.

**Impacto**: Baixa — limitação de consumo da API.

**Evidência**: `PautaService.pautaComStatuses()` (sem chamadas), `PautaController.fetch()`.

---

## PF12 — Autenticidade do associado não é verificada (CPF autodeclarado)

**Gatilho**

Qualquer pessoa com um CPF bem-formado (inclusive o próprio CPF alheio) vota.

**O que acontece**

O sistema valida apenas o **formato** do CPF (`@CPF`). Não há integração com cadastro de
associados nem confirmação de titularidade — o voto é registrado com base no documento
autodeclarado.

**Impacto**: Baixa — limitação de escopo da aplicação, não um erro de implementação; deve ser
lida como regra implícita: "o CPF identifica o eleitor por autodeclaração".

**Evidência**: não existe repositório/endpoint de associados; `VotoDTO` usa somente `@CPF`.

---

## PF13 — Erros 500 genéricos e mensagens heterogêneas

**Gatilho**

Qualquer exceção não mapeada (PF2, PF3, PF4, PF5) ou falha interna.

**O que acontece**

O `GlobalExceptionHandler` responde "Erro interno do servidor" e **não expõe a causa** para
cenários de negócio. As mensagens de exceção também são heterogêneas (português para pauta
duplicada, inglês para os demais), dificultando o tratamento uniforme no cliente.

**Impacto**: Baixa — questões de ergonomia/diagnóstico da API.

**Evidência**: `GlobalExceptionHandler.handleGeneric()`, mensagens de
`DuplicatedPautaException` (PT) vs demais exceções (EN).

---

## PF14 — Testes fora de sincronia com a implementação

**Gatilho**

Manutenção/evolução do comportamento de negócio.

**O que acontece**

`SessaoServiceTest.saveSessao_shouldThrowWhenPautaDoesNotExist` espera
`IllegalArgumentException("Pauta not found")`, mas a implementação atual lança
`PautaNotFoundException`. O teste não reflete o comportamento atual e não cobre os modos de
falha PF1/PF4/PF8.

**Impacto**: Baixa — risco de falsa sensação de cobertura.

**Evidência**: `src/test/java/.../SessaoServiceTest.java` vs `SessaoService.saveSessao()` /
`PautaService.getPautaById()`.

---

## Resumo

| # | Ponto fraco / modo de falha                 | Gatilho                          | Consequência observada                     | Impacto |
| - | ------------------------------------------- | -------------------------------- | ------------------------------------------ | ------- |
| 1 | Duração 0/negativa aceita (PF1)             | `tempo_votacao_minutos` ≤ 0      | **Corrigido** — rejeitado na criação (400) | Corrigida |
| 2 | Sem limite superior de duração              | duração muito grande             | Overflow → HTTP 500                        | Média   |
| 3 | `POST /sessoes` sem validação               | `pauta_id` nulo/ausente          | HTTP 500 em vez de 400                     | Média   |
| 4 | 2ª sessão da pauta                          | violação R5                      | HTTP 500 em vez de 409/400                 | Alta    |
| 5 | Corrida na criação de sessão                | duas requisições simultâneas     | HTTP 500 (integridade não traduzida)       | Média   |
| 6 | Divergência case-sensitive de título        | corrida com caixa variada        | Duplicidade pode escapar da constraint      | Média   |
| 7 | Unicidade de CPF divergente (entidade/schema) | leitura/geração de DDL          | Regra ambígua; restrição futura indevida   | Baixa   |
| 8 | CPF sem normalização                        | mesma pessoa, formatos diferentes | Voto duplicado do mesmo titular (burlou R10) | Alta    |
| 9 | Fechamento depende do relógio local         | cluster/desvio de clock          | Fronteiras de exclusão divergentes         | Média   |
| 10 | Sessão sem votos → EMPATE                   | zero participação                | Empate silencioso                          | Baixa   |
| 11 | `hasSessao` não exposto                     | consulta de pautas               | Necessário cruzamento com `/sessoes`       | Baixa   |
| 12 | CPF autodeclarado (sem verificação)         | votação                          | Sem confirmação de titularidade            | Baixa   |
| 13 | Erros 500 genéricos / mensagens mistas      | exceções não mapeadas            | Diagnóstico dificultado                    | Baixa   |
| 14 | Testes desatualizados                       | evolução de código               | Cobertura incorreta                        | Baixa   |

**Recomendação de prioridade:** tratar PF8 (burlável) e PF3/PF4 (falhas comuns com impacto
de negócio e mapeamento de erro incorreto) antes dos demais itens. PF1 está corrigido.