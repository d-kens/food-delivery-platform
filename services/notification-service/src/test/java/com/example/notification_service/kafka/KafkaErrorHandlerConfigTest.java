package com.example.notification_service.kafka;

import co.novu.models.errors.APIException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.util.backoff.BackOffExecution;
import tools.jackson.core.exc.StreamReadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaErrorHandlerConfigTest {

    @SuppressWarnings("unchecked")
    private static APIException novu(int code) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        return new APIException("novu error", code, "{\"message\":\"workflow_not_found\"}".getBytes(), response, null);
    }

    private static ListenerExecutionFailedException wrapped(Throwable cause) {
        return new ListenerExecutionFailedException("listener failed", cause);
    }

    @Test
    void deadLetterTopic_isTopicGroupDlt() {
        assertThat(KafkaErrorHandlerConfig.deadLetterTopic("user-events", "notification-service"))
                .isEqualTo("user-events.notification-service.DLT");
    }

    @Test
    void retryBackOff_doublesWithJitterThenStops() {
        BackOffExecution execution = KafkaErrorHandlerConfig.retryBackOff().start();

        assertThat(execution.nextBackOff()).isBetween(1_000L, 1_500L);
        assertThat(execution.nextBackOff()).isBetween(1_000L, 3_000L);
        assertThat(execution.nextBackOff()).isBetween(2_000L, 6_000L);
        assertThat(execution.nextBackOff()).isBetween(4_000L, 12_000L);
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }

    @Test
    void malformedJson_isPermanent() {
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(new StreamReadException(null, "bad json")))).isTrue();
    }

    @Test
    void missingField_isPermanent() {
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(new IllegalArgumentException("Event is missing userId")))).isTrue();
    }

    @Test
    void novuClientErrors_arePermanent() {
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(400)))).isTrue();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(401)))).isTrue();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(422)))).isTrue();
    }

    @Test
    void novuTransientErrors_areRetried() {
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(408)))).isFalse();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(409)))).isFalse();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(429)))).isFalse();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(500)))).isFalse();
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(novu(503)))).isFalse();
    }

    @Test
    void networkErrors_areRetried() {
        assertThat(KafkaErrorHandlerConfig.isPermanent(wrapped(new UncheckedIOException(new IOException("timeout"))))).isFalse();
    }

    @Test
    void location_isTopicPartitionOffset() {
        assertThat(KafkaErrorHandlerConfig.location(new ConsumerRecord<>("user-events", 2, 7L, "k", "v")))
                .isEqualTo("user-events-2@7");
    }

    @Test
    void reason_forNovu_isStatusAndBody() {
        assertThat(KafkaErrorHandlerConfig.reason(wrapped(novu(422))))
                .isEqualTo("Novu 422 {\"message\":\"workflow_not_found\"}");
    }

    @Test
    void reason_forOtherErrors_isRootCause() {
        assertThat(KafkaErrorHandlerConfig.reason(wrapped(new IllegalArgumentException("Event is missing userId"))))
                .isEqualTo("IllegalArgumentException: Event is missing userId");
    }
}
