package com.lwz;

/**
 * @author liweizhou 2020/3/29
 */
public class ZClient {

    //同步调用, 返回结果

    //异步调用, 返回future, 可添加onSuccess, onFail

    //实现方式, 单连接/多连接? 连接池. 组装请求req, 注册到selector里, 就是把seq封装成Future加到set里, 然后对seq.wait() maxWait

    //收到响应后, 根据seq找Future, 找到就检查状态, 回应get(). 找不到就找处理器

    //init的时候创建Channel为key,Map<Seq,Future>为value的键值对, 销毁时移除

    //WriteHandler 的时候new ResponseFuture(), 然后塞到Set中ConcurrentHashSet

    //ReadHandler, 从Set中找Future, 把响应setData, 找不到的话, 看策略, 应该是丢弃




    //RestTemplate 自己组装请求信息, 自己解包

    //如果想做成方法调用, 自定义client接口, 自定义方法名, 请求, 响应, 响应如果是Future, 则是异步, 感觉还行, 比较自由, 耦合低

    //要么像thrift一样, 用生成代码的方式, 生成client接口, 还得定义接口idl, 好处是容易拓展跨语言

}
