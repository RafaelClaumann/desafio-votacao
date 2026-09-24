# Votação API

API REST para gerenciar **pautas de votação** de uma assembleia: criar pautas, abrir sessões
de votação, registrar votos (SIM/NÃO) e apurar resultados após o fechamento da sessão.

> Documentação de negócio e detalhes das regras: ver pasta [`docs/`](docs/).

## Stack e Requisitos

- **Java 21**
- **Maven** (o projeto inclui o wrapper `./mvnw`)
- **Banco em memória (H2)** — os dados são perdidos ao encerrar a aplicação

## Como executar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O console H2 fica disponível em
`http://localhost:8080/h2-console`.

Executar os testes:

```bash
./mvnw test
```

## Convenções

- **Formato dos dados:** JSON em `snake_case`.
- **Identificação do eleitor:** CPF (obrigatório; formato validado — `@CPF` — e submetido a um
  validador externo fictício antes de votar).
- **Escolha do voto:** somente `SIM` ou `NAO`.
- **Tempo de votação:** em **minutos**, definido na criação da pauta.

## Fluxo típico de uso

1. **POST /pautas** — cria a pauta com título e duração (min).
2. **POST /sessoes** — abre a votação da pauta (`pauta_id`). O sistema usa a duração da pauta.
   A sessão fica aberta e **fecha sozinha** ao expirar.
3. **POST /votos** — associados votam SIM/NÃO com CPF enquanto a sessão estiver aberta.
4. **GET /sessoes/{id}/resultado** — apura e divulga o resultado **após o fechamento** da sessão.

---

## Operações da API

### 1. Criar uma pauta

**`POST /pautas`**

Cria um tema a ser deliberado. O `tempo_votacao_minutos` define por quanto tempo a sessão
dessa pauta ficará aberta.

**Body:**

```json
{
  "titulo": "Reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": 10
}
```

| Campo                    | Tipo     | Regra                                      |
| ------------------------ | -------- | ------------------------------------------ |
| `titulo`                 | string   | Obrigatório, **20 a 150 caracteres**       |
| `tempo_votacao_minutos`  | numero   | Obrigatório, **1 a 43200** (até 30 dias) |

**curl:**

```bash
curl -X POST http://localhost:8080/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Reforma estatutária do capítulo quatro","tempo_votacao_minutos":10}'
```

**Resposta 201 (Created):**

```json
{
  "id": 1,
  "titulo": "Reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": 10
}
```

**Erros:**

- `400` — título fora do tamanho (20–150) ou ausente.
- `400` — `tempo_votacao_minutos` **0 ou negativo** ("O tempo de votação deve ser maior que zero"),
  ou ausente.
- `400` — `tempo_votacao_minutos` **acima de 43200** ("O tempo de votação não pode exceder
  43200 minutos (30 dias)").
- `409` — já existe uma pauta com o mesmo título (ignorando maiúsculas/minúsculas).

> Os valores `0` ou negativos são **recusados** na criação da pauta (`400`), evitando que a
> sessão dessa pauta nasça já fechada e inutilize o tema. Valores acima de **43200 minutos
> (30 dias)** também são recusados (`400`), evitando que o cálculo de expiração da sessão
> estoure o intervalo de datas suportado.

---

### 2. Listar pautas

**`GET /pautas`**

Retorna todas as pautas criadas.

**curl:**

```bash
curl http://localhost:8080/pautas
```

**Resposta 200 (OK):**

```json
[
  {
    "id": 1,
    "titulo": "Reforma estatutária do capítulo quatro",
    "tempo_votacao_minutos": 10
  }
]
```

---

### 3. Abrir uma sessão de votação

**`POST /sessoes`**

Abre a votação de uma pauta existente. O período de votação é `agora → agora + duração da pauta`.
Uma pauta só pode ter **uma única sessão** (mesmo após o fechamento).

**Body:**

```json
{
  "pauta_id": 1
}
```

**curl:**

```bash
curl -X POST http://localhost:8080/sessoes \
  -H 'Content-Type: application/json' \
  -d '{"pauta_id":1}'
```

**Resposta 201 (Created):**

```json
{
  "id": 1,
  "id_pauta": 1,
  "titulo_pauta": "Reforma estatutária do capítulo quatro",
  "started_at": "2026-09-23T10:00:00",
  "expires_at": "2026-09-23T10:10:00",
  "is_open": true
}
```

`is_open` é calculado no momento da resposta: `true` enquanto `agora < expires_at`.
`titulo_pauta` traz o título da pauta votada, para facilitar a leitura da sessão.

**Erros:**

- `400` — `pauta_id` **ausente ou nulo** ("O id da Pauta é obrigatório").
- `400` — a pauta não existe.
- `409` — a pauta já possui sessão ("Já existe uma sessão para a pauta: N"); consulte a
  pauta/lista de sessões antes de tentar abrir.

---

### 4. Listar sessões

**`GET /sessoes`**

Retorna todas as sessões criadas, com o estado no momento da consulta e o título da pauta.

**curl:**

```bash
curl http://localhost:8080/sessoes
```

**Resposta 200 (OK):**

```json
[
  {
    "id": 1,
    "id_pauta": 1,
    "titulo_pauta": "Reforma estatutária do capítulo quatro",
    "started_at": "2026-09-23T10:00:00",
    "expires_at": "2026-09-23T10:10:00",
    "is_open": false
  }
]
```

---

### 5. Registrar um voto

**`POST /votos`**

Registra o voto de um associado em uma sessão. **Somente enquanto a sessão estiver aberta** e
**apenas um voto por CPF por sessão**.

**Body:**

```json
{
  "id_sessao": 1,
  "documento": "12345678909",
  "escolha_voto": "SIM"
}
```

| Campo          | Tipo    | Regra                                              |
| -------------- | ------- | -------------------------------------------------- |
| `id_sessao`    | numero  | Obrigatório — identificador da sessão              |
| `documento`    | string  | Obrigatório — **CPF válido**                       |
| `escolha_voto` | string  | Obrigatório — somente `SIM` ou `NAO`               |

**curl:**

```bash
curl -X POST http://localhost:8080/votos \
  -H 'Content-Type: application/json' \
  -d '{"id_sessao":1,"documento":"12345678909","escolha_voto":"SIM"}'
```

**Resposta 201 (Created):**

```json
{
  "id_sessao": 1,
  "documento": "12345678909",
  "escolha_voto": "SIM"
}
```

**Erros:**

- `400` — sessão inexistente.
- `400` — sessão fechada (o voto não é aceito após a expiração).
- `400` — CPF inválido, campos ausentes ou `escolha_voto` diferente de `SIM`/`NAO`.
- `400` — CPF rejeitado pelo validador externo (integração fictícia, resultado aleatório).
- `409` — o mesmo CPF **já votou** nesta sessão.
- `503` — falha no serviço externo de validação de CPF (timeout de 500 ms).

> **Atenção:** antes de validar e gravar, o sistema **normaliza o CPF para somente dígitos**.
> Os formatos com e sem pontuação são tratados como o **mesmo documento**.

---

### 6. Apurar o resultado de uma sessão

**`GET /sessoes/{idSessao}/resultado`**

Apura **somente sessões fechadas** (após a expiração). Enquanto a sessão está aberta, o
resultado não é divulgado.

**curl:**

```bash
curl http://localhost:8080/sessoes/1/resultado
```

**Resposta 200 (OK):**

```json
{
  "id_sessao": 1,
  "votos_sim": 3,
  "votos_nao": 2,
  "total": 5,
  "status": "APROVADA"
}
```

| Campo       | Tipo     | Descrição                                    |
| ----------- | -------- | -------------------------------------------- |
| `votos_sim` | numero   | Total de votos SIM                           |
| `votos_nao` | numero   | Total de votos NAO                           |
| `total`     | numero   | Votos SIM + NAO                              |
| `status`    | string   | `SEM_VOTOS` (nenhum voto) · `APROVADA` (SIM > NAO) · `REJEITADA` (SIM < NAO) · `EMPATE` (empate com votos) |

**Erros:**

- `400` — sessão inexistente.
- `400` — sessão ainda **aberta** (espere a expiração).

> **Atenção:** uma sessão fechada sem nenhum voto retorna `SEM_VOTOS` (0 × 0), distinto
> do `EMPATE` real.

---

## Formato de erro

Toda resposta de erro segue o formato:

```json
{
  "timestamp": "2026-09-23T10:00:01",
  "status": 400,
  "error": "Bad Request",
  "message": "mensagem do erro",
  "path": "/votos",
  "field_errors": [
    { "field": "documento", "message": "O documento é obrigatório" }
  ]
}
```

- `field_errors` é preenchido apenas em erros de validação (`400`).
- Erros de negócio retornam 400/409 (ver descrito em cada operação); falhas não previstas
  retornam `500`.

## Resumo das regras de negócio

| # | Regra |
| - | ----- |
| 1 | Título da pauta obrigatório (20–150 caracteres) e **único**. |
| 2 | A sessão só pode ser aberta para uma pauta **existente**. |
| 3 | Uma pauta pode ter **no máximo uma** sessão de votação. |
| 4 | A duração da sessão é definida pela pauta (em minutos). |
| 5 | Votos são aceitos **somente enquanto a sessão estiver aberta**. |
| 6 | Ao expirar, a sessão fecha e **nunca reabre**. |
| 7 | **Um CPF vota uma única vez por sessão.** |
| 8 | Voto somente `SIM` ou `NAO`. |
| 9 | Resultado apurado **apenas para sessões fechadas**. |
| 10 | Status: `SEM_VOTOS` / `APROVADA` / `REJEITADA` / `EMPATE` conforme a participação e a maioria simples. |

Detalhes completos: [`docs/regras-de-negocio.md`](docs/regras-de-negocio.md).

## Exemplo completo (passo a passo)

```bash
# 1. Criar pauta com 10 minutos de votação
curl -X POST http://localhost:8080/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Aprovação do orçamento anual do exercício vigente","tempo_votacao_minutos":10}'

# 2. Abrir a sessão (guardar o "id" retornado, ex.: 1)
curl -X POST http://localhost:8080/sessoes \
  -H 'Content-Type: application/json' \
  -d '{"pauta_id":1}'

# 3. Votar SIM e NAO (com CPFs distintos)
curl -X POST http://localhost:8080/votos \
  -H 'Content-Type: application/json' \
  -d '{"id_sessao":1,"documento":"12345678909","escolha_voto":"SIM"}'
curl -X POST http://localhost:8080/votos \
  -H 'Content-Type: application/json' \
  -d '{"id_sessao":1,"documento":"98765432100","escolha_voto":"NAO"}'

# 4. Consultar sessões para acompanhar abertura
curl http://localhost:8080/sessoes

# 5. Após o tempo expirar, apurar o resultado
curl http://localhost:8080/sessoes/1/resultado
```