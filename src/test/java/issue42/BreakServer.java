package issue42;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BreakServer {
  public static final String NON_CLUSTER_HOST = "localhost";
  public static final int NON_CLUSTER_PORT = 6379;
  public static final String CLUSTER_HOST = "localhost";
  public static final int CLUSTER_PORT = 7379;
  public static final String TEST_KEY = "taco";

  public static int TIMEOUT = 5;
  RedisClusterConnection<String, String> clusterClient;
  RedisConnection<String, String> baseClient;

  @BeforeClass public static void setUpred() throws Exception {
    RedisURI localhost2 = RedisURI.Builder.redis(CLUSTER_HOST, CLUSTER_PORT).build();
    setupFakeCluster(localhost2);
  }

  @Before public void setUp() throws Exception {
    RedisURI localhost = RedisURI.Builder.redis(NON_CLUSTER_HOST, NON_CLUSTER_PORT)
        .withTimeout(TIMEOUT, TimeUnit.SECONDS).build();
    baseClient= new RedisClient(localhost).connect(this.slowCodec);

    RedisURI localhost2 = RedisURI.Builder.redis(CLUSTER_HOST, CLUSTER_PORT)
        .withTimeout(TIMEOUT,TimeUnit.SECONDS).build();
    clusterClient = new RedisClusterClient(localhost2).connectCluster(this.slowCodec);

  }


  public volatile boolean sleep=false;
  @Test
  public void testCluster() throws Exception {
    clusterClient.del(TEST_KEY);
    doTest(clusterClient,0);
  }
  @Test
  public void testStandAlone() throws Exception {
    baseClient.del(TEST_KEY);
    doTest(baseClient,0);
  }
  @Test
  @Ignore
  //For debugging. It will keep issuing the hgetall
  //This will fail when the bug is fixed
  public void testLooping() throws Exception {
    baseClient.del(TEST_KEY);
    doTest(baseClient,100);
  }

  private void doTest(RedisHashesConnection<String, String> client, int loopFor) throws InterruptedException {
    System.out.println("populating hash");
    client.hset(TEST_KEY, TEST_KEY, TEST_KEY);
    for (int i=0; i<16384;i++) {
      client.hset(TEST_KEY, Integer.toString(i), TEST_KEY);
    }
    assertEquals(16385, client.hvals(TEST_KEY).size());

    System.out.println("done");
    try {
      this.sleep=true;
      System.out.println("This should timeout");
      client.hgetall(TEST_KEY) ;
      fail();
    }catch (RedisCommandTimeoutException e){
      System.out.println("got expected timeout");
    }
    TimeUnit.SECONDS.sleep(5);
    if(loopFor!=0){
      for (int x=0;x<=loopFor;x++){
        System.out.println("loop "+ x );
        try{
          client.hvals(TEST_KEY);
          fail();
        }catch(RedisCommandTimeoutException e){
          System.out.println("loop complete"+ x );
        }
      }
    }else{
      System.out.println("This should not timeout");
      assertEquals(16385, client.hvals(TEST_KEY).size());
    }


  }

  private static void setupFakeCluster(RedisURI localhost2) throws InterruptedException {
    int[] slots = new int[16384];

    RedisClient c1 = new RedisClient(localhost2);
    for (int i = 0; i < slots.length; i++) {
      slots[i]=i;
    }
    c1.connect().flushdb();
    c1.connect().clusterFlushslots();
    String s1 = c1.connect().clusterAddSlots(slots);
    for (int i=0;i<10;i++) {
      try {
        List<Object> objects = c1.connect().clusterSlots();
        if(objects.size() == 16384){
          break;
        }
      } catch (RedisCommandExecutionException e) {
        TimeUnit.SECONDS.sleep(1);
      }
    }
    TimeUnit.SECONDS.sleep(1);
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
