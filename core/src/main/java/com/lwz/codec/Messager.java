package com.lwz.codec;

import com.lwz.annotation.Message;
import com.lwz.message.ZZPHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author liweizhou 2020/4/5
 */
public class Messager {

    public static final short VERSION = 1;

    public static final ConcurrentMap<Integer, Class> URI_CLASS_MAP = new ConcurrentHashMap<>();

    public static <T> T read(ByteBuf byteBuf, Class<T> clz) {
        if (clz == null) {
            return null;
        }
        try {
            Object ret = readSimple(byteBuf, clz);
            if (ret == null) {
                Message message = clz.getAnnotation(Message.class);
                Assert.notNull(message, String.format("%s must has annotation: @Message", clz.getSimpleName()));
                T instance = clz.newInstance();
                Field[] fields = clz.getDeclaredFields();
                List<Field> instanceFields = Arrays.stream(fields)
                        .filter(field -> !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(com.lwz.annotation.Field.class) != null)
                        .sorted(Comparator.comparingInt(o -> o.getAnnotation(com.lwz.annotation.Field.class).value()))
                        .map(field -> {field.setAccessible(true);return field;})
                        .collect(Collectors.toList());
                for (Field field : instanceFields) {
                    Object value = read(byteBuf, field.getType());
                    field.set(instance, value);
                }
                return instance;
            }
            return (T) ret;
        } catch (Exception e) {
            throw new DecoderException(e);
        }
    }

    private static <T> Object readSimple(ByteBuf byteBuf, Class<T> clz) {
        if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            return byteBuf.readBoolean();
        }
        if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            return byteBuf.readByte();
        }
        if (clz.equals(short.class) || clz.equals(Short.class)) {
            return byteBuf.readShort();
        }
        if (clz.equals(int.class) || clz.equals(Integer.class)) {
            return byteBuf.readInt();
        }
        if (clz.equals(long.class) || clz.equals(Long.class)) {
            return byteBuf.readLong();
        }
        if (clz.equals(float.class) || clz.equals(Float.class)) {
            return byteBuf.readFloat();
        }
        if (clz.equals(double.class) || clz.equals(Double.class)) {
            return byteBuf.readDouble();
        }
        if (clz.equals(byte[].class)) {
            int length = byteBuf.readInt();
            byte[] bytes = new byte[length];
            byteBuf.readBytes(bytes);
            return bytes;
        }
        if (clz.equals(String.class)) {
            int length = byteBuf.readInt();
            byte[] bytes = new byte[length];
            byteBuf.readBytes(bytes);
            return new String(bytes);
        }

        if (List.class.isAssignableFrom(clz)) {
            int size = byteBuf.readInt();
        }

        if (Map.class.isAssignableFrom(clz)) {
            int size = byteBuf.readInt();
        }
        return null;
    }

    public static void write(ByteBuf byteBuf, Object data) {
        if (data == null) {
            return;
        }
        try {
            int index = byteBuf.writerIndex();
            writeSimple(byteBuf, data);
            if (byteBuf.writerIndex() == index) {
                Class<?> clz = data.getClass();
                Message message = clz.getAnnotation(Message.class);
                Assert.notNull(message, String.format("%s must has annotation: @Message", clz.getSimpleName()));
                Field[] fields = clz.getDeclaredFields();
                List<Field> instanceFields = Arrays.stream(fields)
                        .filter(field -> !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(com.lwz.annotation.Field.class) != null)
                        .sorted(Comparator.comparingInt(o -> o.getAnnotation(com.lwz.annotation.Field.class).value()))
                        .map(field -> {field.setAccessible(true);return field;})
                        .collect(Collectors.toList());
                for (Field field : instanceFields) {
                    write(byteBuf, field.get(data));
                }
            }
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    private static void writeSimple(ByteBuf byteBuf, Object data) {
        Class<?> clz = data.getClass();
        if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            byteBuf.writeBoolean((boolean) data);
        }
        if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            byteBuf.writeByte((byte) data);
        }
        if (clz.equals(short.class) || clz.equals(Short.class)) {
            byteBuf.writeShort((short) data);
        }
        if (clz.equals(int.class) || clz.equals(Integer.class)) {
            byteBuf.writeInt((int) data);
        }
        if (clz.equals(long.class) || clz.equals(Long.class)) {
            byteBuf.writeLong((long) data);
        }
        if (clz.equals(float.class) || clz.equals(Float.class)) {
            byteBuf.writeFloat((float) data);
        }
        if (clz.equals(double.class) || clz.equals(Double.class)) {
            byteBuf.writeDouble((double) data);
        }
        if (clz.equals(byte[].class)) {
            byte[] bytes = (byte[]) data;
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
        if (clz.equals(String.class)) {
            byte[] bytes = data.toString().getBytes();
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }

        if (List.class.isAssignableFrom(clz)) {
            int size = byteBuf.readInt();
        }

        if (Map.class.isAssignableFrom(clz)) {
            int size = byteBuf.readInt();
        }
    }

    public static int getLength(Object data) {
        if (data == null) {
            return 0;
        }
        int length = simpleLength(data);
        if (length == 0) {
            Class<?> clz = data.getClass();
            Message message = clz.getAnnotation(Message.class);
            Assert.notNull(message, String.format("%s must has annotation: @Message", clz.getSimpleName()));
            Field[] fields = clz.getDeclaredFields();
            List<Field> instanceFields = Arrays.stream(fields)
                    .filter(field -> !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(com.lwz.annotation.Field.class) != null)
                    .sorted(Comparator.comparingInt(o -> o.getAnnotation(com.lwz.annotation.Field.class).value()))
                    .map(field -> {field.setAccessible(true);return field;})
                    .collect(Collectors.toList());
            int len = instanceFields.stream().mapToInt(field -> {
                try {
                    return getLength(field.get(data));
                } catch (Exception e) {
                    throw new EncoderException(e);
                }
            }).sum();
            return len;
        }
        return length;
    }

    private static int simpleLength(Object data) {
        Class<?> clz = data.getClass();
        if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            return 1;
        }
        if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            return 1;
        }
        if (clz.equals(short.class) || clz.equals(Short.class)) {
            return 2;
        }
        if (clz.equals(int.class) || clz.equals(Integer.class)) {
            return 4;
        }
        if (clz.equals(long.class) || clz.equals(Long.class)) {
            return 8;
        }
        if (clz.equals(float.class) || clz.equals(Float.class)) {
            return 4;
        }
        if (clz.equals(double.class) || clz.equals(Double.class)) {
            return 8;
        }
        if (clz.equals(byte[].class)) {
            byte[] bytes = (byte[]) data;
            return 4 + bytes.length;
        }
        if (clz.equals(String.class)) {
            byte[] bytes = data.toString().getBytes();
            return 4 + bytes.length;
        }

        if (List.class.isAssignableFrom(clz)) {
        }

        if (Map.class.isAssignableFrom(clz)) {
        }
        return 0;
    }

    public static void main(String[] args) throws Exception {

        ZZPHeader zzpHeader = new ZZPHeader();
        zzpHeader.setUri(5);
        zzpHeader.setSeq(4);
        zzpHeader.setLength(3);
        zzpHeader.setVersion((short)4);
        zzpHeader.setExt((short)5);

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        write(byteBuf, zzpHeader);

        ZZPHeader header = read(byteBuf, ZZPHeader.class);

        System.out.println(header);

    }


}

