package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * The class loops forever, receiving DatagramPackets and creating new threads using the DatagramPackets.
 * @author 164574
 */
public class UDPSocketServer {
    
    protected DatagramSocket socket = null;
    protected InetAddress address;
    protected byte[] buf;

    /**
     * The constructor loops forever waiting to receive packets from the client.
     * Once a new packet has been received, a new thread is run using the DatagramPacket
     * the method received.
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public UDPSocketServer() throws SocketException, UnknownHostException  {
        buf = new byte[516];
        address = InetAddress.getByName("127.0.0.1");
        socket = new DatagramSocket(9000, address);
        System.out.println("Server is waiting to receive a request...");
        try {
            // run forever
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(buf, 516); //create a packet
                socket.receive(receivePacket); //wait until a client sends something (blocking call)
                new UDPSocketServerThread(receivePacket).start();
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
    public static void main(String[] args) throws SocketException, UnknownHostException {
        UDPSocketServer server = new UDPSocketServer();
    }
}
