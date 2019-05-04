package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPSocketClient {
    
    //java function to turn data into a byte array
    
    private byte OP_RRQ = 1; //maybe make int and convert into a byte
    private byte OP_WRQ = 2;
    private byte OP_DATA = 3;
    private byte OP_ACK = 4;
    private byte OP_ERROR = 5;
    
    private Packet generateReadandWrtiePacket(PacketType pktType, byte opcode, String fileName) {
        Packet pkt = new Packet(pktType, opcode, fileName);
        return pkt;
    }
    
    private Packet generateDataPacket(PacketType pktType, byte opcode, byte blockNum, byte[] data) {
        Packet pkt = new Packet(pktType, opcode, blockNum, data);
        return pkt;
    }
    
    private Packet generateAckPacket(PacketType pktType, byte opcode, byte blockNum) {
        Packet pkt = new Packet(pktType, opcode, blockNum);
        return pkt;
    }
    
    private Packet generateErrorPacket(PacketType pktType, byte opcode, byte errorCode, String errorMsg) {
        Packet pkt = new Packet(pktType, opcode, errorCode, errorMsg);
        return pkt;
    }
    
    
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
        
        Scanner input = new Scanner(System.in);
        boolean finished = false;
        while(finished) {
            int userInput;
            System.out.println("Press \"1\" if you would like to send a file to the server");
            System.out.println("Press \"2\" if you would like to get a file from the server");
            System.out.println("Press \"3\" if you would like to exit the program");
            System.out.println("Input: ");
            userInput = input.nextInt();
            if(userInput == 1) {
                //call method to send a file to the server
            } else if(userInput == 2) {
                //call method to get a file from the server
            } else {
                finished = true;
            }
            
        }
        
        if (args.length != 1) {
            System.out.println("the hostname of the server is required");
            return;
        }
        
        int len = 516;
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
        socket.close();
    }
    
}
