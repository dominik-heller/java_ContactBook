/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiiserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server class
 *
 * @author Dominik
 */
public class Server {

    /**
     * Connects the main server thread to the database and makes it to listen on
     * given port (via accept() method). Creates a new ServerThread instance for
     * every newly connected client
     *
     * @param portNumber specifies port
     * @param dbUrl specifies URL
     * @param dbLoginName database entry LoginName
     * @param dbPassword database entry Password
     */
    public void serverStart(int portNumber, String dbUrl, String dbLoginName, String dbPassword) {
        try (
                Connection con = DriverManager.getConnection(dbUrl, dbLoginName, dbPassword);
                ServerSocket serverSocket = new ServerSocket(portNumber);) {
            while (true) {
                Socket socket = serverSocket.accept();
                ServerThread thread = new ServerThread(socket, con);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        } catch (SQLException ex) {
            System.out.println("Exception caught when trying to connect to database");
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
