package win.hgfdodo.thrift.client.failover;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:08 下午<br/>
 *
 * 连接验证器
 */
public interface ConnectionValidator {

    /**
     * 验证connection 是否有效
     *
     * @param thriftServer  ttransport 对应的server信息
     * @param transport 链接
     * @return
     */
    boolean isValid(ThriftServer thriftServer, TTransport transport);
}
