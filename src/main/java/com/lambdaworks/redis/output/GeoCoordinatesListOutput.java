package com.lambdaworks.redis.output;

import static java.lang.Double.parseDouble;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * A list output that creates a list with {@link GeoCoordinates}'s.
 *
 * @author Mark Paluch
 */
public class GeoCoordinatesListOutput<K, V> extends CommandOutput<K, V, List<Value<GeoCoordinates>>> implements StreamingOutput<Value<GeoCoordinates>> {

    private Double x;
	private Subscriber<Value<GeoCoordinates>> subscriber;

	public GeoCoordinatesListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
		setSubscriber(ListSubscriber.of(output));
	}

    @Override
    public void set(ByteBuffer bytes) {

        Double value = (bytes == null) ? 0 : parseDouble(decodeAscii(bytes));

        if (x == null) {
            x = value;
            return;
        }

        subscriber.onNext(Value.fromNullable(new GeoCoordinates(x, value)));
        x = null;
    }

    @Override
    public void multi(int count) {
        if (count == -1) {
            subscriber.onNext(Value.empty());
        }
    }

	@Override
	public void setSubscriber(Subscriber<Value<GeoCoordinates>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
		this.subscriber = subscriber;
	}

	@Override
	public Subscriber<Value<GeoCoordinates>> getSubscriber() {
		return subscriber;
	}
}
