package win.hgfdodo.thrift.client.provider;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.ZKPaths;
import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;
import win.hgfdodo.thrift.client.utils.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/1<br/>
 * Time: 3:39 下午<br/>
 * 从ZK上获取节点信息
 * <p>
 * zk serversParentPath 下所有节点的value 表示所有待链接的thrift server。
 */
@Slf4j
public class ZKThriftServerProvider<P extends TProtocol> implements DynamicThriftServerProvider<P> {
    // zk 链接信息
    private final CuratorFramework zkClient;

    // 所有节点的
    private final String serversParentPath;

    private CopyOnWriteArraySet<ThriftServer<P>> servers;
    private PathChildrenCache pathChildrenCache = null;
    private Class<P> protocol;

    public ZKThriftServerProvider(String zkConnectionString, String serversParentPath, Class<P> protocol) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(zkConnectionString, new RetryNTimes(3, 300));
        client.start();
        this.zkClient = client;
        this.serversParentPath = serversParentPath;
        this.servers = new CopyOnWriteArraySet();
        this.protocol = protocol;
    }

    public ZKThriftServerProvider(CuratorFramework zkClient, String serversParentPath, Class<P> protocol) {
        this.zkClient = zkClient;
        this.serversParentPath = serversParentPath;
        this.servers = new CopyOnWriteArraySet();
        this.protocol = protocol;
    }

    @Override
    public Set<ThriftServer<P>> servers() {
        synchronized (this) {
            if (pathChildrenCache == null) {
                this.pathChildrenCache = new PathChildrenCache(zkClient, serversParentPath, true);
                this.pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                        List<ChildData> nodes = event.getInitialData();
                        byte[] node = event.getData().getData();
                        ThriftServer<P> server;
                        switch (event.getType()) {
                            case CHILD_REMOVED:
                                server = getThriftFromNodeData(node, event.getData().getPath());
                                if (server != null) {
                                    // thrift server 移除了
                                    servers.remove(server);
                                    offline(server);
                                }
                                break;
                            case CHILD_UPDATED:
                                server = getThriftFromNodeData(node, event.getData().getPath());
                                if (server != null) {
                                    // thrift server 更新了
                                    HashMap<String, ChildData> originNodes = new HashMap<>();
                                    for (ChildData childData : nodes) {
                                        originNodes.put(childData.getPath(), childData);
                                    }
                                    ChildData origin = null;
                                    if ((origin = originNodes.get(event.getData().getPath())) != null) {
                                        ThriftServer<P> originServer = getThriftFromNodeData(origin.getData(), origin.getPath());
                                        offline(originServer);
                                        servers.remove(originServer);
                                        ThriftServer<P> now = getThriftFromNodeData(event.getData().getData(), event.getData().getPath());
                                        online(now);
                                        servers.add(now);
                                    }
                                }
                                break;
                            case CHILD_ADDED:
                                server = getThriftFromNodeData(node, event.getData().getPath());
                                if (server != null) {
                                    // thrift server 增加了
                                    servers.add(server);
                                    online(server);
                                }
                                break;
                        }
                    }
                }, Executors.newSingleThreadExecutor());
                try {
                    this.pathChildrenCache.start();
                } catch (Exception e) {
                    log.error("Get Zk thrift servers node:port information error!", e);
                    throw new RuntimeException(e);
                }
            }
        }
        if (CollectionUtils.isEmpty(this.servers)) {
            try {
                List<String> nodes = this.zkClient.getChildren().forPath(serversParentPath);
                for (String node : nodes) {
                    byte[] data = this.zkClient.getData().forPath(ZKPaths.makePath(serversParentPath, node));
                    ThriftServer<P> server = StringThriftServerProvider.parseConnectionString(new String(data), protocol);
                    this.servers.add(server);
                }
            } catch (Exception e) {
                throw new RuntimeException("Get thrift server From zNode error", e);
            }
        }
        return this.servers;

    }

    private ThriftServer<P> getThriftFromNodeData(byte[] data, String path) {
        try {
            ThriftServer<P> server = StringThriftServerProvider.parseConnectionString(new String(data), protocol);
            return server;
        } catch (Exception e) {
            log.warn("NODE data is not connectionString. node={}", path);
        }
        return null;
    }


    @Override
    public void online(ThriftServer<P> thriftServer) {
        this.servers.add(thriftServer);
    }

    @Override
    public void offline(ThriftServer<P> thriftServer) {
        this.servers.remove(thriftServer);
    }
}
