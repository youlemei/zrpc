package com.lwz.message;

import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author liweizhou 2020/5/2
 */
@Data
public class EncodeObj {

    private Header header;

    private Object[] bodys;

    private Type[] bodyTypes;
    
}
