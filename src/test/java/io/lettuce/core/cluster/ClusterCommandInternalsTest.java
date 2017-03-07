/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCommandInternalsTest {

    @Mock
    private RedisChannelWriter writerMock;

    private ClusterCommand<String, String, String> sut;
    private Command<String, String, String> command = new Command<String, String, String>(CommandType.TYPE,
            new StatusOutput<String, String>(new Utf8StringCodec()), null);

    @Before
    public void before() throws Exception {
        sut = new ClusterCommand<String, String, String>(command, writerMock, 1);
    }

    @Test
    public void testException() throws Exception {

        sut.completeExceptionally(new Exception());
        assertThat(sut.isCompleted());
    }

    @Test
    public void testCancel() throws Exception {

        assertThat(command.isCancelled()).isFalse();
        sut.cancel();
        assertThat(command.isCancelled()).isTrue();
    }

    @Test
    public void testComplete() throws Exception {

        sut.complete();
        assertThat(sut.isCompleted()).isTrue();
        assertThat(sut.isCancelled()).isFalse();
    }

    @Test
    public void testRedirect() throws Exception {

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        assertThat(sut.isCompleted()).isFalse();
        assertThat(sut.isCancelled()).isFalse();
        verify(writerMock).write(sut);
    }

    @Test
    public void testRedirectLimit() throws Exception {

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        sut.getOutput().setError("MOVED 1234 127.0.0.1:1000");
        sut.complete();

        assertThat(sut.isCompleted()).isTrue();
        assertThat(sut.isCancelled()).isFalse();
        verify(writerMock).write(sut);
    }

    @Test
    public void testCompleteListener() throws Exception {

        final List<String> someList = new ArrayList<>();

        AsyncCommand<?, ?, ?> asyncCommand = new AsyncCommand<>(sut);

        asyncCommand.thenRun(() -> someList.add(""));
        asyncCommand.complete();
        asyncCommand.await(1, TimeUnit.MINUTES);

        assertThat(sut.isCompleted()).isTrue();
        assertThat(someList.size()).describedAs("Inner listener has to add one element").isEqualTo(1);
    }
}
