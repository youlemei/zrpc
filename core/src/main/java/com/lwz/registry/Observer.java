package com.lwz.registry;

/**
 * @author liweizhou 2020/4/17
 */
public interface Observer<T> {

    void update(T obj);

}
