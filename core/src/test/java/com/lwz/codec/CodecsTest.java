package com.lwz.codec;

import com.lwz.message.Header;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.Test;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author liweizhou 2020/5/2
 */
public class CodecsTest {

    @Test
    public void testCodec() throws Exception {
        Header zzpHeader = new Header();
        zzpHeader.setUri(1);
        zzpHeader.setSeq(2);
        zzpHeader.setLength(3);
        zzpHeader.setVersion((short)4);
        zzpHeader.setExt((short)5);
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();

        int length = Codecs.length(Header.class, zzpHeader);
        Codecs.write(byteBuf, Header.class, zzpHeader);
        Assert.isTrue(length == byteBuf.readableBytes(), "length fail");
        Object header = Codecs.read(byteBuf, Header.class);
        Assert.isTrue(zzpHeader.equals(header), "read write fail");


        Field field = CodecsTest.class.getDeclaredField("TEST_STRUCT");
        Type fieldType = field.getGenericType();
        Object data = field.get(CodecsTest.class);
        length = Codecs.length(fieldType, data);
        Codecs.write(byteBuf, fieldType, data);
        Assert.isTrue(length == byteBuf.readableBytes(), "struct length fail");
        TestStruct<String, Long> struct = (TestStruct<String, Long>) Codecs.read(byteBuf, fieldType);
        Assert.isTrue(TEST_STRUCT.equals(struct), "struct read write fail");


        byteBuf.release();
    }


    static final TestStruct<String, Long> TEST_STRUCT;

    static {
        TestMessage<String, String, Long> testMessage = new TestMessage();
        testMessage.setT("yy");
        testMessage.setList(Arrays.asList("a", "y", "b"));
        HashMap<String, Long> map = new HashMap<>();
        map.put("y", 2L);
        map.put("i", 2L);
        testMessage.setMap(map);
        TEST_STRUCT = new TestStruct(123L, "lwz", Arrays.asList(1L, 2L, 3L), new HashMap<String, Long>(){{
            put("l", 1L);
            put("w", 2L);
            put("z", 3L);
        }}, testMessage);
    }

    /*
    private String[] strings;//Class

    private int[] ints;//Class

    private boolean status;//Class

    private ErrMessage errMessage;//Class

    private Future<ErrMessage> future;//ParameterizedType

    private Future<ErrMessage>[] futureArray;//GenericArrayType

    private T[] ts;//GenericArrayType

    private T t;//TypeVariable

    private Sttuct<String, R> sttuct;//ParameterizedType
    */
}