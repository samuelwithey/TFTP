package client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPSocketClient {
    
    //java function to turn data into a byte array
    
    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;
    
    private InetAddress address;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private Scanner scanner = new Scanner(System.in);
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    
    private UDPSocketClient() {
        //set address
        //set socket with random port and address
        //set timeout
        getUserInput();
    }
    
    private boolean lastPacket(DatagramPacket packet) {
        return packet.getLength() < 512; //needs rethinking
    }
    
    private DatagramPacket generateReadandWrtiePacket(int opcode, String fileName, InetAddress address, int port) {
        /**
         * Need to think of a better way to do this method. It isn't very efficient
         */
        int totalBytes = 0;
        byte[] buf = new byte[516];
        buf[0] = (byte) opcode; //not right
        buf[1] = (byte) opcode; //not right
        totalBytes = 2;
        byte[] file = fileName.getBytes();
        totalBytes += file.length;
        System.arraycopy(file, 0, buf, 2, file.length);
        byte f = 0;
        totalBytes += 1;
        buf[totalBytes] = f;
        byte[] mode = "octet".getBytes();
        totalBytes += 1;
        System.arraycopy(mode, 0, buf, totalBytes , mode.length);
        totalBytes += mode.length;
        totalBytes += 1;
        buf[totalBytes] = f; //don't think this is correct
        DatagramPacket pkt = new DatagramPacket(buf, totalBytes, address, port);
        return pkt;
    }
    
    private DatagramPacket generateDataPacket(int blockNum, byte[] data, InetAddress address, int port) {
        byte[] buf = new byte[516];
        /**
         * I don't think any of the casting is right at all.
         */
        buf[0] = (byte) OP_DATA;
        buf[1] = (byte) OP_DATA;
        buf[2] = (byte) blockNum;
        buf[3] = (byte) blockNum;
        System.arraycopy(data, 0, buf, 5, data.length);
        DatagramPacket pkt = new DatagramPacket(buf, data.length + 4, address, port);
        return null;
    }
    
    private DatagramPacket generateAckPacket(int blockNum, InetAddress address, int port) {
        byte[] buf = new byte[516];
        buf[0] = (byte) OP_ACK;
        buf[1] = (byte) OP_ACK;
        buf[2] = (byte) blockNum;
        buf[3] = (byte) blockNum;
        DatagramPacket pkt = new DatagramPacket(buf, 4, address, port);
        return pkt;
    }
    
    private DatagramPacket generateErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
        byte[] buf = new byte[516];
        buf[0] = (byte) OP_ERROR;
        buf[1] = (byte) OP_ERROR;
        buf[2] = (byte) errorCode;
        buf[3] = (byte) errorCode;
        System.arraycopy(errorMsg.getBytes(), 0, buf, 4, errorMsg.getBytes().length);
        buf[errorMsg.getBytes().length + 4] = (byte) 0;
        DatagramPacket pkt = new DatagramPacket(buf, errorMsg.getBytes().length + 4, address, port);
        return pkt;
    }
    
    public void getUserInput() {
        boolean finished = false;
        while(finished) {
            int userInput;
            System.out.println("Press \"1\" if you would like to send a file to the server");
            System.out.println("Press \"2\" if you would like to get a file from the server");
            System.out.println("Press \"3\" at any time if you would like to exit the program");
            System.out.println("Input: ");
            userInput = scanner.nextInt();
            if(userInput == 1) {
                //get file name
                //call method to send a file to the server
            } else if(userInput == 2) {
                //get file from users local system
                //call method to get a file from the server
            } else {
                finished = true;
            }
            
        }
    }
    
    private void run() {
        //sends or receives the packet
    }
    
    private void writeFile() {
        //send packets of 512 until last packet is sent
    }
    
    private void receiveFile() {
        //get a file and write it locally
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
