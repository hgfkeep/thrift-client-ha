# thrift client ha

## 安装

在项目根目录，执行：

```shell script
mvn clean compile package install
```

会把项目中的依赖库安装到本地的 MAVEN 仓库。

## 使用

### 引入依赖

在 pom.xml 中添加 thrift client ha的依赖

```xml
        <!-- thrift connection pool 核心-->
        <dependency>
            <groupId>win.hgfdodo</groupId>
            <artifactId>core</artifactId>
            <version>1.0.0</version>
        </dependency>
        <!-- thrift connection pool， ZK provide thrift server-->
        <dependency>
            <groupId>win.hgfdodo</groupId>
            <artifactId>zk-server-provider</artifactId>
            <version>1.0.0</version>
        </dependency>
```

### 快速使用

1. 构建 `ThriftServerProvider`

```java
StringThriftServerProvider<TBinaryProtocol> stringThriftServerProvider = new StringThriftServerProvider("127.0.0.1:8080;127.0.0.1:8082", TBinaryProtocol.class);
```

2. 构建 `ThriftConnectionPoolFacade`

```java
ThriftConnectionPoolFacade facade = new ThriftConnectionPoolFacade(TFramedTransport.class, stringThriftServerProvider);
```

3. 获取 ThriftClient 对象

```java
ThriftClient<Echo.Client> client = facade.getClient(Echo.Client.class);
```

4. 使用服务: ThriftClient 对象中包装了Server的Client实例，通过 `getClient()` 获取

```java
client.getClient();
```

### 使用细节

* 自定义实现`ThriftServerProvider`接口， 可以实现自定义的ThriftServer的获取方式；
* 自定义实现`ConnectionValidator`接口， 可以精细化的管理连接 有效校验；
* 配置`DefaultFailoverStrategy`中的`failCount`， `failDuration` 可以改变Thrift Server 进入failedCache的灵敏度；

> 具体使用，请查看对应类文件的注释和说明