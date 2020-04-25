package com.lwz.client;

import com.lwz.annotation.Request;
import lombok.Data;

/**
 * @author liweizhou 2020/4/25
 */
@Data
public class MethodMetadata {

    private Request request;

    private int argsIndex;

    //TODO: returnType

    public MethodMetadata(Request request, int argsIndex) {
        this.request = request;
        this.argsIndex = argsIndex;
    }
}
