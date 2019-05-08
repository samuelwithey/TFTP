///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package server;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import static org.junit.Assert.*;
//import java.io.FileReader;
//import java.io.InputStream;
//import java.io.OutputStream;
///**
// *
// * @author samue
// */
//public class FileTest {
//    
//    private UDPSocketServer server;
//    
//    @Before
//    public void setUp() throws IOException {
//        server = new UDPSocketServerThread();
//    }
//    
//    @Test
//    public void FileTest() throws FileNotFoundException, IOException {
//        String fileName = "test.txt";
//        File file = server.getFile(fileName);
//        boolean fileFound = false;
//        if(file != null) {
//            fileFound = true;
//        }
//        assertEquals(true, fileFound);
//        System.out.println(file.getAbsolutePath());
//        //doesn't work
//        FileReader freader = new FileReader(file);
//        BufferedReader br = new BufferedReader(freader);
//        String st;
//        while((st = br.readLine()) != null) {
//            System.out.println(st);
//        }
//    }
//}
