package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

public class UDPSocketServer extends Thread {

    protected DatagramSocket socket = null;

    public UDPSocketServer() throws SocketException {
        this("UDPSocketServer");
    }

    public UDPSocketServer(String name) throws SocketException {
        super(name);
        
        /*
        Ports below 1024 requre administrative rights when running the applications
        */
        socket = new DatagramSocket(9000); //Instantite a DatagramSocket

    }

    @Override
    public void run() {

        int counter = 0;
        byte[] recvBuf = new byte[256];     // a byte array that will store the data received by the client

        try {
            // run forever
            while (true) {
                DatagramPacket packet = new DatagramPacket(recvBuf, 256); //create a packet
                socket.receive(packet); //wait until a client sends something (blocking call)

                // Get the current date/time and copy it in the byte array
                String dString = new Date().toString() + " - Counter: " + (counter);
                int len = dString.length();                                             // length of the byte array
                byte[] buf = new byte[len];                                             // byte array that will store the data to be sent back to the client
                System.arraycopy(dString.getBytes(), 0, buf, 0, len);

                // Used to send back the response (which is now in the buf byte array)
                InetAddress addr = packet.getAddress(); //extract IP address
                int srcPort = packet.getPort(); //extract source port

                // set the buf as the data of the packet
                packet.setData(buf);
                
                // set the IP address and port extracted above as destination IP address and port in the packet to be sent
                packet.setAddress(addr);
                packet.setPort(srcPort);

                socket.send(packet); //send packet (blocking call)

                counter++;
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        new UDPSocketServer().start();
        System.out.println("Time Server Started");
    }

}
