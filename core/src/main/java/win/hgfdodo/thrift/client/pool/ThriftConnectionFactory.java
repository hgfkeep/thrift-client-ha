package win.hgfdodo.thrift.client.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;
import win.hgfdodo.thrift.client.failover.ConnectionValidator;
import win.hgfdodo.thrift.client.failover.FailoverChecker;

import java.lang.reflect.Constructor;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:15 下午<br/>
 * <p>
 * ThriftConnection 工厂类， 加工连接。包括连接的声明周期管理。
 */
@Slf4j
public class ThriftConnectionFactory<T extends TTransport, P extends TProtocol> implements KeyedPooledObjectFactory<ThriftServer<P>, T> {
    // thrift client 超时时间
    private final int timeout;

    private FailoverChecker failoverChecker;

    private Class<T> transportClz;

    public ThriftConnectionFactory(int timeout, Class<T> transportClz) {
        this.timeout = timeout;
        this.transportClz = transportClz;
    }

    public ThriftConnectionFactory(int timeout, Class<T> transportClz, FailoverChecker failoverChecker) {
        this.timeout = timeout;
        this.transportClz = transportClz;
        this.failoverChecker = failoverChecker;
    }

    @Override
    public PooledObject<T> makeObject(ThriftServer<P> thriftServer) throws Exception {
        TSocket socket = new TSocket(thriftServer.getHost(), thriftServer.getPort());
        socket.setTimeout(timeout);

        Constructor<T> constructor = transportClz.getConstructor(TTransport.class);
        T transport = constructor.newInstance(socket);
        transport.open();

        DefaultPooledObject<T> unit = new DefaultPooledObject<T>(transport);
        log.trace("Make new thrift connection: {}:{}", thriftServer.getHost(), thriftServer.getPort());
        return unit;
    }

    @Override
    public void destroyObject(ThriftServer<P> thriftServer, PooledObject<T> p) throws Exception {
        TTransport transport = p.getObject();
        if (transport != null) {
            transport.close();
            log.trace("Close thrift connection: {}:{}", thriftServer.getHost(), thriftServer.getPort());
        }
    }

    @Override
    public boolean validateObject(ThriftServer<P> thriftServer, PooledObject<T> p) {
        boolean isValid = false;
        T transport = p.getObject();
        if (failoverChecker == null) {
            isValid = transport.isOpen();
        } else {
            ConnectionValidator connectionValidator = failoverChecker.getConnectionValidator();
            isValid = transport.isOpen() && (connectionValidator == null || connectionValidator.isValid(thriftServer, transport));
        }
        log.trace("Valid {}:{} result: {}", thriftServer.getHost(), thriftServer.getPort(), isValid);
        return isValid;
    }

    @Override
    public void activateObject(ThriftServer<P> key, PooledObject<T> p) throws Exception {

    }

    @Override
    public void passivateObject(ThriftServer<P> key, PooledObject<T> p) throws Exception {

    }


    /**
     * 设置failover的检测器
     *
     * @param failoverChecker
     */
    public void setFailoverChecker(FailoverChecker failoverChecker) {
        this.failoverChecker = failoverChecker;
    }
}
