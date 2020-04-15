package com.lwz.message;

import lombok.Data;

/**
 * @author liweizhou 2020/4/5
 */
@Data
public class ZZPMessage {

    private ZZPHeader header;

    private Object body;

}
