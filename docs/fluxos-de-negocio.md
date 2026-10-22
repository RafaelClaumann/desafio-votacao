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
   documento normalizado; documento rejeitado encerra o fluxo (HTTP 400). **Com a validação externa
   desligada (`app.documento-validator.is-enabled=false`, padrão) esta consulta não é executada** —
   o documento é aceito com base apenas no formato (`@CPF`).
6. O sistema verifica se o **mesmo CPF já votou nesta sessão**.
7. O voto é persistido (o banco reforça a unicidade por sessão/documento).
8. O sistema prepara a **notificação do voto** (pauta, sessão, voto gravado e o instante de
   publicação, colhido do relógio do banco) e a publica como evento da aplicação.
9. A transação é **confirmada**; só então a notificação é enviada ao tópico Kafka
   `voto-topic` (ver [Fluxo 6](#fluxo-6--notificar-o-voto-registrado-kafka)).
10. O voto passa a integrar a apuração futura da sessão.

Resultado: voto registrado (HTTP 201).

Rejeições possíveis: sessão inexistente (400) · sessão fechada (400) · CPF/sessão/escolha
inválidos (400) · CPF rejeitado pelo validador externo (400, apenas com a validação externa
habilitada) · CPF já votou nesta sessão (409) · falha no serviço externo de validação (503,
apenas com a validação externa habilitada).

> A notificação do passo 9 é **best-effort**: se o broker estiver indisponível, a falha é
> registrada em log e o **voto permanece registrado** — a resposta ao associado continua sendo
> **201**.

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

---

## Fluxo 6 — Notificar o Voto Registrado (Kafka)

Objetivo: avisar os sistemas consumidores que um voto foi registrado, para que acompanhem a
apuração sem consultar a API.

1. O **Fluxo 3** grava o voto e publica, dentro da transação, um evento de aplicação com a
   **notificação**: pauta, sessão, voto gravado e o instante de publicação (`published_at`,
   colhido do relógio do banco).
2. A transação do voto é **confirmada** (`commit`). Somente agora o evento é consumido pelo
   listener — se a transação for revertida, **nada é publicado**.
3. O listener converte a notificação no **payload** de Kafka (`VotoEvent`), convertendo os
   instantes para `ISO_LOCAL_DATE_TIME` e o enum de escolha para `SIM`/`NAO`.
4. O produtor anexa o header **`X-Correlation-Id`** com o identificador de correlação da
   requisição que registrou o voto.
5. A mensagem é publicada no tópico **`voto-topic`**, com a **chave = identificador da sessão**
   (todos os votos de uma sessão vão para a mesma partição, preservando a ordem).
6. O resultado do envio é registrado em log: sucesso (`INFO` — id do voto, timestamp e partição)
   ou falha (`ERROR`).

Resultado: mensagem publicada em `voto-topic`, disponível para os consumidores do tópico.

Falhas possíveis:

- **Broker indisponível** — a falha é registrada em log (`ERROR` — "Failed to send message:
  idVoto=…, idSessao=…, correlationId=…"). O **voto permanece registrado** e a resposta da API ao
  associado **não** é afetada.
- **Exceção síncrona no envio** (por exemplo, tempo limite do produtor aguardando metadados do
  broker) — a exceção é lançada **após o commit** e **capturada pela aplicação**, que a registra
  em log `ERROR` **com o contexto do voto** (`idVoto`, `idSessao`, `correlationId`) **sem
  propagá-la ao chamador**: a API responde **HTTP 201**, com o voto **gravado** e a notificação
  **perdida, mas com rastro em log**. O tempo de espera do produtor é limitado a ~5 s
  (`max.block.ms=5000`) (ver [PF16](pontos-fracos-e-falhas.md#pf16)).

O contrato do evento (payload, headers, configuração do broker) está em
[notificacao-voto-kafka.md](notificacao-voto-kafka.md).
