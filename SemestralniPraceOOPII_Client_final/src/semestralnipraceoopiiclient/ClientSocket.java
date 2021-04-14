/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiiclient;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Client socket class. Provides methods to communicate with Server side over
 * network (socket).
 *
 * @author Dominik
 */
public class ClientSocket {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectOutputStream outobj;
    private ObjectInputStream inobj;
    private InputStream inStream;
    private OutputStream outStream;
    private String hostName = "localhost";
    private int portNumber = 4337;
    private ByteArrayOutputStream byteArrayOutputStream;

    /**
     * Instantiates socket on the client side as well as all the necessary
     * components to send and receive data to/from server side.
     *
     * @throws IOException
     */
    public void process() throws IOException {
        socket = new Socket(hostName, portNumber);
        outStream = socket.getOutputStream();
        inStream = socket.getInputStream();
        out = new PrintWriter(outStream, true);
        in = new BufferedReader(new InputStreamReader(inStream));
        inobj = new ObjectInputStream(inStream);
        outobj = new ObjectOutputStream(outStream);
        byteArrayOutputStream = new ByteArrayOutputStream();
    }

    /**
     * Sends String to server side.
     *
     * @param s String to be sent
     */
    public void write(String s) {
        out.println(s);
    }

    /**
     * Reads String from server side.
     *
     * @return received String
     * @throws IOException
     */
    public String read() throws IOException {
        String s = in.readLine();
        return s;
    }

    /**
     * Reads Object from server side.
     *
     * @return received Object
     */
    public Object readObj() {
        try {
            return inobj.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Reads Object from server side.
     *
     * @param obj Object to be sent
     */
    public void writeObj(Object obj) {
        try {
            outobj.writeObject(obj);
        } catch (IOException ex) {
            Logger.getLogger(ClientSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Closes the outputStream. Hence the socket as well.
     */
    public void close() {
        try {
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sends Image from server side.
     *
     * @param image instance of image
     * @param fileExtension extension of the image
     */
    public void writeImage(BufferedImage image, String fileExtension) {
        try {
            ImageIO.write(image, fileExtension, byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byte[] size = ByteBuffer.allocate(6).putInt(byteArrayOutputStream.size()).array();
            outStream.write(size);
            System.out.println(size);
            outStream.write(byteArrayOutputStream.toByteArray());
            System.out.println(byteArrayOutputStream.toByteArray());
            outStream.flush();
        } catch (IOException ex) {
            Logger.getLogger(ClientSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
