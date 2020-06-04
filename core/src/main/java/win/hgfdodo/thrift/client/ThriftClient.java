package win.hgfdodo.thrift.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import win.hgfdodo.thrift.client.pool.ThriftConnectionPool;

import java.lang.reflect.Constructor;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/2<br/>
 * Time: 3:37 下午<br/>
 * <p>
 * Thrift客户端实例，构建成本比较低，随用随申请，不用就释放
 * <p>
 * 通过 `getClient()` 获取客户端实例，可以获取实际的 Thrift Server Client， 并使用 server 提供的服务。
 * 当 客户端使用完成后，需要使用 `release()` 方法，释放客户端实例获取的链接。
 * <p>
 * * NOTE:
 * * 如果出现了连接故障（特别是之前建立好了，但是后来由于网络问题），仅仅通过 `TTransport.isOpen()` 是没办法办法判断连接是否断开的。
 * * 此时建议先`closeTransport()`, 然后重新获取ThriftClient实例
 */
@Slf4j
public class ThriftClient<X extends TServiceClient> {
    /**
     * 从连接池中获取的 thrift server信息
     */
    private ThriftServer thriftServer;

    /**
     * 从连接池中获取的 thrift connection
     */
    private TTransport transport;

    private final Class<X> iface;


    private final ThriftConnectionPool thriftConnectionPool;

    /**
     * 构造函数
     *
     * @param thriftServer
     * @param transport
     * @param iface
     * @param thriftConnectionPool
     */
    public ThriftClient(ThriftServer thriftServer, TTransport transport, Class<X> iface, ThriftConnectionPool thriftConnectionPool) {
        this.thriftServer = thriftServer;
        this.transport = transport;
        this.iface = iface;
        this.thriftConnectionPool = thriftConnectionPool;
    }

    /**
     * 释放 thrift 连接
     *
     * @return
     */
    public boolean release() {
        log.info("release thrift client connection! transport hash code: {}", transport.hashCode());
        // 简单的帮助验证连接是否有效
        if (this.thriftConnectionPool != null && transport != null) {
            if (transport.isOpen()) {
                thriftConnectionPool.returnConnection(thriftServer, transport);
            } else {
                log.warn("transport(hash={}) closed! return broken connection, server={}", transport.hashCode(), thriftServer);
                thriftConnectionPool.returnBrokenConnection(thriftServer, transport);
            }
            return true;
        } else {
            return true;
        }
    }

    public ThriftServer getThriftServer() {
        return thriftServer;
    }

    /**
     * 获取Thrift client
     *
     * @return
     */
    public X getClient() {
        log.debug("get client from {}", thriftServer);
        try {
            Constructor<? extends TProtocol> constructorY = thriftServer.getServerProtocol().getConstructor(TTransport.class);
            TProtocol protocol = constructorY.newInstance(transport);
            Constructor<X> constructorX = iface.getConstructor(TProtocol.class);
            return constructorX.newInstance(protocol);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭transport。
     * <p>
     * 出现了 Thrift client故障（即TException）， 则自动关闭当前的transport，下次启动时可以连接到最新的transport 上！
     */
    public void closeTransport() {
        this.transport.close();
    }

}
