package win.hgfdodo.thrift.client.pool;

import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.List;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/2<br/>
 * Time: 10:51 上午<br/>
 * 客户端链接选择器
 */
public interface ThriftServerSelector<P extends TProtocol> {
    /**
     * 选择thrift server 去对客户端服务
     * @return
     */
    ThriftServer<P> select();

    /**
     * 获取所有的可供选择的服务提供者
     * @return
     */
    List<ThriftServer<P>> getAvailableServers();
}
