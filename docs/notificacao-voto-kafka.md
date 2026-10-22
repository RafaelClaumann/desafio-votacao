# Notificação de Voto (Kafka)

Este documento descreve o **contrato e a semântica de entrega** da notificação de voto
publicada em Kafka. O comportamento de negócio correspondente está em
[R14](regras-de-negocio.md#r14--cada-voto-registrado-gera-uma-notificação-de-voto-publicada-em-kafka)
e [R15](regras-de-negocio.md#r15--a-notificação-do-voto-só-e-enviada-depois-da-gravação-definitiva-do-voto),
e nos [modos de falha PF15/PF16](pontos-fracos-e-falhas.md) (PF16 corrigido).

## Visão geral

Toda votação **aceita e gravada** dispara, após a confirmação da gravação, a publicação de um
evento de notificação no tópico **`voto-topic`**. A notificação serve para que outros sistemas
(consumidores do tópico) acompanhem a apuração de cada sessão sem consultar a API.

```
POST /votos
   ↓
VotoService.votar()            [transação JDBC]
   ├─ valida sessão aberta / documento / duplicidade
   ├─ grava o voto
   └─ publica o evento VotoPublishData (dentro da transação)
        ↓
   COMMIT  →  VotoEventKafkaSender.sendEvent()   [@TransactionalEventListener(AFTER_COMMIT)]
                 └─ kafkaTemplate.send("voto-topic", chave, VotoEvent)
```

## Contrato do evento

| Item | Valor |
| ---- | ----- |
| Tópico | `voto-topic` (constante `TOPIC` em `VotoEventKafkaSender`) |
| Chave (key) | `String.valueOf(idSessao)` — o identificador da sessão |
| Valor (value) | `VotoEvent` serializado em JSON |
| Serializador | `JacksonJsonSerializer` com o `JsonMapper` gerenciado pelo Spring (`tools.jackson.databind.json.JsonMapper`) |
| Formato dos campos | `snake_case` — herda `spring.jackson.property-naming-strategy=SNAKE_CASE` do `JsonMapper` do Spring Boot |

### Correlação por chave

A chave é o **identificador da sessão**. Todos os votos de uma mesma sessão caem na **mesma
partição**, o que preserva a **ordem** dos votos de uma sessão para o consumidor. Os votos de
sessões diferentes podem ser distribuídos entre partições (o broker do `docker-compose` é criado
com `KAFKA_NUM_PARTITIONS=3`).

### Payload

| Campo JSON | Origem | Conteúdo |
| ---------- | ------ | -------- |
| `id_pauta` | `pauta.id` | Identificador da pauta votada |
| `titulo` | `pauta.titulo` | Título da pauta (já normalizado, sem espaços nas bordas) |
| `tempo_votacao_minutos` | `pauta.tempoVotacaoMinutos` | Duração de votação da pauta, em minutos |
| `id_sessao` | `sessao.id` | Identificador da sessão |
| `started_at` | `sessao.startedAt` | Início da sessão — `ISO_LOCAL_DATE_TIME` |
| `expires_at` | `sessao.expiresAt` | Expiração da sessão — `ISO_LOCAL_DATE_TIME` |
| `id_voto` | `voto.id` | Identificador do voto registrado |
| `documento` | `voto.documento` | CPF do associado **normalizado** (somente dígitos) |
| `escolha_voto` | `voto.escolhaVoto` | `SIM` ou `NAO` (nome do enum) |
| `published_at` | `VotoPublishData.publishedAt` | Momento da publicação — `ISO_LOCAL_DATE_TIME`, colhido do **relógio do banco** |

Exemplo de mensagem:

```json
{
  "id_pauta": 1,
  "titulo": "Reforma estatutária do capítulo quatro",
  "tempo_votacao_minutos": 10,
  "id_sessao": 1,
  "started_at": "2026-10-03T12:35:30",
  "expires_at": "2026-10-03T12:45:30",
  "id_voto": 1,
  "documento": "04622215047",
  "escolha_voto": "SIM",
  "published_at": "2026-10-03T12:35:30.987654"
}
```

> O mapeamento `LocalDateTime → String` (campos `started_at`, `expires_at` e `published_at`) é
> feito pelo MapStruct com `DateTimeFormatter.ISO_LOCAL_DATE_TIME` (ver `VotoMapper.toEvent`).

## Headers da mensagem

| Header | Origem | Finalidade |
| ------ | ------ | ---------- |
| `X-Correlation-Id` | `KafkaProducerInterceptor` | Propaga o identificador de correlação da requisição HTTP que registrou o voto, permitindo rastrear o mesmo fluxo nos logs da API e do consumidor |
| `__TypeId__` | `JacksonJsonSerializer` (comportamento padrão) | Classe Java do valor (`com.votacao.entrypoint.kafka.VotoEvent`), para desserialização tipada pelo consumidor |

O `X-Correlation-Id` é o **mesmo valor** recebido no header da requisição ou gerado pelo
`MDCRequestFilter` quando o cliente não o envia — e é o valor devolvido no campo `correlation_id`
do corpo de erro da API.

Como o MDC só é preenchido no ciclo HTTP, o `KafkaProducerInterceptor` **não anexa** o header
quando o identificador de correlação está ausente (ou em branco) — nesse caso ele registra um
`WARN` e **não interrompe** a publicação.

## Semântica de entrega

| Aspecto | Comportamento |
| ------- | ------------- |
| Momento | **Depois** do commit da transação do voto (`TransactionPhase.AFTER_COMMIT`) |
| Transação revertida | **Nenhuma** notificação é publicada |
| Escopo transacional | **Fora** da transação: o voto já está gravado quando a notificação é enviada |
| Assincronia | O envio é assíncrono; a resposta HTTP não espera a confirmação do broker |
| Falha no envio | Registrada em log (`ERROR` — "Failed to send message: idVoto=…, idSessao=…, correlationId=…"); o **voto permanece registrado** |
| Falha **síncrona** no envio | **Tratada pela aplicação**: `VotoEventKafkaSender` captura a exceção e a registra em log `ERROR` com `idVoto`, `idSessao` e `correlationId`; a API responde **201** e a notificação é perdida, mas **deixando rastro** (PF16 corrigida) |
| Sucesso no envio | Registrado em log (`INFO` — "Message sent successfully: idVoto=…, timestamp=…, partition=…") |
| Retentativa | **Não há** retentativa, dead-letter ou persistência da notificação (ver PF15) |

O produtor é configurado com `acks=all` e `enable.idempotence=true`, o que evita mensagens
duplicadas por reenvio interno do produtor — mas **não** protege contra a **perda** da mensagem
quando o broker está indisponível no momento do envio.

> **Tempo de espera limitado:** embora o envio seja assíncrono, o `kafkaTemplate.send(...)`
> aguarda os **metadados do tópico** de forma **síncrona**, limitada por `max.block.ms`
> (**5000 ms**, configurado). Com o broker inalcançável, a thread da requisição do `POST /votos`
> fica bloqueada por **até ~5 s** antes de a falha ser registrada em log `ERROR` — a API continua
> respondendo **201**, pois o voto já está gravado.

## Configuração

| Propriedade | Padrão | Ambiente | Efeito |
| ----------- | ------ | --------- | ------ |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | default | Endereços do broker |
| `spring.kafka.bootstrap-servers` | `${KAFKA_BROKERS_URLS}` | `prod` | Idem, lido da variável de ambiente |
| `spring.kafka.producer.client-id` | `votacao-sdui` | default | Identificação do produtor nos logs do broker |
| `spring.kafka.producer.acks` | `all` | default | Confirmação de escrita em todas as réplicas |
| `spring.kafka.producer.properties.enable.idempotence` | `true` | default | Produção idempotente |
| `spring.kafka.producer.properties.max.block.ms` | `5000` | default | Tempo máximo (ms) que o produtor bloqueia esperando metadados do tópico |
| `spring.kafka.producer.properties.request.timeout.ms` | `10000` | default | Tempo máximo (ms) de espera por uma resposta do broker |
| `spring.kafka.producer.properties.delivery.timeout.ms` | `30000` | default | Tempo máximo (ms) para entregar a mensagem (deve ser ≥ `request.timeout.ms` + `linger.ms`) |
| `spring.kafka.producer.key-serializer` | `StringSerializer` | default | Chave textual |
| `spring.kafka.producer.value-serializer` | `JsonSerializer` | default | Valor em JSON |

> A propriedade `spring.kafka.producer.value-serializer` é apenas **documental**: o valor
> efetivo é o `JacksonJsonSerializer` instanciado em `KafkaConfig.producerFactory()`, que ignora
> o serializador declarado nas propriedades.

Ambiente local com o `docker-compose`:

```bash
docker compose -f .docker/compose.yaml up -d kafka kafka-ui
```

- Broker: `localhost:9092` (host) / `kafka:29092` (rede interna do compose).
- Kafka UI: `http://localhost:8086` — lista as mensagens do tópico `voto-topic`.
- O tópico é **criado automaticamente** (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`) e o broker é
  criado com **3 partições** (`KAFKA_NUM_PARTITIONS=3`); na primeira publicação o produtor pode
  registrar um aviso recuperável `UNKNOWN_TOPIC_OR_PARTITION` até o tópico existir.

## Implementação

| Responsabilidade | Classe |
| ---------------- | ------ |
| Publicação do evento de aplicação | `EventPublisherAdapter` (implementa `VotoEventPublishGateway`) |
| Contrato do evento | `VotoPublishData` (dados publicados) e `VotoEvent` (payload serializado) |
| Envio ao Kafka | `VotoEventKafkaSender` (`@TransactionalEventListener(AFTER_COMMIT)`) |
| Mapeamento para o payload | `VotoMapper.toEvent()` (MapStruct) |
| Configuração do produtor | `KafkaConfig` (`ProducerFactory`, `KafkaTemplate`, serializador JSON) |
| Header de correlação | `KafkaProducerInterceptor` (`ProducerInterceptor`) |
| Autoridade de hora do `published_at` | `TimeProvider` → `JpaTimeProvider` (`SELECT CURRENT_TIMESTAMP`) |
