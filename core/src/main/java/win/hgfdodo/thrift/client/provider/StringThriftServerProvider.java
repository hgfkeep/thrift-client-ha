package win.hgfdodo.thrift.client.provider;

import org.apache.thrift.protocol.TProtocol;
import win.hgfdodo.thrift.client.ThriftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/3<br/>
 * Time: 5:54 下午<br/>
 * <p>
 * 从字符串中提供的Thrift server信息。
 * <p>
 * 默认分割符： 使用`;`作为两个thrift server间的分割符， `:`作为host 和port 间的分割符
 */
public class StringThriftServerProvider<P extends TProtocol> implements ThriftServerProvider<P> {
    public final static String DEFAULT_SERVER_SEPERATOR = ";";
    public final static String DEFAULT_HOST_PORT_SEPERATOR = ":";

    private final Set<ThriftServer<P>> servers;

    // host 和port间的分割符
    private String hostPortSeperator = DEFAULT_HOST_PORT_SEPERATOR;

    //thrift server间的分割符
    private String serverSeperator = DEFAULT_SERVER_SEPERATOR;

    public StringThriftServerProvider(String serverString, Class<P> protocol) {
        this.servers = getServerFromString(serverString, protocol);
    }

    /**
     * 使用用户自定义的分割字符串分割
     *
     * @param serverString
     * @param hostPortSeperator
     * @param serverSeperator
     */
    public StringThriftServerProvider(String serverString, String hostPortSeperator, String serverSeperator, Class<P> protocol) {
        this.hostPortSeperator = hostPortSeperator;
        this.serverSeperator = serverSeperator;
        this.servers = getServerFromString(serverString, protocol);
    }

    public Set<ThriftServer<P>> getServerFromString(String serverString, Class<P> protocol) {
        return parseThriftServers(serverString, hostPortSeperator, serverSeperator, protocol);
    }

    public static <P extends TProtocol> Set<ThriftServer<P>> parseThriftServers(String serverString, Class<P> protocol) {
        return parseThriftServers(serverString, DEFAULT_HOST_PORT_SEPERATOR, DEFAULT_SERVER_SEPERATOR, protocol);
    }

    public static <P extends TProtocol> Set<ThriftServer<P>> parseThriftServers(String serverString, String hostPortSeperator, String serverSeperator, Class<P> protocol) {
        Set<ThriftServer<P>> servers = new HashSet<>();
        String[] serversStr = serverString.split(serverSeperator);
        for (String serverStr : serversStr) {
            servers.add(parseConnectionString(serverStr, hostPortSeperator, protocol));
        }
        return servers;
    }

    public static <P extends TProtocol> ThriftServer<P> parseConnectionString(String serverStr, Class<P> protocol) {
        return parseConnectionString(serverStr, DEFAULT_HOST_PORT_SEPERATOR, protocol);
    }

    public static <P extends TProtocol> ThriftServer<P> parseConnectionString(String serverStr, String hostPortSeperator, Class<P> protocol) {
        int index = serverStr.indexOf(hostPortSeperator);
        String host = null;
        int port = 80;
        if (index != -1) {
            host = serverStr.substring(0, index);
            port = Integer.parseInt(serverStr.substring(index + 1));
        } else {
            host = serverStr;
        }

        return new ThriftServer(host, port, protocol);
    }

    @Override
    public Set<ThriftServer<P>> servers() {
        return this.servers;
    }
}
