package com.lwz.codec;

import com.alibaba.fastjson.JSON;
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
public class ZrpcCodecsTest {

    @Test
    public void testCodec() throws Exception {
        Header zzpHeader = new Header();
        zzpHeader.setUri(1);
        zzpHeader.setSeq(2);
        zzpHeader.setLength(3);
        zzpHeader.setVersion((short)4);
        zzpHeader.setExt((short)5);
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();

        int length = ZrpcCodecs.length(Header.class, zzpHeader);
        ZrpcCodecs.write(byteBuf, Header.class, zzpHeader);
        Assert.isTrue(length == byteBuf.readableBytes(), "length fail");
        Object header = ZrpcCodecs.read(byteBuf, Header.class);
        Assert.isTrue(zzpHeader.equals(header), "read write fail");


        Field field = ZrpcCodecsTest.class.getDeclaredField("TEST_STRUCT");
        Type fieldType = field.getGenericType();
        Object data = field.get(ZrpcCodecsTest.class);
        length = ZrpcCodecs.length(fieldType, data);
        ZrpcCodecs.write(byteBuf, fieldType, data);
        System.out.println(length);
        Assert.isTrue(length == byteBuf.readableBytes(), "struct length fail");
        TestStruct<String, Long> struct = (TestStruct<String, Long>) ZrpcCodecs.read(byteBuf, fieldType);
        Assert.isTrue(TEST_STRUCT.equals(struct), "struct read write fail");


        byteBuf.release();
    }

    @Test
    public void testJSON() throws Exception{
        //事实上, json只在传输数字上比较吃亏
        System.out.println(JSON.toJSONString(TEST_STRUCT).getBytes().length);
    }


    static final TestStruct<String, Long> TEST_STRUCT;

    static {
        TestMessage<String, String, Long> testMessage = new TestMessage();
        testMessage.setT("yy");
        testMessage.setList(Arrays.asList("abcd", "yy isd y", "bbbbb"));
        HashMap<String, Long> map = new HashMap<>();
        map.put("yy", 222222L);
        map.put("ii", 233333L);
        testMessage.setMap(map);
        TEST_STRUCT = new TestStruct(123L, "what are", Arrays.asList(11111L, 222222L, 333333L), new HashMap<String, Long>(){{
            put("lwwer", 19999L);
            put("weedfd", 2999L);
            put("z4534s", 3999L);
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