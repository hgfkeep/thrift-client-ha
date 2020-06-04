package win.hgfdodo.thrift.client.provider;

import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 2:53 下午<br/>
 * <p>
 * 提供thrift server的地址
 */
public interface ThriftServerProvider<P extends TProtocol> {

    /**
     * 获取所有可供使用 Thrift server
     *
     * @return
     */
    Set<ThriftServer<P>> servers();
}
