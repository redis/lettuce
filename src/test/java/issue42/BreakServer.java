package issue42;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisHashesConnection;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.Utf8StringCodec;

import static org.junit.Assert.*;

public class BreakServer {
  public static final String NON_CLUSTER_HOST = "localhost";
  public static final int NON_CLUSTER_PORT = 6379;
  public static final String TEST_KEY = "taco";

  public static int TIMEOUT = 5;
  RedisConnection<String, String> baseClient;
  public volatile boolean sleep=false;

  @Before public void setUp() throws Exception {
    RedisURI localhost = RedisURI.Builder.redis(NON_CLUSTER_HOST, NON_CLUSTER_PORT)
        .withTimeout(TIMEOUT, TimeUnit.SECONDS).build();
    baseClient= new RedisClient(localhost).connect(this.slowCodec);
  }


  @Test
  public void testStandAlone() throws Exception {
    baseClient.del(TEST_KEY);
    populateTest(baseClient, 0);
    assertEquals(16385, baseClient.hvals(TEST_KEY).size());
    breakClient(baseClient);
    assertEquals(16385, baseClient.hvals(TEST_KEY).size());
  }
  @Test
  public void testLooping() throws Exception {
    baseClient.del(TEST_KEY);
    populateTest(baseClient, 100);
    assertEquals(16385+100, baseClient.hvals(TEST_KEY).size());

    breakClient(baseClient);

    for (int x=0;x<100;x++){
      int i = Integer.parseInt(baseClient.hget(TEST_KEY, "GET-" + x));
      Assert.assertEquals(x, i);
    }
  }


  private void breakClient(RedisHashesConnection<String, String> client) throws InterruptedException {
    try {
      this.sleep=true;
      System.out.println("This should timeout");
      client.hgetall(TEST_KEY) ;
      fail();
    }catch (RedisCommandTimeoutException e){
      System.out.println("got expected timeout");
    }
    TimeUnit.SECONDS.sleep(5);
  }

  private void populateTest(RedisHashesConnection<String, String> client, int loopFor) {
    System.out.println("populating hash");
    client.hset(TEST_KEY, TEST_KEY, TEST_KEY);
    for(int x=0;x<loopFor;x++){
      client.hset(TEST_KEY, "GET-"+x, Integer.toString(x));
    }
    for (int i=0; i<16384;i++) {
      client.hset(TEST_KEY, Integer.toString(i), TEST_KEY);
    }
    assertEquals(16385+loopFor, client.hvals(TEST_KEY).size());
    System.out.println("done");

  }

  Utf8StringCodec slowCodec = new Utf8StringCodec() {
    public String decodeValue(ByteBuffer bytes) {
      if (sleep) {
        System.out.println("Sleeping for "+ (TIMEOUT+3) +" seconds in slowCodec");
        sleep = false;
        try {
          TimeUnit.SECONDS.sleep(TIMEOUT+3);
        } catch (InterruptedException e) {
        }
        System.out.println("Done sleeping in slowCodec");
      }
      return super.decodeValue(bytes);
    }
  };
}
