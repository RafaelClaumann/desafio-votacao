# Estados e Ciclo de Vida

## Estados da Sessão de Votação

A sessão possui **dois estados de negócio**, que **não são persistidos** nem controlados por
agendador: são **derivados do tempo** no momento da consulta, usando como "agora" o relógio do
banco de dados (`CURRENT_TIMESTAMP`), único e comum a todas as instâncias (PF9 resolvido).

```
ABERTA
   │   (o relógio do banco atinge a expiração)
   ▼
FECHADA   (terminal)
```

### Regras de transição

| Item                     | Descrição                                                                                       |
| ------------------------ | ----------------------------------------------------------------------------------------------- |
| Estado inicial           | **ABERTA** no momento da criação, desde que a duração da pauta seja positiva (expiração futura). |
| Entrada em ABERTA        | Somente pela criação da sessão (não existe sessão "agendada" — a sessão já nasce valendo).       |
| Transição ABERTA → FECHADA | Ocorre automaticamente quando o relógio do banco atinge `expiresAt` (`expires_at > CURRENT_TIMESTAMP` deixa de ser verdade). No instante exato da expiração a sessão já está fechada. |
| Retorno FECHADA → ABERTA | Não existe. **FECHADA é terminal.**                                                             |
| Duração 0 ou negativa    | A sessão nasce **fechada** (nunca chega a ficar aberta de fato).                                 |

### Operações conforme o estado

| Operação                 | ABERTA        | FECHADA       |
| ------------------------ | ------------- | ------------- |
| Registrar voto (R7/R8)   | Permitido     | Recusado (400) |
| Apurar resultado (R12)   | Recusado (400) | Permitido     |
| Reabrir / encerrar         | Não existe operação | Não existe operação |

### Significado de negócio

- **ABERTA** = o período de coleta de votos está em andamento; a pauta ainda pode receber
  manifestações. O resultado ainda não é divulgado.
- **FECHADA** = o período de coleta terminou; votos não são mais aceitos e o resultado pode
  ser apurado e divulgado.

## Estados de outros conceitos

- **Pauta**: não possui estado explícito. A única informação derivada é se uma sessão já foi
  criada para ela — esse indicador não é exposto por endpoint próprio; o consumidor cruza
  `GET /pautas` com `GET /sessoes`.
- **Voto**: não possui estados; é um registro permanente e imutável.
- **Resultado**: não é um estado — é um valor calculado (SEM_VOTOS, APROVADA, REJEITADA,
  EMPATE) a partir da contagem de votos de uma sessão fechada.

## Ciclo de vida da notificação de voto

A notificação de voto (R14/R15) tem um ciclo curto, fora da transação do voto:

```
VOTO GRAVADO (commit confirmado)
   │
   ├─ (falha no envio) ──▶ NOTIFICAÇÃO PERDIDA ──▶ [registro apenas em log]
   │
   ▼
NOTIFICAÇÃO PUBLICADA em `voto-topic`
```

| Item | Descrição |
| ---- | --------- |
| Estado inicial | Não existe — a notificação só é criada a partir de um voto gravado. |
| Entrada em PUBLICADA | Ocorre **após o commit** da transação do voto (`TransactionPhase.AFTER_COMMIT`). |
| Transição para PERDIDA | Falha no envio ao broker (broker indisponível, timeout, erro de serialização) ou **exceção síncrona** no envio — capturada pela aplicação e registrada em log `ERROR` com `idVoto`, `idSessao` e `correlationId`, sem chegar ao chamador (PF16 resolvido). Não há retentativa. |
| Retorno de PERDIDA para PUBLICADA | Não existe. |
| Terminal | **PUBLICADA** e **PERDIDA** são terminais; o voto registrado **nunca** é desfeito por causa da notificação. |

> O estado "notificação pendente" é efêmero: entre o commit e a tentativa de envio, a notificação
> só existe na memória da aplicação. Não há **outbox** nem persistência da notificação
> (ver [PF15](pontos-fracos-e-falhas.md#pf15)).

## Invariantes de Negócio

Condições que **sempre devem ser verdadeiras**. Se violadas, o sistema recusa a operação
ou o banco impede a gravação.

| # | Invariante                                                                  | Onde é garantida                                                                                                        | Consequência se violada                                           |
| - | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| I1 | Toda pauta tem título e duração de votação preenchidos.                     | `PautaEntity` (colunas `NOT NULL`) + validação `PautaRequestDTO`.                                                       | Criação recusada (400).                                           |
| I2 | O título da pauta é único (ignorando caixa).                                | `PautaService` (verificação) + índice único `uk_pauta_titulo_lower` sobre `LOWER(titulo)` no banco.                        | Criação recusada (409).                                           |
| I3 | Toda sessão pertence a uma pauta existente.                                 | FK `sessoes.id_pauta → pautas.id` + verificação em `SessaoService`.                                                    | Criação recusada (400).                                           |
| I4 | Uma pauta não possui mais de uma sessão.                                    | Verificação em `SessaoService` + índice único `uk_sessao_pauta` no banco.                                               | Nova sessão recusada (409).                                  |
| I5 | A sessão é válida por exatamente a duração definida na pauta.               | `SessaoService.saveSessao()` (`expiresAt = startedAt + duração`).                                                       | —                                                                 |
| I6 | Toda sessão possui início e expiração preenchidos.                          | `SessaoEntity` (colunas `NOT NULL`).                                                                                    | —                                                                 |
| I7 | Votos só existem vinculados a uma sessão existente.                         | FK `votos.sessao_id → sessoes.id`.                                                                                      | Voto recusado (400), pois a sessão é validada antes.              |
| I8 | No máximo um voto por (sessão, documento).                                  | Verificação em `VotoService` + constraint única `UNIQUE(sessao_id, documento)`.                                         | Voto recusado (409).                                              |
| I9 | Todo voto é SIM ou NÃO.                                                     | Enum `Voto.Escolha`.                                                                                                    | Requisição recusada (400) na desserialização.                     |
| I10 | Sessão fechada nunca volta a aceitar votos.                                 | `Sessao.isOpen(now)` (regra no domínio) com `now` de `TimeProvider.now()`, a cada operação.                             | Votos rejeitados (400); não há mecanismo de reabertura.           |
| I11 | O resultado de uma sessão aberta nunca é divulgado.                         | `SessaoService.getClosedSessaoById()` antes da apuração.                                                                | Apuração recusada (400) enquanto aberta.                          |
| I12 | Uma notificação de voto só é publicada para um voto **já confirmado** no banco. | `VotoEventKafkaSender` com `@TransactionalEventListener(phase = AFTER_COMMIT)`.                                          | Transação revertida → **nenhuma** notificação é publicada.        |
| I13 | A falha da notificação **não** desfaz o voto registrado **nem** altera a resposta da API. | Envio fora da transação; a falha assíncrona é registrada em log (`ERROR`) e a exceção síncrona é capturada pelo Spring (`TransactionSynchronizationUtils.invokeAfterCompletion`). | Voto permanece registrado e apurável; a API responde **201**. |

## Condições sempre válidas na criação

- `startedAt` da sessão = momento da criação.
- `expiresAt` = `startedAt` + duração em minutos da pauta.
- A duração da pauta está sempre no intervalo **1 a 43200 minutos** (validação `@Positive` +
  `@Max(43200)` no `PautaRequestDTO`), portanto `expiresAt` nunca excede o intervalo
  representável pelo `LocalDateTime`.
- `publishedAt` da notificação de voto = instante colhido do **relógio do banco** no momento do
  registro do voto (mesma autoridade de hora usada para o estado da sessão).