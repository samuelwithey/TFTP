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
   
    private PacketType pktType;
    private byte opcode;
    
    public Packet(PacketType pktType, byte opcode) {
        this.pktType = pktType;
        this.opcode = opcode;
    }
    
    public PacketType getPacketType() {
        return pktType;
    }
    
    public byte getOpcode() {
        return opcode;
    }
    
    public void setPacketType(PacketType pktType) {
        this.pktType = pktType;
    }
    
    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }
    
}
