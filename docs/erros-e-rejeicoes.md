# Motivos de Rejeição e Mapeamento de Erros

Esta seção explica **por que** uma operação de negócio pode ser rejeitada e como o sistema
traduz isso para a API.

| HTTP  | Situação de negócio                                                              | Exceção / origem                                    |
| ----- | -------------------------------------------------------------------------------- | --------------------------------------------------- |
| 400    | Dados de entrada inválidos (título, duração, sessão/`pauta_id`, CPF, escolha).     | `MethodArgumentNotValidException` (Bean Validation) |
| 400    | Corpo da requisição ilegível — ex.: escolha fora de SIM/NAO, JSON malformado.    | `HttpMessageNotReadableException`                 |
| 400    | A pauta informada para abrir sessão **não existe** (R4).                          | `PautaNotFoundException`                           |
| 400    | A sessão informada para votar/apurar **não existe** (R4/R12).                    | `SessaoNotFoundException`                          |
| 400    | Tentativa de votar em uma sessão **fechada** (R7/R8).                            | `SessaoIsClosedException`                          |
| 400    | Tentativa de apurar resultado de uma sessão ainda **aberta** (R12).              | `SessaoIsOpenException`                            |
| 409    | Tentativa de criar pauta com **título duplicado** (R2).                          | `DuplicatedPautaException`                         |
| 409    | Tentativa de votar com **CPF que já votou na sessão** (R10).                     | `DuplicatedVoteException`                          |
| 500    | Erros não previstos (inclusive cenários abaixo).                                  | `Exception` genérica                               |

## Interpretação de negócio dos códigos

- **HTTP 400 (Bad Request):** a operação é legítima, mas as condições de negócio não foram
  atendidas (objeto inexistente, sessão no estado errado, dados inválidos). Nada é gravado.
- **HTTP 409 (Conflict):** a operação tenta violar uma regra de unicidade — título de pauta
  ou voto do mesmo CPF na mesma sessão. Nada é gravado.
- **HTTP 500 (Internal Server Error):** erro inesperado do servidor.

## Cenários adicionais

### Tentar abrir a segunda sessão de uma pauta (violação de R5)

A verificação em `SessaoService.saveSessao()` lança `IllegalArgumentException`, que **não
possui tratamento específico** no `GlobalExceptionHandler`. Na prática, o cliente recebe
**HTTP 500**, embora a causa seja uma violação de regra de negócio. O banco também bloqueia
(índice único), mas `SessaoRepositoryAdapter` não converte essa violação de integridade —
portanto ela também cai no erro genérico 500.

### Concorrência ao votar com o mesmo CPF na mesma sessão

A verificação prévia (`existsBySessaoIdAndDocumento`) evita duplicidade na maioria dos casos.
Se duas requisições simultâneas passarem pela verificação ao mesmo tempo, a restrição única do
banco captura a duplicidade e o `VotoRepositoryAdapter` a converte em `DuplicatedVoteException`
→ **HTTP 409**. O mesmo tratamento de integridade existe para o título da pauta.

### Duração da votação inválida (≤ 0 ou acima de 43200 minutos)

A duração é validada na criação da pauta: deve ser **maior que zero** (`@Positive`) e **no
máximo 43200 minutos / 30 dias** (`@Max`). Valores `0`/negativos fariam a sessão nascer **já
fechada** (expiração igual ou anterior ao início); valores acima do teto estourariam o
intervalo de datas no cálculo de expiração. Ambos os casos são recusados com **HTTP 400** na
criação da pauta, com a mensagem de Bean Validation correspondente.