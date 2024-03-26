package io.lettuce.core.event.jfr;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import org.junit.jupiter.api.Test;

import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.metrics.CommandLatencyEvent;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * Unit tests for {@link JfrEventRecorder}.
 *
 * @author Mark Paluch
 */
class JfrEventRecorderUnitTests {

    @Test
    void shouldRecordEvent() throws IOException {

        Recording recording = new Recording();
        recording.start();

        EventRecorder.getInstance().record(new ConnectionActivatedEvent("my-uri", "0x1", "0x2", new DomainSocketAddress("/foo"),
                new DomainSocketAddress("/bar")));

        recording.stop();

        File temp = getFile(recording);

        RecordingFile input = new RecordingFile(temp.toPath());

        RecordedEvent recordedEvent = input.readEvent();
        assertThat(recordedEvent.getEventType().getName()).endsWith("JfrConnectionActivatedEvent");
        assertThat(recordedEvent.getEventType().getLabel()).isEqualTo("Connection Activated");
        input.close();
    }

    @Test
    void shouldNotEmitEventForAbsentJfrEventType() throws IOException {

        Recording recording = new Recording();
        recording.start();

        EventRecorder.getInstance().record(new CommandLatencyEvent(Collections.emptyMap()));
        recording.stop();

        File temp = getFile(recording);

        RecordingFile input = new RecordingFile(temp.toPath());

        assertThat(input.hasMoreEvents()).isFalse();
        input.close();
    }

    private static File getFile(Recording recording) throws IOException {

        InputStream stream = recording.getStream(recording.getStartTime(), recording.getStopTime());

        File temp = File.createTempFile("recording", ".jfr");
        FileOutputStream fos = new FileOutputStream(temp);
        temp.deleteOnExit();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();

        return temp;
    }

}
