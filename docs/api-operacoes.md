# Operações da API

A API REST expõe as operações de negócio descritas nos fluxos. Os campos JSON seguem o padrão
`snake_case` (configuração `spring.jackson.property-naming-strategy=SNAKE_CASE`).

## Modelo de erro

Todas as falhas tratadas pelo `GlobalExceptionHandler` seguem o mesmo formato:

```json
{
  "timestamp": "2026-09-24T18:00:01Z",
  "status": 400,
  "error": "Bad Request",
  "message": "mensagem do erro",
  "path": "/votos",
  "correlation_id": "uuid-da-requisicao",
  "field_errors": []
}
```

- `timestamp` é o momento do erro no formato `Instant` (UTC, com `Z`).
- `field_errors` é preenchido somente em falhas de Bean Validation; demais falhas retornam uma
  lista vazia.
- `correlation_id` é o `X-Correlation-Id` recebido ou um UUID gerado pelo `MDCRequestFilter`, que
  também ecoa o valor no header de resposta.
- Mapeamento transversal: 400 (validação/regra de negócio), 404 (rota inexistente), 405 (método
  não suportado), 409 (conflito de unicidade), 503 (falha do validador externo), 500 (erro não
  previsto, com mensagem genérica).

---

## `POST /pautas` — Criar pauta

**Propósito**

Registrar um novo tema a ser deliberado, com o tempo de votação em minutos.

**Entrada**

| Campo                 | Tipo   | Regra                                              |
| --------------------- | ------ | -------------------------------------------------- |
| `titulo`              | texto  | Obrigatório, 20–150 caracteres; espaços das bordas são removidos antes de persistir |
| `tempo_votacao_minutos` | inteiro | Obrigatório, **1 a 43200** (maior que zero e no máximo 30 dias) |

**Regras de negócio**

R1 (campos obrigatórios) · R2 (título único, ignorando caixa) · R3 (normalização e tamanho).

O título é normalizado (sem espaços nas bordas) pelo modelo `Pauta`; a validação de tamanho
acontece antes, no `PautaRequestDTO`, sobre o texto recebido.

**Sucesso**

HTTP 201 — header `Location` e corpo com `id`, `titulo` e `tempo_votacao_minutos`.

**Erros**

- `400` — título fora do tamanho (20–150) ou ausente.
- `400` — `tempo_votacao_minutos` ausente, `0`, negativo ou acima de 43200.
- `409` — já existe pauta com o mesmo título, ignorando maiúsculas/minúsculas (a salvaguarda do
  banco `uk_pauta_titulo_lower` preserva esse 409 mesmo em corrida).

---

## `GET /pautas` — Listar pautas

**Propósito**

Retornar todas as pautas criadas.

**Sucesso**

HTTP 200 — lista de pautas (`id`, `titulo`, `tempo_votacao_minutos`). Não há indicador de sessão:
quem precisa saber se a pauta já possui sessão cruza com `GET /sessoes`.

**Erros**

Nenhum tratamento de erro de negócio específico.

---

## `POST /sessoes` — Abrir sessão de votação

**Propósito**

Abrir a votação de uma pauta existente, pelo tempo definido na própria pauta.

**Entrada**

| Campo     | Tipo   | Regra                          |
| --------- | ------ | ------------------------------ |
| `id_pauta` | inteiro | Obrigatório; identificador da pauta-alvo |

**Regras de negócio**

R4 (pauta deve existir) · R5 (uma sessão por pauta) · R6 (duração definida pela pauta).

Fluxo executado: procura a pauta; recusa se a pauta já tiver sessão; obtém `CURRENT_TIMESTAMP` do
banco; define `started_at` como esse horário e `expires_at` como `started_at + duração da pauta`;
persiste a sessão.

**Sucesso**

HTTP 201 — header `Location` e corpo com `id`, `id_pauta`, `titulo_pauta`, `started_at`,
`expires_at` e `is_open`. Na criação, o controller informa `is_open=true`; consultas posteriores
calculam o estado pelo relógio do banco (`agora < expires_at`).

**Erros**

- `400` — `id_pauta` ausente ou nulo (validação de entrada).
- `400` — a pauta não existe.
- `409` — a pauta já possui sessão ("Já existe uma sessão para a pauta: N"); a salvaguarda do
  banco `uk_sessao_pauta` preserva esse 409 mesmo em corrida.

---

## `GET /sessoes` — Listar sessões

**Propósito**

Retornar todas as sessões criadas.

**Sucesso**

HTTP 200 — lista de sessões (`id`, `id_pauta`, `titulo_pauta`, `started_at`, `expires_at`,
`is_open`). O valor de `is_open` é calculado na consulta aplicando `now.isBefore(expires_at)` a
cada sessão, com um único `CURRENT_TIMESTAMP` do banco para a lista. Não existe estado
aberto/fechado persistido nem job de fechamento.

**Erros**

Nenhum tratamento de erro de negócio específico.

---

## `POST /votos` — Registrar voto

**Propósito**

Registrar a manifestação de um associado em uma sessão aberta.

**Entrada**

| Campo          | Tipo    | Regra                              |
| -------------- | ------- | ---------------------------------- |
| `id_sessao`      | inteiro | Obrigatório                        |
| `documento`      | texto   | Obrigatório, CPF válido (`@CPF`, aceita pontuação); normalizado para somente dígitos; submetido também a um validador externo fictício |
| `escolha_voto`   | enum    | Obrigatório, `SIM` ou `NAO`        |

**Regras de negócio**

R7/R8 (sessão deve estar aberta) · R9 (CPF válido — formato `@CPF` + validador externo) ·
R10 (um voto por CPF por sessão) · R11 (escolha SIM ou NÃO).

Fluxo executado: normaliza o documento para somente dígitos; localiza a sessão e exige que esteja
aberta segundo o `CURRENT_TIMESTAMP` do banco; submete o documento normalizado ao
`DocumentoValidator`; verifica se já existe voto desse documento na sessão; salva o voto. Formatos
pontuado e não pontuado do mesmo CPF são tratados como o mesmo documento.

**Sucesso**

HTTP 201 — header `Location` e corpo com `id`, `id_sessao`, `id_pauta`, `titulo_pauta`,
`documento` (normalizado, somente dígitos), `escolha_voto`, `started_at` e `expires_at`.

**Erros**

- `400` — sessão inexistente.
- `400` — sessão fechada (o voto não é aceito após a expiração).
- `400` — CPF inválido, campos ausentes, JSON ilegível ou `escolha_voto` diferente de `SIM`/`NAO`
  (falha de desserialização).
- `400` — CPF rejeitado pelo validador externo ("Documento inválido: \<cpf\>").
- `409` — o mesmo CPF **já votou** nesta sessão; a salvaguarda do banco
  `uk_voto_sessao_documento` preserva esse 409 mesmo em corrida.
- `503` — falha no serviço externo de validação de CPF (status 5xx ou indisponibilidade).

---

## `GET /sessoes/{idSessao}/resultado` — Apurar resultado

**Propósito**

Divulgar o resultado de uma sessão já encerrada.

**Entrada**

| Caminho           | Tipo   | Regra                  |
| ----------------- | ------ | ---------------------- |
| `idSessao` (path) | inteiro | Identificador da sessão |

**Regras de negócio**

R12 (sessão deve existir e estar fechada) · R13 (status por maioria simples).

A apuração conta os votos SIM e NÃO da sessão e calcula o total. O status é `SEM_VOTOS` quando o
total é zero, `APROVADA` quando `SIM > NAO`, `REJEITADA` quando `SIM < NAO` e `EMPATE` quando há
votos e as contagens são iguais.

**Sucesso**

HTTP 200 — `id_sessao`, `votos_sim`, `votos_nao`, `total` e `status` (`SEM_VOTOS`, `APROVADA`,
`REJEITADA` ou `EMPATE`).

**Erros**

- `400` — sessão inexistente.
- `400` — sessão ainda aberta (espere a expiração).

---

## Configuração que afeta o comportamento

No perfil padrão, a aplicação usa H2 em memória e o validador fictício usa `https://httpbin.org`,
com timeout de conexão e leitura de 500 ms. A URL e os timeouts são configuráveis por
`app.documento-validator.base-url`, `app.documento-validator.connect-timeout-ms` e
`app.documento-validator.read-timeout-ms`. O perfil de produção usa PostgreSQL e desabilita o
console H2.