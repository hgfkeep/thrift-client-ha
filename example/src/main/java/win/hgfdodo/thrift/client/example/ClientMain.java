package win.hgfdodo.thrift.client.example;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import win.hgfdodo.thrift.client.ThriftClient;
import win.hgfdodo.thrift.client.ThriftConnectionPoolFacade;
import win.hgfdodo.thrift.client.example.thrift.Echo;
import win.hgfdodo.thrift.client.provider.StringThriftServerProvider;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/4<br/>
 * Time: 6:16 下午<br/>
 * <p>
 * 核心例子
 */
public class ClientMain {
    public static void main(String[] args) throws TException {
        // 获取thrift server
        StringThriftServerProvider<TBinaryProtocol> stringThriftServerProvider = new StringThriftServerProvider("127.0.0.1:8080;127.0.0.1:8082", TBinaryProtocol.class);
//        ZKThriftServerProvider<TBinaryProtocol> zkThriftServerProvider = new ZKThriftServerProvider("127.0.0.1:2181", "/serveres", TBinaryProtocol.class);

        // 使用门面对象
        ThriftConnectionPoolFacade facade = new ThriftConnectionPoolFacade(TFramedTransport.class, stringThriftServerProvider);

        // 从门面对象中获取 thrift client 抽象
        ThriftClient<Echo.Client> client = facade.getClient(Echo.Client.class);
        try {
            String pong = client.getClient().ping();
            System.out.println(pong);
        } catch (TException e) {
            // 连接出现问题时， 自动断开，下次可以恢复
            e.printStackTrace();
            client.closeTransport();
        }
        client.release();

    }
}
