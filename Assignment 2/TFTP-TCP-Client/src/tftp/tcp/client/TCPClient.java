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
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * The class acts as a client that connects to the Server. It allows for the user to send read and write
 * requests to the server and manages the functionality of sending and receiving the files based on the
 * input of the file name from the user.
 * @author 164574
 */
public class TCPClient {

    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ERROR = 5;
    
    private InetAddress address;
    private Socket socket;
    private DataInputStream inputData;
    private DataOutputStream outputData;
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    private int blockNum;
    
    /**
     * Constructor which sets the address and calls the method getUserInput()
     * @throws IOException 
     */
    public TCPClient() throws IOException {
        address = InetAddress.getByName("127.0.0.1");
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
     * The method writes a file to the server. The method creates a new socket and new instance of inputData and outputData. The method
     * then generates a request packet using the requestPacket method, taking the WRQ opcode and filename passed through the parameter
     * from getUserInput(). The request is then sent to the server. inputStream then opens a connection to the file based on the filename.
     * The method will then generate byte arrays of size 512 of data and send these buf arrays with the correct opcode and block number
     * until the final byte array is sent of size bytes left when bytes left is less than 512. Once all data has been sent, the socket is closed.
     * @param fileName passed from getUserInput() to get the file from the client.
     */
    public void writeFile(String fileName) throws IOException {
        
        socket = new Socket(address, 9000);
        inputData = new DataInputStream(socket.getInputStream());
        outputData = new DataOutputStream(socket.getOutputStream());
        
        System.out.println("Generating write request...");
        byte[] request = requestPacket(OP_WRQ, fileName);
        outputData.write(request);
        System.out.println("Write request sent to server.");
        
        blockNum = 0;
        inputStream = new FileInputStream(new File(fileName));
        
        byte[] data;
        int bytesLeft = inputStream.available();
        
        try {
            do{
                
                System.out.println("Bytes left to send: " + bytesLeft);
                if(bytesLeft >= 512) {
                    data = new byte[512];
                } else {
                    data = new byte[bytesLeft];
                }
                inputStream.read(data);
                byte[] allData = new byte[data.length + 4];
                setOpcode(allData, OP_DATA);
                setBlockNum(allData, blockNum++);
                setData(allData, data);
                outputData.write(allData);
                System.out.println("Data packet sent with block #: " + blockNum);
                bytesLeft = inputStream.available();
            } while(bytesLeft > 0) ;
        } catch (IOException e) {
            System.out.println("Error");
        }
        socket.close();
    }
    
    /**
     * The method receives files from the server and writes the data to a file. The method takes a file name as the parameter which is
     * used to send in the RRQ Packet so the server knows what file to send back. The Read Request Packet (RRQ) is sent to the client and
     * the method waits to receive a packet in the loop. Once it receives the packet, it checks if the packet is a Data Packet and that it
     * has the expected block code. The method will then write the data from the packet to the file stored locally and responds to the server
     * with an Acknowledgement Packet with the same block number that was sent. This will be continued until the final Data Packet has less than
     * 512 bytes of data. The socket will then close and the user menu will be displayed.
     * 
     * The method receives a file from the server and writes the data to a file. The method takes a file name through the parameter which is used
     * to send the read request. The read request is generated using the opcode and filename and sent the the server. The method generates a new file
     * based on the file name. The method then reads data sent from the server in byte array size 516 and writes this data to the file until a byte array
     * is received of data less than 516 bytes. Once the final byte array has been written to the file, the socket is closed.
     * @param fileName passed as a parameter from getUserInput() and used to send to the server through a RRQ.
     */
    public void receiveFile(String fileName) throws IOException {
        socket = new Socket(address, 9000);
        inputData = new DataInputStream(socket.getInputStream());
        outputData = new DataOutputStream(socket.getOutputStream());
        
        System.out.println("Generating read request...");
        byte[] request = requestPacket(OP_RRQ, fileName);
        outputData.write(request);
        System.out.println("Read request sent to server.");
        
        try {
            outputStream = new FileOutputStream(new File(fileName));
        } catch (FileNotFoundException ex) {
            System.out.println("Error");
        }
        byte[] data;
        try {
            do {
                data = new byte[516];
                inputData.read(data);
                if(getOpcode(data) == OP_DATA) {
                    outputStream.write(removeTrailingBytes(Arrays.copyOfRange(getData(data), 0, data.length - 4)));
                    System.out.println("Data has been written to file.");
                } else if(getOpcode(data) == OP_ERROR) {
                    System.out.println("Error");
                }
            } while(removeTrailingBytes(data).length == 516);
            System.out.println("All data has been received and written to file");
        } catch(IOException e) {
            System.out.println("Error");
        }
        socket.close();
    }
    
    /**
     * The method generates a request of byte array 516 which includes the opcode, filename and
     * mode. The byte array is then returned.
     * @param opcode The opcode to be sent as two bytes
     * @param fileName The file name generated into a byte array
     * @return byte array of a request including opcode, filename and mode to be sent to the server
     */
    public byte[] requestPacket(int opcode, String fileName) {
        byte[] arr = new byte[516];
        setOpcode(arr, opcode);
        setFileName(arr, fileName);
        setMode(arr, fileName.length());
        return arr;
    }
    
    /**
     * The method sets the filename for the byte array arr. It converts the file name into a byte array
     * and inserts this and byte '0' into the array at index 2.
     * @param arr The array which the filename needs to be inserted into as an array of bytes
     * @param fileName The file name which needs to be inserted into as an array of bytes
     */
    public void setFileName(byte[] arr, String fileName) {
        System.arraycopy((fileName+"\0").getBytes(), 0, arr, 2, fileName.length() + 1);
    }
    
    /**
     * The method sets the mode "Octet" for the byte array arr. It converts the mode into a byte array
     * and inserts this and an additional byte '0' into the array at index fileNameLenght + 3.
     * @param arr The array which the mode needs to be inserted into as an array of bytes
     * @param fileNameLength The length of the file name, used for calculating the correct indexing.
     */
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
     * The method copies the byte array data into byte array arr starting from index 4.
     * @param arr The array where the data needs to be copied into
     * @param data The array which needs to be copied into arr
     */
    public void setData(byte[] arr, byte[] data) {
        System.arraycopy(data, 0, arr, 4, data.length);
    }
    
    /**
     * The method gets the data from the byte array arr from index 4.
     * @param arr The byte array
     * @return arr excluding the first 4 elements of the array
     */
    public byte[] getData(byte[] arr) {
        return Arrays.copyOfRange(arr, 4, arr.length);
    }
    
    /**
     * The method takes a opcode through the parameter and splits the opcode into a byte array of size 2.
     * This byte array is then returned.
     * @param opcode The opcode to be converted into a byte array of size 2.
     * @return byte array of the converted opcode.
     */
    public void setOpcode(byte[] arr, int opcode) {
        arr[0] = (byte) ((opcode >> 8) & 0xFF);
        arr[1] = (byte) (opcode & 0xFF);
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
     * Main method creating a new instance of TCPClient.
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        TCPClient client = new TCPClient();
    }
    
}
