/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp.tcp.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 164574
 */
public class TCPClient {

    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;
    
    private InetAddress address;
    private Socket socket;
    private DataInputStream inputData;
    private DataOutputStream outputData;
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    private int blockNum;
    
    private int destinationTID = 9000;
    
    /**
     * Constructor which sets the address and a new instance of socket which is created using 
     * this address and a randomly generated port.
     * @throws IOException 
     */
    public TCPClient() throws IOException {
        address = InetAddress.getByName("127.0.0.1");
        socket = new Socket(address, 9000);
        inputData = new DataInputStream(socket.getInputStream());
        outputData = new DataOutputStream(socket.getOutputStream());
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
    public void writeFile(String fileName) throws IOException {
        System.out.println("Generating request packet");
        byte[] requestPacket = requestPacket(OP_WRQ, fileName, address, 9000);
        outputData.writeInt(requestPacket.length);
        outputData.write(requestPacket);
        System.out.println("Request packet sent");
        blockNum = 1;
        inputStream = new FileInputStream(new File(fileName));
        byte[] data;
        int bytesLeft;
        try {
            do{
                bytesLeft = inputStream.available();
                if(bytesLeft >= 512) {
                    data = new byte[512];
                } else {
                    data = new byte[bytesLeft];
                }
                inputStream.read(data);
                byte[] allData = new byte[data.length + 4];
                outputData.writeInt(allData.length);
                setOpcode(allData, OP_DATA);
                setBlockNum(allData, blockNum++);
                setData(allData, data);
                outputData.write(allData);
            } while(bytesLeft != 0) ;
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
        byte[] requestPacket = requestPacket(OP_RRQ, fileName, address, 9000); //send WRQ
        try {
            outputData.writeInt(requestPacket.length);
            outputData.write(requestPacket);
        } catch (IOException e) {
            System.out.println("Error");;
        }
        try {
            outputStream = new FileOutputStream(new File(fileName));
        } catch (FileNotFoundException ex) {
            System.out.println("Error");
        }
        byte[] data;
        try {
            do {
                data = new byte[inputData.readInt()];
                inputData.read(data);
                if(getOpcode(data) == OP_DATA) {
                    outputStream.write(Arrays.copyOfRange(getData(data), 0, data.length - 4));
                } else if(getOpcode(data) == OP_ERROR) {
                    System.out.println("Error");
                }
            } while(removeTrailingBytes(data).length == 516);
            System.out.println("All data has been received and written to file");
            outputStream.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Error");
        }
    }
    
    public byte[] requestPacket(int opcode, String fileName, InetAddress address, int port ) {
        byte[] arr = new byte[516];
        setOpcode(arr, opcode);
        setFileName(arr, fileName);
        setMode(arr, fileName.length());
        return arr;
    }
    
    public void setFileName(byte[] arr, String fileName) {
        System.arraycopy((fileName+"/0").getBytes(), 0, arr, 2, fileName.length() + 1);
    }
    
    public void setMode(byte[] arr, int fileNameLength) {
        String mode = "Octet";
        System.arraycopy((mode+"\0").getBytes(), 0, arr, fileNameLength + 3, mode.length() + 1);
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
    
    public void setData(byte[] arr, byte[] data) {
        System.arraycopy(data, 0, arr, 4, data.length);
    }
    
    public byte[] getData(byte[] arr) {
        return Arrays.copyOfRange(arr, 4, arr.length);
    }
    
    /**
     * The method takes a opcode through the parameter and splits the opcode into a byte array of size 2.
     * This byte array is then returned.
     * @param opcode The opcode to be converted into a byte array of size 2.
     * @return byte array of the converted opcode.
     */
    public byte[] setOpcode(byte[] arr, int opcode) {
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
    public byte[] setBlockNum(byte[] arr, int blockNum) {
        arr[2] = (byte) ((blockNum >> 8) & 0xFF);
        arr[3] = (byte) (blockNum & 0xFF);
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
        TCPClient client = new TCPClient();
    }
    
}
