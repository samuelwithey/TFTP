package tftp.tcp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author 164574
 */
public class TCPServer {
    
    protected InetAddress address; 
    protected ServerSocket masterSocket = null; //does the work of the program
    protected Socket slaveSocket = null; //thread created by master

    /**
     * The constructor loops forever waiting to receive packets from the client.
     * Once a new packet has been received, a new thread is run using the DatagramPacket
     * the method received.
     * @throws SocketException
     * @throws UnknownHostException 
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
