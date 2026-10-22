# Motivos de Rejeição e Mapeamento de Erros

Esta seção explica **por que** uma operação de negócio pode ser rejeitada e como o sistema
traduz isso para a API.

| HTTP  | Situação de negócio                                                              | Exceção / origem                                    |
| ----- | -------------------------------------------------------------------------------- | --------------------------------------------------- |
| 400    | Dados de entrada inválidos (título, duração, sessão/`id_pauta`, CPF, escolha).     | `MethodArgumentNotValidException` (Bean Validation) |
| 400    | Documento (CPF) rejeitado pelo **validador externo** (R9 — apenas com `is-enabled=true`). | `InvalidDocumentoException`                        |
| 400    | Corpo da requisição ilegível — ex.: escolha fora de SIM/NAO, JSON malformado.    | `HttpMessageNotReadableException`                 |
| 400    | Parâmetro de caminho com tipo inválido (ex.: id não numérico).                    | `MethodArgumentTypeMismatchException`              |
| 400    | A pauta informada para abrir sessão **não existe** (R4).                          | `PautaNotFoundException`                           |
| 400    | A sessão informada para votar/apurar **não existe** (R4/R12).                    | `SessaoNotFoundException`                          |
| 400    | Tentativa de votar em uma sessão **fechada** (R7/R8).                            | `SessaoIsClosedException`                          |
| 400    | Tentativa de apurar resultado de uma sessão ainda **aberta** (R12).              | `SessaoIsOpenException`                            |
| 404    | Rota não existe.                                                                 | `NoResourceFoundException`                         |
| 405    | Método HTTP não suportado na rota (ex.: `PUT /pautas`).                          | `HttpRequestMethodNotSupportedException`           |
| 409    | Tentativa de criar pauta com **título duplicado** (R2).                          | `DuplicatedPautaException`                         |
| 409    | Tentativa de abrir a **2ª sessão da mesma pauta** (R5).                          | `DuplicatedSessaoException`                        |
| 409    | Tentativa de votar com **CPF que já votou na sessão** (R10).                     | `DuplicatedVoteException`                          |
| 503    | Falha no **validador externo** de CPF (status 5xx ou indisponibilidade de rede — apenas com `is-enabled=true`). | `HttpIntegrationException`                         |
| 500    | Erros não previstos.                                                              | `Exception` genérica                               |

## Interpretação de negócio dos códigos

- **HTTP 400 (Bad Request):** a operação é legítima, mas as condições de negócio não foram
  atendidas (objeto inexistente, sessão no estado errado, dados inválidos) ou a requisição está
  malformada (corpo ilegível, parâmetro de caminho com tipo errado). Nada é gravado.
- **HTTP 404 (Not Found):** a rota não existe.
- **HTTP 405 (Method Not Allowed):** a rota existe, mas o método HTTP não é suportado.
- **HTTP 409 (Conflict):** a operação tenta violar uma regra de unicidade — título de pauta,
  sessão por pauta ou voto do mesmo CPF na mesma sessão. Nada é gravado.
- **HTTP 500 (Internal Server Error):** erro inesperado do servidor. O corpo é **genérico de
  propósito** (não vaza detalhe interno) e traz o **`correlation_id`** (header `X-Correlation-Id`
  ou UUID gerado pelo `MDCRequestFilter`); todas as linhas de log da requisição são correlacionadas
  sob o mesmo id, permitindo ao consumidor citá-lo no chamado de suporte. No `POST /votos`, a
  falha da **notificação em Kafka não gera 500**: ela ocorre após o commit e é capturada pela
  aplicação, que a registra em log com o contexto do voto (ver
  [PF16](pontos-fracos-e-falhas.md#pf16)).
- **HTTP 503 (Service Unavailable):** falha na integração com o validador externo de CPF.
  Nada é gravado. Só pode ocorrer com `app.documento-validator.is-enabled=true` — com a validação
  desligada (padrão) nenhuma chamada externa é feita.

## Cenários adicionais

### Tentar abrir a segunda sessão de uma pauta (violação de R5)

A verificação em `SessaoService.saveSessao()` lança `DuplicatedSessaoException`, mapeada para
**HTTP 409** com a mensagem "Já existe uma sessão para a pauta: N". Em corrida, o banco
bloqueia (índice único `uk_sessao_pauta`) e `SessaoRepositoryAdapter` converte a violação de
integridade na mesma `DuplicatedSessaoException` → igualmente **409**. As
`Duplicated*Exception` preservam a `DataIntegrityViolationException` original como **causa**
(`Throwable cause`), mantendo a mensagem do contrato intacta e o detalhe do banco disponível
no log para diagnóstico (correlacionado por `correlation_id`).

### Concorrência ao votar com o mesmo CPF na mesma sessão

A verificação prévia (`existsByIdSessaoAndDocumento`) evita duplicidade na maioria dos casos.
Se duas requisições simultâneas passarem pela verificação ao mesmo tempo, a restrição única do
banco captura a duplicidade e o `VotoRepositoryAdapter` a converte em `DuplicatedVoteException`
→ **HTTP 409**. O mesmo tratamento de integridade existe para o título da pauta. Em todos os
casos, a exceção de negócio encadeia a `DataIntegrityViolationException` original como causa,
para rastreio da violação no log (o corpo HTTP permanece genérico e igual em ambas as camadas).

### Validação externa (fictícia) do CPF ao votar

Após a sessão ser confirmada como aberta, `VotoService.votar()` consulta o gateway
`DocumentoValidator` — implementado por `DocumentoValidatorClient` via `https://httpbin.org`
(`GET /status/200,400,404,500` sorteia o status). A semântica da simulação: **200** → documento
válido; **4xx** → documento rejeitado → `InvalidDocumentoException` → **HTTP 400**
("Documento inválido: \<cpf\>"); **5xx** ou indisponibilidade de rede → `HttpIntegrationException`
→ **HTTP 503**. URLs e timeouts (500 ms) vêm de `app.documento-validator.*`.
A integração é **fictícia** (aleatória), portanto não confirma a titularidade do CPF (PF12).

Com `app.documento-validator.is-enabled=false` (padrão), **nenhuma chamada ao serviço externo é
feita** — nem a *simulação de status aleatório*, nem a *consulta*. O documento deixa de ser
rejeitado aleatoriamente e passa a ser aceito sempre que tenha formato válido; como não há
requisição externa, uma falha de rede ou um status 5xx do validador **não pode mais recusar o voto
com HTTP 503** — o `POST /votos` funciona sem acesso à internet (ver
[R16](regras-de-negocio.md#r16--a-consulta-ao-validador-externo-de-cpf-pode-ser-desligada) e
[PF17](pontos-fracos-e-falhas.md#pf17)).

### Notificação de voto indisponível (Kafka fora do ar)

A notificação de voto é enviada **após** a confirmação da gravação e **fora** da transação.
Duas situações distintas:

| Situação | Efeito | Resposta ao associado |
| -------- | ------ | ---------------------- |
| **Falha assíncrona** no envio (broker indisponível, erro de serialização, timeout de metadados já vencido) | Registrada em log `ERROR` ("Failed to send message: idVoto=…, idSessao=…, correlationId=…"). O **voto permanece registrado** e apurável. | **201** — o sucesso da operação de negócio é o registro do voto. |
| **Exceção síncrona** durante o envio (por exemplo, tempo limite do produtor aguardando os metadados do broker — ver [PF16](pontos-fracos-e-falhas.md#pf16)) | A exceção é lançada **após o commit** e **capturada pela própria aplicação** (`VotoEventKafkaSender`), que a **registra em log `ERROR` com o contexto do voto** (`idVoto`, `idSessao`, `correlationId`) — sem propagá-la ao chamador. A notificação é perdida, mas **deixando rastro**. | **201** — embora a notificação **não** tenha sido publicada. |

Em ambos os casos o **voto não é desfeito** e o consumidor da API **nunca** recebe um erro por
falha de notificação: a perda é **silenciosa** para o consumidor (PF15), mas **diagnosticável**
(PF16 corrigido). O diagnóstico é feito pelo log — o `correlation_id` (mesmo id propagado no
header `X-Correlation-Id` da mensagem Kafka) é a pista
para correlacionar a requisição que votou e a tentativa de notificação.

### Duração da votação inválida (≤ 0 ou acima de 43200 minutos)

A duração é validada na criação da pauta: deve ser **maior que zero** (`@Positive`) e **no
máximo 43200 minutos / 30 dias** (`@Max`). Valores `0`/negativos fariam a sessão nascer **já
fechada** (expiração igual ou anterior ao início); valores acima do teto estourariam o
intervalo de datas no cálculo de expiração. Ambos os casos são recusados com **HTTP 400** na
criação da pauta, com a mensagem de Bean Validation correspondente.

### Erro não previsto (HTTP 500) e diagnóstico por `correlation_id`

Quando uma exceção **não mapeada** ocorre, o `GlobalExceptionHandler.handleGeneric()` responde
**HTTP 500** com a mensagem fixa "Erro interno do servidor" — sem expor stack ou detalhe interno.
O detalhe completo fica no log (`ERROR`), com todas as linhas da requisição correlacionadas sob o
mesmo id (header `X-Correlation-Id` ou UUID do `MDCRequestFilter`):

```
2026-09-24 15:47:12 [http-nio-8080-exec-3] ERROR a3f9c2d1-… POST /votos com.votacao.entrypoint.api.handler.GlobalExceptionHandler - Erro não tratado em /votos
  → (stack trace completo, registrado pelo handleGeneric)
```

O **corpo** da resposta expõe o id no campo **`correlation_id`** para o consumidor citar no
chamado de suporte:

```json
{
  "timestamp": "2026-09-24T15:47:12.000Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Erro interno do servidor",
  "path": "/votos",
  "correlation_id": "a3f9c2d1-…",
  "field_errors": []
}
```

Quem tem acesso ao log pode filtrar pelo `correlation_id` e recompor a requisição inteira; o corpo
continua genérico (não vaza causa) por design.
