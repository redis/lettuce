package com.lambdaworks.redis.SimpleSerializers;

import com.lambdaworks.redis.RedisException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.*;

/**
 * @author <a href="mailto:a.abdelfatah@live.com">Ahmed Kamal</a>
 * @since 12.01.16 09:17
 */

public class JavaSerializer {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(JavaSerializer.class);

    public byte[] serializeObject(Object object) {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)){

            out.writeObject(object);
            byte[] byteArray = bos.toByteArray();

            return byteArray;
        }
        catch (Exception e){
            logger.error("Exception occurred while Java Object Serialization", e);
            throw new RedisException("Java Serialization Failed", e);
        }
    }

    public <T> T deserializeObject(byte[] bytes) {

        try(ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput in = new ObjectInputStream(bis)){

            Object o = in.readObject();

            return (T) o;
        }
        catch (Exception e){
            logger.error("Exception occurred while Java Object Deserialization", e);
            throw new RedisException("Java Deserialization Failed", e);
        }
    }
}
