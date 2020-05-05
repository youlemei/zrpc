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

最后再看看Spring MVC, Spring MVC并不是RPC框架, 但里面的设计模式却值得参考. 首先, 使用注解@RequestMapping标记服务接口, 服务接口之间是0耦合的, 然后客户端调用接口时, 只需要调用自己关注的接口, 不用引入其他接口.

因此, 最终的设想如下:

- 使用注解@Handler标记方法, 注册服务接口
- 使用Java接口定义需要调用服务接口

这样设计的理由是: 1.服务接口之间耦合低,便于对接口分类; 2.客户端只需要定义自己关注的接口,然后注入接口代理对象即可调用; 3.可根据方法签名做一些定制处理,例如返回类型为Future时可注册异步处理.


#### ZRPC是一个事件驱动的RPC框架. 在ZRPC中, 服务接口的调用/处理是非阻塞的, 同一Socket连接可同时发起/处理多个请求/响应

如图所示

#### 序列化

#### 服务发现

#### 负载均衡

#### 可靠连接

#### 熔断降级



