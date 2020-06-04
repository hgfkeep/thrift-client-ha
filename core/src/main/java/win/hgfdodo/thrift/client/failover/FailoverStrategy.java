package win.hgfdodo.thrift.client.failover;


import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 4:41 下午<br/>
 *
 * Failover 策略
 */
public interface FailoverStrategy<P extends TProtocol> {
    /**
     * thrift server failover 处理逻辑
     * @param thriftServer
     */
    void fail(ThriftServer<P> thriftServer);

    /**
     * 是否使用 backup thrift server
     * @return true 开启使用 backup thrift server
     */
    boolean useBackupServers();

    /**
     * 获取所有的failover的thrift server
     * @return
     */
    Set<ThriftServer<P>> getFailedServers();
}
