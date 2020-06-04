package win.hgfdodo.thrift.client.provider;

import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/2<br/>
 * Time: 10:45 上午<br/>
 * <p>
 * 默认的 thrift server provider
 */
public class DefaultThriftServerProvider<P extends TProtocol> implements ThriftServerProvider<P> {
    private Set<ThriftServer<P>> servers;

    public DefaultThriftServerProvider(Set<ThriftServer<P>> servers) {
        this.servers = servers;
    }

    @Override
    public Set<ThriftServer<P>> servers() {
        return this.servers;
    }

    /**
     * 添加thrift server
     *
     * @param thriftServer
     */
    public void addThriftServer(ThriftServer<P> thriftServer) {
        this.servers.add(thriftServer);
    }

    /**
     * 删除 thrift server
     *
     * @param thriftServer
     * @return
     */
    public boolean rmThriftServer(ThriftServer<P> thriftServer) {
        return this.servers.remove(thriftServer);
    }
}
