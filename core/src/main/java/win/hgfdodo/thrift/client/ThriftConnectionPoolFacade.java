package win.hgfdodo.thrift.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import win.hgfdodo.thrift.client.failover.ConnectionValidator;
import win.hgfdodo.thrift.client.failover.DefaultFailoverStrategy;
import win.hgfdodo.thrift.client.failover.FailoverChecker;
import win.hgfdodo.thrift.client.failover.FailoverStrategy;
import win.hgfdodo.thrift.client.pool.*;
import win.hgfdodo.thrift.client.provider.ThriftServerProvider;

import java.util.concurrent.TimeUnit;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/4<br/>
 * Time: 3:43 下午<br/>
 * <p>
 * thrift connection 门面对象
 * 通过build 方法可以构建出thrift connection pool，然后获取
 */
@Slf4j
public class ThriftConnectionPoolFacade {
    private final static int DEFAULT_CONN_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(5);
    private boolean started = false;

    private ThriftConnectionPool thriftConnectionPool;


    private Class<? extends TTransport> transportClass;

    public ThriftConnectionPoolFacade(Class<? extends TTransport> transportClass, ThriftServerProvider thriftServerProvider) {
        this.transportClass = transportClass;
        this.serverProvider = thriftServerProvider;
    }

    /**
     * 连接池配置
     */
    private GenericKeyedObjectPoolConfig poolConfig;

    /**
     * 连接超时时间
     */
    private int connTimeout;

    private ThriftServerProvider serverProvider;

    /**
     * 备份server列表
     */
    private ThriftServerProvider backupServerProvider;

    /**
     * 连接验证工具
     */
    private FailoverChecker failoverChecker;

    /**
     * get Connection 时的ThriftServerSelector
     */
    private ThriftServerSelector thriftServerSelector;

    private KeyedPooledObjectFactory<ThriftServer, TTransport> pooledObjectFactory;


    public ThriftConnectionPoolFacade setThriftConnectionPool(ThriftConnectionPool thriftConnectionPool) {
        this.thriftConnectionPool = thriftConnectionPool;
        return this;
    }

    public ThriftConnectionPoolFacade setPoolConfig(GenericKeyedObjectPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
        return this;
    }

    public ThriftConnectionPoolFacade setConnTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
        return this;
    }

    public ThriftConnectionPoolFacade setServerProvider(ThriftServerProvider serverProvider) {
        this.serverProvider = serverProvider;
        return this;
    }

    public ThriftConnectionPoolFacade setBackupServerProvider(ThriftServerProvider backupServerProvider) {
        this.backupServerProvider = backupServerProvider;
        return this;
    }

    public ThriftConnectionPoolFacade setFailoverChecker(FailoverChecker failoverChecker) {
        this.failoverChecker = failoverChecker;
        return this;
    }

    public ThriftConnectionPoolFacade setThriftServerSelector(ThriftServerSelector thriftServerSelector) {
        this.thriftServerSelector = thriftServerSelector;
        return this;
    }

    public ThriftConnectionPoolFacade setPooledObjectFactory(KeyedPooledObjectFactory<ThriftServer, TTransport> pooledObjectFactory) {
        this.pooledObjectFactory = pooledObjectFactory;
        return this;
    }

    /**
     * 初始化连接池和相关对象
     *
     * @return
     */
    private void checkAndInit() {

        if (this.serverProvider == null) {
            throw new RuntimeException("MUST CONFIG: Thrift server provider!");
        }
        if (this.backupServerProvider == null) {
            log.warn("NO Backup Server!");
        }
        if (this.poolConfig == null) {
            this.poolConfig = new GenericKeyedObjectPoolConfig();
        }

        if (this.connTimeout == 0) {
            this.connTimeout = DEFAULT_CONN_TIMEOUT;
        }

        if (failoverChecker == null) {
            ConnectionValidator connectionValidator = new ConnectionValidator() {
                @Override
                public boolean isValid(ThriftServer thriftServer, TTransport transport) {
                    return thriftServer != null && transport.isOpen();
                }
            };
            FailoverStrategy failoverStrategy = new DefaultFailoverStrategy();
            this.failoverChecker = new FailoverChecker(failoverStrategy, connectionValidator);
        }

        if (pooledObjectFactory == null) {
            this.pooledObjectFactory = new ThriftConnectionFactory(this.connTimeout, transportClass);
        }

        thriftConnectionPool = new DefaultThriftConnectionPool(pooledObjectFactory, poolConfig, serverProvider, backupServerProvider);

        if (this.thriftServerSelector == null) {
            this.thriftServerSelector = new DefaultThriftServerSelector(thriftConnectionPool, failoverChecker);
        }

        started = true;
    }

    /**
     * 获取connection 并构造 ThriftClient
     * <p>
     * 可能会返回
     *
     * @param ifaceClass thrift client 类型
     * @return
     */
    public <X extends TServiceClient> ThriftClient<X> getClient(Class<X> ifaceClass) {
        if (!started) {
            checkAndInit();
        }
        ThriftServer selected = null;
        TTransport transport = null;
        int timesToTry = serverProvider.servers().size() + 1;
        int tried = 0;
        while (tried < timesToTry) {
            selected = this.thriftServerSelector.select();
            log.debug("generate client(type={}) from server={}", ifaceClass, selected);

            try {
                transport = this.thriftConnectionPool.getConnection(selected);
                log.debug("Connection(hash={}) from server={}", transport.hashCode(), selected);
                break;
            } catch (RuntimeException e) {
                if (e != null && e.getCause() instanceof TTransportException) {
                    this.failoverChecker.getFailoverStrategy().fail(selected);
                }
                log.error("GET CONNECTION to {} ERROR: ", selected, e);
            }
            tried += 1;
        }
        return new ThriftClient<X>(selected, transport, ifaceClass, thriftConnectionPool);
    }

    public void status() {
        if (thriftConnectionPool instanceof DefaultThriftConnectionPool) {
            ((DefaultThriftConnectionPool) thriftConnectionPool).printPoolState();
        }
    }
}
