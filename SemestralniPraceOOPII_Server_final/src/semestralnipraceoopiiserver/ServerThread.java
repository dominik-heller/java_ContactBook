/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiiserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import static java.lang.Thread.currentThread;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import semestralnipraceoopiidata.Contact;
import semestralnipraceoopiidata.User;

/**
 * ServerThread class
 *
 * @author Dominik
 */
public class ServerThread extends Thread {

    private Socket socket = null;
    private Connection con;
    private PreparedStatement pst = null;
    private ResultSet rs = null;
    private Statement st = null;
    private String loginName;
    private int userID;

    /**
     * Creates a dedicated ServerThread for newly connected client.
     *
     * @param socket instance of server socket
     * @param con instance of server connection
     * @throws IOException
     */
    public ServerThread(Socket socket, Connection con) throws IOException {
        this.socket = socket;
        this.con = con;
    }

    /**
     * Causes ServerThread to listen via BufferedReader instance for messages
     * from client to determine further action.
     */
    @Override
    public void run() {
        try (
                OutputStream outStream = socket.getOutputStream();
                InputStream inStream = socket.getInputStream();
                PrintWriter out = new PrintWriter(outStream, true);
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                ObjectOutputStream outobj = new ObjectOutputStream(outStream);
                ObjectInputStream inobj = new ObjectInputStream(inStream);) {
            sendUserList(outobj);
            while (true) {
                String input;
                if (in.ready()) {
                    input = in.readLine();
                    if (input.equals("LOGIN")) {
                        login(in, out);
                        continue;
                    }
                    if (input.equals("SIGNUP")) {
                        signup(in, out);
                        continue;
                    }
                    if (input.equals("GETCONTACTS")) {
                        sendContacts(outobj);
                        continue;
                    }
                    if (input.equals("GETUSERS")) {
                        sendUserList(outobj);
                        continue;
                    }
                    if (input.equals("INSERT")) {
                        insert(in, out);
                        continue;
                    }
                    if (input.equals("UPDATE")) {
                        update(in, out);
                        continue;
                    }
                    if (input.equals("DELETE")) {
                        delete(in, out);
                        continue;
                    }
                    if (input.equals("SHARE")) {
                        share(outobj, inobj, in, out);
                        continue;
                    }
                    if (input.equals("SHAREDWITH")) {
                        sharedWith(in, out, outobj);
                        continue;
                    }
                    if (input.equals("USERIMAGE")) {
                        storeUserImage(inStream, out);
                        continue;
                    }
                    if (input.equals("CONTACTIMAGE")) {
                        storeContactImage(in, out, inStream);
                        continue;
                    }
                    if (input.equals("END")) {
                        break;
                    }
                }
            }
        } catch (IOException | SQLException | ClassNotFoundException ex) {
            try {
                closeSQL();
            } catch (SQLException ex1) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        System.out.println("Konec: " + currentThread().getName());
    }

    /**
     * Sends ArrayList of User instances collected from database to client.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void sendUserList(ObjectOutputStream outobj) throws SQLException {
        try {
            ArrayList<User> userList = new ArrayList<User>();
            String query = "select ID, LoginName, FirstName, LastName, Image from user;";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt(1);
                String loginname = rs.getString(2);
                String fname = rs.getString(3);
                String lname = rs.getString(4);
                byte[] image = rs.getBytes(5);
                User user = new User(id, loginname, fname, lname, image);
                userList.add(user);
            }
            outobj.writeObject(userList);
            outobj.flush();
            closeSQL();
        } catch (SQLException | IOException ex) {
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checks if login credetials sent by client are valid. Especially
     * password.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void login(BufferedReader in, PrintWriter out) throws SQLException {
        try {
            loginName = in.readLine();
            String password = in.readLine();
            String query1 = "select ID from user where LoginName='" + loginName + "';";
            st = con.createStatement();
            rs = st.executeQuery(query1);
            if (rs.next()) {
                userID = rs.getInt(1);
                String query2 = "select password from user where LoginName = ?;";
                pst = con.prepareStatement(query2);
                pst.setString(1, loginName);
                rs = pst.executeQuery();
                if (rs.next() && rs.getString(1).equals(password)) {
                    closeSQL();
                    out.println("OK");
                    return;
                }
            }
            closeSQL();
            out.println("ERROR");
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Registers new client to database.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void signup(BufferedReader in, PrintWriter out) throws SQLException {
        try {
            loginName = in.readLine();
            String query = "select LoginName from user where LoginName = ?;";
            pst = con.prepareStatement(query);
            pst.setString(1, loginName);
            rs = pst.executeQuery();
            if (rs.next()) {
                out.println("ERROR");
                closeSQL();
                return;
            }
            out.println("OK");
            query = in.readLine();
            st = con.createStatement();
            st.executeUpdate(query);
            out.println("OK");
            closeSQL();
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sends ArrayList of contact instances collected from database to client.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void sendContacts(ObjectOutputStream outobj) throws SQLException {
        try {
            ArrayList<Contact> contactList = new ArrayList<Contact>();
            String query = "Select c.ID, c.FirstName, c.LastName, c.PhoneNumber, c.EmailAddress, c.GroupName, c.Image, uc.Privilege "
                    + "from user u Inner join user_contact uc "
                    + "on u.ID = uc.User_ID Inner join contact c "
                    + "on uc.Contact_ID = c.ID "
                    + "where LoginName = ?;";
            pst = con.prepareStatement(query);
            pst.setString(1, loginName);
            rs = pst.executeQuery();
            Contact contact;
            while (rs.next()) {
                contact = new Contact(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getBytes(7), rs.getString(8));
                contactList.add(contact);
            }
            outobj.writeObject(contactList);
            outobj.flush();
            closeSQL();
        } catch (SQLException | IOException ex) {
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Executes SQL INSERT operation on newly created contact.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void insert(BufferedReader in, PrintWriter out) throws SQLException {
        try {
            con.setAutoCommit(false);
            String query1 = in.readLine();
            String query2 = "insert into user_contact(User_ID, Contact_ID, Privilege) "
                    + "select(select (ID) from user where LoginName='" + loginName + "') as User_ID, "
                    + "(select (MAX(ID)) from contact) as Contact_ID, 'owner';";
            st = con.createStatement();
            st.addBatch(query1);
            st.addBatch(query2);
            st.executeBatch();
            con.commit();
            con.setAutoCommit(true);
            closeSQL();
            out.println("OK");
        } catch (SQLException | IOException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Executes SQL UPDATE operation on given contact.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void update(BufferedReader in, PrintWriter out) throws SQLException {
        try {
            String query = in.readLine();
            st = con.createStatement();
            st.executeUpdate(query);
            closeSQL();
            out.println("OK");
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Executes SQL DELETE operation on given contact.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void delete(BufferedReader in, PrintWriter out) throws SQLException {
        try {
            String contactId = in.readLine();
            System.out.println(contactId);
            con.setAutoCommit(false);
            String query = "delete from user_contact where Contact_ID=" + contactId + ";";
            st = con.createStatement();
            st.addBatch(query);
            String query2 = "delete from contact where ID=" + contactId + ";";
            st.addBatch(query2);
            st.executeBatch();
            con.commit();
            con.setAutoCommit(true);
            closeSQL();
            out.println("OK");
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Receives ArrayList of contacts to be shared among selected users. Stores
     * these values to database with information about each user privileges.
     *
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void share(ObjectOutputStream outobj, ObjectInputStream inobj, BufferedReader in, PrintWriter out) throws ClassNotFoundException, SQLException {
        try {
            sendUserList(outobj);
            ArrayList<Contact> contactList = (ArrayList<Contact>) inobj.readObject();
            int size = contactList.size();
            int contactIndices[] = new int[size];
            con.setAutoCommit(false);
            String query1 = "update user_contact set Privilege = 'owner (shared)' where User_ID=" + userID + " and " + " Contact_ID=?;";
            for (int i = 0; i < size; i++) {
                contactIndices[i] = contactList.get(i).getId();
                pst = con.prepareStatement(query1);
                pst.setInt(1, contactIndices[i]);
                pst.executeUpdate();
            }
            if (in.readLine().equals("SHARE2")) {
                int length = Integer.parseInt(in.readLine());
                int[] userIndices = new int[length];
                for (int i = 0; i < length; i++) {
                    userIndices[i] = Integer.parseInt(in.readLine());
                }
                String query2 = "insert into user_contact (User_ID, Contact_ID, Privilege) values (?,?,?)";
                String privilege = in.readLine();
                for (int i = 0; i < length; i++) {
                    for (int j = 0; j < size; j++) {
                        pst = con.prepareStatement(query2);
                        pst.setInt(1, userIndices[i]);
                        pst.setInt(2, contactIndices[j]);
                        pst.setString(3, privilege);
                        pst.executeUpdate();
                    }
                }
                con.commit();
                con.setAutoCommit(true);
                out.println("OK");
                closeSQL();
            } else {
                closeSQL();
            }
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sends ArraList of users and their privileges with regard to selected
     * contact to the client. Consequently allows client to revoke these
     * privileges for selected users.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void sharedWith(BufferedReader in, PrintWriter out, ObjectOutputStream outobj) throws SQLException {
        try {
            ArrayList<User> sharedWithUserList = new ArrayList<User>();
            String contactIndex = in.readLine();
            String query = "select u.ID, u.FirstName, u.LastName, uc.Privilege "
                    + "from user u inner join user_contact uc "
                    + "on u.ID = uc.User_ID "
                    + "where uc.Contact_ID = " + contactIndex + " and u.ID<>" + userID + ";";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt(1);
                String fname = rs.getString(2);
                String lname = rs.getString(3);
                String privilege = rs.getString(4);
                User user = new User(id, fname, lname, privilege);
                sharedWithUserList.add(user);
            }
            outobj.writeObject(sharedWithUserList);
            closeSQL();
            if (in.readLine().equals("DELETEPRIVILEGE")) {
                con.setAutoCommit(false);
                String query2 = "delete from user_contact where User_ID=? and Contact_ID=" + contactIndex + ";";
                int length = Integer.parseInt(in.readLine());
                int userIndex;
                for (int i = 0; i < length; i++) {
                    userIndex = Integer.parseInt(in.readLine());
                    System.out.println(userIndex);
                    pst = con.prepareStatement(query2);
                    pst.setInt(1, userIndex);
                    pst.executeUpdate();
                }
                if (sharedWithUserList.size() == length) {
                    String query3 = "update user_contact set Privilege='owner' where Contact_ID = " + contactIndex + ";";
                    st = con.createStatement();
                    st.executeUpdate(query3);
                }
                con.commit();
                con.setAutoCommit(true);
                closeSQL();
                out.println("OK");
            } else {
                closeSQL();
            }
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Stores profile image of the user to database.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void storeUserImage(InputStream inStream, PrintWriter out) throws ClassNotFoundException, SQLException {
        try {
            byte[] sizeAr = new byte[6];
            inStream.read(sizeAr);
            System.out.println(sizeAr);
            int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
            byte[] imageArray = new byte[size];
            inStream.read(imageArray);
            System.out.println(imageArray);
            String query = "update user set Image = ? where ID= ? ";
            pst = con.prepareStatement(query);
            pst.setBytes(1, imageArray);
            pst.setInt(2, userID);
            pst.executeUpdate();
            closeSQL();
            out.println("OK");
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Stores contact image sent by user to database as longBlob.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void storeContactImage(BufferedReader in, PrintWriter out, InputStream inStream) throws ClassNotFoundException, SQLException {
        try {
            int contactID = Integer.parseInt(in.readLine());
            byte[] sizeAr = new byte[6];
            inStream.read(sizeAr);
            int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
            byte[] imageArray = new byte[size];
            inStream.read(imageArray);
            String query = "update contact set Image = ? where ID= ? ";
            pst = con.prepareStatement(query);
            pst.setBytes(1, imageArray);
            pst.setInt(2, contactID);
            pst.executeUpdate();
            closeSQL();
            out.println("OK");
        } catch (IOException | SQLException ex) {
            out.println("ERROR");
            closeSQL();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Closes JDBC statement, preparedstatement and resultset. If needed
     * provides rollback of transaction.
     *
     * @throws SQLException
     */
    private void closeSQL() throws SQLException {
        if (!con.getAutoCommit()) {
            con.rollback();
            con.setAutoCommit(true);
        }
        if (rs != null) {
            rs.close();
        }
        if (st != null) {
            st.close();
        }
        if (pst != null) {
            pst.close();
        }
    }
}
