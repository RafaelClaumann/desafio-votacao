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

## PF2 — (RESOLVIDO) Ausência de limite superior de duração (risco de overflow)

**Status: corrigido** — `PautaRequestDTO.tempoVotacaoMinutos` agora exige valor **no máximo
43200 minutos (30 dias)** (`@Max`, junto do `@Positive` e `@NotNull`).

**Comportamento anterior**

`tempo_votacao_minutos` recebia um valor extremamente grande (ex.: próximo de
`Long.MAX_VALUE`), pois a entrada tinha apenas `@Positive`. Sem `@Max`, o cálculo
`now.plusMinutes(dur)` excedia a capacidade de representação interna do `LocalDateTime`,
resultando em erro de aritmética/`DateTimeException` → erro genérico **HTTP 500** ao abrir a
sessão, sem mensagem de negócio.

**Comportamento atual**

Valores acima de **43200 minutos (30 dias)** são **recusados na criação da pauta** (HTTP 400)
com a mensagem "O tempo de votação não pode exceder 43200 minutos (30 dias)". Como toda duração
persistida é ≤ 43200, o cálculo `now.plusMinutes(dur)` nunca excede o intervalo representável
do `LocalDateTime` (30 dias é uma fração minúscula do limite suportado).

**Exemplo (comportamento atual)**

```json
POST /pautas
{
  "titulo": "Reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": 43201
}
→ 400 Bad Request — "O tempo de votação não pode exceder 43200 minutos (30 dias)"
```

**Impacto**: originalmente Média — corrigido; sem impacto residual (validação na entrada).

**Evidência**: `PautaRequestDTO` (`@Max tempoVotacaoMinutos`, com `@Positive` e `@NotNull`);
teste `PautaControllerTest.save_shouldReject_whenDurationExceedsUpperLimit`;
`SessaoService.saveSessao()` (`now.plusMinutes(dur)`).

---

## PF3 — (RESOLVIDO) Criação de sessão sem validação da entrada (pautaId nulo)

**Status: corrigido** — `SessaoDTO.pautaId` agora exige valor **presente** (`@NotNull`) e
`SessaoController.save()` valida o corpo com `@Valid`.

**Comportamento anterior**

`POST /sessoes` com corpo `{}` (ou sem `pauta_id`) fazia `pautaId = null` chegar a
`SessaoService.saveSessao(null)` → `findById(null)`. O Spring Data lançava
`IllegalArgumentException` ("id must not be null"), que não possui handler específico →
**HTTP 500** no lugar de um 400 de validação. Diferente de `/pautas` e `/votos`, o controller
de sessões não usava `@Valid` e `SessaoDTO.pautaId` não tinha constraints.

**Comportamento atual**

`pauta_id` ausente ou nulo é **recusado na entrada** (HTTP 400) pela Bean Validation, com a
mensagem "O id da Pauta é obrigatório" — o `GlobalExceptionHandler` devolve o erro de campo
`pautaId`. Nenhuma chamada chega ao serviço/repositório.

**Exemplo (comportamento atual)**

```json
POST /sessoes
{}
→ 400 Bad Request — "Erro de validação"
   [
     { "field": "pautaId", "message": "O id da Pauta é obrigatório" }
   ]
```

**Impacto**: originalmente Média — corrigido; sem impacto residual (validação na entrada,
mesmo padrão de `/pautas` e `/votos`).

**Evidência**: `SessaoDTO` (`@NotNull pautaId`), `SessaoController.save()` (`@Valid`);
teste `SessaoControllerTest.save_shouldReject_whenPautaIdIsMissing`.

---

## PF4 — (RESOLVIDO) Abrir a 2ª sessão da mesma pauta resulta em HTTP 500

**Status: corrigido** — `SessaoService.saveSessao()` agora lança `DuplicatedSessaoException`,
mapeada para **HTTP 409** pelo `GlobalExceptionHandler` (mesmo padrão de
`DuplicatedPautaException`/`DuplicatedVoteException`).

**Comportamento anterior**

`SessaoService.saveSessao()` lançava `IllegalArgumentException("Sessão already exists for this
Pauta")`. Essa exceção **não possuía handler** no `GlobalExceptionHandler`, então caía no caso
genérico → **HTTP 500 "Erro interno do servidor"**, embora se tratasse de uma regra de negócio
esperada e previsível (deveria ser 409/400).

**Comportamento atual**

Abrir uma sessão para uma pauta que **já possui sessão** (violação R5) é recusado com
**HTTP 409 Conflict** e mensagem "Já existe uma sessão para a pauta: N". Nenhuma sessão é
criada. A violação é capturada na verificação do serviço (sequencial) e também pelo banco
(índice único), pois `SessaoRepositoryAdapter.save()` traduz `DataIntegrityViolationException`.

**Exemplo (comportamento atual)**

```json
POST /sessoes
{
  "pauta_id": 1
}
→ 409 Conflict — "Já existe uma sessão para a pauta: 1"
```

**Impacto**: originalmente Alta — corrigido; sem impacto residual (erro de negócio mapeado).

**Evidência**: `DuplicatedSessaoException`, `SessaoService.saveSessao()`
(`existsByPautaId`), `GlobalExceptionHandler.handleDuplicatedSessao()` (409);
testes `SessaoServiceTest.saveSessao_shouldThrowWhenSessionAlreadyExistsForPauta` e
`SessaoControllerTest.save_shouldReject_whenPautaAlreadyHasSession`.

---

## PF5 — (RESOLVIDO) Concorrência ao criar sessão não é traduzida (salvaguarda parcial)

**Status: corrigido** — `SessaoRepositoryAdapter.save()` agora captura
`DataIntegrityViolationException` (mesmo padrão de `PautaRepositoryAdapter` e
`VotoRepositoryAdapter`) e a converte em `DuplicatedSessaoException` → **HTTP 409**.

**Comportamento anterior**

Duas requisições simultâneas tentavam abrir sessão para a mesma pauta. A verificação
`existsByPautaId` é **check-then-act** sem trava: as duas podiam passar pela checagem.
O índice único `uk_sessao_pauta` bloqueava a duplicidade no banco, **mas**
`SessaoRepositoryAdapter.save()` **não capturava** `DataIntegrityViolationException` (diferente
dos adapters de Pauta e Voto) → a violação propagava como **HTTP 500**.

**Comportamento atual**

Em corrida, a violação do índice único é traduzida para `DuplicatedSessaoException` →
**HTTP 409**, com a mesma mensagem do caso sequencial. O cliente recebe um erro de conflito
previsível em ambos os cenários.

**Impacto**: originalmente Média — corrigido; a integridade passa a ser traduzida.

**Evidência**: `SessaoRepositoryAdapter.save()` (captura de `DataIntegrityViolationException`),
`schema.sql` (índice `uk_sessao_pauta`), comparação com `PautaRepositoryAdapter`/`VotoRepositoryAdapter`.

---

## PF6 — (RESOLVIDO) Regra de título único aplicada de forma inconsistente entre aplicação e banco

**Status: corrigido** — a salvaguarda do banco agora é **insensível à caixa**, igual à
verificação da aplicação. O `schema.sql` trocou a constraint case-sensitive
`UNIQUE(titulo)` por um **índice único funcional `uk_pauta_titulo_lower` em
`LOWER(titulo)`**.

**Comportamento anterior**

- A aplicação verificava duplicidade **ignorando caixa** (`existsByTituloIgnoreCase`) → regra
  R2 sequencialmente bloqueia outercase.
- A constraint do banco (`UNIQUE(titulo)`) era **sensível à caixa** (H2 padrão). Em uma
  corrida em que ambas passassem da checagem em memória, o banco **não** bloqueava os dois
  títulos com variação de caixa → pautas "duplicadas" podiam ser persistidas.

**Comportamento atual**

O banco aplica a mesma unicidade da aplicação: dois títulos que diferem apenas pela caixa
violam o índice `LOWER(titulo)` → `PautaRepositoryAdapter.save()` converte para
`DuplicatedPautaException` → **HTTP 409**, mesmo em corrida.

**Exemplo (comportamento atual)**

```json
POST /pautas   → 201 "Reforma estatutária do capítulo quatro"
POST /pautas
{
  "titulo": "reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": 10
}
→ 409 Conflict — "Já existe uma pauta com o título: reforma estatutária do capítulo quatro"
```

**Impacto**: originalmente Média — corrigido; aplicação e banco aplicam a mesma regra.

**Evidência**: `schema.sql` (índice `uk_pauta_titulo_lower` em `LOWER(titulo)`),
`SpringDataPautaRepository.existsByTituloIgnoreCase()`;
teste `PautaRepositoryAdapterITTest.save_shouldReject_whenTitleDiffersOnlyByCase`.

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

Qualquer exceção não mapeada ou falha interna.

**O que acontece**

O `GlobalExceptionHandler` responde "Erro interno do servidor" e **não expõe a causa** para
cenários de negócio. As mensagens de exceção também são heterogêneas (português para pauta
duplicada, inglês para os demais), dificultando o tratamento uniforme no cliente.

**Impacto**: Baixa — questões de ergonomia/diagnóstico da API.

**Evidência**: `GlobalExceptionHandler.handleGeneric()`, mensagens de
`DuplicatedPautaException` (PT) vs demais exceções (EN).

---

## PF14 — (RESOLVIDO) Testes fora de sincronia com a implementação

**Status: corrigido** — `SessaoServiceTest` foi reativado e alinhado à implementação atual:
`saveSessao_shouldThrowWhenPautaDoesNotExist` agora espera `PautaNotFoundException`, e foi
adicionada cobertura para a violação R5 (`DuplicatedSessaoException`) e para a criação bem-sucedida.

**Comportamento anterior**

`SessaoServiceTest` estava **inteiramente comentado** e, quando ativo, esperava
`IllegalArgumentException("Pauta not found")`, mas a implementação atual lança
`PautaNotFoundException`. O teste não refletia o comportamento atual e não cobria os modos de
falha PF1/PF4/PF8.

**Impacto**: originalmente Baixa — corrigido; os testes de `SessaoService` acompanham a
implementação.

**Evidência**: `src/test/java/.../SessaoServiceTest.java` (reativado) vs
`SessaoService.saveSessao()` / `PautaService.getPautaById()`. "Sessão não encontrada" e "sessão
fechada" possuem cobertura (`getOpenSessaoById*`).

---

## Resumo

| # | Ponto fraco / modo de falha                 | Gatilho                          | Consequência observada                     | Impacto |
| - | ------------------------------------------- | -------------------------------- | ------------------------------------------ | ------- |
| 1 | Duração 0/negativa aceita (PF1)             | `tempo_votacao_minutos` ≤ 0      | **Corrigido** — rejeitado na criação (400) | Corrigida |
| 2 | Sem limite superior de duração              | duração muito grande             | **Corrigido** — rejeitado na criação (400) | Corrigida |
| 3 | `POST /sessoes` sem validação               | `pauta_id` nulo/ausente          | **Corrigido** — rejeitado na entrada (400) | Corrigida |
| 4 | 2ª sessão da pauta                          | violação R5                      | **Corrigido** — rejeitado com 409 Conflict | Corrigida |
| 5 | Corrida na criação de sessão                | duas requisições simultâneas     | **Corrigido** — integridade traduzida (409) | Corrigida |
| 6 | Divergência case-sensitive de título        | corrida com caixa variada        | **Corrigido** — índice único `LOWER(titulo)` | Corrigida |
| 7 | Unicidade de CPF divergente (entidade/schema) | leitura/geração de DDL          | Regra ambígua; restrição futura indevida   | Baixa   |
| 8 | CPF sem normalização                        | mesma pessoa, formatos diferentes | Voto duplicado do mesmo titular (burlou R10) | Alta    |
| 9 | Fechamento depende do relógio local         | cluster/desvio de clock          | Fronteiras de exclusão divergentes         | Média   |
| 10 | Sessão sem votos → EMPATE                   | zero participação                | Empate silencioso                          | Baixa   |
| 11 | `hasSessao` não exposto                     | consulta de pautas               | Necessário cruzamento com `/sessoes`       | Baixa   |
| 12 | CPF autodeclarado (sem verificação)         | votação                          | Sem confirmação de titularidade            | Baixa   |
| 13 | Erros 500 genéricos / mensagens mistas      | exceções não mapeadas            | Diagnóstico dificultado                    | Baixa   |
| 14 | Testes desatualizados                       | evolução de código               | **Corrigido** — `SessaoServiceTest` alinhado    | Corrigida |

**Recomendação de prioridade:** tratar PF8 (burlável) antes dos demais itens. PF1, PF2, PF3,
PF4, PF5, PF6 e PF14 estão corrigidos.