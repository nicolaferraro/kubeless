package io.kubeless.server.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
public class GenericJsonCodec<T> implements MessageCodec<T, T> {

    private static ObjectMapper mapper = new ObjectMapper();

    private Class<T> type;

    public GenericJsonCodec(Class<T> type) {
        this.type = type;
    }

    public static <U> GenericJsonCodec<U> of(Class<U> type) {
        return new GenericJsonCodec<U>(type);
    }

    /**
     * Called by Vert.x when marshalling a message to the wire.
     *  @param buffer  the message should be written into this buffer
     * @param t  the message that is being sent
     */
    @Override
    public void encodeToWire(Buffer buffer, T t) {
        try {
            byte[] value = mapper.writeValueAsBytes(t);
            buffer.appendInt(value.length);
            buffer.appendBytes(value);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Called by Vert.x when a message is decoded from the wire.
     *
     * @param pos  the position in the buffer where the message should be read from.
     * @param buffer  the buffer to read the message from
     * @return the read message
     */
    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        try {
            int length = buffer.getInt(pos);
            pos += 4;
            byte[] value = buffer.getBytes(pos, pos + length);
            T obj = mapper.readValue(value, type);
            return obj;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * If a message is sent <i>locally</i> across the event bus, this method is called to transform the message from
     * the sent type S to the received type R
     *
     * @param t  the sent message
     * @return the transformed message
     */
    @Override
    public T transform(T t) {
        return t;
    }

    /**
     * The codec name. Each codec must have a unique name. This is used to identify a codec when sending a message and
     * for unregistering codecs.
     *
     * @return the name
     */
    @Override
    public String name() {
        return "generic-json:" + type.getCanonicalName();
    }

    /**
     * Used to identify system codecs. Should always return -1 for a user codec.
     *
     * @return -1 for a user codec.
     */
    @Override
    public byte systemCodecID() {
        return -1;
    }
}
