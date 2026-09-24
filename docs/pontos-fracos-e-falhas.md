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
(`now.plusMinutes(dur)`, com `now` vindo do relógio do banco), abertura condicionada a
`expires_at > CURRENT_TIMESTAMP` (R7/R8), R5.

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

## PF3 — (RESOLVIDO) Criação de sessão sem validação da entrada (idPauta nulo)

**Status: corrigido** — `SessaoDTO.idPauta` agora exige valor **presente** (`@NotNull`) e
`SessaoController.save()` valida o corpo com `@Valid`.

**Comportamento anterior**

`POST /sessoes` com corpo `{}` (ou sem `id_pauta`) fazia `idPauta = null` chegar a
`SessaoService.saveSessao(null)` → `findById(null)`. O Spring Data lançava
`IllegalArgumentException` ("id must not be null"), que não possui handler específico →
**HTTP 500** no lugar de um 400 de validação. Diferente de `/pautas` e `/votos`, o controller
de sessões não usava `@Valid` e `SessaoDTO.idPauta` não tinha constraints.

**Comportamento atual**

`id_pauta` ausente ou nulo é **recusado na entrada** (HTTP 400) pela Bean Validation, com a
mensagem "O id da Pauta é obrigatório" — o `GlobalExceptionHandler` devolve o erro de campo
`idPauta`. Nenhuma chamada chega ao serviço/repositório.

**Exemplo (comportamento atual)**

```json
POST /sessoes
{}
→ 400 Bad Request — "Erro de validação"
   [
     { "field": "idPauta", "message": "O id da Pauta é obrigatório" }
   ]
```

**Impacto**: originalmente Média — corrigido; sem impacto residual (validação na entrada,
mesmo padrão de `/pautas` e `/votos`).

**Evidência**: `SessaoDTO` (`@NotNull idPauta`), `SessaoController.save()` (`@Valid`);
teste `SessaoControllerTest.save_shouldReject_whenIdPautaIsMissing`.

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
  "id_pauta": 1
}
→ 409 Conflict — "Já existe uma sessão para a pauta: 1"
```

**Impacto**: originalmente Alta — corrigido; sem impacto residual (erro de negócio mapeado).

**Evidência**: `DuplicatedSessaoException`, `SessaoService.saveSessao()`
(`existsByIdPauta`), `GlobalExceptionHandler.handleDuplicatedSessao()` (409);
testes `SessaoServiceTest.saveSessao_shouldThrowWhenSessionAlreadyExistsForPauta` e
`SessaoControllerTest.save_shouldReject_whenPautaAlreadyHasSession`.

---

## PF5 — (RESOLVIDO) Concorrência ao criar sessão não é traduzida (salvaguarda parcial)

**Status: corrigido** — `SessaoRepositoryAdapter.save()` agora captura
`DataIntegrityViolationException` (mesmo padrão de `PautaRepositoryAdapter` e
`VotoRepositoryAdapter`) e a converte em `DuplicatedSessaoException` → **HTTP 409**.

**Comportamento anterior**

Duas requisições simultâneas tentavam abrir sessão para a mesma pauta. A verificação
`existsByIdPauta` é **check-then-act** sem trava: as duas podiam passar pela checagem.
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

## PF7 — (RESOLVIDO) Unicidade do CPF declarada de forma divergente entre entidade e schema

**Status: corrigido** — `VotoEntity` não declara mais unicidade global do documento; a
entidade passou a declarar a **constraint composta correta** `uk_voto_sessao_documento`
(`(sessao_id, documento)`), com o mesmo nome do schema.

**Comportamento anterior**

- `VotoEntity` declara `@Column(unique = true)` em `documento` (sugeriria **um único voto do CPF
  em todo o sistema**).
- O `schema.sql` aplica unicidade por par `(sessao_id, documento)` — e a regra de negócio
  (R10) é **um voto por CPF por sessão**.
- Com `ddl-auto=validate` + `schema.sql`, a regra efetiva era a do schema (por sessão). A
  anotação da entidade era contraditória e, se a DDL fosse gerada automaticamente, poderia
  impor uma regra mais restritiva do que a documentada.

**Comportamento atual**

A entidade é consistente com o schema e com a regra R10: o mesmo CPF pode votar em **diferentes
sessões**, mas apenas **uma vez por sessão**. A unidade da regra é o par `(sessao_id, documento)`
(`@Table` com `@UniqueConstraint uk_voto_sessao_documento`), alinhado ao `schema.sql`/`init.sql`.

**Impacto**: originalmente Baixa — corrigido; sem ambiguidade residual.

**Evidência**: `VotoEntity` (`@Table(uniqueConstraints = @UniqueConstraint(name =
"uk_voto_sessao_documento", columnNames = {sessao_id, documento}))`, `@Column(length = 14)` sem
`unique`), `schema.sql` (`uk_voto_sessao_documento`), `VotoService.votar()` (check por sessão);
teste `VotoRepositoryAdapterITTest.save_shouldAllowSameDocumentoInDifferentSessions` e
`save_shouldReject_whenDocumentoAlreadyVotedInSession`.

---

## PF8 — (RESOLVIDO) CPF sem normalização permite votar 2 vezes com o mesmo documento

**Status: corrigido** — `VotoService.votar()` **normaliza** o documento para somente dígitos
antes da verificação de duplicidade (R10) e da persistência. Com isso, "123.456.789-09" e
"12345678909" são tratados como **o mesmo documento**: o segundo voto na mesma sessão é recusado
(409), e a salvaguarda `uk_voto_sessao_documento` do banco passa a bloquear também a variação de
formatação.

**Gatilho**

Um associado vota na sessão usando o CPF com pontuação ("123.456.789-09") e depois com o CPF
somente números ("12345678909").

**O que acontecia antigamente**

A validação `@CPF` aceita **ambos os formatos**, e o sistema **não normalizava** o documento
antes de comparar/armazenar. Como a unicidade (R10) compara a string literal, os dois registros
eram considerados **documentos diferentes** → o mesmo titular conseguia registrar **dois votos na
mesma sessão**, burlando a regra R10 pela formatação.

**Impacto**: **Alta** — regra de negócio contornável por um valor trivial e legal de CPF.

**Evidência**: `VotoService.votar()` (`normalizarDocumento` antes de `existsByIdSessaoAndDocumento`
e da persistência); `VotoServiceTest` com cenários de normalização do documento e de recusa com
formatação distinta.

---

## PF9 — (RESOLVIDO) Fechamento da sessão depende do relógio do servidor

**Status: corrigido** — o **relógio do banco de dados** passou a ser a única autoridade de hora
do sistema, e a regra "a sessão está aberta?" voltou a viver **no domínio**. Não há mais
`LocalDateTime.now()` da JVM de quem atende a requisição.

**Comportamento anterior**

O estado aberta/fechada era **derivado** de `LocalDateTime.now()` (relógio local de quem
processa a requisição), sempre. Não havia relógio centralizado nem job de fechamento: cada
instância/servidor podia "ver" uma borda de expiração diferente. Um relógio adiantado fechava a
sessão cedo; um atrasado deixava aceitando votos além da expiração. O mesmo valia para o campo
`is_open` da resposta (`SessaoMapper.toDTO` também usava `LocalDateTime.now()`).

**Comportamento atual**

O "agora" do sistema é o `CURRENT_TIMESTAMP` do banco (único, compartilhado por todas as
instâncias). A regra de negócio "está aberta?" é um **método puro do domínio** —
`Sessao.isOpen(now)` (`now.isBefore(expiresAt)`) — e o `now` vem sempre do banco
(`SessaoRepository.now()`). Quem vota/apura consulta a sessão e avalia a regra com esse relógio
(`SessaoService.getOpenSessaoById`/`getClosedSessaoById`); quem lista sessões calcula o `is_open`
com um único `now()`. Todas as instâncias veem a mesma borda de expiração, sem espalhar a regra
em queries SQL.

**Impacto**: originalmente Média — corrigido; a borda de fechamento é única (relógio centralizado
no banco) e a regra fica central e visível no domínio. Em single-node o comportamento observado
não muda.

**Evidência**: `Sessao.isOpen(LocalDateTime)` (regra no domínio), `SpringDataSessaoRepository.now`
(`SELECT CURRENT_TIMESTAMP`), `SessaoRepositoryAdapter` (delegação), `SessaoService`
(`getOpenSessaoById`/`getClosedSessaoById`/`saveSessao`/`getSessoesComStatus`), `SessaoMapper`
(mapeia `SessaoComStatus`, sem `LocalDateTime.now()`); testes `SessaoServiceTest`
(com `now()` fixo) e `SessaoRepositoryAdapterITTest` (`now` do banco).

---

## PF10 — (RESOLVIDO) Sessão fechada sem nenhum voto resulta em EMPATE silencioso

**Status: corrigido** — `ResultadoVotacao.status()` agora retorna **`SEM_VOTOS`** quando o total
de votos é zero, antes da comparação de maioria. `EMPATE` passou a significar apenas **empate
real** (ao menos um voto registrado e SIM == NÃO).

**Comportamento anterior**

O resultado reportava `votos_sim = 0`, `votos_nao = 0`, `total = 0` e status **EMPATE** — a
mesma classificação de uma disputa empatada de verdade. Não havia distinção entre "empate
real" e "votação sem nenhuma participação" (nem quórum mínimo).

**Comportamento atual**

Uma sessão fechada sem nenhum voto retorna status **`SEM_VOTOS`** (`SIM = 0`, `NÃO = 0`),
distinto do empate. A resposta mantém o mesmo formato (`votos_sim`, `votos_nao`, `total`,
`status`); somente o valor de `status` muda — sem breaking de contrato de campos.

**Exemplo (comportamento atual)**

```json
GET /sessoes/1/resultado
→ 200 OK
   {
     "id_sessao": 1,
     "votos_sim": 0,
     "votos_nao": 0,
     "total": 0,
     "status": "SEM_VOTOS"
   }
```

**Impacto**: originalmente Baixa — corrigido; zero participação deixou de ser indistinguível
do empate.

**Evidência**: `StatusVotacao.SEM_VOTOS`; `ResultadoVotacao.status()` (retorna `SEM_VOTOS`
quando `totalVotos() == 0`); teste `ResultadoVotacaoTest` (casos 0×0 → SEM_VOTOS, 0×0 → EMPATE
ausente, maioria e empate real).

---

## PF11 — (RESOLVIDO) Indicador "pauta já possui sessão" calculado mas não exposto

**Status: corrigido** — o código morto foi **removido**: `PautaService.pautaComStatuses()`,
o record `PautaComStatus` e a consulta `SessaoRepository.findPautaIdsComSessao()` não existem
mais.

**Comportamento anterior**

`PautaService.pautaComStatuses()` calculava `hasSessao` para cada pauta, mas **nenhum controller
o utilizava** — `GET /pautas` retornava apenas id/título/duração. A informação precisava ser
inferida cruzando `GET /pautas` com `GET /sessoes`.

**Comportamento atual**

O indicador deixou de existir (era código sem chamadas, remanescente de uma feature nunca
conectada à API). `GET /pautas` permanece retornando id/título/duração; quem precisar saber se
a pauta já possui sessão cruza com `GET /sessoes`.

**Impacto**: originalmente Baixa — corrigido; sem código morto residual.

---

## PF12 — Autenticidade do associado não é verificada (CPF autodeclarado)

**Gatilho**

Qualquer pessoa com um CPF bem-formado (inclusive o próprio CPF alheio) vota.

**O que acontece**

O sistema valida o **formato** do CPF (`@CPF`) e também submete o documento a um **validador
externo** (`DocumentoValidator` → `DocumentoValidatorClient`). Essa integração, porém, é
**fictícia**: usa `https://httpbin.org`, que responde 200/400/404 de forma **aleatória**, sem
consultar nenhuma base real de associados. A "decisão" de validar ou rejeitar o CPF não tem
relação com a titularidade — na prática o voto continua sendo registrado com base no documento
autodeclarado, apenas com uma etapa aleatória adicional.

**Impacto**: Baixa — limitação de escopo da aplicação, não um erro de implementação; deve ser
lida como regra implícita: "o CPF identifica o eleitor por autodeclaração". A validação externa
é apenas uma **simulação** da integração e não deve ser tratada como confirmação de titularidade.

**Evidência**: `DocumentoValidatorClient` chama `https://httpbin.org/status/200,400,404,500` e
`/anything` (resultado aleatório); não existe repositório/endpoint de associados; `VotoDTO` usa
`@CPF`.

---

## PF13 — (RESOLVIDO) Erros 500 genéricos e mensagens heterogêneas

**Status: corrigido** — mensagens padronizadas em **PT-BR**, exceções HTTP comuns mapeadas para
4xx, mensagem estável para corpo ilegível e exposição do **`correlation_id`** no corpo de erro.

**O que acontecia antigamente**

O `GlobalExceptionHandler` mapeava as exceções de negócio e a `HttpIntegrationException` (503),
mas: (i) erros não previstos respondiam "Erro interno do servidor" sem expor o `correlationId` —
o consumidor não tinha como referenciar o erro num chamado (é intencional **não** vazar a causa
interna, mas o id deveria acompanhar o corpo); (ii) as **mensagens eram heterogêneas** (português
para pauta duplicada/documento inválido, inglês para as demais); e (iii) exceções comuns do
framework — rota inexistente, método não suportado, tipo inválido em `@PathVariable`, corpo
ilegível — caíam no caso genérico ou vinham com mensagens em inglês.

**Comportamento atual**

- **Mensagens padronizadas em PT-BR** em todas as exceções de negócio (`PautaNotFoundException`,
  `SessaoNotFoundException`, `SessaoIsClosedException`, `SessaoIsOpenException`,
  `DuplicatedVoteException`) e na `HttpIntegrationException` (cujo construtor agora recebe apenas
  o `HttpStatusCode`, sem propagar `getMessage()` de baixo nível).
- **Exceções HTTP comuns mapeadas**: rota inexistente → **404** ("Recurso não encontrado");
  método não suportado → **405** ("Método HTTP não suportado para este recurso"); tipo inválido em
  caminho → **400** ("Parâmetro de caminho com tipo inválido"); corpo ilegível → **400** com
  mensagem **fixa** ("Corpo da requisição malformado ou em formato inválido"), qualquer que seja o
  problema de parsing.
- **`correlation_id` no corpo de erro e header de resposta**: o `ApiError` expõe o id vindo do MDC
  (header `X-Correlation-Id` ou UUID gerado pelo `MDCRequestFilter`) no campo `correlation_id`, e o
  filtro **ecoa o `X-Correlation-Id` na resposta** — o consumidor correlaciona qualquer resposta
  sem ler log. O `GlobalExceptionHandler` apenas lê `MDC.get("correlationId")` ao montar o corpo;
  no `finally`, o `MDCRequestFilter` **remove as chaves que ele mesmo colocou** (`MDC.remove(...)`)
  — não mais `MDC.clear()` — para não varrer o MDC da thread. O `logback-spring.xml` **não mudou**.
- **Erros não previstos continuam genéricos** (HTTP 500, mensagem fixa "Erro interno do
  servidor", sem vazar stack/detalhe interno); o detalhe completo fica no log correlacionado sob o
  mesmo `correlation_id`.

**Exemplo (comportamento atual)**

```json
POST /votos
{ "id_sessao": 1, "documento": "123.456.789-09", "escolha_voto": "SIM" }

→ 503 Service Unavailable
   {
     "timestamp": "2026-09-24T16:49:00.000Z",
     "status": 503,
     "error": "Service Unavailable",
     "message": "Falha na integração externa de validação de documento: 503",
     "path": "/votos",
     "correlation_id": "a3f9c2d1-…",
     "field_errors": []
   }
```

**Impacto**: originalmente Baixa — corrigido; mensagens uniformes em PT-BR, 404/405/400 mapeados,
`correlation_id` exposto no corpo de erro e `X-Correlation-Id` ecoado na resposta.

**Evidência**: `GlobalExceptionHandler` (handlers de `NoResourceFoundException`,
`HttpRequestMethodNotSupportedException`, `MethodArgumentTypeMismatchException`; `handleUnreadable`
com mensagem estável; `buildResponse` lê `MDC.get(MDC_CORRELATION_ID_KEY)`); `ApiError` (campo
`String correlationId` → `correlation_id`); `MDCRequestFilter` (`@Order(HIGHEST_PRECEDENCE)`,
constantes `CORRELATION_ID_HEADER`/`MDC_CORRELATION_ID_KEY`, ecoa o header, `MDC.remove(...)` no
`finally`, `shouldNotFilterAsyncDispatch()`); exceções de negócio com mensagens PT-BR;
`HttpIntegrationException` (construtor `HttpStatusCode`); `DocumentoValidatorClient` (lançamentos
sem `getMessage()`); testes `MDCRequestFilterTest` e `GlobalExceptionHandlerTest`. O `correlationId`
é **gerado manualmente** (MDC) — em branches futuros será migrado para o tracing do Spring Boot
(Micrometer), mantendo `X-Correlation-Id` como header compatível.

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
| 3 | `POST /sessoes` sem validação               | `id_pauta` nulo/ausente          | **Corrigido** — rejeitado na entrada (400) | Corrigida |
| 4 | 2ª sessão da pauta                          | violação R5                      | **Corrigido** — rejeitado com 409 Conflict | Corrigida |
| 5 | Corrida na criação de sessão                | duas requisições simultâneas     | **Corrigido** — integridade traduzida (409) | Corrigida |
| 6 | Divergência case-sensitive de título        | corrida com caixa variada        | **Corrigido** — índice único `LOWER(titulo)` | Corrigida |
| 7 | Unicidade de CPF divergente (entidade/schema) | leitura/geração de DDL          | **Corrigido** — entidade declara `uk_voto_sessao_documento` | Corrigida |
| 8 | CPF sem normalização                        | mesma pessoa, formatos diferentes | **Corrigido** — `VotoService` normaliza antes do check | Corrigida |
| 9 | Fechamento depende do relógio local         | cluster/desvio de clock          | **Corrigido** — relógio do banco (`CURRENT_TIMESTAMP`) | Corrigida |
| 10 | Sessão sem votos → EMPATE                   | zero participação                | **Corrigido** — status `SEM_VOTOS` quando total = 0 | Corrigida |
| 11 | `hasSessao` não exposto                     | consulta de pautas               | **Corrigido** — código removido; consome-se via `GET /sessoes` | Corrigida |
| 12 | CPF autodeclarado (sem verificação)         | votação                          | Validação externa **fictícia** (httpbin, aleatória 200/400/404/500) — sem confirmação de titularidade | Baixa   |
| 13 | Erros 500 genéricos / mensagens mistas      | exceções não mapeadas            | **Corrigido** — mensagens PT-BR; 404/405/400 mapeados; `correlation_id` exposto no corpo de erro | Corrigida |
| 14 | Testes desatualizados                       | evolução de código               | **Corrigido** — `SessaoServiceTest` alinhado    | Corrigida |

**Recomendação de prioridade:** PF1 a PF11, PF13 e PF14 estão corrigidos. PF12 recebeu uma
**validação externa fictícia** (aleatória, via httpbin) que não confirma titularidade — segue em
aberto se o objetivo for autenticidade real do associado. Com o PF13, a `HttpIntegrationException`
tem mapeamento próprio (503), as mensagens estão padronizadas em PT-BR, rotas inexistentes (404),
métodos não suportados (405) e parâmetros de caminho com tipo inválido (400) têm respostas
dedicadas, e o **`correlation_id`** é exposto no corpo de erro (vindo do MDC/`MDCRequestFilter`)
para o consumidor citá-lo em chamados.
