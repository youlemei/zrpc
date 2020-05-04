# ZRPC

An event-driven RPC framework base on Netty.


### Getting Started

```java
// Server - Declare a handler.
@Server
public class HelloHandler {

    @Handler(1)
    public HelloResponse hello(HelloRequest helloRequest) {

        log.info("hello {}", helloRequest);

        HelloResponse helloResponse = new HelloResponse();
        helloResponse.setTime(System.currentTimeMillis());
        return helloResponse;
    }

}
    
// Client - Declare an interface.
@Client("hello")
public interface HelloClient {

    @Request(1)
    HelloResponse hello(HelloRequest helloRequest);

    @Request(1)
    Future<HelloResponse> helloAsync(HelloRequest helloRequest);

}
// Then. You can use HelloClient by @Autowired.
```


### Features

- 二进制协议
- 事件驱动
- 可靠连接
- 服务发现
- 负载均衡
- 熔断降级


#### 协议格式

| 字段 | Uri | Seq | Length | Version | Ext | Body |
| :--: | :--: | :--: | :--: | :--: | :--: | :--: |
| 长度(byte) | 4 | 4 | 4 | 2 | 2 | Length |

- Uri: 请求标识
- Seq: 请求序列号
- Length: Body字节长度
- Version: 版本
- Ext: 拓展字段 (ping/pong/exception/json)

#### 设计思路

基于Netty事件驱动 (造轮子) 的思想, 想做一个基于事件驱动的RPC框架. 

要支持事件驱动, 数据协议很重要, 必须包含Seq字段, 某则在同一Socket中, 只能按照请求的顺序依次响应, 这违背了事件驱动的思想.

因此设计了一个包含Seq的协议, 该协议由Uri, Seq, Length, Version, Ext, Body组成. 

`Uri为请求标识, 服务端可以根据Uri寻找对应的处理器, 处理完成并响应. Seq为请求序列号, 由客户端自动生成, 用于识别响应. Ext拓展了ping/pong心跳功能和body打包格式.`

##### 数据协议有了, 那框架要做成什么样子呢?

我们先看看Dubbo. Dubbo的做法是定义一个Java接口, 然后服务端/客户端双方依赖这个接口文件, 服务端实现该接口提供服务, 客户端通过该接口的代理调用服务. 这种模式是RPC框架常见的形态, 语法简洁, 使用方便.

我们再看看Thrift. Thrift的做法是定义一份IDL文件, 然后生成对应的Java文件. 服务端实现生成的服务接口提供服务, 客户端通过生成的Client调用服务. 这种模式的好处是可拓展多语言.

最后再看看Spring Cloud. Spring Cloud的做法类似Dubbo, 定义一个接口文件, 服务端实现该接口提供服务, 客户端通过该接口的代理调用服务, 不同之处是基于Http协议.



ZRPC也是一个事件驱动的RPC框架. 在ZRPC中, 事件处理是非阻塞的, 同一Socket连接可同时处理多个请求, 并支持异步响应, 
