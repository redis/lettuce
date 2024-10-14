package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.RedisStateMachine;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link ObjectOutput}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class ObjectOutputUnitTests {

    @Test
    void shouldParseHelloWithModules() {

        String in = "%7\r\n"
                // 1
                + "$6\r\n" + "server\r\n" + "$5\r\n" + "redis\r\n"
                // 2
                + "$7\r\n" + "version\r\n" + "$5\r\n" + "7.2.0\r\n"

                // 3
                + "$5\r\n" + "proto\r\n" + ":3\r\n"

                // 4
                + "$2\r\n" + "id\r\n" + ":3\r\n"

                // 5
                + "$4\r\n" + "mode\r\n" + "$7\r\n" + "cluster\r\n"

                // 6
                + "$4\r\n" + "role\r\n" + "$6\r\n" + "master\r\n"

                // 7
                + "$7\r\n" + "modules\r\n"
                // list
                + "*4\r\n"

                // map
                + "%4\r\n"

                + "$4\r\n" + "name\r\n" + "$10\r\n" + "timeseries\r\n"

                + "$3\r\n" + "ver\r\n" + ":11001\r\n"

                + "$4\r\n" + "path\r\n" + "$19\r\n" + "/enterprise-managed\r\n"

                + "$4\r\n" + "args\r\n" + "*0\r\n"

                // elem 1
                + "%4\r\n"

                + "$4\r\n" + "name\r\n" + "$11\r\n" + "searchlight\r\n"

                + "$3\r\n" + "ver\r\n" + ":20803\r\n"

                + "$4\r\n" + "path\r\n" + "$19\r\n" + "/enterprise-managed\r\n"

                + "$4\r\n" + "args\r\n"

                + "*10\r\n"
                // 1
                + "$23\r\n" + "FORK_GC_CLEAN_THRESHOLD\r\n" + "$3\r\n" + "100\r\n"
                // 2
                + "$19\r\n" + "MAXAGGREGATERESULTS\r\n" + "$5\r\n" + "10000\r\n"
                // 3
                + "$16\r\n" + "MAXSEARCHRESULTS\r\n" + "$5\r\n" + "10000\r\n"

                // 4
                + "$7\r\n" + "MT_MODE\r\n" + "$26\r\n" + "MT_MODE_ONLY_ON_OPERATIONS\r\n"

                // 5
                + "$14\r\n" + "WORKER_THREADS\r\n" + "$1\r\n" + "4\r\n"

                // 6
                + "%4\r\n" + "$4\r\n" + "name\r\n" + "$6\r\n" + "ReJSON\r\n" + "$3\r\n" + "ver\r\n" + ":20602\r\n" + "$4\r\n"
                + "path\r\n" + "$19\r\n" + "/enterprise-managed\r\n" + "$4\r\n" + "args\r\n" + "*0\r\n"

                + "%4\r\n" + "$4\r\n" + "name\r\n" + "$2\r\n" + "bf\r\n" + "$3\r\n" + "ver\r\n" + ":20601\r\n" + "$4\r\n"
                + "path\r\n" + "$19\r\n" + "/enterprise-managed\r\n" + "$4\r\n" + "args\r\n" + "*0\r\n";

        RedisStateMachine rsm = new RedisStateMachine(ByteBufAllocator.DEFAULT);
        ObjectOutput<String, String> output = new ObjectOutput<>(StringCodec.UTF8);
        rsm.decode(Unpooled.wrappedBuffer(in.getBytes()), output);
        assertThat(output.get()).isInstanceOf(Map.class);

        Map<String, Object> map = (Map) output.get();
        assertThat(map).containsEntry("server", "redis").containsEntry("proto", 3L);

        List<Map<String, Object>> modules = (List) map.get("modules");

        assertThat(modules).hasSize(4);

        Map<String, Object> searchlight = modules.get(1);
        assertThat(searchlight).containsEntry("name", "searchlight");

        List<Object> args = (List) searchlight.get("args");
        assertThat(args).containsSequence("MAXSEARCHRESULTS", "10000");
    }

}
