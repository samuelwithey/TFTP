/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 *
 * @author samue
 */
public class PacketTest {
    
    UDPSocketServer server;
    private final int OP_RRQ = 1;
    private final int OP_WRQ = 2;
    private final int OP_DATA = 3;
    private final int OP_ACK = 4;
    private final int OP_ERROR = 5;
    
    @Before
    public void setUp() throws SocketException {
        server = new UDPSocketServer();
    }

    @Test
    public void createReadRequest() throws IOException {
        String fileName = "helloworld.txt";
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket pkt = server.generateReadandWrtiePacket(OP_RRQ, fileName, address, 2000);
        System.out.println("Read Request Opcode: " + server.getOpcode(pkt.getData()));
        assertThat(OP_RRQ, is(equalTo(server.getOpcode(pkt.getData()))));
        System.out.println("Read Request filename: " + server.getFileName(pkt.getData()));
        assertThat(fileName, is(equalTo(server.getFileName(pkt.getData()))));
    }
    
    @Test
    public void createWriteRequest() throws IOException{
        String fileName = "helloworld.txt";
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket pkt = server.generateReadandWrtiePacket(OP_WRQ, fileName, address, 2000);
        System.out.println("Write Request Opcode: " + server.getOpcode(pkt.getData()));
        assertThat(OP_WRQ, is(equalTo(server.getOpcode(pkt.getData()))));
        System.out.println("Write Request filename: " + server.getFileName(pkt.getData()));
        assertThat(fileName, is(equalTo(server.getFileName(pkt.getData()))));
    }
    
    @Test
    public void createDataPacket() throws IOException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        //DatagramPacket pkt = server.generateDataPacket(OP_RRQ, data, address, OP_RRQ)
    }
    
    @Test
    public void createAckPacket() throws IOException{
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket pkt = server.generateAckPacket(0, address, 2000);
        System.out.println("Ack packet opcode: " + server.getOpcode(pkt.getData()));
        assertThat(OP_ACK, is(equalTo(server.getOpcode(pkt.getData()))));
        System.out.println("Ack packet block #: " + server.getBlockNum(pkt.getData()));
        assertThat(0, is(equalTo(server.getBlockNum(pkt.getData()))));
    }
    
    @Test
    public void createErrorPacket() throws IOException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        String e = "File not found !";
        DatagramPacket pkt = server.generateErrorPacket(1, e, address, 2000);
        System.out.println("Error packet opcode: " + server.getOpcode(pkt.getData()));
        assertThat(OP_ERROR, is(equalTo(server.getOpcode(pkt.getData()))));
        System.out.println("Error packet error code: " + server.getErrorCode(pkt.getData()));
        assertThat(1, is(equalTo(server.getErrorCode(pkt.getData()))));
        System.out.println("Error packet error message: " + server.getErrorMsg(pkt.getData()));
        //assertThat(e, is(equalTo(server.getErrorMsg(pkt.getData()))));
    }
    
}