# Estados e Ciclo de Vida

## Estados da Sessão de Votação

A sessão possui **dois estados de negócio**, que **não são persistidos** nem controlados por
agendador: são **derivados do tempo** no momento da consulta.

```
ABERTA
   │   (o horário atual atinge os horário de expiração)
   ▼
FECHADA   (terminal)
```

### Regras de transição

| Item                     | Descrição                                                                                       |
| ------------------------ | ----------------------------------------------------------------------------------------------- |
| Estado inicial           | **ABERTA** no momento da criação, desde que a duração da pauta seja positiva (expiração futura). |
| Entrada em ABERTA        | Somente pela criação da sessão (não existe sessão "agendada" — a sessão já nasce valendo).       |
| Transição ABERTA → FECHADA | Ocorre automaticamente quando `agora >= expiresAt`. No instante exato da expiração a sessão já está fechada. |
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
  criada para ela (o serviço `PautaService.pautaComStatuses()` calcula esse indicador, mas ele
  **não é exposto por nenhum endpoint** da API).
- **Voto**: não possui estados; é um registro permanente e imutável.
- **Resultado**: não é um estado — é um valor calculado (APROVADA, REJEITADA, EMPATE) a partir
  da contagem de votos de uma sessão fechada.

## Invariantes de Negócio

Condições que **sempre devem ser verdadeiras**. Se violadas, o sistema recusa a operação
ou o banco impede a gravação.

| # | Invariante                                                                  | Onde é garantida                                                                                                        | Consequência se violada                                           |
| - | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| I1 | Toda pauta tem título e duração de votação preenchidos.                     | `PautaEntity` (colunas `NOT NULL`) + validação `PautaRequestDTO`.                                                       | Criação recusada (400).                                           |
| I2 | O título da pauta é único.                                                  | `PautaService` (verificação) + constraint `UNIQUE(titulo)` no banco.                                                    | Criação recusada (409).                                           |
| I3 | Toda sessão pertence a uma pauta existente.                                 | FK `sessoes.pauta_id → pautas.id` + verificação em `SessaoService`.                                                    | Criação recusada (400).                                           |
| I4 | Uma pauta não possui mais de uma sessão.                                    | Verificação em `SessaoService` + índice único `uk_sessao_pauta` no banco.                                               | Nova sessão recusada (observação: mapeada como 500 — ver erros).  |
| I5 | A sessão é válida por exatamente a duração definida na pauta.               | `SessaoService.saveSessao()` (`expiresAt = startedAt + duração`).                                                       | —                                                                 |
| I6 | Toda sessão possui início e expiração preenchidos.                          | `SessaoEntity` (colunas `NOT NULL`).                                                                                    | —                                                                 |
| I7 | Votos só existem vinculados a uma sessão existente.                         | FK `votos.sessao_id → sessoes.id`.                                                                                      | Voto recusado (400), pois a sessão é validada antes.              |
| I8 | No máximo um voto por (sessão, documento).                                  | Verificação em `VotoService` + constraint única `UNIQUE(sessao_id, documento)`.                                         | Voto recusado (409).                                              |
| I9 | Todo voto é SIM ou NÃO.                                                     | Enum `Voto.Escolha`.                                                                                                    | Requisição recusada (400) na desserialização.                     |
| I10 | Sessão fechada nunca volta a aceitar votos.                                 | `Sessao.isOpen(now)` compara com o horário atual a cada operação.                                                       | Votos rejeitados (400); não há mecanismo de reabertura.           |
| I11 | O resultado de uma sessão aberta nunca é divulgado.                         | `SessaoService.getClosedSessaoById()` antes da apuração.                                                                | Apuração recusada (400) enquanto aberta.                          |

## Condições sempre válidas na criação

- `startedAt` da sessão = momento da criação.
- `expiresAt` = `startedAt` + duração em minutos da pauta.
- A duração da pauta está sempre no intervalo **1 a 43200 minutos** (validação `@Positive` +
  `@Max(43200)` no `PautaRequestDTO`), portanto `expiresAt` nunca excede o intervalo
  representável pelo `LocalDateTime`.