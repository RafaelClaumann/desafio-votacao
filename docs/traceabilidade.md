# Rastreabilidade de Implementação

Mapa das regras de negócio para as classes e métodos que as implementam.

| Regra | Implementação |
| ----- | ------------- |
| R1 — Campos obrigatórios da pauta | `PautaRequestDTO` (`@NotBlank`, `@NotNull`); `PautaEntity` (colunas `NOT NULL`) |
| R2 — Título único da pauta | `PautaService.savePauta()` → `PautaRepository.existsByTituloIgnoreCase()`; `PautaRepositoryAdapter.save()` captura `DataIntegrityViolationException` → `DuplicatedPautaException`; constraint `uk_pauta_titulo` em `schema.sql` |
| R3 — Normalização e tamanho do título | `Pauta` (constructor faz `titulo.trim()`); `PautaRequestDTO` (`@Size(20–150)`); `titulo VARCHAR(150)` em `schema.sql` |
| R4 — Sessão exige pauta existente | `SessaoService.saveSessao()` → `PautaService.getPautaById()` → `PautaNotFoundException` |
| R5 — Uma sessão por pauta | `SessaoService.saveSessao()` → `sessaoRepository.existsByPautaId()`; índice único `uk_sessao_pauta(sessoes.pauta_id)` em `schema.sql` |
| R6 — Duração definida pela pauta | `SessaoService.saveSessao()`: `expiresAt = now.plusMinutes(pauta.tempoVotacaoMinutos())` |
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
| `DuplicatedVoteException` | 409 |
| demais                    | 500 |

## Observações

1. **Duração sem validação de positividade.** `PautaRequestDTO` exige apenas presença da
   duração (`@NotNull`), sem `@Min`/`@Positive`. Valores 0/negativos criam sessão já fechada.
2. **`IllegalArgumentException` não mapeada em R5.** `SessaoService.saveSessao()` a usa para
   "já existe sessão para a pauta"; sem handler específico, o cliente recebe 500. Violações de
   integridade em `SessaoRepositoryAdapter` também não são traduzidas (diferente dos adapters
   de Pauta e Voto).
3. **`pautaComStatuses()` não exposto.** O serviço `PautaService.pautaComStatuses()` calcula
   o indicador `hasSessao` (se a pauta já possui sessão), mas **nenhum controller o utiliza**;
   `GET /pautas` retorna apenas id/título/duração.
4. **Unicidade do documento divergente entre entidade e schema.** `VotoEntity` declara
   `@Column(unique = true, length = 14)` em `documento` (sugeriria unicidade global do CPF em
   todo o sistema), mas o `schema.sql` aplica unicidade por par `(sessao_id, documento)`, e a
   verificação de negócio (`existsBySessaoIdAndDocumento`) também é por sessão. A regra efetiva
   é **um CPF por sessão**, e a anotação de entidade diverge do schema aplicado.
5. **CPF sem normalização.** O documento chega à persistência sem formatação canônica.
   O mesmo CPF numérico com formatações diferentes seria armazenado/comparado como valores
   distintos (relevante para a unicidade de R10).
6. **Teste divergente da implementação.** `SessaoServiceTest.saveSessao_shouldThrowWhenPautaDoesNotExist`
   espera `IllegalArgumentException("Pauta not found")`, mas a implementação atual lança
   `PautaNotFoundException`. A documentação descreve o comportamento da implementação atual.
7. **`Sessao.isOpen()` desconsidera `startedAt`.** O estado aberta/fechada depende somente de
   `expiresAt` (e de `expiresAt != null`). Como a criação sempre define `startedAt` e
   `expiresAt`, a janela efetiva de votação é `[criação, expiração)`.