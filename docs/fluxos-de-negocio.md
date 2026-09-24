# Fluxos de Negócio

## Fluxo 1 — Criar Pauta

Objetivo: registrar o tema que será deliberado e o tempo de votação.

1. O organizador informa o **título** e o **tempo de votação em minutos**.
2. O sistema valida a entrada: título obrigatório com 20 a 150 caracteres, duração obrigatória
   entre 1 e 43200 minutos (até 30 dias).
3. O sistema normaliza o título (remove espaços nas bordas).
4. O sistema verifica se **já existe outra pauta com o mesmo título** (ignorando caixa).
5. Se não houver duplicidade, a pauta é persistida (o banco reforça a unicidade do título).
6. A pauta recebe um identificador e está pronta para receber uma sessão de votação.

Resultado: pauta criada (HTTP 201).

Rejeições possíveis: título inválido (400) · duração fora do intervalo 1–43200 (400) · título duplicado (409).

---

## Fluxo 2 — Abrir Sessão de Votação para uma Pauta

Objetivo: abrir o período de coleta de votos de uma pauta existente.

1. O organizador informa o identificador da **pauta**.
2. O sistema verifica se a pauta **existe**.
3. O sistema verifica se a pauta **ainda não possui** uma sessão de votação.
4. O sistema cria a sessão com **início = agora** (relógio do banco) e **expiração = agora +
   duração da pauta**.
5. A sessão é persistida (o banco impede mais de uma sessão por pauta).
6. A sessão fica **aberta** e passa a aceitar votos (desde que a duração seja positiva).

Resultado: sessão criada (HTTP 201).

Rejeições possíveis: `id_pauta` ausente/nulo (400) · pauta inexistente (400) · pauta já possui
sessão (409 — ver [erros-e-rejeicoes.md](erros-e-rejeicoes.md)).

---

## Fluxo 3 — Registrar um Voto

Objetivo: registrar a manifestação de um associado em uma sessão aberta.

1. O associado informa o identificador da **sessão**, o **CPF** e a **escolha** (SIM/NÃO).
2. O sistema valida a entrada: sessão obrigatória, CPF bem-formado, escolha obrigatória e
   somente SIM ou NÃO.
3. O sistema verifica se a **sessão existe**.
4. O sistema verifica se a **sessão está aberta** (pelo relógio do banco, a expiração ainda
   não foi alcançada).
5. O sistema consulta o **validador externo de CPF** (integração fictícia/aleatória) com o
   documento normalizado; documento rejeitado encerra o fluxo (HTTP 400).
6. O sistema verifica se o **mesmo CPF já votou nesta sessão**.
7. O voto é persistido (o banco reforça a unicidade por sessão/documento).
8. O voto passa a integrar a apuração futura da sessão.

Resultado: voto registrado (HTTP 201).

Rejeições possíveis: sessão inexistente (400) · sessão fechada (400) · CPF/sessão/escolha
inválidos (400) · CPF rejeitado pelo validador externo (400) · CPF já votou nesta sessão (409) ·
falha no serviço externo de validação (503).

---

## Fluxo 4 — Apurar Resultado de uma Sessão

Objetivo: divulgar o resultado de uma sessão já encerrada.

1. O organizador informa o identificador da **sessão**.
2. O sistema verifica se a **sessão existe**.
3. O sistema verifica se a **sessão está fechada** (pelo relógio do banco, a expiração já foi
   alcançada).
4. O sistema conta os votos **SIM** e os votos **NÃO** registrados na sessão.
5. O sistema calcula o **total de votos** e o **status**:
   - nenhum voto → **SEM_VOTOS**
   - SIM > NÃO → **APROVADA**
   - SIM < NÃO → **REJEITADA**
   - SIM = NÃO (com ao menos um voto) → **EMPATE**
6. O resultado é retornado ao organizador.

Resultado: resposta com totais e status (HTTP 200).

Rejeições possíveis: sessão inexistente (400) · sessão ainda aberta (400).

---

## Fluxo 5 — Consultar Pautas e Sessões

Objetivo: visualizar as pautas e sessões existentes.

1. **Listar pautas**: o sistema retorna todas as pautas criadas, com título e duração.
2. **Listar sessões**: o sistema retorna todas as sessões criadas, com a pauta relacionada
   (id e título), os horários de início/expiração e o indicador de aberta/fechada no momento da
   consulta (calculado pelo relógio do banco, consistente para todas as instâncias).

Não há restrição de negócio para consulta — são listas simples de leitura.
