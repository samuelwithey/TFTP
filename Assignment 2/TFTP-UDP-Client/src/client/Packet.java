/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 *
 * @author 
 */
public class Packet {
   
    private PacketType pktType; //used for all packets
    private int opcode;    //used for all packets
    private String fileName; //used for RRQ and WRQ
    private String octet;   //used to set the DATA packet mode
    private byte blockNum;  //used for DATA and ACK packets
    private byte[] data;  //used for DATA packets
    private byte errorCode; //used for error packets
    private String errorMsg; //used for error packets
    public Packet(PacketType pktType, int opcode, String fileName) { //constructor for RRQ and WRQ
        this.pktType = pktType;
        this.opcode = opcode;
        this.fileName = fileName;
        this.octet = "octet";
    }
    
    public Packet(PacketType pktType, byte opcode, byte blockNum, byte[] data) { //used for data packets
        this.pktType = pktType;
        this.opcode = opcode;
        this.blockNum = blockNum;
        this.data = data;
    }
    
    public Packet(PacketType pktType, byte opcode, byte blockNum) { //used to generate ack packets
        this.pktType = pktType;
        this.opcode = opcode;
        this.blockNum = blockNum;
    }
    
    public Packet(PacketType pktType, byte opcode, byte errorCode, String errorMsg) { //used to generate error packets
        this.pktType = pktType;
        this.opcode = opcode;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
    
    public PacketType getPacketType() {
        return this.pktType;
    }
    
    public int getOpcode() {
        return this.opcode;
    }
    
    public String getFileName() {
        return this.fileName;
    }
    
    public byte getBlockNum() {
        return this.blockNum;
    }
    
//    public byte getData() {
//        return this.data;
//    }
    
    public byte getErrorCode() {
        return this.errorCode;
    }
    
    public String getErrorMsg() {
        return this.errorMsg;
    }
    
    public void setPacketType(PacketType pktType) {
        this.pktType = pktType;
    }
    
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public void setBlockNum(byte blockNum) {
        this.blockNum = blockNum;
    }
    
//    public void setData(byte data) {
//        this.data = data;
//    }
    
    public void setErrorCode(byte errorCode){
        this.errorCode = errorCode;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
