package tftp.tcp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * The class loops forever waiting to receive new connections from a client.
 * @author 164574
 */
public class TCPServer {
    
    protected InetAddress address; 
    protected ServerSocket masterSocket = null;
    protected Socket slaveSocket = null; //thread created by master

    /**
     * The constructor loops forever waiting to receive connections from the client.
     * Once a new connection has been initialised, a new thread is run using the slave socket.
     * the method received.
     * @throws SocketException
     * @throws UnknownHostException 
     * @throws IOException
     */
    public TCPServer() throws SocketException, UnknownHostException, IOException  {
        address = InetAddress.getByName("127.0.0.1");
        masterSocket = new ServerSocket(9000);
        System.out.println("Server is waiting to receive a request...");
        try {
            // run forever
            while (true) {
                slaveSocket = masterSocket.accept();
                new TCPServerThread(slaveSocket).start();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    /**
     * The main method creates a new instance of UDPSocketServer.
     * @param args
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        TCPServer server = new TCPServer();
    }
}
