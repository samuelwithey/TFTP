/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tftp.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Byte.compare;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;

    /**
     * The constructor sets the address, generates a new socket, assigns the initial packet to a DatagramPacket and sets the TID
     * to the random TID of the received packet.
     * @param receivedPacket
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public TCPServerThread(Socket socket) throws SocketException, UnknownHostException, IOException {
        address = InetAddress.getByName("127.0.0.1");
        this.socket = socket;
        inputData = new DataInputStream(socket.getInputStream());
        outputData = new DataOutputStream(socket.getOutputStream());
        System.out.println("Thread has been created");
    }
    
    /**
     * The method checks the opcode of the received packet. It calls the corresponding methods based on the opcode.
     */
    @Override
    public void run() {
        try {
            byte[] buf = new byte[516];
            outputData.write(buf);
            int opcode = getOpcode(buf);
            System.out.println("opcode: " + opcode);
            System.out.println("First packet has been received.");
            if(opcode == OP_RRQ) {
                writeFileToClient();
            } else if(opcode == OP_WRQ) {
                getFileFromClient();
            }
        } catch (IOException ex) {
            //Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error with packets");
        }
    }
    
    /**
     * The method handles the functionality of the Read Request (RRQ). It gets the file name from the RRQ Packet and gets the file
     * locally. If the file is not found, an error message is displayed in the log and an error packet is sent back. If the file is found,
     * the method sends the data in 512 bytes using the data packet. It waits to receive an Acknowledgement Packet with the correct block number
     * before sending each chunk of 512 bytes. Once all the data has been sent and the final acknowledgement packet has been received, the connection
     * is closed.
     */
    public void writeFileToClient(){ //read request
        try {
            byte[] data = new byte[inputData.readInt()];
            inputData.read(data);
            System.out.println("Server read a file");
            File file = new File(getFileName(data));
            System.out.println("File");
            inputStream = new FileInputStream(file);
            byte[] fileData;
            int bytesLeft;
            bytesLeft = inputStream.available();
            if(bytesLeft >= 512) {
                fileData = new byte[512];
            } else {
                fileData = new byte[bytesLeft];
            }
            do {
                inputStream.read(data);
                byte[] total = new byte[data.length + 4];
                outputData.writeInt(total.length);
                setOpcode(total, OP_DATA);
                setBlockNum(total, blockNum++);
                setData(total, data);
                outputData.write(data);
                bytesLeft = inputStream.available();
                if(bytesLeft >= 512) {
                    fileData = new byte[512];
                } else {
                    fileData = new byte[bytesLeft];
                }
            } while(bytesLeft > 0);
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
    public void getFileFromClient() throws IOException { //WRQ
        byte[] data;
        data = new byte[inputData.readInt()];
        inputData.read(data);
        File file = new File(getFileName(data));
        outputStream = new FileOutputStream(file);
        byte[] fileData;
        try {
            do {
                fileData = new byte[inputData.readInt()];
                inputData.read(data);
                if(OP_DATA == getOpcode(fileData)) {
                    outputData.write(Arrays.copyOfRange(data, 4, data.length));
                }
            } while(removeTrailingBytes(fileData).length == 516);
        } catch(IOException e){
            System.out.println("Error");
        }
    }
    
    public void setData(byte[] arr, byte[] data) {
        System.arraycopy(data, 0, arr, 4, arr.length);
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
        System.arraycopy(("Octet"+"\0").getBytes(), 0, arr, fileNameLength + 3, ("Octet"+"\0").length() + 1);
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
