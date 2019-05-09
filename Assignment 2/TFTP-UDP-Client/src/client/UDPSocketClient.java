package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

/**
 * 
 * @author 164574
 * The class acts as a client that connects to the server. It allows for user input to send a file to the server and receive a file
 * from the server. 
 */
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
    
    /**
     * Constructor which sets the address and a new instance of socket which is created using 
     * this address and a randomly generated port.
     * @throws IOException 
     */
    public UDPSocketClient() throws IOException {
        address = InetAddress.getByName("127.0.0.1");
        socket = new DatagramSocket(generateTID(), address);
        getUserInput();
    }
    
    /**
     * The method gets the user input and calls the corresponding method. The method runs infinitely until option
     * "3" has been selected, which terminates the program.
     */
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
     * The method writes a file to the server. It takes a file name passed from the user input and checks if the file
     * exists on the system. If it does not exist, an error message is displayed and the user input menu is displayed.
     * If the file exists, a new socket is generated using a new random TID and a Write Request (WRQ) is sent to the server.
     * A new receiving packet is generated in the do while loop and this waits to receive an Acknowledgement Packet.
     * Once it has received the packet, it checks if it is an Acknowledgement Packet and that it has the correct block number.
     * The method then sends the data extracted from the file in 512 bytes and sends this to the server. The method waits until
     * it receives the correct Acknowledgement number and this is completed until all the data from the file has been sent and the
     * socket is closed.
     * @param fileName passed from getUserInput() to get the file from the client.
     */
    public void writeFile(String fileName) {
        try {
            File file = getFile(fileName);
            if(!file.isFile()) {
                System.out.println("File not found");
                getUserInput();
            }
            int blockNum = 0;
            int index = 0;
            socket = new DatagramSocket(generateTID());
            byte[] fileData = Files.readAllBytes(file.toPath());
            int bytesLeft = fileData.length;
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
    
    /**
     * The method receives files from the server and writes the data to a file. The method takes a file name as the parameter which is
     * used to send in the RRQ Packet so the server knows what file to send back. The Read Request Packet (RRQ) is sent to the client and
     * the method waits to receive a packet in the loop. Once it receives the packet, it checks if the packet is a Data Packet and that it
     * has the expected block code. The method will then write the data from the packet to the file stored locally and responds to the server
     * with an Acknowledgement Packet with the same block number that was sent. This will be continued until the final Data Packet has less than
     * 512 bytes of data. The socket will then close and the user menu will be displayed.
     * @param fileName passed as a parameter from getUserInput() and used to send to the server through a RRQ packet.
     */
    public void receiveFile(String fileName) {
        try {
            int expectedBlockNum = 1;
            Path currentRelativePath = Paths.get("");
            String filePath = currentRelativePath.toAbsolutePath().toString();
            filePath += "//" + fileName; //could move
            outputStream = new FileOutputStream(filePath); 
            socket = new DatagramSocket(generateTID()); 
            DatagramPacket RRQ = generateReadorWritePacket(OP_RRQ, fileName, address, 9000);
            socket.send(RRQ); 
            System.out.println("Read Request sent to server");
            DatagramPacket receivedPacket;
            do {
                receivedPacket = new DatagramPacket(new byte[516], 516);
                socket.receive(receivedPacket);
                destinationTID = receivedPacket.getPort();
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
     * Returns a random int between 1024 and 60000 to be used as a port.
     * @return random int between 1024 and 60000
     */
    public int generateTID() {
        Random rand = new Random();
        return rand.nextInt(60000) + 1024;
    }
    
    /**
     * The method generates a Read or Write packet based on the opcode passed through the parameter. It calls other methods to convert
     * each parameter into a byte array and uses an array copy to copy each byte array into the correct index of the buf array. 
     * The buf array, address and port are all used in the constructor to create a new Packet and this Packet is returned.
     * @param opcode The opcode of either Read or Write to generate the correct packet.
     * @param fileName The file name to be added to the buf array of the packet
     * @param address The address used for the constructor of DatagramPacket
     * @param port The port used for the constructor of DatagramPacket
     * @return DatagramPacket for a Read or Write Packet depending on the opcode.
     */
    public DatagramPacket generateReadorWritePacket(int opcode, String fileName, InetAddress address, int port) {
        byte[] buf = new byte[516];
        System.arraycopy(setOpcode(opcode), 0, buf, 0 , setOpcode(opcode).length);
        System.arraycopy((fileName+"\0").getBytes(), 0, buf, setOpcode(opcode).length, fileName.length() + 1);
        System.arraycopy(("octet"+"\0").getBytes(), 0, buf, (setOpcode(opcode).length + fileName.length()) + 1, ("octet"+"\0").length());
        DatagramPacket pkt = new DatagramPacket(buf, buf.length, address, port);
        return pkt;
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

    /**
     * Main method creating a new instance of UDPSocketClient.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        UDPSocketClient client = new UDPSocketClient();
    }
    
}
