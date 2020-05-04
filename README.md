# zrpc
An RPC framework base on Netty.

### Preview

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


