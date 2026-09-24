# Documentação de Negócio — Sistema de Votação

Esta documentação descreve o **comportamento de negócio** implementado pela aplicação,
baseando-se exclusivamente no código-fonte atual. O foco é explicar **o que o sistema faz
e por quê**, e não apenas como o código está organizado.

## Visão Geral do Sistema

O sistema gerencia **pautas de votação** (pautas) de uma assembleia. Uma pauta representa
um tema/assunto que será deliberado por meio de votação associada. O fluxo geral é:

1. Um organizador cria uma **pauta**, informando o título e o tempo de votação em minutos
   (entre 1 e 43200 — até 30 dias).
2. Um organizador abre uma **sessão de votação** para a pauta. A sessão fica aberta pelo
   tempo definido na pauta e, ao fim, fecha-se automaticamente (sem intervenção externa).
3. Durante a janela em que a sessão está aberta, associados (identificados por **CPF**)
   registram **votos** do tipo SIM ou NÃO.
4. Após o fechamento da sessão, o sistema apura o **resultado**: total de votos SIM,
   total de votos NÃO e o status (sem votos, aprovada, rejeitada ou empate).

Os estados da sessão (aberta/fechada) **não são armazenados**: são derivados em tempo de
execução pela comparação da expiração com o **relógio do banco de dados** (`CURRENT_TIMESTAMP`,
única autoridade de hora do sistema — consistente para todas as instâncias).

## Conceitos de Negócio

| Conceito           | Significado                                                                                                |
| ------------------ | ---------------------------------------------------------------------------------------------------------- |
| **Pauta**          | Assunto/tema a ser deliberado. Possui título e tempo de votação em minutos.                                |
| **Sessão de votação** | Período de tempo em que uma pauta específica pode receber votos. Pertence a exatamente uma pauta.       |
| **Voto**           | Manifestação de um associado em uma sessão. É do tipo **SIM** ou **NÃO**. O associado é identificado por CPF. |
| **Resultado da votação** | Apuração dos votos de uma sessão já fechada: total SIM, total NÃO e status (SEM_VOTOS, APROVADA, REJEITADA, EMPATE). |

### Relacionamentos entre conceitos

- Uma **Pauta** pode ter **no máximo uma Sessão** de votação em toda a sua existência.
- Uma **Sessão** pertence a **uma única Pauta** (relação 1 para 1 no banco).
- Um **Voto** pertence a **uma única Sessão**.
- Uma Sessão pode receber **vários Votos**, mas **no máximo um por CPF**.
- O **Resultado** é derivado dos votos de uma Sessão.

## Resumo das Regras de Negócio

| #   | Regra de Negócio                                                        |
| --- | ----------------------------------------------------------------------- |
| R1  | Uma pauta só pode ser criada com título e duração de votação informados. |
| R2  | O título da pauta é único; títulos duplicados impedem a criação.        |
| R3  | O título da pauta é normalizado (sem espaços nas bordas) e deve ter entre 20 e 150 caracteres. |
| R4  | Uma sessão só pode ser aberta para uma pauta existente.                 |
| R5  | Uma pauta pode ter no máximo uma sessão de votação.                     |
| R6  | A duração da sessão é definida pela pauta (início = criação; fim = início + duração). |
| R7  | Uma sessão aceita votos apenas enquanto estiver aberta.                 |
| R8  | Ao atingir a expiração, a sessão fecha e nunca mais aceita votos.       |
| R9  | Somente associados com CPF válido podem votar (formato `@CPF` + consulta a um validador externo fictício). |
| R10 | Cada CPF pode votar apenas uma vez por sessão.                          |
| R11 | O voto só pode ser SIM ou NÃO.                                          |
| R12 | O resultado só pode ser apurado para uma sessão já fechada.             |
| R13 | O status do resultado é definido pela maioria: SEM_VOTOS (nenhum voto), SIM > NÃO (APROVADA), SIM < NÃO (REJEITADA), empate com votos (EMPATE). |

## Estrutura Desta Documentação

| Documento                                        | Conteúdo                                                        |
| ------------------------------------------------ | --------------------------------------------------------------- |
| [regras-de-negocio.md](regras-de-negocio.md)     | Regras de negócio detalhadas (condição, comportamento, violação, implementação). |
| [pontos-fracos-e-falhas.md](pontos-fracos-e-falhas.md) | Pontos fracos e modos de falha (ex.: sessão com tempo negativo). |
| [estados-e-invariantes.md](estados-e-invariantes.md) | Ciclo de vida da sessão e invariantes de negócio.              |
| [fluxos-de-negocio.md](fluxos-de-negocio.md)     | Principais fluxos de negócio, passo a passo.                    |
| [api-operacoes.md](api-operacoes.md)             | Operações da API REST e como acionam as regras de negócio.      |
| [erros-e-rejeicoes.md](erros-e-rejeicoes.md)     | Motivos de rejeição e mapeamento de erros HTTP.                 |
| [traceabilidade.md](traceabilidade.md)           | Mapa regra → implementação, índices do banco e observações do código. |
| [decisao-relogio-da-sessao.md](decisao-relogio-da-sessao.md) | Decisão técnica: por que a "hora atual" da sessão vem do relógio do banco (contexto, alternativas e trade-offs). |

## Observações Importantes

- O sistema **não possui limite de sessões abertas simultaneamente** nem conceito de
  "sessão agendada": ao ser criada, a sessão já começa válida e aberta.
- Não há **quórum mínimo**: o status (aprovada/rejeitada) depende apenas da comparação
  entre votos SIM e NÃO registrados. Uma sessão fechada sem nenhum voto resulta em **SEM_VOTOS**
  (0 × 0), distinto do **EMPATE** real (PF10 resolvido).
- Não existe conceito de abstenção: todo voto registrado é obrigatoriamente SIM ou NÃO.
- A validação do CPF no voto combina o **formato** (`@CPF`) com uma consulta a um **validador
  externo fictício** (`DocumentoValidatorClient`, via `https://httpbin.org`), cujo resultado é
  **aleatório** (200/400/404/500) — não confirma a titularidade do CPF (PF12). Status **4xx**
  do serviço recusa o voto (400); **falha** (5xx ou indisponibilidade, com timeout de 500 ms)
  gera **503**.
- A duração de votação **é validada como valor positivo** na criação da pauta; valores `0` ou
  negativos são recusados com HTTP 400 (ver R6 em [regras-de-negocio.md](regras-de-negocio.md)).