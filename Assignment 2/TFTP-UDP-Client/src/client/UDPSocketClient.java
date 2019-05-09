package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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
    
    private int destinationTID = 9000;
    
    public UDPSocketClient() throws IOException {
        address = InetAddress.getByName("127.0.0.1");
        socket = new DatagramSocket(generateTID(), address);
        getUserInput();
    }
    
    public void getUserInput() {
        BufferedReader input = new BufferedReader (new InputStreamReader(System.in));
        boolean finished = false;
        try {
            while(!finished) {
                int userInput;
                System.out.println("\n");
                System.out.println("Press \"1\" if you would like to send a file to the server");
                System.out.println("Press \"2\" if you would like to get a file from the server");
                System.out.println("Press \"3\" if you would like to exit the program");
                System.out.println("Input: ");
                userInput = Integer.parseInt(input.readLine());
                if(userInput == 1) {
                    System.out.println("Please enter the filename to send to the server: ");
                    String fileName = input.readLine();
                    writeFile(fileName);
                    getUserInput();
                } else if(userInput == 2) {
                    System.out.println("Please enter the filename of the file on the server: ");
                    String fileName = input.readLine();
                    receiveFile(fileName);
                    getUserInput();
                } else if(userInput == 3){
                    System.exit(0);
                    finished = true;
                } else{
                    System.out.println("Please enter the correct input: ");
                    getUserInput();
                }
            }
        } catch(IOException e) {
            System.out.println("Error");
        }
    }
    
    /**
     *
     * @param fileName
     */
    public void writeFile(String fileName) {
        try {
            File file = getFile(fileName);
            if(!file.isFile()) {
                System.out.println("File not found");
                getUserInput();
            }
            int blockNum = 0;
            socket = new DatagramSocket(generateTID());
            byte[] fileData = Files.readAllBytes(file.toPath());
            int bytesLeft = fileData.length;
            int index = 0;
            DatagramPacket WRQ = generateReadorWritePacket(OP_WRQ, fileName, address, 9000);
            socket.send(WRQ);
            System.out.println("Write request has been sent to server");
            do {
                packet = new DatagramPacket(new byte[4], 4);
                socket.receive(packet);
                destinationTID = packet.getPort();
                if(getOpcode(packet.getData()) == OP_ACK && getBlockNum(packet.getData()) == blockNum) {
                    System.out.println("Ack packet has been received from server with block #: " + getBlockNum(packet.getData()));
                    if(bytesLeft >= 512) {
                        byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + 512);
                        bytesLeft -= 512;
                        index += 512;
                        blockNum++;
                        DatagramPacket dataPacket = generateDataPacket(blockNum, dataToSend, address, destinationTID); //sends back the same block number
                        socket.send(dataPacket);
                        System.out.println("Data packet has been sent to server with block #: " + blockNum);
                    } else if(bytesLeft < 512 && bytesLeft > 0) {
                        byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + bytesLeft);
                        bytesLeft = 0;
                        index += bytesLeft;
                        blockNum++;
                        DatagramPacket dataPacket = generateDataPacket(blockNum, dataToSend, address, destinationTID); //sends back the same block number
                        socket.send(dataPacket);
                        System.out.println("Final data packet has been sent with block #: " + blockNum);
                    } else if(bytesLeft == 0){
                        System.out.println("All data has been sent and final ack received");
                    }
                } else {
                    System.out.println("Error");
                }
            } while(bytesLeft > 0);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error");
        }
    }
    
    
    public void receiveFile(String fileName) {
        try {
            int expectedBlockNum = 1;
            Path currentRelativePath = Paths.get("");
            String filePath = currentRelativePath.toAbsolutePath().toString();
            filePath += "//" + fileName;
            outputStream = new FileOutputStream(filePath); //uses generate path with fileName to write to
            socket = new DatagramSocket(generateTID()); //generate new socket using random TID port
            DatagramPacket RRQ = generateReadorWritePacket(OP_RRQ, fileName, address, 9000); //create RRQ using fileName and initalise port to 9000
            //create a timeout
            socket.send(RRQ); //send RRQ
            System.out.println("Read Request sent to server");
            DatagramPacket receivedPacket;
            do {
                receivedPacket = new DatagramPacket(new byte[516], 516);
                socket.receive(receivedPacket);
                destinationTID = receivedPacket.getPort(); //getting TID from server
                if(getOpcode(receivedPacket.getData()) == OP_DATA && getBlockNum(receivedPacket.getData()) == expectedBlockNum) {
                    System.out.println("Data packet received");
                    outputStream.write(Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength()));
                    DatagramPacket ackPack = generateAckPacket(getBlockNum(receivedPacket.getData()), address, destinationTID);
                    socket.send(ackPack);
                    System.out.println("Ack packet sent to client with block #: " + getBlockNum(receivedPacket.getData()));
                    expectedBlockNum++;
                } else if(getOpcode(receivedPacket.getData()) == OP_ERROR) {
                    System.out.println("An error packet was received");
                    socket.close();
                    getUserInput();
                }
            } while(removeTrailingBytes(receivedPacket.getData()).length == 516);
            System.out.println("All data has been received and written to file");
            outputStream.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Error");
        }
    }
    
    private byte[] removeTrailingBytes(byte[] bytes) {
        int i = bytes.length - 1;
        while(i >= 0 && bytes[i] == 0) {
            i--;
        }
        return Arrays.copyOf(bytes, i + 1);
    }
    
    public int generateTID() {
        Random rand = new Random();
        return rand.nextInt(60000) + 1024;
    }
    
    public DatagramPacket generateReadorWritePacket(int opcode, String fileName, InetAddress address, int port) {
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
    
    public File getFile(String fileName) {
        Path currentRelativePath = Paths.get("");
        String filePath = currentRelativePath.toAbsolutePath().toString();
        File file = new File(filePath + "//" + fileName);
        return file;
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

    public static void main(String[] args) throws IOException {
        UDPSocketClient client = new UDPSocketClient(); //error here
    }
    
}
