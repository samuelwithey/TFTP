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
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPSocketServerThread extends Thread {
    
    protected DatagramSocket socket = null;
    protected InetAddress address;
    protected DatagramPacket receivePacket;
    private int destinationTID;
    private byte OP_RRQ = 1;
    private byte OP_WRQ = 2;
    private byte OP_DATA = 3;
    private byte OP_ACK = 4;
    private byte OP_ERROR = 5;

    public UDPSocketServerThread(DatagramPacket receivedPacket) throws SocketException, UnknownHostException {
        address = InetAddress.getByName("127.0.0.1");
        socket = new DatagramSocket(generateTID(), address);
        receivePacket = receivedPacket;
        destinationTID = receivePacket.getPort();
        System.out.println("Thread has been created. Packet opcode: " + getOpcode(receivePacket.getData()));
    }
    
    @Override
    public void run() {
        if(getOpcode(receivePacket.getData()) == OP_RRQ) {
            try {
                writeFileToClient();
            } catch (IOException ex) {
                //Logger.getLogger(UDPSocketServerThread.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error");
            }
        } else if(getOpcode(receivePacket.getData()) == OP_WRQ) {
            try {
                getFileFromClient();
                System.out.println("Write request received");
            } catch (IOException ex) {
                //Logger.getLogger(UDPSocketServerThread.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error");
            }
        } else if(getOpcode(receivePacket.getData()) == OP_ERROR) {
        
        }
    }
    
    public int generateTID() {
        Random rand = new Random();
        return rand.nextInt(60000) + 1024;
    }
    
    public void writeFileToClient() throws SocketException, IOException {
        //socket = new DatagramSocket(generateTID()); //generate new socket using random TID port
        int blockNum = 0;
        byte[] fileData;
        int bytesLeft = 0;
        int index = 0;
        File file;
        String fileName = getFileName(receivePacket.getData());
        file = getFile(fileName);
        fileData = Files.readAllBytes(file.toPath());
        bytesLeft = fileData.length;
        DatagramPacket dataPacket = generateDataPacket(blockNum, Arrays.copyOfRange(fileData, index, 512), address, destinationTID);
        socket.send(dataPacket);
        index += 512;
        bytesLeft -= 512;
        DatagramPacket receivedPacket;
        do {
            receivedPacket = new DatagramPacket(new byte[4], 4);
            socket.receive(receivedPacket);
            destinationTID = receivePacket.getPort();
            if(getOpcode(receivePacket.getData()) == OP_ACK && getBlockNum(receivePacket.getData()) == blockNum){
                blockNum++;
                if(bytesLeft >= 512) {
                    byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + 512);
                    bytesLeft -= 512;
                    index += 512;
                    DatagramPacket dataPack = generateDataPacket(blockNum, dataToSend, address, destinationTID);
                    socket.send(dataPack);
                } else if(bytesLeft < 512) {
                    byte[] dataToSend = Arrays.copyOfRange(fileData, index, index + bytesLeft + 1);
                    bytesLeft = 0;
                    index += bytesLeft;
                    DatagramPacket dataPack = generateDataPacket(blockNum, dataToSend, address, destinationTID); //sends back the same block number
                    socket.send(dataPack);
                }
            }
        } while(bytesLeft > 0);
         socket.close();
    }
    
    public void getFileFromClient() throws SocketException, IOException {
        int blockNum = 0;
        Path currentRelativePath = Paths.get("");
        String filePath = currentRelativePath.toAbsolutePath().toString();
        filePath += "//" + getFileName(receivePacket.getData());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            System.out.println("File output has been created");
            DatagramPacket ackPacket = generateAckPacket(blockNum, address, destinationTID);
            socket.send(ackPacket);
            System.out.println("Ack packet has been sent");
            DatagramPacket receivedPacket;
            do {
                receivedPacket = new DatagramPacket(new byte[516], 516);
                socket.receive(receivedPacket);
                System.out.println("Packet has been received");
                if(getOpcode(receivedPacket.getData()) == OP_DATA && getBlockNum(receivedPacket.getData()) == blockNum) {
                    System.out.println("Packet is a data packet");
                    outputStream.write(Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength() - 1));
                    System.out.println("data has been written to file");
                    DatagramPacket ackPack = generateAckPacket(getBlockNum(receivedPacket.getData()), address, destinationTID);
                    socket.send(ackPack);
                    System.out.println("Ack packet has been sent");
                    blockNum++;
                } else if(getOpcode(receivedPacket.getData()) == OP_ERROR) {
                    System.out.println("An error packet was received");
                }
            } while(receivedPacket.getData().length == 516 && ((getOpcode(receivedPacket.getData()) == OP_DATA) || (getOpcode(receivedPacket.getData()) == OP_ERROR)));
        }
        socket.close();
    }
    
    public File getFile(String fileName) {
        try{
            Path currentRelativePath = Paths.get("");
            String filePath = currentRelativePath.toAbsolutePath().toString();
            File file = new File(filePath + "//" + fileName);
            return file;
        } catch(Exception e) {
            System.out.println("Error");
        }
        return null;
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
}
