package com.lwz.message;

import java.lang.reflect.Type;

/**
 * @author liweizhou 2020/5/2
 */
public class EncodeObj {

    private Header header;

    private Object[] bodys;

    private Type[] bodyTypes;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Object[] getBodys() {
        return bodys;
    }

    public void setBodys(Object[] bodys) {
        this.bodys = bodys;
    }

    public Type[] getBodyTypes() {
        return bodyTypes;
    }

    public void setBodyTypes(Type[] bodyTypes) {
        this.bodyTypes = bodyTypes;
    }
}
