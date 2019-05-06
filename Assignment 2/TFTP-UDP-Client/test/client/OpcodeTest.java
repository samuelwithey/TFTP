/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author samue
 */
public class OpcodeTest {
  
    private int opcode = 2;
    
   @Test
   public void setOpcodeTest() throws IOException {
       UDPSocketClient client = new UDPSocketClient();
       byte[] arr = client.setOpcode(opcode);
       int returnOpcode = client.getOpcode(arr);
       System.out.println(returnOpcode);
   }
}
