package com.lambdaworks.redis.SimpleSerializers;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.*;

/**
 * @author <a href="mailto:a.abdelfatah@live.com">Ahmed Kamal</a>
 * @since 12.01.16 09:17
 */

public class JavaSerializer {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(JavaSerializer.class);

    public byte[] serializeObject(Object object) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            byte[] byteArray = bos.toByteArray();

            return byteArray;
        } catch (IOException e) {
            logger.error("Exception while trying serialize java object", e);
            throw e;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                logger.error("Exception while trying to close object output stream", ex);
                throw ex;
            }
            try {
                bos.close();
            } catch (IOException ex) {
                logger.error("Exception while trying to close bytes output stream", ex);
                throw ex;
            }
        }
    }

    public <T> T deserializeObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();

            return (T) o;

        } catch (ClassNotFoundException e) {
            logger.error("Exception because class of deserialized object can't be determined", e);
            throw e;
        } catch (IOException e) {
            logger.error("Exception occurred while deserializing an object", e);
            throw e;
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                logger.error("Exception while trying to close ByteArray Input Stream", ex);
                throw ex;
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error("Exception while trying to close Object Input Stream", ex);
                throw ex;
            }
        }
    }
}
