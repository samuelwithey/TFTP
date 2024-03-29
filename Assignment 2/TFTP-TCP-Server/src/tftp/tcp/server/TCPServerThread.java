package tftp.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Byte.compare;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * The class allows the client to interact with the server thread. The thread will receive a request from the client
 * and it will fulfil the read or write request.
 * @author 164574
 */
public class TCPServerThread extends Thread {
    
    protected Socket socket = null;
    protected InetAddress address;
    protected DataInputStream inputData = null;
    protected DataOutputStream outputData = null;
    protected FileOutputStream outputStream;
    protected FileInputStream inputStream;
    protected int blockNum;
    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ERROR = 5;

    /**
     * The constructor sets the address, generates a new socket, initialises inputData and outputData and sets blockNum to 0.
     * @param socket
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public TCPServerThread(Socket socket) throws SocketException, UnknownHostException, IOException {
        address = InetAddress.getByName("127.0.0.1");
        this.socket = socket;
        inputData = new DataInputStream(socket.getInputStream());
        outputData = new DataOutputStream(socket.getOutputStream());
        blockNum = 0;
        System.out.println("Thread has been created");
    }
    
    /**
     * The method checks the opcode of the received packet. It calls the corresponding methods based on the opcode and passes the filename
     * extracted from the request.
     */
    @Override
    public void run() {
        try {
            byte[] buf = new byte[516];
            inputData.read(buf);
            int opcode = getOpcode(buf);
            System.out.println("opcode: " + opcode);
            System.out.println("Request has been received.");
            if(opcode == OP_RRQ) {
                System.out.println("Read request has been received.");
                String fileName = getFileName(buf);
                System.out.println("File Name: " + fileName);
                writeFileToClient(fileName);
            } else if(opcode == OP_WRQ) {
                System.out.println("Write request has been received.");
                String fileName = getFileName(buf);
                System.out.println("File Name: " + fileName);
                getFileFromClient(fileName);
            }
        } catch (IOException ex) {
            System.out.println("Error with packets");
        }
    }
    
    /**
     * The method handles the functionality of the Read Request (RRQ). It gets the file locally using the fileName from run(). The method creates byte arrays
     * of size 512 which contains the data from the file. This data is sent to the client in bytes arrays of size 516 which includes the opcode and block number
     * until all the data from the file has been sent to the client.
     */
    public void writeFileToClient(String fileName){ 
        try {
            byte[] data;
            inputStream = new FileInputStream(getFile(fileName));
            int bytesLeft;
            bytesLeft = inputStream.available();
            if(bytesLeft >= 512) {
                data = new byte[512];
            } else {
                data = new byte[bytesLeft];
            }
            do {
                inputStream.read(data);
                byte[] total = new byte[data.length + 4];
                setOpcode(total, OP_DATA);
                setBlockNum(total, blockNum++);
                setData(total, data);
                outputData.write(total);
                bytesLeft = inputStream.available();
                if(bytesLeft >= 512) {
                    data = new byte[512];
                } else {
                    data = new byte[bytesLeft];
                }
            } while(bytesLeft > 0);
        } catch(IOException e) {
            System.out.println("Error with read request");
        }
    }
    
    /**
     * The method handles the functionality of the Write Request (WRQ). The method checks if the opcode of the received buf array is of type data
     * and writes the data to the file until a buf array is received with less than 516 bytes of data.
     */
    public void getFileFromClient(String fileName) throws IOException { //WRQ
        byte[] data;
        outputStream = new FileOutputStream(getFile(fileName));
        try {
            do {
                data = new byte[516];
                inputData.read(data);
                if(OP_DATA == getOpcode(data)) {
                    outputStream.write(removeTrailingBytes(Arrays.copyOfRange(data, 4, data.length)));
                }
            } while(removeTrailingBytes(data).length == 516);
        } catch(IOException e){
            System.out.println("Error");
        }
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
     * The method sets the filename for the byte array arr. It converts the file name into a byte array
     * and inserts this and byte '0' into the array at index 2.
     * @param arr The array which the filename needs to be inserted into as an array of bytes
     * @param fileName The file name which needs to be inserted into as an array of bytes
     */
    public void setFileName(byte[] arr, String fileName) {
        System.arraycopy((fileName+"/0").getBytes(), 0, arr, 2, fileName.length() + 1);
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
    
}
