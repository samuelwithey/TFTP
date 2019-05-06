package client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UDPSocketClient {
    
    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;
    
    private InetAddress address;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    
    public UDPSocketClient() throws IOException {
        address = InetAddress.getByName("127.0.0.1");
        getUserInput();
    }
    
    private boolean lastPacket(DatagramPacket packet) {
        return packet.getLength() < 512; //needs rethinking
    }
    
    public DatagramPacket generateReadandWrtiePacket(int opcode, String fileName, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(opcode), 0, buf, 0 , setOpcode(opcode).length);
        System.arraycopy((fileName+"\0").getBytes(), 0, buf, setOpcode(opcode).length, fileName.length() + 1);
        System.arraycopy(("octet"+"\0").getBytes(), 0, buf, (setOpcode(opcode).length + fileName.length()) + 1, ("octet"+"\0").length());
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, address, port);
        return pkt;
    }
    
    public DatagramPacket generateDataPacket(int blockNum, byte[] data, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(OP_DATA), 0, buf, 0, setOpcode(OP_DATA).length);
        System.arraycopy(setBlockNum(blockNum), 0, buf,setOpcode(OP_DATA).length, setBlockNum(blockNum).length);
        System.arraycopy(data, 0, buf, (setOpcode(OP_DATA).length + setBlockNum(blockNum).length), data.length);
        DatagramPacket pkt = new DatagramPacket(buf, data.length + 4, address, port);
        return pkt;
    }
    
    public DatagramPacket generateAckPacket(int blockNum, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(OP_ACK), 0, buf, 0, setOpcode(OP_ACK).length);
        System.arraycopy(setBlockNum(blockNum), 0, buf, setOpcode(OP_ACK).length,  setBlockNum(blockNum).length);
        DatagramPacket pkt = new DatagramPacket(buf, 4, address, port);
        return pkt;
    }
    
    public DatagramPacket generateErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
        byte[] buf = new byte[516];
        byte[] errCode = setErrorCode(errorCode);
        System.arraycopy(setOpcode(OP_ERROR), 0, buf, 0, setOpcode(OP_ERROR).length);
        System.arraycopy(setErrorCode(errorCode), 0, buf, setOpcode(OP_ERROR).length, setErrorCode(errorCode).length);
        System.arraycopy((errorMsg+"\0").getBytes(), 0, buf, setOpcode(OP_ERROR).length + setErrorCode(errorCode).length, errorMsg.getBytes().length + 1);
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, address, port);
        return pkt;
    }
    
    public void getUserInput() {
        BufferedReader input = new BufferedReader (new InputStreamReader(System.in));
        boolean finished = false;
        try {
            while(finished) {
                int userInput;
                System.out.println("Press \"1\" if you would like to send a file to the server");
                System.out.println("Press \"2\" if you would like to get a file from the server");
                System.out.println("Press \"3\" if you would like to exit the program");
                System.out.println("Input: ");
                userInput = Integer.parseInt(input.readLine());
                if(userInput == 1) {
                    System.out.println("Please enter the filename to send to the server: ");
                    String fileName = input.readLine();
                    writeFile(fileName);
                    finished = true; //could call the method again?
                } else if(userInput == 2) {
                    System.out.println("Please enter the filename of the file on the server");
                    String fileName = input.readLine();
                    receiveFile(fileName);
                    finished = true; //could call the method again?
                } else {
                    finished = true;
                }
            }
        } catch(IOException e) {
            System.out.println("Error");
        }
    }
    
    private void run() {
        //sends or receives the packet
    }
    
    private void writeFile(String fileName) {
        //get file
        //start sending packets
        //send packets of 512 until last packet is sent
    }
    
    private void receiveFile(String fileName) {
        //get file from server
        //get the packet data
        //get a file and write it locally
    }
    
    public byte[] setOpcode(int opcode) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((opcode >> 8) & 0xFF);
        arr[1] = (byte) (opcode & 0xFF);
        return arr;
    }
    
    public int getOpcode(byte[] arr) {
        return((arr[0] & 0xFF) << 8) | (arr[1] & 0xFF);
    }
    
    public byte[] setBlockNum(int blockNum) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((blockNum >> 8) & 0xFF);
        arr[1] = (byte) (blockNum & 0xFF);
        return arr;
    }
    
    public int getBlockNum(byte[] arr) {
        return((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }
    
    public byte[] setErrorCode(int errorCode) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((errorCode >> 8) & 0xFF);
        arr[1] = (byte) (errorCode & 0xFF);
        return arr;
    }
    
    public int getErrorCode(byte[] arr) {
        return((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }
    
    public String getFileName(byte[] file) {
        String fileName = "";
        byte zero = 0;
        boolean found = false;
        int i = 2;
        while(!found && i < file.length) {
            if(compare(file[i], zero) != 0){
                fileName += (char) file[i];
            } else {
                found = true;
            }
            i++;
        }
        return fileName;
    }
    
    public String getErrorMsg(byte[] errorMsg) {
        byte[] errMsg = Arrays.copyOfRange(errorMsg, 4, errorMsg.length - 1);
        return new String(errMsg);
    }
    
    /**
     * No need
     * @param mode
     * @return 
     */
//    public String getMode(byte[] mode) {
//        return null;
//    }
            

    // the client will take the IP Address of the server (in dotted decimal format as an argument)
    // given that for this tutorial both the client and the server will run on the same machine, you can use the loopback address 127.0.0.1
    public static void main(String[] args) throws IOException {
//        
//        DatagramSocket socket;
//        DatagramPacket packet;
//        
//        
//        if (args.length != 1) {
//            System.out.println("the hostname of the server is required");
//            return;
//        }
//        
//        int len = 516;
//        byte[] buf = new byte[len];
//
//        /*
//        This is not the port set in the server. If the same port is selected, it will throw an exception because the server
//        is listening to this port and both processes run on the same machine.
//        */
//        socket = new DatagramSocket(4000); //Instantiating the DatagramSocket socket object
//
//        
//        // The address must be transfomed from a String to an InetAddress (an IP addresse object in Java).
//        InetAddress address = InetAddress.getByName(args[0]); //getting the address from args[0]
//
//        packet = new DatagramPacket(buf, len); //Instantiating a packet
//        packet.setAddress(address); //set IP address
//        packet.setPort(9000); //set port fields
//
//        // The server will respond to any kind of request (i.e. regardless of the packet payload)
//        socket.send(packet); //Send datagram packet to server (blocking call)
//
//        // DatagramPacket can be reused and values are overriden
//        socket.receive(packet); //Receive a packet containing the servers response
//
//        // display response
//        String received = new String(packet.getData());
//        socket.close();
    }
    
}
