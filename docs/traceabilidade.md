# Rastreabilidade de Implementação

Mapa das regras de negócio para as classes e métodos que as implementam.

| Regra | Implementação |
| ----- | ------------- |
| R1 — Campos obrigatórios da pauta | `PautaRequestDTO` (`@NotBlank`, `@NotNull`); `PautaEntity` (colunas `NOT NULL`) |
| R2 — Título único da pauta | `PautaService.savePauta()` → `PautaRepository.existsByTituloIgnoreCase()`; `PautaRepositoryAdapter.save()` captura `DataIntegrityViolationException` → `DuplicatedPautaException`; salvaguarda do banco `uk_pauta_titulo_lower`: em H2, coluna gerada `titulo_normalizado (LOWER(titulo))` no `schema.sql`; em PostgreSQL, índice funcional `LOWER(titulo)` em `.docker/postgres/init.sql` |
| R3 — Normalização e tamanho do título | `Pauta` (constructor faz `titulo.trim()`); `PautaRequestDTO` (`@Size(20–150)`); `titulo VARCHAR(150)` em `schema.sql` |
| R4 — Sessão exige pauta existente | `SessaoDTO` (`@NotNull pautaId`) + `SessaoController.save()` (`@Valid`); `SessaoService.saveSessao()` → `PautaService.getPautaById()` → `PautaNotFoundException` |
| R5 — Uma sessão por pauta | `SessaoService.saveSessao()` → `sessaoRepository.existsByPautaId()` → `DuplicatedSessaoException`; `SessaoRepositoryAdapter.save()` converte a violação do índice único `uk_sessao_pauta(sessoes.pauta_id)` em `schema.sql` |
| R6 — Duração definida pela pauta | `PautaRequestDTO` (`@Positive` + `@Max(43200)` em `tempoVotacaoMinutos`); `SessaoService.saveSessao()`: `expiresAt = sessaoRepository.now().plusMinutes(...)` (relógio do banco) |
| R7/R8 — Votos somente em sessão aberta | `VotoService.votar()` → `SessaoService.getOpenSessaoById()` → `Sessao.isOpen(now)` (`now.isBefore(expiresAt)`) e `SessaoIsClosedException`; `now` de `SessaoRepository.now()` |
| R9 — CPF válido | `VotoDTO` (anotação `@CPF`) + `VotoService.votar()` (após sessão aberta) → `DocumentoValidator.isValidDocumento()` (validação externa fictícia) → `InvalidDocumentoException` (400) / `HttpIntegrationException` (503) |
| R10 — Um voto por CPF por sessão | `VotoService.votar()` → `VotoRepository.existsBySessaoIdAndDocumento()`; `VotoRepositoryAdapter.save()` → `DuplicatedVoteException`; constraint `uk_voto_sessao_documento(sessao_id, documento)` em `schema.sql` |
| R11 — Voto apenas SIM/NÃO | Enum `Voto.Escolha { SIM, NAO }`; `HttpMessageNotReadableException` para valores inválidos |
| R12 — Resultado só para sessão fechada | `VotoService.apurarVotosSessao()` → `SessaoService.getClosedSessaoById()` → `Sessao.isOpen(now)` com `now` de `SessaoRepository.now()` → `SessaoIsOpenException` / `SessaoNotFoundException` |
| R13 — Status por maioria simples | `ResultadoVotacao.status()`; contagem em `VotoRepository.countBySessaoIdAndEscolha()` |

## Caminhos de execução (controller → regra)

```
POST /pautas
  PautaController.save()
   → PautaService.savePauta()        [R1, R2, R3]
   → PautaRepositoryAdapter.save()   [R2 — salvaguarda do banco]

POST /sessoes
  SessaoController.save()
   → SessaoService.saveSessao()      [R4, R5, R6]
   → PautaService.getPautaById()     [R4]

POST /votos
  VotoController.save()
   → VotoService.votar()             [R7/R8, R9, R10]
   → SessaoService.getOpenSessaoById()  [R7/R8 — Sessao.isOpen(now), now do banco]
   → DocumentoValidatorClient.isValidDocumento()  [R9 — integração externa fictícia]
   → VotoRepositoryAdapter.save()    [R10 — salvaguarda do banco]

GET /sessoes/{id}/resultado
  SessaoController.apurar()
   → VotoService.apurarVotosSessao() [R12]
   → SessaoService.getClosedSessaoById() [R12 — Sessao.isOpen(now), now do banco]
   → ResultadoVotacao.status()       [R13]
```

## Mapeamento de erros → API

As exceções são traduzidas para HTTP no `GlobalExceptionHandler`:

| Exceção                    | HTTP            |
| -------------------------- | --------------- |
| `MethodArgumentNotValidException`      | 400 |
| `HttpMessageNotReadableException`      | 400 |
| `MethodArgumentTypeMismatchException`  | 400 |
| `NoResourceFoundException`            | 404 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `PautaNotFoundException`  | 400 |
| `SessaoNotFoundException` | 400 |
| `SessaoIsClosedException` | 400 |
| `SessaoIsOpenException`   | 400 |
| `DuplicatedPautaException`| 409 |
| `DuplicatedSessaoException`| 409 |
| `DuplicatedVoteException` | 409 |
| `InvalidDocumentoException`| 400 |
| `HttpIntegrationException`| 503 |
| demais                    | 500 |

## Índices do banco

As regras de unicidade e performance são garantidas por índices, definidos por ambiente:

- **H2 (default/dev):** `src/main/resources/schema.sql`
- **PostgreSQL (produção/docker):** `.docker/postgres/init.sql`

Um índice B-tree mantém chaves ordenadas com ponteiros para as linhas, evitando varrer a
tabela inteira em buscas (O(log n)). Num índice **único**, antes de gravar uma linha o banco
procura a chave no índice; se ela já existir, rejeita a operação — é assim que o índice vira
uma "constraint".

| Índice | Tipo | O que impõe | Definição |
| ------ | ---- | ----------- | --------- |
| `uk_pauta_titulo_lower` | funcional único sobre `LOWER(titulo)` | R2 — título único da pauta **ignorando caixa** (salvaguarda do banco contra corridas) | H2: coluna gerada `titulo_normalizado` + UNIQUE; PG: `CREATE UNIQUE INDEX ... ON pautas (LOWER(titulo))` |
| `uk_sessao_pauta` | único sobre `pauta_id` | R5 — no máximo uma sessão por pauta | `CREATE UNIQUE INDEX uk_sessao_pauta ON sessoes (pauta_id)` (idêntico nos dois) |
| `uk_voto_sessao_documento` | constraint única sobre `(sessao_id, documento)` | R10 — um voto por CPF por sessão | `CONSTRAINT uk_voto_sessao_documento UNIQUE (sessao_id, documento)` |
| `idx_voto_sessao_escolha` | comum sobre `(sessao_id, escolha_voto)` | R13 — performance da apuração (`VotoRepository.countBySessaoIdAndEscolha`) | `CREATE INDEX idx_voto_sessao_escolha ON votos (sessao_id, escolha_voto)` |

Observações:

- **Índice funcional (PG).** A chave guardada não é o valor da coluna, mas o resultado da
  expressão `LOWER(titulo)` calculado na escrita; por isso `"Reforma X"` e `"reforma x"`
  geram a mesma chave e a segunda inserção viola o índice. Consultas que filtram por
  `lower(titulo)` — caso do `existsByTituloIgnoreCase` (`lower(p.titulo) = lower(?)`) —
  podem usar esse índice em vez de varrer a tabela.
- **H2.** `schema.sql` usa coluna gerada `titulo_normalizado` porque o H2 2.4 não suporta
  índice funcional em `CREATE INDEX`; o efeito de unicidade case-insensitive é o mesmo.
- **PostgreSQL.** `init.sql` aplica o mesmo nome `uk_pauta_titulo_lower` via índice
  funcional, sem coluna extra.
- **Constraint × índice.** `uk_voto_sessao_documento` é declarada como constraint (que
  cria o índice automaticamente); os demais são `CREATE INDEX`/`CREATE UNIQUE INDEX` soltos.
  O `ddl-auto=validate` do Hibernate confere apenas as colunas mapeadas pelas entidades —
  índices e constraints extras são ignorados.

## Observações

1. **Duração validada no intervalo 1–43200 minutos.** `PautaRequestDTO` exige presença da
   duração (`@NotNull`), valor **maior que zero** (`@Positive`) e **no máximo 43200 minutos /
   30 dias** (`@Max`). Valores 0/negativos criariam sessão já fechada; valores acima do teto
   estourariam o intervalo de datas no cálculo de expiração.
2. **`DuplicatedSessaoException` para R5.** `SessaoService.saveSessao()` usa
   `existsByPautaId` e lança essa exceção (mapeada para 409). Em corrida, a violação do
   índice único em `SessaoRepositoryAdapter` também é convertida na mesma exceção (mesmo
   padrão dos adapters de Pauta e Voto).
3. **Sem indicador `hasSessao` para pautas (PF11 resolvido).** O record `PautaComStatus`, o
   método `PautaService.pautaComStatuses()` e a consulta `SessaoRepository.findPautaIdsComSessao()`
   foram removidos por serem código sem chamadas; `GET /pautas` retorna apenas id/título/duração
   e o consumidor cruza com `GET /sessoes`.
4. **Unicidade do voto declarada na entidade (PF7 resolvido).** `VotoEntity` declara a
   constraint composta `uk_voto_sessao_documento` em `@Table(uniqueConstraints = ...)`, com
   `(sessao_id, documento)` — mesma regra nome/filhos do `schema.sql`/`init.sql`. `documento`
   deixou de ter `unique = true` (que sugeriria unicidade global do CPF). A regra efetiva é
   **um voto por CPF por sessão**: o mesmo CPF pode votar em sessões diferentes.
5. **CPF normalizado (PF8 resolvido).** `VotoService.votar()` reduz o documento a somente
   dígitos antes da verificação de R10 e da persistência. Os formatos com e sem pontuação são
   armazenados de forma canônica (11 dígitos), tornando a unicidade `(sessao_id, documento)`
   imune à variação de formatação.
6. **Testes de `SessaoService` alinhados.** `SessaoServiceTest` está ativo e cobre
   `PautaNotFoundException`, `DuplicatedSessaoException` e a criação bem-sucedida, de acordo
   com a implementação atual.
7. **Regra no domínio, relógio do banco (PF9 resolvido).** "A sessão está aberta?" é um método
   puro do domínio — `Sessao.isOpen(now)` → `now.isBefore(expiresAt)` — e o `now` vem sempre de
   `SessaoRepository.now()` (`SELECT CURRENT_TIMESTAMP`, única autoridade de hora, comum a todas
   as instâncias). A regra não mora mais em queries SQL (`isOpenById`/`findOpenIds` foram
   removidas): `getOpenSessaoById`/`getClosedSessaoById` e a lista `getSessoesComStatus`
(`findAll()` + um único `now()`, transportando `SessaoComStatus`) aplicam a mesma regra.
    Não há nenhum `LocalDateTime.now()` da JVM no fluxo; não há status persistido nem job de
    fechamento — o estado continua derivado somente de `expires_at`. Os motivos da escolha e os
    trade-offs estão em [decisao-relogio-da-sessao.md](decisao-relogio-da-sessao.md).
8. **Janela de corrida no limite da expiração.** Entre a leitura do `now` do banco e a gravação
   do voto existe uma janela de poucos milissegundos em que um voto pode ser gravado logo após a
   expiração ter sido "vista" como aberta. É residual e pré-existente; a constraint
   `uk_voto_sessao_documento` não tem relação com o tempo.
9. **Resultado sem votos = `SEM_VOTOS` (PF10 resolvido).** `ResultadoVotacao.status()` retorna
   `SEM_VOTOS` quando `totalVotos() == 0`, antes da comparação de maioria. `EMPATE` passa a
   significar apenas empate real com ao menos um voto registrado. Coberto por
   `ResultadoVotacaoTest`.
10. **Validação externa de CPF (R9).** `VotoService.votar()` consulta o gateway
    `DocumentoValidator` — implementado por `DocumentoValidatorClient`, integração **fictícia**
    via `https://httpbin.org` (`GET /status/200,400,404,500` sorteia o status) — com o CPF já
    normalizado, **após** a sessão ser confirmada como aberta e **antes** da duplicidade.
    Semântica: 200 → válido; **4xx** → `InvalidDocumentoException` (400, "Documento inválido:
    \<cpf\>"); **5xx**/indisponibilidade → `HttpIntegrationException` (**503**). Timeouts de
    500 ms e URL configuráveis em `app.documento-validator.*`
    (`DocumentoValidatorConfig`/`DocumentoValidatorProperties`). Como a simulação é aleatória e
    não há base real de associados, PF12 (titularidade) segue em aberto.
11. **Correlação de logs e do corpo de erro por requisição (PF13 resolvido).**
    `MDCRequestFilter` (`com.votacao.entrypoint.filter`, `@Order(Ordered.HIGHEST_PRECEDENCE)`) lê o
    header `X-Correlation-Id` (ou gera UUID), **ecoa a header `X-Correlation-Id` na resposta** e
    popula o MDC com `correlationId`, `requestMethod` e `requestURI`; no `finally` **remove as
    chaves que ele mesmo colocou** (`MDC.remove(...)`) — não `MDC.clear()` — evitando vazar entre
    requisições na thread pool. O `logback-spring.xml` renderiza as chaves (pattern textual no
    default; campos JSON via `LogstashEncoder` no prod), de modo que todas as linhas de uma
    requisição — incluindo o `ERROR` do `GlobalExceptionHandler.handleGeneric()` — compartilham o
    mesmo id. O `GlobalExceptionHandler` lê `MDC.get("correlationId")` ao montar o `ApiError`,
    expondo o campo **`correlation_id`** no corpo de erro. O `correlationId` é **gerado manualmente**
    (MDC); em branches futuros será migrado para o tracing do Spring Boot (Micrometer).
