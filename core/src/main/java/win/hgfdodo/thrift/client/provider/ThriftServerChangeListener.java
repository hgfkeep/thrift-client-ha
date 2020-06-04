package win.hgfdodo.thrift.client.provider;

import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:48 下午<br/>
 *
 * Thrift Server信息的监听器， 监听ThriftServer 的 上线和下线事件，并作出响应
 */
public interface ThriftServerChangeListener<P extends TProtocol> {

    /**
     * Thrift Server 上线， 可以加入到连接池集合中
     * @param thriftServer
     */
    void online(ThriftServer<P> thriftServer);

    /**
     * Thrift Server 下线， 则需要将已经加入到连接池集合中的连接设置为断开的连接
     * @param thriftServer
     */
    void offline(ThriftServer<P> thriftServer);
}
