package win.hgfdodo.thrift.client;

import lombok.Data;
import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.utils.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/3<br/>
 * Time: 4:58 下午<br/>
 * <p>
 * Thrift Server 信息, 包括了server的地址，端口和协议类型
 */
@Data
public class ThriftServer<P extends TProtocol> {
    public final static String SERVER_SEPERATOR = ";";
    public final static String HOST_PORT_SEPERATOR = ":";

    /**
     * server 的 host
     */
    private final String host;
    /**
     * server 的 端口
     */
    private final int port;

    private final Class<P> serverProtocol;

    /**
     * Optional: server 权重信息， ThriftServerSelector 选择合适的ThriftServer时的策略因素
     */
    private int weight;

    public ThriftServer(String host, int port, Class<P> serverProtocol) {
        this.host = host;
        this.port = port;
        this.serverProtocol = serverProtocol;
    }

    public Class<P> getServerProtocol() {
        return serverProtocol;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String connectionString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThriftServer that = (ThriftServer) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
