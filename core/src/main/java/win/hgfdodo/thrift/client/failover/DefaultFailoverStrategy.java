package win.hgfdodo.thrift.client.failover;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import lombok.extern.slf4j.Slf4j;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 4:41 下午<br/>
 * <p>
 * 失败后的策略, 包括默认的 失败持续时间， 失败次数， 恢复持续时间等。
 */
@Slf4j
public class DefaultFailoverStrategy implements FailoverStrategy {
    /**
     * 默认的认定thrift server有问题的尝试次数，当fail 次数达到 FAIL_COUNT 时， 会将thrift server 加入到 失败缓存，下次不会使用这个 Thrift server
     *
     * failCount 可控制thrift server 进入failCache的灵敏度
     */
    private static final int DEFAULT_FAIL_COUNT = 3;
    private static final long DEFAULT_FAIL_DURATION = TimeUnit.MINUTES.toMillis(1);
    private static final long DEFAULT_RECOVER_DURATION = TimeUnit.MINUTES.toMillis(1);

    /**
     * 失效的 thrift server 数量，failCount 可控制thrift server 进入failCache的灵敏度， 越低越灵敏
     */
    private final long failCount;

    // fial 持续时间， 越大，当thrift server failed 的时候，就会越长的时间在failCache中。
    private final long failDuration;

    // thrift server failed list
    private final Cache<ThriftServer, Boolean> failedCache;

    //失效的thrift server的加载次数缓存
    private final LoadingCache<ThriftServer, EvictingQueue<Long>> failCountMap;

    // 默认策略
    public DefaultFailoverStrategy() {
        this(DEFAULT_FAIL_COUNT, DEFAULT_FAIL_DURATION, DEFAULT_RECOVER_DURATION);
    }

    /**
     * 自定义 failover 策略
     *
     * @param failCount       失败的最多Thrift server
     * @param failDuration    失效持续时间
     * @param recoverDuration 恢复持续时间， failedCache 在恢复期到了就会主动的将缓存的失效的 thrift Server释放出来
     */
    public DefaultFailoverStrategy(final int failCount, long failDuration, long recoverDuration) {
        this.failCount = failCount;
        this.failDuration = failDuration;
        this.failedCache = CacheBuilder.newBuilder().weakKeys().expireAfterWrite(recoverDuration, TimeUnit.MILLISECONDS).build();
        this.failCountMap = CacheBuilder.newBuilder().weakKeys().build(
                new CacheLoader<ThriftServer, EvictingQueue<Long>>() {

                    @Override
                    public EvictingQueue<Long> load(ThriftServer thriftServer) throws Exception {
                        return EvictingQueue.create(failCount);
                    }
                });
    }

    /**
     * 将失效的连接放入 失效缓存中
     *
     * @param thriftServer
     */
    @Override
    public void fail(ThriftServer thriftServer) {
        log.info("Server {} failed", thriftServer);
        boolean addToFail = false;

        try {
            EvictingQueue<Long> evictingQueue = failCountMap.get(thriftServer);
            synchronized (evictingQueue) {
                evictingQueue.add(System.currentTimeMillis());

                // 最多允许失败failCount 次， 然后第一次fail 和当前fail的时间在failDuration内，则 加入到failed 缓存， 不会继续尝试，直到缓存失效
                if (evictingQueue.remainingCapacity() == 0 && evictingQueue.element() >= (System.currentTimeMillis() - failDuration)) {
                    addToFail = true;
                }
            }
        } catch (ExecutionException e) {
            log.error("ops.", e);
        }
        if (addToFail) {
            failedCache.put(thriftServer, Boolean.TRUE);
            log.info("Server {} failed. add to fail cache");
        }

    }

    @Override
    public boolean useBackupServers() {
        return this.failedCache.size() >= failCount;
    }

    /**
     * 获取失效的 thrift server
     *
     * @return
     */
    @Override
    public Set<ThriftServer> getFailedServers() {
        return failedCache.asMap().keySet();
    }
}
