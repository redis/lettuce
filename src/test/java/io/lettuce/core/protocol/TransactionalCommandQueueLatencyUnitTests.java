/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.MultiOutput;
import io.lettuce.core.output.StatusOutput;

@Tag(UNIT_TEST)
class TransactionalCommandQueueLatencyUnitTests {

    private final LongConsumer completion = mock(LongConsumer.class);

    @Test
    void queuedAcknowledgementShouldNotRecordCompletion() {
        TransactionalCommand<String, String, String> command = transactionalCommand();

        command.complete();

        assertThat(command).isNotDone();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void execResponseShouldRecordCompletionExactlyOnce() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();
        long beforeCompletion = System.nanoTime();

        command.completeResponse();
        long afterCompletion = System.nanoTime();
        command.completeResponse();
        command.complete();

        assertThat(command).isDone();
        ArgumentCaptor<Long> completedAt = ArgumentCaptor.forClass(Long.class);
        verify(completion, times(1)).accept(completedAt.capture());
        assertThat(completedAt.getValue() - beforeCompletion).isGreaterThanOrEqualTo(0);
        assertThat(afterCompletion - completedAt.getValue()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void discardCompletionShouldNotRecordExecResponse() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();

        command.complete();
        command.completeResponse();

        assertThat(command).isDone();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void cancelledTransactionShouldNotRecordCompletion() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();
        command.cancel();

        command.completeResponse();

        assertThat(command).isCancelled();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void cancellationOfUserFutureShouldNotRecordCompletion() {
        AsyncCommand<String, String, String> userCommand = new AsyncCommand<>(plainCommand());
        TransactionalCommand<String, String, String> transaction = new TransactionalCommand<>(userCommand);
        transaction.onQueueLatencyCompletion(completion);
        transaction.complete();
        userCommand.cancel();

        transaction.completeResponse();

        assertThat(userCommand).isCancelled();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void userFutureCancellationDuringExecCompletionShouldNotRecordCompletion() {
        AsyncCommand<String, String, String> userCommand = new AsyncCommand<>(plainCommand());
        TransactionalCommand<String, String, String> transaction = new TransactionalCommand<String, String, String>(
                userCommand) {

            private int completions;

            @Override
            public void complete() {
                if (++completions == 2) {
                    // Cancel after completeResponse snapshots cancellation and before it completes the user future.
                    userCommand.cancel();
                }
                super.complete();
            }

        };
        transaction.onQueueLatencyCompletion(completion);
        transaction.complete();

        transaction.completeResponse();

        assertThat(userCommand).isCancelled();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void multiOutputShouldRecordNormallyDecodedExecResult() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();
        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        output.add(command);

        output.multi(1);
        output.set(ByteBuffer.wrap("OK".getBytes(StandardCharsets.US_ASCII)));
        output.complete(1);
        output.complete(0);

        assertThat(command).isCompletedWithValue("OK");
        assertThat(output.get()).containsExactly("OK");
        verify(completion, times(1)).accept(anyLong());
    }

    @Test
    void multiOutputShouldNotRecordAbortedExecResult() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();
        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        output.add(command);

        output.multi(-1);
        output.complete(0);

        assertThat(command).isDone();
        assertThat(output.get().wasDiscarded()).isTrue();
        verify(completion, never()).accept(anyLong());
    }

    @Test
    void multiOutputCancelShouldNotRecordExecResponse() {
        TransactionalCommand<String, String, String> command = transactionalCommand();
        command.complete();
        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        output.add(command);

        output.cancel();

        assertThat(command).isDone();
        verify(completion, never()).accept(anyLong());
    }

    private TransactionalCommand<String, String, String> transactionalCommand() {
        TransactionalCommand<String, String, String> command = new TransactionalCommand<>(plainCommand());
        command.onQueueLatencyCompletion(completion);
        return command;
    }

    private Command<String, String, String> plainCommand() {
        return new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
    }

}
