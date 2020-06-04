package win.hgfdodo.thrift.client.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;
import win.hgfdodo.thrift.client.failover.FailoverChecker;
import win.hgfdodo.thrift.client.provider.ThriftServerProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:33 下午<br/>
 * 默认的thrift 连接池
 */
@Slf4j
public class DefaultThriftConnectionPool implements ThriftConnectionPool {
    // 默认的连接超时时间
    private final static int DEFAULT_CONN_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(5);

    // 连接池的连接
    private final GenericKeyedObjectPool<ThriftServer, TTransport> connections;

    private ThriftServerProvider serverProvider;

    /**
     * 备份server列表
     */
    private ThriftServerProvider backupServerProvider;

    public DefaultThriftConnectionPool(KeyedPooledObjectFactory<ThriftServer, TTransport> pooledObjectFactory, GenericKeyedObjectPoolConfig poolConfig, ThriftServerProvider thriftServerProvider) {
        this(pooledObjectFactory, poolConfig,  thriftServerProvider, null);
    }

    public DefaultThriftConnectionPool(KeyedPooledObjectFactory<ThriftServer, TTransport> pooledObjectFactory, GenericKeyedObjectPoolConfig poolConfig, ThriftServerProvider thriftServerProvider, ThriftServerProvider backupServerProvider) {
        this.connections = new GenericKeyedObjectPool<>(pooledObjectFactory, poolConfig);
        this.serverProvider = thriftServerProvider;
        this.backupServerProvider = backupServerProvider;
    }

    @Override
    public Set<ThriftServer> servers() {
        return this.serverProvider.servers();
    }

    @Override
    public Set<ThriftServer> backupServers() {
        if (this.backupServerProvider != null) {
            return this.backupServerProvider.servers();
        } else {
            return new HashSet<>();
        }
    }

    /**
     * NOTE: getConnection 出现异常时， 可能是 thrift server 的问题，需要触发失效策略
     * @param thriftServer
     * @return
     */
    @Override
    public TTransport getConnection(ThriftServer thriftServer) {
        try {
            return connections.borrowObject(thriftServer);
        } catch (Exception e) {
            log.error("Failed to get connection for {}:{}", thriftServer.getHost(), thriftServer.getPort());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void returnConnection(ThriftServer thriftServer, TTransport transport) {
        connections.returnObject(thriftServer, transport);
    }

    @Override
    public void returnBrokenConnection(ThriftServer thriftServer, TTransport transport) {
        try {
            connections.invalidateObject(thriftServer, transport);
        } catch (Exception e) {
            log.warn("Fail to invalid object: {}, {}", thriftServer, transport, e);
        }
    }

    @Override
    public void close() {
        connections.close();
    }

    @Override
    public void clear(ThriftServer thriftServer) {
        connections.clear(thriftServer);
    }

    public void printPoolState() {
        System.out.println("ACTI: " + connections.getNumActive());
        System.out.println("IDLE: " + connections.getNumIdle());
        System.out.println("WAIT: " + connections.getNumWaiters());
        System.out.println("CREA: " + connections.getCreatedCount());
        System.out.println("BROW: " + connections.getBorrowedCount());
        System.out.println("DEST: " + connections.getDestroyedCount());
        System.out.println("DES-BV: " + connections.getDestroyedByBorrowValidationCount());
        System.out.println("DES-EV: " + connections.getDestroyedByEvictorCount());
        System.out.println("RET: " + connections.getReturnedCount());
        System.out.println("TOTA: " + connections.getMaxTotal());

    }
}
