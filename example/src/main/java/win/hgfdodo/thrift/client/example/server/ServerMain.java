package win.hgfdodo.thrift.client.example.server;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import win.hgfdodo.thrift.client.example.thrift.Echo;

import java.net.InetSocketAddress;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/4<br/>
 * Time: 6:35 下午<br/>
 * <p>
 * 例子的server 端
 */
public class ServerMain {
    static class Handler implements Echo.Iface {

        @Override
        public String ping() throws TException {
            return "pong";
        }

        @Override
        public String echo(String input) throws TException {
            return input;
        }
    }

    public static void main(String[] args) throws TTransportException {
        Handler handler = new Handler();
        int port = 8080;
        TServerTransport transport = new TServerSocket(new InetSocketAddress("localhost", port));
        TServer.Args arg = new TServer.Args(transport);
        arg.protocolFactory(new TBinaryProtocol.Factory());
        arg.transportFactory(new TFramedTransport.Factory());
        arg.processor(new Echo.Processor<>(handler));
        TSimpleServer simpleServer = new TSimpleServer(arg);
        System.out.println("starting server @ " + port + " ...");
        simpleServer.serve();
    }
}
