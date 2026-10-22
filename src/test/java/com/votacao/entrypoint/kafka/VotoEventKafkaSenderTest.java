package com.votacao.entrypoint.kafka;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.service.query.VotoPublishData;
import com.votacao.entrypoint.mapper.VotoMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static com.votacao.entrypoint.filter.MDCRequestFilter.MDC_CORRELATION_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("VotoEventKafkaSender")
class VotoEventKafkaSenderTest {

    private static final String TOPIC = "voto-topic";
    private static final String DOCUMENTO = "12345678909";
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 3, 10, 0, 0);

    @Mock
    private KafkaTemplate<String, VotoEvent> kafkaTemplate;

    @Mock
    private VotoMapper votoMapper;

    private VotoEventKafkaSender sender;

    @BeforeEach
    void setUp() {
        sender = new VotoEventKafkaSender(kafkaTemplate, votoMapper);
        when(votoMapper.toEvent(any(VotoPublishData.class))).thenReturn(event());
    }

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should publish the event to the voto topic with the session id as key")
    void sendEvent_shouldPublishToVotoTopic_whenSendSucceeds() {
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), any(VotoEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        sender.sendEvent(publishData());

        verify(kafkaTemplate).send(TOPIC, "1", event());
    }

    @Test
    @DisplayName("Should not propagate the exception when the send fails synchronously")
    void sendEvent_shouldNotPropagateException_whenSendFailsSynchronously() {
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), any(VotoEvent.class)))
                .thenThrow(new TimeoutException("Topic voto-topic not present in metadata after 5000 ms."));

        assertDoesNotThrow(() -> sender.sendEvent(publishData()));
    }

    @Test
    @DisplayName("Should not propagate the exception when the asynchronous send fails")
    void sendEvent_shouldNotPropagateException_whenSendFailsAsynchronously() {
        CompletableFuture<SendResult<String, VotoEvent>> failed =
                CompletableFuture.failedFuture(new TimeoutException("broker unreachable"));
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), any(VotoEvent.class))).thenReturn(failed);

        assertDoesNotThrow(() -> sender.sendEvent(publishData()));
    }

    @Test
    @DisplayName("Should log the failure with the vote context when the send fails synchronously")
    void sendEvent_shouldLogFailureWithVoteContext_whenSendFailsSynchronously(CapturedOutput output) {
        MDC.put(MDC_CORRELATION_ID_KEY, "correlation-123");
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), any(VotoEvent.class)))
                .thenThrow(new TimeoutException("Topic voto-topic not present in metadata after 5000 ms."));

        sender.sendEvent(publishData());

        assertAll(
                () -> assertTrue(output.getOut().contains("Failed to send message synchronously")),
                () -> assertTrue(output.getOut().contains("idVoto=1")),
                () -> assertTrue(output.getOut().contains("idSessao=1")),
                () -> assertTrue(output.getOut().contains("correlationId=correlation-123"))
        );
    }

    @Test
    @DisplayName("Should log the failure with the vote context when the asynchronous send fails")
    void sendEvent_shouldLogFailureWithVoteContext_whenSendFailsAsynchronously(CapturedOutput output) {
        CompletableFuture<SendResult<String, VotoEvent>> failed =
                CompletableFuture.failedFuture(new TimeoutException("broker unreachable"));
        when(kafkaTemplate.send(eq(TOPIC), eq("1"), any(VotoEvent.class))).thenReturn(failed);

        sender.sendEvent(publishData());

        assertAll(
                () -> assertTrue(output.getOut().contains("Failed to send message")),
                () -> assertTrue(output.getOut().contains("idVoto=1")),
                () -> assertTrue(output.getOut().contains("idSessao=1"))
        );
    }

    private SendResult<String, VotoEvent> sendResult() {
        ProducerRecord<String, VotoEvent> producerRecord = new ProducerRecord<>(TOPIC, "1", event());

        return new SendResult<>(producerRecord, new RecordMetadata(new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0));
    }

    private VotoEvent event() {
        return new VotoEvent(1L, "Reforma estatutária do capítulo quatro", 10L, 1L, "2026-09-23T10:00:00",
                "2026-09-23T10:10:00", 1L, DOCUMENTO, "SIM", "2026-09-23T10:01:00");
    }

    private VotoPublishData publishData() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 23, 10, 0);
        Pauta pauta = new Pauta(1L, "Reforma estatutária do capítulo quatro", 10L, FIXED_NOW);
        Sessao sessao = new Sessao(1L, pauta, now, now.plusMinutes(10));
        Voto voto = new Voto(1L, sessao, DOCUMENTO, Voto.Escolha.SIM, FIXED_NOW);

        return new VotoPublishData(pauta, sessao, voto, now.plusSeconds(1));
    }

}
