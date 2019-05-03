package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSocketClient {
    
    private static final byte OP_RRQ = 1;
    private static final byte OP_WRQ = 2;
    private static final byte OP_DATA = 3;
    private static final byte OP_ACK = 4;
    private static final byte OP_ERROR = 5;
    
    //make changes to ensure it can commit
    
    
    
    private void error() {
    
    }
    
    private void sendAck() {
        
    }
    
    private void writeFile() {
    
    }
    
    private void receiveFile() {
    
    }

    // the client will take the IP Address of the server (in dotted decimal format as an argument)
    // given that for this tutorial both the client and the server will run on the same machine, you can use the loopback address 127.0.0.1
    public static void main(String[] args) throws IOException {
        
        DatagramSocket socket;
        DatagramPacket packet;
        
        if (args.length != 1) {
            System.out.println("the hostname of the server is required");
            return;
        }
        
        int len = 512;
        byte[] buf = new byte[len];

        /*
        This is not the port set in the server. If the same port is selected, it will throw an exception because the server
        is listening to this port and both processes run on the same machine.
        */
        socket = new DatagramSocket(4000); //Instantiating the DatagramSocket socket object

        
        // The address must be transfomed from a String to an InetAddress (an IP addresse object in Java).
        InetAddress address = InetAddress.getByName(args[0]); //getting the address from args[0]

        packet = new DatagramPacket(buf, len); //Instantiating a packet
        packet.setAddress(address); //set IP address
        packet.setPort(9000); //set port fields

        // The server will respond to any kind of request (i.e. regardless of the packet payload)
        socket.send(packet); //Send datagram packet to server (blocking call)

        // DatagramPacket can be reused and values are overriden
        socket.receive(packet); //Receive a packet containing the servers response

        // display response
        String received = new String(packet.getData());
        System.out.println("Today's date: " + received.substring(0, packet.getLength()));
        socket.close();
    }
    
}
