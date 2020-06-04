package win.hgfdodo.thrift.client.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;
import win.hgfdodo.thrift.client.failover.ConnectionValidator;
import win.hgfdodo.thrift.client.failover.FailoverChecker;
import win.hgfdodo.thrift.client.failover.FailoverStrategy;

import java.util.List;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/2<br/>
 * Time: 10:53 上午<br/>
 * <p>
 * 基于使用次数的thrift server 选择
 */
@Slf4j
public class DefaultThriftServerSelector implements ThriftServerSelector {
    private final FailoverChecker failoverChecker;
    private final ThriftConnectionPool thriftConnectionPool;

    private int index = 0;

    public DefaultThriftServerSelector(ThriftConnectionPool thriftConnectionPool, FailoverChecker failoverChecker) {
        this.failoverChecker = failoverChecker;
        this.thriftConnectionPool = thriftConnectionPool;
    }

    @Override
    public ThriftServer select() {
        List<ThriftServer> servers = getAvailableServers();
        int size = servers.size();
        if (size <= 0) {
            throw new RuntimeException("No Available Server!");
        }
        if (index >= size) {
            index = 0;
        }
        return servers.get(index++);
    }

    @Override
    public List<ThriftServer> getAvailableServers() {
        return this.failoverChecker.getAvailableThriftServers(thriftConnectionPool);
    }

}
