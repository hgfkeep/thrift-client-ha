package win.hgfdodo.thrift.client.pool;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;

import java.io.Closeable;
import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:04 下午<br/>
 * Thrift 连接池抽象接口
 */
public interface ThriftConnectionPool extends Closeable {

    /**
     * 获取连接池中的所有正常Server
     *
     * @return
     */
    Set<ThriftServer> servers();

    /**
     * 获取连接池中的备份server
     *
     * @return
     */
    Set<ThriftServer> backupServers();

    /**
     * 获取链接
     *
     * @param thriftServer
     * @return
     */
    TTransport getConnection(ThriftServer thriftServer);

    /**
     * 释放连接，返回到连接池
     *
     * @param thriftServer
     * @param transport
     */
    void returnConnection(ThriftServer thriftServer, TTransport transport);

    /**
     * 释放断开的连接，返回到连接池
     *
     * @param thriftServer
     * @param transport
     */
    void returnBrokenConnection(ThriftServer thriftServer, TTransport transport);

    /**
     * 关闭链接
     */
    void close();

    /**
     * 清理与某个thrift server的连接
     *
     * @param thriftServer
     */
    void clear(ThriftServer thriftServer);

}
