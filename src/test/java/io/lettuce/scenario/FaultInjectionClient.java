package io.lettuce.scenario;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

/**
 * Async Fault Injection Client using reactor.netty.http.client.HttpClient.
 */
public class FaultInjectionClient {

    private static final String BASE_URL;

    static {
        BASE_URL = System.getenv().getOrDefault("FAULT_INJECTION_API_URL", "http://127.0.0.1:20324");
    }

    private static final Logger log = LoggerFactory.getLogger(FaultInjectionClient.class);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public FaultInjectionClient() {
        this.httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10));

        this.objectMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static class TriggerActionResponse {

        @JsonProperty("action_id")
        private String actionId;

        // Default constructor for Jackson
        public TriggerActionResponse() {
        }

        public TriggerActionResponse(String actionId) {
            this.actionId = actionId;
        }

        public String getActionId() {
            return actionId;
        }

        public void setActionId(String actionId) {
            this.actionId = actionId;
        }

    }

    /**
     * Triggers an action with the specified type and parameters.
     *
     * @param actionType the type of action to trigger
     * @param parameters the parameters for the action
     * @return a Mono that emits the TriggerActionResponse when the action is triggered
     */
    public Mono<TriggerActionResponse> triggerAction(String actionType, Map<String, Object> parameters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", actionType);
        payload.put("parameters", parameters);

        try {
            String jsonString = objectMapper.writeValueAsString(payload);
            byte[] bytes = jsonString.getBytes();
            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

            return httpClient.headers(h -> h.add("Content-Type", "application/json")).post().uri(BASE_URL + "/action")
                    .send(ByteBufFlux.fromInbound(Mono.just(byteBuf))).responseSingle((response, body) -> body.asString())
                    .map(result -> {
                        log.info("Trigger action response: {}", result);
                        try {
                            return objectMapper.readValue(result, TriggerActionResponse.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse response", e);
                        }
                    }).onErrorResume(e -> {
                        log.error("Failed to trigger action", e);
                        return Mono.error(new RuntimeException("Failed to trigger action", e));
                    });
        } catch (Exception e) {
            log.error("Failed to serialize request", e);
            return Mono.error(new RuntimeException("Failed to serialize request", e));
        }
    }

    /**
     * Checks the status of an action.
     *
     * @param actionId the ID of the action to check
     * @return Mono that emits true if the action is completed, empty if still in progress
     */
    private Mono<Boolean> checkActionStatus(String actionId) {
        return httpClient.get().uri(BASE_URL + "/action/" + actionId).responseSingle((response, body) -> body.asString())
                .flatMap(result -> {
                    log.info("Action status: {}", result);
                    if (result.contains("success")) {
                        return Mono.just(true);
                    }
                    // Return empty to trigger retry
                    return Mono.empty();
                })
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2))
                        .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage() != null
                                && throwable.getMessage().contains("Fault injection proxy error")))
                        .doBeforeRetry(retrySignal -> log.warn("Retrying action status check after error, attempt: {}",
                                retrySignal.totalRetries() + 1)))
                .onErrorResume(e -> {
                    log.error("Fault injection proxy error after retries", e);
                    return Mono.error(new RuntimeException("Fault injection proxy error", e));
                });
    }

    /**
     * Waits for an action to complete by polling the status endpoint. Uses Reactor's retry capabilities for a more idiomatic
     * approach.
     *
     * @param actionId the ID of the action to wait for
     * @param checkInterval interval between status checks
     * @param delayAfter delay after completion before returning true
     * @param timeout maximum time to wait for completion
     * @return Mono that completes with true when the action is completed and the delay has passed
     */
    public Mono<Boolean> waitForCompletion(String actionId, Duration checkInterval, Duration delayAfter, Duration timeout) {
        return Mono.defer(() -> checkActionStatus(actionId)).flatMap(completed -> {
            if (completed) {
                // If we need to wait after completion, delay and then return true
                if (!delayAfter.isZero()) {
                    return Mono.delay(delayAfter).thenReturn(true);
                }
                return Mono.just(true);
            }
            return Mono.just(false);
        }).repeatWhenEmpty(repeat -> repeat.delayElements(checkInterval).timeout(timeout)
                .doOnError(e -> log.error("Timeout waiting for action to complete", e)));
    }

    /**
     * Triggers an action and waits for it to complete.
     *
     * @param actionType the type of action to trigger
     * @param parameters the parameters for the action
     * @param checkInterval interval between status checks
     * @param delayAfter delay after completion before returning true
     * @param timeout maximum time to wait for completion
     * @return a Mono that emits true when the action is completed
     */
    public Mono<Boolean> triggerActionAndWait(String actionType, Map<String, Object> parameters, Duration checkInterval,
            Duration delayAfter, Duration timeout) {
        return triggerAction(actionType, parameters)
                .flatMap(response -> waitForCompletion(response.getActionId(), checkInterval, delayAfter, timeout));
    }

}
