# Regras de Negócio

Cada regra abaixo é apresentada em termos de negócio. A seção **Implementação** indica o
código que a valida — detalhes completos em [traceabilidade.md](traceabilidade.md).

## Criação de Pauta

### R1 — Uma pauta só pode ser criada com título e duração de votação informados

**Condição**

Um organizador submete a criação de uma pauta. O sistema exige que o título e a duração de
votação estejam presentes.

**Comportamento**

A pauta é registrada com o título e a duração informados.

**Violação**

Se o título ou a duração não for informado, a criação é recusada com a mensagem de validação.
Isso é uma **validação de entrada** (campo obrigatório), não uma decisão de negócio.

**Implementação**

`PautaRequestDTO` — anotações `@NotBlank(titulo)` e `@NotNull(tempoVotacaoMinutos)`.

### R2 — O título da pauta é único; títulos duplicados impedem a criação

**Condição**

Uma pauta é criada com um título.

**Comportamento**

O sistema verifica se outro título igual já existe, ignorando diferenças de caixa
(maiúsculas/minúsculas). Se não existir, a pauta é criada normalmente.

**Violação**

Se já existir uma pauta com o mesmo título (desconsiderando caixa), a criação é recusada
(HTTP 409 — Conflito). A duplicidade também é bloqueada no banco pela salvaguarda
`uk_pauta_titulo_lower`, que aplica a mesma regra insensível à caixa e funciona como
defesa em caso de concorrência: em H2 (dev/teste) via coluna gerada `titulo_normalizado`
(`LOWER(titulo)`) no `schema.sql`; em PostgreSQL (produção) via índice funcional único
`LOWER(titulo)` no `.docker/postgres/init.sql`.

**Implementação**

`PautaService.savePauta()` → `existsByTituloIgnoreCase()`; persistência via
`PautaRepositoryAdapter.save()` (que converte violação de integridade em
`DuplicatedPautaException`); salvaguarda `uk_pauta_titulo_lower` (coluna gerada no H2;
índice funcional no Postgres).

### R3 — O título da pauta é normalizado e deve ter entre 20 e 150 caracteres

**Condição**

O título da pauta é submetido como texto livre.

**Comportamento**

Espaços em branco nas bordas são removidos antes do armazenamento.

**Violação**

Títulos com menos de 20 ou mais de 150 caracteres são recusados na validação de entrada.
O limite de 150 também é imposto no banco (coluna `titulo VARCHAR(150)`).

**Implementação**

`Pauta` (constructor do record normaliza o título com `trim`) e `PautaRequestDTO`
(`@Size(min = 20, max = 150)`).

## Abertura de Sessão de Votação

### R4 — Uma sessão só pode ser aberta para uma pauta existente

**Condição**

Um organizador abre uma sessão informando o identificador de uma pauta.

**Comportamento**

Se a pauta existir, o processo de abertura segue adiante.

**Violação**

- Se o identificador da pauta estiver **ausente ou nulo**, a abertura é recusada na validação de
  entrada (HTTP 400) — `SessaoDTO.pautaId` é obrigatório (PF3 corrigido, ver
  [pontos-fracos-e-falhas.md](pontos-fracos-e-falhas.md)).
- Se a pauta **não existir**, a abertura é recusada (HTTP 400). Nenhuma sessão é criada.

**Implementação**

`SessaoDTO` (`@NotNull pautaId`) e `SessaoController.save()` (`@Valid`);
`SessaoService.saveSessao()` delega para `PautaService.getPautaById()`, que lança
`PautaNotFoundException`.

### R5 — Uma pauta pode ter no máximo uma sessão de votação

**Condição**

Uma sessão é aberta para uma pauta.

**Comportamento**

Se a pauta ainda não possui sessão, a sessão é criada.

**Violação**

Se a pauta já possui uma sessão, a abertura é recusada e nenhuma nova sessão é criada
(**HTTP 409** — "Já existe uma sessão para a pauta: N"). A regra
vale pela vida inteira da pauta: mesmo depois de a sessão existente fechar, não é possível abrir
outra sessão para a mesma pauta. O banco também aplica essa unicidade
(índice único em `sessoes.pauta_id`), traduzida pelo adapter em 409 sob corrida.

**Implementação**

`SessaoService.saveSessao()` → `sessaoRepository.existsByPautaId()` lança
`DuplicatedSessaoException` (mapeada para 409); `SessaoRepositoryAdapter.save()` converte a
violação do índice único `uk_sessao_pauta` no `schema.sql` em `DuplicatedSessaoException`.

### R6 — A duração da sessão é definida pela pauta

**Condição**

Uma pauta válida é selecionada para abertura de sessão.

**Comportamento**

A sessão passa a valer a partir de **agora** (o relógio do banco de dados) e tem como
expiração **agora + duração de votação (em minutos) definida na pauta**. Se a duração for
positiva, a sessão fica aberta pelo tempo configurado.

**Violação**

A duração é **validada como valor entre 1 e 43200 minutos** (`@Positive` + `@Max(43200)`)
já na criação da pauta. Com duração `0` ou negativa, a pauta é recusada (HTTP 400) — portanto
não é possível abrir sessão com duração inválida nem criar uma sessão que já nasça fechada por
esse motivo (PF1 corrigido, ver
[pontos-fracos-e-falhas.md](pontos-fracos-e-falhas.md)). Com duração acima de **43200 minutos
(30 dias)**, a pauta também é recusada (HTTP 400) — o limite impede que `now.plusMinutes(dur)`
exceda o intervalo representável do `LocalDateTime` ao abrir a sessão (PF2 corrigido).

**Implementação**

`PautaRequestDTO` (`@Positive` + `@Max` em `tempoVotacaoMinutos`, com `@NotNull`);
`SessaoService.saveSessao()` = `sessaoRepository.now().plusMinutes(pauta.tempoVotacaoMinutos())`
(o "agora" vem do `CURRENT_TIMESTAMP` do banco — PF9 resolvido).

## Votação

### R7 — Uma sessão aceita votos apenas enquanto estiver aberta

**Condição**

Um associado tenta registrar um voto em uma sessão.

**Comportamento**

Se a sessão existir e ainda estiver no período de validade, o voto é aceito.

**Violação**

Se a sessão estiver fechada, o voto é recusado (HTTP 400) e **nada é registrado**.

**Implementação**

`VotoService.votar()` → `SessaoService.getOpenSessaoById()`, que lança `SessaoIsClosedException`.
A exceção é regra de negócio do domínio (`Sessao.isOpen(now)`), avaliada com o **relógio do banco
de dados** (`SessaoRepository.now()` = `CURRENT_TIMESTAMP` — PF9 resolvido).

### R8 — Ao atingir a expiração, a sessão fecha e nunca mais aceita votos

**Condição**

O relógio do banco de dados atinge o horário de expiração (ou já passou dele).

**Comportamento**

A sessão passa a ser tratada como fechada: votos passam a ser rejeitados e o resultado pode
ser apurado (ver R12).

**Violação**

Nenhuma: é um comportamento de passagem de tempo. Não há reabertura de sessão — o estado
fechado é terminal. Não existem agentes externos para fechar/reabrir manualmente.

**Implementação**

`Sessao.isOpen(now)` (domínio: `now.isBefore(expiresAt)`), com `now` de
`SessaoRepository.now()` (`SELECT CURRENT_TIMESTAMP`) — a sessão está aberta somente enquanto,
pelo relógio do banco, `expiresAt` é estritamente posterior a `CURRENT_TIMESTAMP`. No instante
exato da expiração a sessão já é considerada fechada. Não existe comparação com
`LocalDateTime.now()` do servidor (PF9 resolvido).

### R9 — Somente associados com CPF válido podem votar

**Condição**

Um associado registra um voto informando seu documento.

**Comportamento**

O documento é validado em duas camadas antes do voto ser aceito:
1. **Formato** — deve ser um CPF bem-formado (validação de entrada `@CPF`).
2. **Situação cadastral** — o documento normalizado é submetido a um **validador externo**
   (`DocumentoValidator`). Essa integração é **fictícia**: não consulta uma base real de
   associados e o resultado é aleatório (ver observação abaixo).

A consulta externa acontece **depois** de a sessão ser confirmada como existente e aberta
(`getOpenSessaoById`, primeira validação de negócio em `VotoService.votar()`) e **antes** da
verificação de duplicidade.

**Violação**

Um documento que não passe na validação de formato recusa o voto (HTTP 400) na validação de
entrada. Um documento que passe no formato, mas seja **rejeitado pelo validador externo**
(status **4xx**), também recusa o voto (HTTP 400) com a mensagem "Documento inválido: \<cpf\>"
— por meio da exceção `InvalidDocumentoException`. Em caso de **falha do serviço externo**
(status **5xx** ou indisponibilidade de rede), a operação falha com `HttpIntegrationException`,
mapeada para **HTTP 503** (Service Unavailable) no `GlobalExceptionHandler`.

> Observação (validação externa fictícia): `DocumentoValidatorClient` simula o serviço externo
> com o `https://httpbin.org` — o `GET /status/200,400,404,500` sorteia o status e o determina o
> resultado: **200** → documento válido, **4xx** → rejeitado (400), **5xx** → falha de
> integração (503). Não corresponde a uma base real de associados; a autodeclaração do CPF
> (PF12) permanece (ver [pontos-fracos-e-falhas.md](pontos-fracos-e-falhas.md#pf12)). Config
> via `app.documento-validator.*` (URL e timeouts de 500 ms).

> Observação (normalização): o documento é **normalizado** para somente dígitos em
> `VotoService.votar()` antes de ser comparado e armazenado. Os formatos com e sem pontuação
> são tratados como o **mesmo documento** (reforça a unicidade de R10; ver
> [traceabilidade.md](traceabilidade.md#observações)).

**Implementação**

`VotoDTO` — anotação `@CPF` (Bean Validation do Hibernate Validator); `VotoService.votar()` →
`DocumentoValidator.isValidDocumento()` (após a sessão estar aberta; lança
`InvalidDocumentoException` quando falso); `DocumentoValidatorClient` — implementação
`@Component` do gateway via `RestClient` (URL e timeouts de 500 ms configurados em
`DocumentoValidatorConfig`/`DocumentoValidatorProperties`, `app.documento-validator.*`);
`GlobalExceptionHandler` — mapeia `InvalidDocumentoException` (400) e `HttpIntegrationException`
(503).

### R10 — Cada CPF pode votar apenas uma vez por sessão

**Condição**

Um associado já votou (ou tentou votar) na sessão.

**Comportamento**

Se o mesmo documento ainda não votou naquela sessão, o voto é aceito.

**Violação**

Se o documento já possui voto na mesma sessão, o novo voto é recusado (HTTP 409 — Conflito).
A restrição também existe no banco (unicidade de `(sessao_id, documento)`), funcionando como
salvaguarda contra votos simultâneos abusando da verificação em memória.

**Implementação**

`VotoService.votar()` (normaliza o CPF para somente dígitos e consulta
`votoRepository.existsBySessaoIdAndDocumento()` com a forma canônica); e
`VotoRepositoryAdapter.save()` traduz violação de integridade em `DuplicatedVoteException`.

### R11 — O voto só pode ser SIM ou NÃO

**Condição**

Um associado registra sua escolha em uma votação.

**Comportamento**

O sistema aceita `SIM` ou `NÃO`.

**Violação**

Qualquer outro valor é recusado (HTTP 400) na desserialização da requisição. Não há opção de
abstenção — todo voto registrado é a favor ou contra.

**Implementação**

Enum `Voto.Escolha { SIM, NAO }`.

## Apuração do Resultado

### R12 — O resultado só pode ser apurado para uma sessão já fechada

**Condição**

Um organizador solicita o resultado de uma sessão.

**Comportamento**

Se a sessão existir e estiver fechada, o resultado é calculado a partir dos votos registrados.

**Violação**

- Se a sessão não existir, a apuração é recusada (HTTP 400).
- Se a sessão ainda estiver **aberta**, a apuração é recusada (HTTP 400) — enquanto houver
  votação em andamento o resultado não é divulgado. A apuração considera somente os votos
  registrados até o fechamento.

**Implementação**

`VotoService.apurarVotosSessao()` → `SessaoService.getClosedSessaoById()`, que lança
`SessaoNotFoundException` ou `SessaoIsOpenException` (estado fechado avaliado por `Sessao.isOpen(now)`
com `now` do relógio do banco — PF9 resolvido).

### R13 — O status do resultado é definido pela maioria simples

**Condição**

Uma sessão fechada é apurada.

**Comportamento**

- Nenhum voto registrado (SIM = NÃO = 0) → **SEM_VOTOS**.
- Votos SIM **>** votos NÃO → **APROVADA**.
- Votos SIM **<** votos NÃO → **REJEITADA**.
- Votos SIM **==** votos NÃO (com ao menos um voto) → **EMPATE**.

O resultado reporta também os totais de SIM, NÃO e o total geral de votos. Não existe quórum
mínimo nem percentual de aprovação — apenas a comparação direta das contagens. A sessão
fechada sem nenhum voto é distinta do empate real (PF10 resolvido).

**Implementação**

`ResultadoVotacao.status()` e a contagem via
`votoRepository.countBySessaoIdAndEscolha()` para cada escolha.
