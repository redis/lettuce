/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ClusterCommandUnitTests {

    @Mock
    private RedisChannelWriter writerMock;

    private ClusterCommand<String, String, String> sut;

    private Command<String, String, String> command = new Command<>(CommandType.TYPE, new StatusOutput<>(StringCodec.UTF8),
            null);

    @BeforeEach
    void before() {
        sut = new ClusterCommand<>(command, writerMock, 1);
    }

    @Test
    void testException() {

        sut.completeExceptionally(new Exception());
        assertThat(sut.isCompleted());
    }

    @Test
    void testCancel() {

        assertThat(command.isCancelled()).isFalse();
        sut.cancel();
        assertThat(command.isCancelled()).isTrue();
    }

    @Test
    void testComplete() {

        sut.complete();
        assertThat(sut.isCompleted()).isTrue();
        assertThat(sut.isCancelled()).isFalse();
    }

    @Test
    void testRedirect() {

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        assertThat(sut.isCompleted()).isFalse();
        assertThat(sut.isCancelled()).isFalse();
        verify(writerMock).write(sut);
    }

    @Test
    void testRedirectLimit() {

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        assertThat(sut.isCompleted()).isTrue();
        assertThat(sut.isCancelled()).isFalse();
        verify(writerMock).write(sut);
    }

    @Test
    void testCompleteListener() {

        final List<String> someList = new ArrayList<>();

        AsyncCommand<?, ?, ?> asyncCommand = new AsyncCommand<>(sut);

        asyncCommand.thenRun(() -> someList.add(""));
        asyncCommand.complete();
        asyncCommand.await(1, TimeUnit.MINUTES);

        assertThat(sut.isCompleted()).isTrue();
        assertThat(someList.size()).describedAs("Inner listener has to add one element").isEqualTo(1);
    }

}
