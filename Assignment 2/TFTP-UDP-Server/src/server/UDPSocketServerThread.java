package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

/**
 * The class allows the client to interact with the server thread. The initial packet sent will be checked if it is a 
 * RRQ or WRQ and the corresponding methods will be called.
 * @author 164574
 */
public class UDPSocketServerThread extends Thread {
    
    protected DatagramSocket socket = null;
    protected InetAddress address;
    protected DatagramPacket receivePacket;
    private int destinationTID;
    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;

    /**
     * The constructor sets the address, generates a new socket, assigns the initial packet to a DatagramPacket and sets the TID
     * to the random TID of the received packet.
     * @param receivedPacket
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public UDPSocketServerThread(DatagramPacket receivedPacket) throws SocketException, UnknownHostException {
        address = InetAddress.getByName("127.0.0.1");
        socket = new DatagramSocket(generateTID(), address);
        receivePacket = receivedPacket;
        destinationTID = receivePacket.getPort();
        System.out.println("Thread has been created");
    }
    
    /**
     * The method checks the opcode of the received packet. It calls the corresponding methods based on the opcode.
     */
    @Override
    public void run() {
        if(getOpcode(receivePacket.getData()) == OP_RRQ) {
            System.out.println("\n" + "Read request received from client");
            writeFileToClient();
        } else if(getOpcode(receivePacket.getData()) == OP_WRQ) {
            System.out.println("\n" + "Write request received from client");
            getFileFromClient();
        }
    }
    
    /**
     * Returns a random int between 1024 and 60000 to be used as a port.
     * @return random int between 1024 and 60000
     */
    public int generateTID() {
        Random rand = new Random();
        return rand.nextInt(60000) + 1024;
    }
    
    /**
     * The method handles the functionality of the Read Request (RRQ). It gets the file name from the RRQ Packet and gets the file
     * locally. If the file is not found, an error message is displayed in the log and an error packet is sent back. If the file is found,
     * the method sends the data in 512 bytes using the data packet. It waits to receive an Acknowledgement Packet with the correct block number
     * before sending each chunk of 512 bytes. Once all the data has been sent and the final acknowledgement packet has been received, the connection
     * is closed.
     */
    public void writeFileToClient(){
        try {
            int blockNum = 1;
            int expectedBlockNum = 1;
            byte[] fileData;
            int bytesLeft = 0;
            int index = 0;
            File file;
            String fileName = getFileName(receivePacket.getData());
            file = getFile(fileName);
            if(!file.isFile()) {
                System.out.println("File not found");
                DatagramPacket errPkt = generateErrorPacket(1, "File not found", address, receivePacket.getPort());
                socket.send(errPkt);
                new UDPSocketServerThread(receivePacket);
            }
            fileData = Files.readAllBytes(file.toPath());
            bytesLeft = fileData.length;
            /*
            * This conditional statement is needed to initially start sending data packets after receiving the RRQ and before
            * receiving the first acknowledgement packet.
            */
            if(bytesLeft >= 512) {
                byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + 512);
                DatagramPacket dataPacket = generateDataPacket(blockNum, dataToSend, address, destinationTID);
                socket.send(dataPacket);
                System.out.println("Data packet sent with block #: " + blockNum);
                blockNum++;
                index += 512;
                bytesLeft -= 512;
            } else if(bytesLeft < 512) {
                byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + bytesLeft);
                bytesLeft = 0;
                index += bytesLeft;
                DatagramPacket dataPack = generateDataPacket(blockNum, dataToSend, address, destinationTID);
                socket.send(dataPack);
                System.out.println("Final data packet has been sent with block #: " + blockNum);
                blockNum++;
            }
            DatagramPacket receivedPacket;
            do {
                receivedPacket = new DatagramPacket(new byte[4], 4);
                socket.receive(receivedPacket);
                destinationTID = receivedPacket.getPort();
                if(getOpcode(receivedPacket.getData()) == OP_ACK && getBlockNum(receivedPacket.getData()) == expectedBlockNum){
                    System.out.println("Ack packet received from client with block #: " + getBlockNum(receivedPacket.getData()));
                    expectedBlockNum++;
                    if(bytesLeft >= 512) {
                        byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + 512);
                        bytesLeft -= 512;
                        index += 512;
                        DatagramPacket dataPack = generateDataPacket(blockNum, dataToSend, address, destinationTID);
                        socket.send(dataPack);
                        System.out.println("Data packet sent with block #: " + blockNum);
                        blockNum++;
                    } else if(bytesLeft < 512 && bytesLeft > 0) {
                        byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + bytesLeft);
                        bytesLeft = 0;
                        index += bytesLeft;
                        DatagramPacket dataPack = generateDataPacket(blockNum, dataToSend, address, destinationTID);
                        socket.send(dataPack);
                        System.out.println("Final data packet sent with block #: " + blockNum);
                        blockNum++;
                    } else if(bytesLeft == 0) {
                        System.out.println("All data has been sent and final ack has been received");
                    }
                } else {
                    System.out.println("Error");
                }
            } while(bytesLeft > 0);
             socket.close();
        } catch(IOException e) {
            System.out.println("Error");
        }
    }
    
    /**
     * The method handles the functionality of the Write Request Packet (WRQ). The method replies to the initial WRQ packet with an
     * acknowledgement packet. The method then waits to receive data packets from the client and sends back acknowledgements
     * with the correct block number. Once the final packet has been received and all the data has been written to the file in the WRQ, 
     * a final acknowledgement is sent and the connection is closed.
     * 
     */
    public void getFileFromClient() {
        try {
            int expectedBlockNum = 0;
            Path currentRelativePath = Paths.get("");
            String filePath = currentRelativePath.toAbsolutePath().toString();
            filePath += "//" + getFileName(receivePacket.getData());
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                DatagramPacket ackPacket = generateAckPacket(expectedBlockNum, address, destinationTID);
                System.out.println("Ack packet has been sent to client with block #: " + expectedBlockNum);
                expectedBlockNum++;
                socket.send(ackPacket);
                DatagramPacket receivedPacket;
                do {
                    receivedPacket = new DatagramPacket(new byte[516], 516);
                    socket.receive(receivedPacket);
                    if(getOpcode(receivedPacket.getData()) == OP_DATA && getBlockNum(receivedPacket.getData()) == expectedBlockNum) {
                        outputStream.write(Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength()));
                        System.out.println("Data has been written to file");
                        DatagramPacket ackPack = generateAckPacket(getBlockNum(receivedPacket.getData()), address, destinationTID);
                        socket.send(ackPack);
                        System.out.println("Ack packet has been sent to client with block #: " + getBlockNum(receivedPacket.getData()));
                        expectedBlockNum++;
                    } else if(getOpcode(receivedPacket.getData()) == OP_ERROR) {
                        System.out.println("An error packet was received");
                    }
                } while(removeTrailingBytes(receivedPacket.getData()).length == 516);
            }
            System.out.println("All data has been written to file");
            socket.close();
        } catch(IOException e){
            System.out.println("Error");
        }
    }
    
    /**
     * The method removes the empty bytes from the byte array.
     * @param bytes byte array that the trailing bytes need to be removed.
     * @return 
     */
    private byte[] removeTrailingBytes(byte[] bytes) {
        int i = bytes.length - 1;
        while(i >= 0 && bytes[i] == 0) {
            i--;
        }
        return Arrays.copyOf(bytes, i + 1);
    }
    
    /**
     * The method generates a file path using the file name passed through the parameter and gets the file locally.
     * The file is then returned.
     * @param fileName The file name of the file to be found
     * @return The file from the file name given
     */
    public File getFile(String fileName) {
        Path currentRelativePath = Paths.get("");
        String filePath = currentRelativePath.toAbsolutePath().toString();
        File file = new File(filePath + "//" + fileName);
        return file;
    }
    
    /**
     * The method generates a Data Packet. It uses methods to convert the opcode and block number into byte arrays and these
     * are copied into the buf array. The data can be directly copied into the buf array as it is already a byte array.
     * The buf array, address and port are all used in the constructor to generate a new packet and this is returned.
     * @param blockNum The block number of the current Data Packet
     * @param data The data to be added to the buf array
     * @param address The address used for the constructor of DatagramPacket
     * @param port The port used for the constructor of DatagramPacket
     * @return DatagramPacket for a Data Packet
     */
    public DatagramPacket generateDataPacket(int blockNum, byte[] data, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(OP_DATA), 0, buf, 0, setOpcode(OP_DATA).length);
        System.arraycopy(setBlockNum(blockNum), 0, buf,setOpcode(OP_DATA).length, setBlockNum(blockNum).length);
        System.arraycopy(data, 0, buf, (setOpcode(OP_DATA).length + setBlockNum(blockNum).length), data.length);
        DatagramPacket pkt = new DatagramPacket(buf, data.length + 4, address, port);
        return pkt;
    }
    
     /**
     * The method generates an Acknowledgement Packet. It uses methods to convert the opcode and block number into byte arrays
     * and these are copied into the byte array. The buf array, address and port are passed through the constructor to create a 
     * new DatagramPacket. This is then returned.
     * @param blockNum The block number of the current Acknowledgement Packet.
     * @param address The address used for the constructor of DatagramPacket
     * @param port The port used for the constructor of DatagramPacket
     * @return DatagramPacket for a Acknowledgement Packet
     */
    public DatagramPacket generateAckPacket(int blockNum, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(OP_ACK), 0, buf, 0, setOpcode(OP_ACK).length);
        System.arraycopy(setBlockNum(blockNum), 0, buf, setOpcode(OP_ACK).length,  setBlockNum(blockNum).length);
        DatagramPacket pkt = new DatagramPacket(buf, 4, address, port);
        return pkt;
    }
    
    /**
     * The method generates an ErrorPacket. It uses methods to convert the the opcode, error code and the error message
     * into byte arrays and these are copied into the buf array. The byf array, address and port are passed through the 
     * constructor to create a new DatagramPacket. This is then returned.
     * @param errorCode The error code corresponding to the error
     * @param errorMsg The error message as a string
     * @param address The address used for the constructor of DatagramPacket
     * @param port The port used for the constructor of DatagramPacket
     * @return DatagramPacket for a Error Packet
     */
    public DatagramPacket generateErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
        byte[] buf = new byte[516];
        byte[] errCode = setErrorCode(errorCode);
        System.arraycopy(setOpcode(OP_ERROR), 0, buf, 0, setOpcode(OP_ERROR).length);
        System.arraycopy(setErrorCode(errorCode), 0, buf, setOpcode(OP_ERROR).length, setErrorCode(errorCode).length);
        System.arraycopy((errorMsg+"\0").getBytes(), 0, buf, setOpcode(OP_ERROR).length + setErrorCode(errorCode).length, errorMsg.getBytes().length + 1);
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, address, port);
        return pkt;
    }
    
    /**
     * The method takes a opcode through the parameter and splits the opcode into a byte array of size 2.
     * This byte array is then returned.
     * @param opcode The opcode to be converted into a byte array of size 2.
     * @return byte array of the converted opcode.
     */
    public byte[] setOpcode(int opcode) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((opcode >> 8) & 0xFF);
        arr[1] = (byte) (opcode & 0xFF);
        return arr;
    }
    
    /**
     * The method takes a byte array and returns the opcode as an int from the byte array.
     * @param arr The byte array containing the opcode.
     * @return Opcode as an int
     */
    public int getOpcode(byte[] arr) {
        return((arr[0] & 0xFF) << 8) | (arr[1] & 0xFF);
    }
    
    /**
     * The method takes a block number through the parameter and splits the block number into a byte array of size 2.
     * This byte array is then returned.
     * @param blockNum The block number to be converted into a byte array of size 2.
     * @return byte array of the converted block number.
     */
    public byte[] setBlockNum(int blockNum) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((blockNum >> 8) & 0xFF);
        arr[1] = (byte) (blockNum & 0xFF);
        return arr;
    }
    
    /**
     * The method takes a byte array and returns the block number as an int from the byte array.
     * @param arr The byte array containing the block number.
     * @return Block number as an int.
     */
    public int getBlockNum(byte[] arr) {
        return((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }
    
    /**
     * The method takes an error code through the parameter and splits the error code into a byte array of size 2.
     * This byte array is then returned.
     * @param errorCode The error code to be converted into a byte array of size 2.
     * @return byte array of the converted error code.
     */
    public byte[] setErrorCode(int errorCode) {
        byte[] arr = new byte[2];
        arr[0] = (byte) ((errorCode >> 8) & 0xFF);
        arr[1] = (byte) (errorCode & 0xFF);
        return arr;
    }
    
    /**
     * The method takes a byte array and returns the error code as an int from the byte array.
     * @param arr The byte array containing the error code.
     * @return Error code as an int.
     */
    public int getErrorCode(byte[] arr) {
        return((arr[2] & 0xFF) << 8) | (arr[3] & 0xFF);
    }
    
    /**
     * The method returns a string containing the file name from the byte array passed through the parameter. The method
     * iterates over the byte array from index 2 until it reaches end of the file length - 1 or it reaches the 0 byte.
     * @param file The byte array where the file name needs to be extracted.
     * @return The file name as a String.
     */
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
    
    /**
     * The method returns the error message in the given byte array. The method copies the array from index 4 until the end of the byte array - 1.
     * This is then returned as a String.
     * @param errorMsg The byte array containing the error message.
     * @return The error message as a String.
     */
    public String getErrorMsg(byte[] errorMsg) {
        byte[] errMsg = Arrays.copyOfRange(errorMsg, 4, errorMsg.length - 1);
        return new String(errMsg);
    }
}
