package win.hgfdodo.thrift.client.failover;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.transport.TTransport;
import win.hgfdodo.thrift.client.ThriftServer;
import win.hgfdodo.thrift.client.pool.ThriftConnectionPool;
import win.hgfdodo.thrift.client.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:27 下午<br/>
 * <p>
 * 验证和处理失效 连接 的核心工具
 */
@Slf4j
public class FailoverChecker {
    // 默认的检查间隔3m
    private final long DEFAULT_CHECK_INTERVAL = 3 * 60 * 1000;

    private final FailoverStrategy failoverStrategy;
    private final ConnectionValidator connectionValidator;

    public FailoverChecker(FailoverStrategy failoverStrategy, ConnectionValidator connectionValidator) {
        this.connectionValidator = connectionValidator;
        this.failoverStrategy = failoverStrategy;
    }

    /**
     * 验证  thriftConnectionPool 中所有 thrift server 是否可用
     *
     * @param thriftConnectionPool 待验证的连接池
     */
    public void check(ThriftConnectionPool thriftConnectionPool) {
        List<ThriftServer> thriftServerList = getAvailableThriftServers(thriftConnectionPool);
        for (ThriftServer thriftServer : thriftServerList) {
            checkThriftServer(thriftServer, thriftConnectionPool);
        }
    }

    /**
     * 验证 连接池中的某个thrift server
     *
     * @param thriftServer
     * @param thriftConnectionPool
     */
    public void checkThriftServer(ThriftServer thriftServer, ThriftConnectionPool thriftConnectionPool) {
        TTransport transport = null;
        boolean valid = false;
        try {
            transport = thriftConnectionPool.getConnection(thriftServer);
            if (transport != null) {
                valid = connectionValidator.isValid(thriftServer, transport);
            }
        } catch (Exception e) {
            log.warn("Valid thrift server error, thrift server={}", thriftServer, e);
        } finally {
            // 将新增的验证失败的thriftServer， 进行失效策略处理
            if (transport != null) {
                if (valid) {
                    thriftConnectionPool.returnConnection(thriftServer, transport);
                } else {
                    log.warn("Invalid Connection: hash={}, server={}", transport.hashCode(), thriftServer);
                    failoverStrategy.fail(thriftServer);
                    thriftConnectionPool.returnBrokenConnection(thriftServer, transport);
                }
            } else {
                failoverStrategy.fail(thriftServer);
            }
        }
    }

    /**
     * 获取thrift connection 中的连接，并验证连接的有效性
     * <p>
     *
     * @return
     */
    public List<ThriftServer> getAvailableThriftServers(ThriftConnectionPool thriftConnectionPool) {
        // 两次连接间无错误
        List<ThriftServer> res = new ArrayList<>();


        // 验证所有thrift server 是否可用
        for (ThriftServer server : thriftConnectionPool.servers()) {
            checkThriftServer(server, thriftConnectionPool);
        }

        // 过滤掉failed 的server
        Set<ThriftServer> failedServers = failoverStrategy.getFailedServers();
        for (ThriftServer thriftServer : thriftConnectionPool.servers()) {
            if (!failedServers.contains(thriftServer)) {
                res.add(thriftServer);
            }
        }

        //策略满足使用 backup server 的条件 或者 res 中无可用thrift server后，增加backup server
        if ((failoverStrategy.useBackupServers() || CollectionUtils.isEmpty(res)) && thriftConnectionPool.backupServers() != null) {
            for (ThriftServer thriftServer : thriftConnectionPool.backupServers()) {
                if (!failedServers.contains(thriftServer)) {
                    res.add(thriftServer);
                }
            }
        }

        return res;
    }

    public ConnectionValidator getConnectionValidator() {
        return connectionValidator;
    }

    public FailoverStrategy getFailoverStrategy() {
        return failoverStrategy;
    }
}
