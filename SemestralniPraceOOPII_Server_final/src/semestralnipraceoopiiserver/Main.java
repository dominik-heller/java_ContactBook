/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiiserver;

/**
 * Main class to initialize Server side application.
 *
 * @author Dominik
 */
public class Main {

    /**
     * Creates new Sever instance on which the serverStart method is initialized.
     * Please specify the method parameters based on your database login
     * credentials.
     *
     * @param args
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.serverStart(4337, "jdbc:mysql://localhost:3306/contact_book", "root", "root");
    }
}
