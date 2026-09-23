# Rastreabilidade de Implementação

Mapa das regras de negócio para as classes e métodos que as implementam.

| Regra | Implementação |
| ----- | ------------- |
| R1 — Campos obrigatórios da pauta | `PautaRequestDTO` (`@NotBlank`, `@NotNull`); `PautaEntity` (colunas `NOT NULL`) |
| R2 — Título único da pauta | `PautaService.savePauta()` → `PautaRepository.existsByTituloIgnoreCase()`; `PautaRepositoryAdapter.save()` captura `DataIntegrityViolationException` → `DuplicatedPautaException`; salvaguarda do banco `uk_pauta_titulo_lower`: em H2, coluna gerada `titulo_normalizado (LOWER(titulo))` no `schema.sql`; em PostgreSQL, índice funcional `LOWER(titulo)` em `.docker/postgres/init.sql` |
| R3 — Normalização e tamanho do título | `Pauta` (constructor faz `titulo.trim()`); `PautaRequestDTO` (`@Size(20–150)`); `titulo VARCHAR(150)` em `schema.sql` |
| R4 — Sessão exige pauta existente | `SessaoDTO` (`@NotNull pautaId`) + `SessaoController.save()` (`@Valid`); `SessaoService.saveSessao()` → `PautaService.getPautaById()` → `PautaNotFoundException` |
| R5 — Uma sessão por pauta | `SessaoService.saveSessao()` → `sessaoRepository.existsByPautaId()` → `DuplicatedSessaoException`; `SessaoRepositoryAdapter.save()` converte a violação do índice único `uk_sessao_pauta(sessoes.pauta_id)` em `schema.sql` |
| R6 — Duração definida pela pauta | `PautaRequestDTO` (`@Positive` + `@Max(43200)` em `tempoVotacaoMinutos`); `SessaoService.saveSessao()`: `expiresAt = now.plusMinutes(pauta.tempoVotacaoMinutos())` |
| R7/R8 — Votos somente em sessão aberta | `VotoService.votar()` → `SessaoService.getOpenSessaoById()` → `Sessao.isOpen(now)` e `SessaoIsClosedException` |
| R9 — CPF válido | `VotoDTO` (anotação `@CPF`) |
| R10 — Um voto por CPF por sessão | `VotoService.votar()` → `VotoRepository.existsBySessaoIdAndDocumento()`; `VotoRepositoryAdapter.save()` → `DuplicatedVoteException`; constraint `uk_voto_sessao_documento(sessao_id, documento)` em `schema.sql` |
| R11 — Voto apenas SIM/NÃO | Enum `Voto.Escolha { SIM, NAO }`; `HttpMessageNotReadableException` para valores inválidos |
| R12 — Resultado só para sessão fechada | `VotoService.apurarVotosSessao()` → `SessaoService.getClosedSessaoById()` → `SessaoIsOpenException` / `SessaoNotFoundException` |
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
   → VotoService.votar()             [R7/R8, R10]
   → SessaoService.getOpenSessaoById()  [R7/R8]
   → VotoRepositoryAdapter.save()    [R10 — salvaguarda do banco]

GET /sessoes/{id}/resultado
  SessaoController.apurar()
   → VotoService.apurarVotosSessao() [R12]
   → SessaoService.getClosedSessaoById() [R12]
   → ResultadoVotacao.status()       [R13]
```

## Mapeamento de erros → API

As exceções são traduzidas para HTTP em `GlobalExceptionHandler`:

| Exceção                    | HTTP            |
| -------------------------- | --------------- |
| `MethodArgumentNotValidException`      | 400 |
| `HttpMessageNotReadableException`      | 400 |
| `PautaNotFoundException`  | 400 |
| `SessaoNotFoundException` | 400 |
| `SessaoIsClosedException` | 400 |
| `SessaoIsOpenException`   | 400 |
| `DuplicatedPautaException`| 409 |
| `DuplicatedSessaoException`| 409 |
| `DuplicatedVoteException` | 409 |
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
3. **`pautaComStatuses()` não exposto.** O serviço `PautaService.pautaComStatuses()` calcula
   o indicador `hasSessao` (se a pauta já possui sessão), mas **nenhum controller o utiliza**;
   `GET /pautas` retorna apenas id/título/duração.
4. **Unicidade do voto declarada na entidade (PF7 resolvido).** `VotoEntity` declara a
   constraint composta `uk_voto_sessao_documento` em `@Table(uniqueConstraints = ...)`, com
   `(sessao_id, documento)` — mesma regra nome/filhos do `schema.sql`/`init.sql`. `documento`
   deixou de ter `unique = true` (que sugeriria unicidade global do CPF). A regra efetiva é
   **um voto por CPF por sessão**: o mesmo CPF pode votar em sessões diferentes.
5. **CPF sem normalização.** O documento chega à persistência sem formatação canônica.
   O mesmo CPF numérico com formatações diferentes seria armazenado/comparado como valores
   distintos (relevante para a unicidade de R10).
6. **Testes de `SessaoService` alinhados.** `SessaoServiceTest` está ativo e cobre
   `PautaNotFoundException`, `DuplicatedSessaoException` e a criação bem-sucedida, de acordo
   com a implementação atual.
7. **`Sessao.isOpen()` desconsidera `startedAt`.** O estado aberta/fechada depende somente de
   `expiresAt` (e de `expiresAt != null`). Como a criação sempre define `startedAt` e
   `expiresAt`, a janela efetiva de votação é `[criação, expiração)`.