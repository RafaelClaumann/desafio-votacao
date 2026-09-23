# Operações da API

A API REST expõe as operações de negócio descritas nos fluxos. Os campos JSON seguem o padrão
`snake_case` (configuração `spring.jackson.property-naming-strategy=SNAKE_CASE`).

## `POST /pautas` — Criar pauta

**Propósito**

Registrar um novo tema a ser deliberado, com o tempo de votação em minutos.

**Entrada**

| Campo                 | Tipo   | Regra                                              |
| --------------------- | ------ | -------------------------------------------------- |
| `titulo`              | texto  | Obrigatório, 20–150 caracteres, esp. das bordas removidos |
| `tempo_votacao_minutos` | inteiro | Obrigatório (sem validação de positividade)     |

**Regras de negócio**

R1 (campos obrigatórios) · R2 (título único) · R3 (normalização e tamanho).

**Sucesso**

HTTP 201 — pauta criada, com `id`, `titulo` e `tempo_votacao_minutos`.

**Erros**

Título inválido → 400. Título duplicado (ignorando caixa) → 409.

---

## `GET /pautas` — Listar pautas

**Propósito**

Retornar todas as pautas criadas.

**Sucesso**

HTTP 200 — lista de pautas (`id`, `titulo`, `tempo_votacao_minutos`).

**Erros**

Nenhum tratamento de erro de negócio específico.

---

## `POST /sessoes` — Abrir sessão de votação

**Propósito**

Abrir a votação de uma pauta existente, pelo tempo definido na própria pauta.

**Entrada**

| Campo     | Tipo   | Regra                          |
| --------- | ------ | ------------------------------ |
| `pauta_id` | inteiro | Identificador da pauta-alvo |

**Regras de negócio**

R4 (pauta deve existir) · R5 (uma sessão por pauta) · R6 (duração definida pela pauta).

**Sucesso**

HTTP 201 — sessão criada, com `id`, `id_pauta`, `started_at`, `expires_at` e `is_open`
(calculado no momento da resposta).

**Erros**

Pauta inexistente → 400. Pauta já possui sessão → em condições normais dispara
`IllegalArgumentException` não mapeada → **500** (ver [erros-e-rejeicoes.md](erros-e-rejeicoes.md#cenários-adicionais)).

---

## `GET /sessoes` — Listar sessões

**Propósito**

Retornar todas as sessões criadas.

**Sucesso**

HTTP 200 — lista de sessões (`id`, `id_pauta`, `started_at`, `expires_at`, `is_open`).
O campo `is_open` reflete o estado no momento da consulta.

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
| `documento`      | texto   | Obrigatório, CPF válido            |
| `escolha_voto`   | enum    | Obrigatório, `SIM` ou `NAO`        |

**Regras de negócio**

R7/R8 (sessão deve estar aberta) · R9 (CPF válido) · R10 (um voto por CPF por sessão) ·
R11 (escolha SIM ou NÃO).

**Sucesso**

HTTP 201 — voto registrado; o corpo reflete `id_sessao`, `documento` e `escolha_voto`.

**Erros**

Sessão inexistente → 400. Sessão fechada → 400. CPF inválido / campos ausentes → 400.
Escolha diferente de SIM/NAO → 400 (falha de desserialização). CPF já votou nesta sessão → 409.

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

**Sucesso**

HTTP 200 — `id_sessao`, `votos_sim`, `votos_nao`, `total` e `status`
(`APROVADA`, `REJEITADA` ou `EMPATE`).

**Erros**

Sessão inexistente → 400. Sessão ainda aberta → 400.