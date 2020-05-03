package com.lwz.codec;

import com.lwz.annotation.Message;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * TODO: null
 *
 * @author liweizhou 2020/5/2
 */
public class ZrpcCodecs {

    public static final String CHARSET = "UTF-8";

    public static final short VERSION = 1;

    private static final ConcurrentMap<Class, List<Field>> CLASS_CACHE_MAP = new ConcurrentHashMap<>();

    public static <T> T read(ByteBuf byteBuf, Class<T> clz) {
        return (T) read(byteBuf, (Type) clz);
    }

    public static Object read(ByteBuf byteBuf, Type type) {
        if (type == null) {
            return null;
        }
        try {
            return read(byteBuf, type, null);
        } catch (Exception e) {
            throw new DecoderException(e);
        }
    }

    private static Object read(ByteBuf byteBuf, Type type, Map<TypeVariable, Type> actualTypeMap) throws Exception {

        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type itemType = genericArrayType.getGenericComponentType();
            int length = byteBuf.readInt();
            Object[] array = new Object[length];
            for (int i = 0; i < length; i++) {
                Object item = read(byteBuf, itemType, actualTypeMap);
                array[i] = item;
            }
            return array;

        } else if (type instanceof TypeVariable) {
            if (actualTypeMap != null) {
                Type actualType = actualTypeMap.get(type);
                if (actualType != null) {
                    return read(byteBuf, actualType, null);
                }
            }

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class clz = (Class) parameterizedType.getRawType();
            if (List.class.isAssignableFrom(clz)) {
                int size = byteBuf.readInt();
                List list = new ArrayList(size);
                Type itemType = parameterizedType.getActualTypeArguments()[0];
                for (int i = 0; i < size; i++) {
                    Object item = read(byteBuf, itemType, actualTypeMap);
                    list.add(item);
                }
                return list;
            } else if (Set.class.isAssignableFrom(clz)) {
                int size = byteBuf.readInt();
                Set set = new HashSet(size);
                Type itemType = parameterizedType.getActualTypeArguments()[0];
                for (int i = 0; i < size; i++) {
                    Object item = read(byteBuf, itemType, actualTypeMap);
                    set.add(item);
                }
                return set;
            } else if (Map.class.isAssignableFrom(clz)) {
                int size = byteBuf.readInt();
                Map map = new HashMap(size);
                Type keyType = parameterizedType.getActualTypeArguments()[0];
                Type valueType = parameterizedType.getActualTypeArguments()[1];
                for (int i = 0; i < size; i++) {
                    Object key = read(byteBuf, keyType, actualTypeMap);
                    Object value = read(byteBuf, valueType, actualTypeMap);
                    map.put(key, value);
                }
                return map;
            } else if (clz.getAnnotation(Message.class) != null) {
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                TypeVariable[] clzTypes = clz.getTypeParameters();
                Map<TypeVariable, Type> clzActualTypeMap = new HashMap<>();
                for (int i = 0; i < clzTypes.length; i++) {
                    Type actualType = actualTypeMap != null ? actualTypeMap.getOrDefault(actualTypes[i], actualTypes[i]) : actualTypes[i];
                    clzActualTypeMap.put(clzTypes[i], actualType);
                }
                return readMessage(byteBuf, clzActualTypeMap, clz);
            }

        } else if (type instanceof Class) {
            Class clz = (Class) type;
            if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                return byteBuf.readBoolean();
            } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                return byteBuf.readByte();
            } else if (clz.equals(short.class) || clz.equals(Short.class)) {
                return byteBuf.readShort();
            } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
                return byteBuf.readInt();
            } else if (clz.equals(long.class) || clz.equals(Long.class)) {
                return byteBuf.readLong();
            } else if (clz.equals(float.class) || clz.equals(Float.class)) {
                return byteBuf.readFloat();
            } else if (clz.equals(double.class) || clz.equals(Double.class)) {
                return byteBuf.readDouble();
            } else if (clz.equals(byte[].class)) {
                int length = byteBuf.readInt();
                byte[] bytes = new byte[length];
                byteBuf.readBytes(bytes);
                return bytes;
            } else if (clz.equals(String.class)) {
                int length = byteBuf.readInt();
                byte[] bytes = new byte[length];
                byteBuf.readBytes(bytes);
                return new String(bytes, CHARSET);
            } else if (clz.getAnnotation(Message.class) != null) {
                return readMessage(byteBuf, actualTypeMap, clz);
            } else if (clz.equals(void.class)) {
                return null;
            }
        }

        throw new IllegalArgumentException(String.format("'type' %s not support. (tips: struct must annotation @Message)", type.getTypeName()));
    }

    private static Object readMessage(ByteBuf byteBuf, Map<TypeVariable, Type> actualTypeMap, Class clz) throws Exception {
        Object instance = clz.newInstance();
        List<Field> instanceFields = getMessageFields(clz);
        for (Field field : instanceFields) {
            Type fieldType = field.getGenericType();
            Object value = read(byteBuf, fieldType, actualTypeMap);
            field.set(instance, value);
        }
        return instance;
    }

    private static List<Field> getMessageFields(Class clz) {
        return CLASS_CACHE_MAP.computeIfAbsent(clz, c -> {
            Field[] fields = clz.getDeclaredFields();
            List<Field> instanceFields = Arrays.stream(fields)
                    .filter(field -> !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(com.lwz.annotation.Field.class) != null)
                    .sorted(Comparator.comparingInt(o -> o.getAnnotation(com.lwz.annotation.Field.class).value()))
                    .map(field -> {
                        field.setAccessible(true);
                        return field;
                    })
                    .collect(Collectors.toList());
            for (int i = 0; i < instanceFields.size(); i++) {
                Assert.isTrue(instanceFields.get(i).getAnnotation(com.lwz.annotation.Field.class).value() == i + 1,
                        String.format("Filed in Message must be sorted and start at 1. 'clz': %s", clz.getName()));
            }
            return instanceFields;
        });
    }

    public static void write(ByteBuf byteBuf, Type type, Object data) {
        if (type == null) {
            return;
        }
        try {
            write(byteBuf, type, null, data);
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    private static void write(ByteBuf byteBuf, Type type, Map<TypeVariable, Type> actualTypeMap, Object data) throws Exception {

        int writeIndex = byteBuf.writerIndex();
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type itemType = genericArrayType.getGenericComponentType();
            if (data == null) {
                byteBuf.writeInt(0);
            } else {
                Object[] array = (Object[]) data;
                byteBuf.writeInt(array.length);
                for (Object item : array) {
                    write(byteBuf, itemType, actualTypeMap, item);
                }
            }

        } else if (type instanceof TypeVariable) {
            if (actualTypeMap != null) {
                Type actualType = actualTypeMap.get(type);
                if (actualType != null) {
                    write(byteBuf, actualType, null, data);
                }
            }

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class clz = (Class) parameterizedType.getRawType();
            if (List.class.isAssignableFrom(clz)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    List list = (List) data;
                    byteBuf.writeInt(list.size());
                    Type itemType = parameterizedType.getActualTypeArguments()[0];
                    for (Object item : list) {
                        write(byteBuf, itemType, actualTypeMap, item);
                    }
                }
            } else if (Set.class.isAssignableFrom(clz)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    Set set = (Set) data;
                    byteBuf.writeInt(set.size());
                    Type itemType = parameterizedType.getActualTypeArguments()[0];
                    for (Object item : set) {
                        write(byteBuf, itemType, actualTypeMap, item);
                    }
                }
            } else if (Map.class.isAssignableFrom(clz)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    Map<Object, Object> map = (Map) data;
                    byteBuf.writeInt(map.size());
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];
                    for (Map.Entry entry : map.entrySet()) {
                        write(byteBuf, keyType, actualTypeMap, entry.getKey());
                        write(byteBuf, valueType, actualTypeMap, entry.getValue());
                    }
                }

            } else if (clz.getAnnotation(Message.class) != null) {
                Map clzactualTypeMap = new HashMap<>();
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                TypeVariable[] clzTypes = clz.getTypeParameters();
                for (int i = 0; i < clzTypes.length; i++) {
                    Type actualType = actualTypeMap != null ? actualTypeMap.getOrDefault(actualTypes[i], actualTypes[i]) : actualTypes[i];
                    clzactualTypeMap.put(clzTypes[i], actualType);
                }
                writeMessage(byteBuf, clzactualTypeMap, data, clz);
            }

        } else if (type instanceof Class) {
            Class clz = (Class) type;
            if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                if (data == null) {
                    byteBuf.writeBoolean(false);
                } else {
                    byteBuf.writeBoolean((boolean) data);
                }
            } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                if (data == null) {
                    byteBuf.writeByte(0);
                } else {
                    byteBuf.writeByte((byte) data);
                }
            } else if (clz.equals(short.class) || clz.equals(Short.class)) {
                if (data == null) {
                    byteBuf.writeShort(0);
                } else {
                    byteBuf.writeShort((short) data);
                }
            } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    byteBuf.writeInt((int) data);
                }
            } else if (clz.equals(long.class) || clz.equals(Long.class)) {
                if (data == null) {
                    byteBuf.writeLong(0);
                } else {
                    byteBuf.writeLong((long) data);
                }
            } else if (clz.equals(float.class) || clz.equals(Float.class)) {
                if (data == null) {
                    byteBuf.writeFloat(0);
                } else {
                    byteBuf.writeFloat((float) data);
                }
            } else if (clz.equals(double.class) || clz.equals(Double.class)) {
                if (data == null) {
                    byteBuf.writeDouble(0);
                } else {
                    byteBuf.writeDouble((double) data);
                }
            } else if (clz.equals(byte[].class)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    byte[] bytes = (byte[]) data;
                    byteBuf.writeInt(bytes.length);
                    byteBuf.writeBytes(bytes);
                }
            } else if (clz.equals(String.class)) {
                if (data == null) {
                    byteBuf.writeInt(0);
                } else {
                    byte[] bytes = data.toString().getBytes(CHARSET);
                    byteBuf.writeInt(bytes.length);
                    byteBuf.writeBytes(bytes);
                }
            } else if (clz.getAnnotation(Message.class) != null) {
                writeMessage(byteBuf, actualTypeMap, data, clz);
            } else if (clz.equals(void.class)) {
                return;
            }
        }

        if (writeIndex == byteBuf.writerIndex()) {
            throw new IllegalArgumentException(String.format("'type' %s not support. (tips: struct must annotation @Message)", type.getTypeName()));
        }
    }

    private static void writeMessage(ByteBuf byteBuf, Map<TypeVariable, Type> actualTypeMap, Object data, Class clz) throws Exception {
        List<Field> instanceFields = getMessageFields(clz);
        for (Field field : instanceFields) {
            Type fieldType = field.getGenericType();
            Object fieldValue = data != null ? field.get(data) : null;
            write(byteBuf, fieldType, actualTypeMap, fieldValue);
        }
    }

    public static int length(Type type, Object data) {
        if (type == null) {
            return 0;
        }
        try {
            return length(type, null, data);
        } catch (Exception e) {
            throw new EncoderException(e);
        }
    }

    private static int length(Type type, Map<TypeVariable, Type> actualTypeMap, Object data) throws Exception {

        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type itemType = genericArrayType.getGenericComponentType();
            int length = 4;
            if (data != null) {
                Object[] array = (Object[]) data;
                for (Object item : array) {
                    length += length(itemType, actualTypeMap, item);
                }
            }
            return length;

        } else if (type instanceof TypeVariable) {
            if (actualTypeMap != null) {
                Type actualType = actualTypeMap.get(type);
                if (actualType != null) {
                    return length(actualType, null, data);
                }
            }

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class clz = (Class) parameterizedType.getRawType();
            if (List.class.isAssignableFrom(clz)) {
                int length = 4;
                if (data != null) {
                    List list = (List) data;
                    Type itemType = parameterizedType.getActualTypeArguments()[0];
                    for (Object item : list) {
                        length += length(itemType, actualTypeMap, item);
                    }
                }
                return length;
            } else if (Set.class.isAssignableFrom(clz)) {
                int length = 4;
                if (data != null) {
                    Set set = (Set) data;
                    Type itemType = parameterizedType.getActualTypeArguments()[0];
                    for (Object item : set) {
                        length += length(itemType, actualTypeMap, item);
                    }
                }
                return length;
            } else if (Map.class.isAssignableFrom(clz)) {
                int length = 4;
                if (data != null) {
                    Map<Object, Object> map = (Map) data;
                    Type keyType = parameterizedType.getActualTypeArguments()[0];
                    Type valueType = parameterizedType.getActualTypeArguments()[1];
                    for (Map.Entry entry : map.entrySet()) {
                        length += length(keyType, actualTypeMap, entry.getKey());
                        length += length(valueType, actualTypeMap, entry.getValue());
                    }
                }
                return length;

            } else if (clz.getAnnotation(Message.class) != null) {
                Map clzactualTypeMap = new HashMap<>();
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                TypeVariable[] clzTypes = clz.getTypeParameters();
                for (int i = 0; i < clzTypes.length; i++) {
                    Type actualType = actualTypeMap != null ? actualTypeMap.getOrDefault(actualTypes[i], actualTypes[i]) : actualTypes[i];
                    clzactualTypeMap.put(clzTypes[i], actualType);
                }
                return messageLength(clzactualTypeMap, data, clz);
            }

        } else if (type instanceof Class) {
            Class clz = (Class) type;
            if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
                return 1;
            } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
                return 1;
            } else if (clz.equals(short.class) || clz.equals(Short.class)) {
                return 2;
            } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
                return 4;
            } else if (clz.equals(long.class) || clz.equals(Long.class)) {
                return 8;
            } else if (clz.equals(float.class) || clz.equals(Float.class)) {
                return 4;
            } else if (clz.equals(double.class) || clz.equals(Double.class)) {
                return 8;
            } else if (clz.equals(byte[].class)) {
                int length = 4;
                if (data != null) {
                    byte[] bytes = (byte[]) data;
                    length += bytes.length;
                }
                return length;
            } else if (clz.equals(String.class)) {
                int length = 4;
                if (data != null) {
                    byte[] bytes =  data.toString().getBytes(CHARSET);
                    length += bytes.length;
                }
                return length;
            } else if (clz.getAnnotation(Message.class) != null) {
                return messageLength(actualTypeMap, data, clz);
            } else if (clz.equals(void.class)) {
                return 0;
            }
        }

        throw new IllegalArgumentException(String.format("'type' %s not support. (tips: struct must annotation @Message)", type.getTypeName()));
    }

    private static int messageLength(Map<TypeVariable, Type> actualTypeMap, Object data, Class clz) throws Exception {
        int length = 0;
        List<Field> instanceFields = getMessageFields(clz);
        for (Field field : instanceFields) {
            Type fieldType = field.getGenericType();
            Object fieldValue = data != null ? field.get(data) : null;
            length += length(fieldType, actualTypeMap, fieldValue);
        }
        return length;
    }

}
