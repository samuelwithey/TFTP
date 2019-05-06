package server;

import java.io.IOException;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;

public class UDPSocketServer extends Thread {
    
    protected DatagramSocket socket = null;
    private byte OP_RRQ = 1;
    private byte OP_WRQ = 2;
    private byte OP_DATA = 3;
    private byte OP_ACK = 4;
    private byte OP_ERROR = 5;

    public UDPSocketServer() throws SocketException {
        this("UDPSocketServer");
    }

    public UDPSocketServer(String name) throws SocketException {
        super(name);
        
        /*
        Ports below 1024 requre administrative rights when running the applications
        */
        //socket = new DatagramSocket(9000); //Instantite a DatagramSocket

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

//    @Override
//    public void run() {
//
//        int counter = 0;
//        byte[] recvBuf = new byte[256];     // a byte array that will store the data received by the client
//
//        try {
//            // run forever
//            while (true) {
//                DatagramPacket packet = new DatagramPacket(recvBuf, 256); //create a packet
//                socket.receive(packet); //wait until a client sends something (blocking call)
//
//                // Get the current date/time and copy it in the byte array
//                String dString = new Date().toString() + " - Counter: " + (counter);
//                int len = dString.length();                                             // length of the byte array
//                byte[] buf = new byte[len];                                             // byte array that will store the data to be sent back to the client
//                System.arraycopy(dString.getBytes(), 0, buf, 0, len);
//
//                // Used to send back the response (which is now in the buf byte array)
//                InetAddress addr = packet.getAddress(); //extract IP address
//                int srcPort = packet.getPort(); //extract source port
//
//                // set the buf as the data of the packet
//                packet.setData(buf);
//                
//                // set the IP address and port extracted above as destination IP address and port in the packet to be sent
//                packet.setAddress(addr);
//                packet.setPort(srcPort);
//
//                socket.send(packet); //send packet (blocking call)
//
//                counter++;
//            }
//        } catch (IOException e) {
//            System.err.println(e);
//        }
//        socket.close();
//    }

    public static void main(String[] args) throws IOException {
        //new UDPSocketServer().start();
    }

}
