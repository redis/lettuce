package io.lettuce.core.tracing;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import io.lettuce.test.ReflectionTestUtils;

import io.lettuce.core.protocol.AsyncCommand;
import zipkin2.Span;
import brave.Tracer;
import brave.Tracing;
import brave.handler.MutableSpan;
import brave.propagation.CurrentTraceContext;
import io.lettuce.core.TestSupport;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * Unit tests for {@link BraveTracing}.
 *
 * @author Mark Paluch
 * @author Daniel Albuquerque
 */
@Tag(UNIT_TEST)
class BraveTracingUnitTests extends TestSupport {

    private static Tracing clientTracing;

    private static Queue<Span> spans = new LinkedBlockingQueue<>();

    @BeforeAll
    static void beforeClass() {

        clientTracing = Tracing.newBuilder().localServiceName("client")
                .currentTraceContext(CurrentTraceContext.Default.create()).spanReporter(spans::add).build();
    }

    @BeforeEach
    void before() {

        Tracer tracer = clientTracing.tracer();
        if (tracer.currentSpan() != null) {
            clientTracing.tracer().currentSpan().abandon();
        }

        spans.clear();
    }

    @AfterAll
    static void afterClass() {

        clientTracing.close();
    }

    @Test
    void shouldReportSimpleServiceName() {

        BraveTracing tracing = BraveTracing.create(clientTracing);
        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("redis");
        assertThat(endpoint.endpoint.port()).isNull();
        assertThat(endpoint.endpoint.ipv4()).isNull();
        assertThat(endpoint.endpoint.ipv6()).isNull();
    }

    @Test
    void shouldReportCustomServiceName() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing).serviceName("custom-name-goes-here").build();

        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("custom-name-goes-here");
        assertThat(endpoint.endpoint.port()).isNull();
        assertThat(endpoint.endpoint.ipv4()).isNull();
        assertThat(endpoint.endpoint.ipv6()).isNull();
    }

    @Test
    void shouldCustomizeEndpoint() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing).endpointCustomizer(it -> it.serviceName("foo-bar"))
                .build();
        BraveTracing.BraveEndpoint endpoint = (BraveTracing.BraveEndpoint) tracing
                .createEndpoint(new DomainSocketAddress("foo"));

        assertThat(endpoint.endpoint.serviceName()).isEqualTo("foo-bar");
    }

    @Test
    void shouldCustomizeSpan() {

        BraveTracing tracing = BraveTracing.builder().tracing(clientTracing)
                .spanCustomizer((command, span) -> span.tag("cmd", command.getType().toString())).build();

        BraveTracing.BraveSpan span = (BraveTracing.BraveSpan) tracing.getTracerProvider().getTracer().nextSpan();
        span.start(new AsyncCommand<>(new Command<>(CommandType.AUTH, null)));

        MutableSpan braveSpan = ReflectionTestUtils.getField(span.getSpan(), "state");
        Object[] tags = ReflectionTestUtils.getField(braveSpan, "tags");

        assertThat(tags).contains("cmd", "AUTH");
    }

}
