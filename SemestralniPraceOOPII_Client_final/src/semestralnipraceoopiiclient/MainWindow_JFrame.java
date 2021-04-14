/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiiclient;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import semestralnipraceoopiidata.Contact;
import semestralnipraceoopiidata.User;

/**
 * MainWindow class. Takes care of client side application logic, as well as
 * presentation logic and services.
 *
 * @author Dominik
 */
public class MainWindow_JFrame extends javax.swing.JFrame {

    private ClientSocket socket;
    private CardLayout card;
    private boolean connected;
    private int mousePositionX;
    private int mousePositionY;
    private DefaultTableModel defaultModel;
    private DefaultTableModel defaultModelShare;
    private DefaultTableModel defaultModelSharedWith;
    private String loginName;
    private ArrayList<User> userList;
    private ArrayList<User> sharedWithUserList;
    private ArrayList<Contact> contactList;
    private ArrayList<Contact> sharedContactList;
    private int currentContactId;

    /**
     * Creates MainWindow frame instance.
     */
    public MainWindow_JFrame() {
        initComponents();
        setupDisplay();
        connect();
    }

    /**
     * Sets up frame to be displayed in the middle of the screen, provides
     * transparent background for undecorated window presentation and sets
     * default card using cardLayout.
     */
    private void setupDisplay() {
        //Následné dvě řádky zobrazí okno do středu obrazovky
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        this.setBackground(new Color(0, 0, 0, 0));
        MainPanel.setBackground(new Color(0, 0, 0, 0));
        card = (CardLayout) MainPanel.getLayout();
    }

    /**
     * Connects client to server.
     */
    private void connect() {
        socket = new ClientSocket();
        try {
            socket.process();
            userList = (ArrayList<User>) socket.readObj();
            connected = true;
        } catch (IOException ex) {
            connected = false;
        }
    }

    /**
     * Checks user´s privilleges to execute certain operation.
     *
     * @param p characterizes operation (D=Delete, U=Update, R=ReadOnly)
     * @return true if requirements are met false otherwise
     */
    private boolean checkPrivileges(String p) {
        int u = ContactBookTable_Table.getSelectedRow();
        String s = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(u), 5);
        if (s.contains(p) || s.contains("owner")) {
            return true;
        } else {
            JOptionPane.showMessageDialog(rootPane, "Nemáte oprávnění");
            return false;
        }
    }

    /**
     * Checks if table row is selected
     *
     * @return true if yes false otherwise
     */
    private boolean checkEmptySelection() {
        if (ContactBookTable_Table.getSelectedRowCount() != 0) {
            return true;
        } else {
            JOptionPane.showMessageDialog(rootPane, "Nevybrány žádné kontakty");
            return false;
        }
    }

    /**
     * Determines which privileges was granted.
     *
     * @return String characterizing certain privilege or combination of
     * privileges.
     */
    private String chosenPrivilege() {
        String privilege = null;
        if (InternalFrameReadOnlyPrivilege_RadioButton.isSelected()) {
            privilege = "R";
        }
        if (InternalFrameUpdatePrivilege_RadioButton.isSelected()) {
            privilege = "R+U";
        }
        if (InternalFrameDeletePrivilege_RadioButton.isSelected()) {
            privilege = "R+D";
        }
        if (InternalFrameReadOnlyPrivilege_RadioButton.isSelected() && InternalFrameUpdatePrivilege_RadioButton.isSelected()) {
            privilege = "R+U";
        }
        if (InternalFrameReadOnlyPrivilege_RadioButton.isSelected() && InternalFrameDeletePrivilege_RadioButton.isSelected()) {
            privilege = "R+D";
        }
        if (InternalFrameUpdatePrivilege_RadioButton.isSelected() && InternalFrameDeletePrivilege_RadioButton.isSelected()) {
            privilege = "R+U+D";
        }
        if (InternalFrameReadOnlyPrivilege_RadioButton.isSelected() && InternalFrameDeletePrivilege_RadioButton.isSelected() && InternalFrameUpdatePrivilege_RadioButton.isSelected()) {
            privilege = "R+U+D";
        }
        return privilege;
    }

    /**
     * Requests list of contacts from server and displays them in the table.
     */
    private void getContacts() {
        socket.write("GETCONTACTS");
        contactList = (ArrayList<Contact>) socket.readObj();
        if (!contactList.isEmpty()) {
            defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
            Object[] row = new Object[6];
            for (int i = 0; i < contactList.size(); i++) {
                row[0] = contactList.get(i).getFirstName();
                row[1] = contactList.get(i).getSurname();
                row[2] = contactList.get(i).getPhone();
                row[3] = contactList.get(i).getEmail();
                row[4] = contactList.get(i).getGroup();
                row[5] = contactList.get(i).getPrivilege();
                defaultModel.addRow(row);
            }
        }
    }

    /**
     * Hash password.
     *
     * @param Password String representation of password.
     * @return Hash code.
     */
    private int hashPassword(String Password) {
        String password = Password;
        int hash = 7;
        for (int i = 0; i < password.length(); i++) {
            hash = hash * 31 + password.charAt(i);
        }
        return hash;
    }

    /**
     * Fills the table of users to share contacts with.
     */
    private void displayTableContentInternalFrame() {
        if (!userList.isEmpty()) {
            defaultModelShare = (DefaultTableModel) InternalFrameUserTable_Table.getModel();
            defaultModelShare.setRowCount(0);
            Object[] row = new Object[3];
            for (int i = 0; i < userList.size(); i++) {
                row[0] = userList.get(i).getId();
                row[1] = userList.get(i).getFirstName();
                row[2] = userList.get(i).getLastName();
                defaultModelShare.addRow(row);
            }
        }
    }

    /**
     * Fills the table of users which share the selected contact.
     */
    private void displayTableContentInternalFrameSharedWith() {
        if (!sharedWithUserList.isEmpty()) {
            defaultModelSharedWith = (DefaultTableModel) InternalFrameSharedWithUserTable_Table.getModel();
            defaultModelSharedWith.setRowCount(0);
            Object[] row = new Object[4];
            for (int i = 0; i < sharedWithUserList.size(); i++) {
                row[0] = sharedWithUserList.get(i).getId();
                row[1] = sharedWithUserList.get(i).getFirstName();
                row[2] = sharedWithUserList.get(i).getLastName();
                row[3] = sharedWithUserList.get(i).getPrivilege();
                defaultModelSharedWith.addRow(row);
            }
        }
    }

    /**
     * Displays contacts to the table.
     */
    private void displayTable() {
        defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
        defaultModel.setRowCount(0);
        getContacts();
    }

    /**
     * Clears given textfields.
     */
    private void clearContactBookTextField() {
        ContactBookName_TextField.setText("");
        ContactBookSurname_TextField.setText("");
        ContactBookEmail_TextField.setText("");
        ContactBookPhone_TextField.setText("");
        ContactBookGroup_TextField.setText("");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainPanel = new javax.swing.JPanel();
        LoginPanel = new javax.swing.JPanel();
        LoginLogin_Label = new javax.swing.JLabel();
        LoginPassword_Label = new javax.swing.JLabel();
        LoginTitle_Label = new javax.swing.JLabel();
        LoginLogin_TextField = new javax.swing.JTextField();
        LoginPassword_PasswordField = new javax.swing.JPasswordField();
        LoginShowPassword_CheckBox = new javax.swing.JCheckBox();
        LoginConfirm_Button = new javax.swing.JButton();
        LoginToSignUp_Button = new javax.swing.JButton();
        Minimize_Label = new javax.swing.JLabel();
        Close_Label = new javax.swing.JLabel();
        Background_Label = new javax.swing.JLabel();
        SignUpPanel = new javax.swing.JPanel();
        SignUpTitle_Label = new javax.swing.JLabel();
        SignUpLogin_Label = new javax.swing.JLabel();
        SIgnUpName_Label = new javax.swing.JLabel();
        SIgnUpSurname_Label = new javax.swing.JLabel();
        SignUpPassword_Label = new javax.swing.JLabel();
        SignUpPasswordAgain_Label = new javax.swing.JLabel();
        SignUpEmail_Label = new javax.swing.JLabel();
        SignUpLogin_TextField = new javax.swing.JTextField();
        SignUpName_TextField = new javax.swing.JTextField();
        SignUpSurname_TextField = new javax.swing.JTextField();
        SignUpEmail_TextField = new javax.swing.JTextField();
        SignUpConfirm_Button = new javax.swing.JButton();
        SignUpBackToLogin_Button = new javax.swing.JButton();
        SignUpPassword_TextField = new javax.swing.JPasswordField();
        SignUpPasswordAgain_TextField = new javax.swing.JPasswordField();
        Minimize_Label1 = new javax.swing.JLabel();
        Close_Label1 = new javax.swing.JLabel();
        Background_Label1 = new javax.swing.JLabel();
        ContactBookPanel = new javax.swing.JPanel();
        ContactBookInternalFrameSharedWith_InternalFrame = new javax.swing.JInternalFrame();
        InternalFrameSharedWithDeleteButton_Button = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        InternalFrameSharedWithUserTable_Table = new javax.swing.JTable();
        ContactBookInternalFrame_InternalFrame = new javax.swing.JInternalFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        InternalFrameUserTable_Table = new javax.swing.JTable();
        InternalFrameShareButton_Button = new javax.swing.JButton();
        InternalFrameReadOnlyPrivilege_RadioButton = new javax.swing.JRadioButton();
        InternalFrameDeletePrivilege_RadioButton = new javax.swing.JRadioButton();
        InternalFrameUpdatePrivilege_RadioButton = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        ContactBookTitle_Label = new javax.swing.JLabel();
        ContactBookUserImage_Label = new javax.swing.JLabel();
        ContactBookInsert_Button = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        ContactBookDelete_Button = new javax.swing.JButton();
        ContactBookReload_Label = new javax.swing.JLabel();
        ContactBookUpdate_Button = new javax.swing.JButton();
        ContactBookShareButton_Button = new javax.swing.JButton();
        ContactBookName_TextField = new javax.swing.JTextField();
        ContactBookSurname_TextField = new javax.swing.JTextField();
        ContactBookPhone_TextField = new javax.swing.JTextField();
        ContactBookEmail_TextField = new javax.swing.JTextField();
        ContactBookGroup_TextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        ContactBookTable_Table = new javax.swing.JTable();
        ContactBookFilter_TextField = new javax.swing.JTextField();
        ContactBookLogout_Button = new javax.swing.JButton();
        ContactBookSharedWithButton_Button = new javax.swing.JButton();
        ContactBookContactImage_Label = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        Minimize_Label2 = new javax.swing.JLabel();
        Close_Label2 = new javax.swing.JLabel();
        Background_Label2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);

        MainPanel.setOpaque(false);
        MainPanel.setPreferredSize(new java.awt.Dimension(1024, 768));
        MainPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                MainPanelMouseDragged(evt);
            }
        });
        MainPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                MainPanelMousePressed(evt);
            }
        });
        MainPanel.setLayout(new java.awt.CardLayout());

        LoginPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        LoginPanel.setMinimumSize(new java.awt.Dimension(1450, 750));
        LoginPanel.setOpaque(false);
        LoginPanel.setPreferredSize(new java.awt.Dimension(1400, 700));
        LoginPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        LoginLogin_Label.setFont(new java.awt.Font("Tahoma", 1, 26)); // NOI18N
        LoginLogin_Label.setForeground(new java.awt.Color(0, 0, 0));
        LoginLogin_Label.setText("Login:");
        LoginPanel.add(LoginLogin_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 190, 90, 40));

        LoginPassword_Label.setFont(new java.awt.Font("Tahoma", 1, 26)); // NOI18N
        LoginPassword_Label.setForeground(new java.awt.Color(0, 0, 0));
        LoginPassword_Label.setText("Heslo:");
        LoginPanel.add(LoginPassword_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 290, 120, 40));

        LoginTitle_Label.setFont(new java.awt.Font("Tahoma", 3, 36)); // NOI18N
        LoginTitle_Label.setForeground(new java.awt.Color(0, 0, 0));
        LoginTitle_Label.setText("Přihlašovací stránka");
        LoginPanel.add(LoginTitle_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 30, 400, 90));

        LoginLogin_TextField.setBackground(new java.awt.Color(255, 255, 255));
        LoginLogin_TextField.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        LoginLogin_TextField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));
        LoginPanel.add(LoginLogin_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 190, 220, 40));

        LoginPassword_PasswordField.setBackground(new java.awt.Color(255, 255, 255));
        LoginPassword_PasswordField.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        LoginPassword_PasswordField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));
        LoginPassword_PasswordField.setCaretColor(new java.awt.Color(255, 255, 255));
        LoginPanel.add(LoginPassword_PasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 290, 220, 40));

        LoginShowPassword_CheckBox.setBackground(new java.awt.Color(153, 255, 255));
        LoginShowPassword_CheckBox.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        LoginShowPassword_CheckBox.setForeground(new java.awt.Color(0, 0, 0));
        LoginShowPassword_CheckBox.setText("Zobrazit heslo");
        LoginShowPassword_CheckBox.setBorder(null);
        LoginShowPassword_CheckBox.setOpaque(false);
        LoginShowPassword_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginShowPassword_CheckBoxActionPerformed(evt);
            }
        });
        LoginPanel.add(LoginShowPassword_CheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 290, 150, 40));

        LoginConfirm_Button.setBackground(new java.awt.Color(255, 255, 255));
        LoginConfirm_Button.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        LoginConfirm_Button.setForeground(new java.awt.Color(0, 0, 0));
        LoginConfirm_Button.setText("Potvrdit");
        LoginConfirm_Button.setOpaque(false);
        LoginConfirm_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginConfirm_ButtonActionPerformed(evt);
            }
        });
        LoginPanel.add(LoginConfirm_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 390, 170, 50));

        LoginToSignUp_Button.setBackground(new java.awt.Color(255, 255, 255));
        LoginToSignUp_Button.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        LoginToSignUp_Button.setForeground(new java.awt.Color(0, 0, 0));
        LoginToSignUp_Button.setText("Registrovat");
        LoginToSignUp_Button.setOpaque(false);
        LoginToSignUp_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginToSignUp_ButtonActionPerformed(evt);
            }
        });
        LoginPanel.add(LoginToSignUp_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 490, 160, 40));

        Minimize_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Minimize_Label.setForeground(new java.awt.Color(0, 0, 0));
        Minimize_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Minimize_Label.setText("_");
        Minimize_Label.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 3, 1));
        Minimize_Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Minimize_LabelMouseClicked(evt);
            }
        });
        LoginPanel.add(Minimize_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 10, 26, 30));

        Close_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Close_Label.setForeground(new java.awt.Color(0, 0, 0));
        Close_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Close_Label.setText("X");
        Close_Label.setAlignmentY(0.0F);
        Close_Label.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Close_Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Close_LabelMouseClicked(evt);
            }
        });
        LoginPanel.add(Close_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(970, 10, 20, 40));

        Background_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Background_Label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/semestralnipraceoopiiclient/background_image.png"))); // NOI18N
        Background_Label.setText("jLabel21");
        LoginPanel.add(Background_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 1040, 570));

        MainPanel.add(LoginPanel, "LoginPanel");

        SignUpPanel.setOpaque(false);
        SignUpPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        SignUpTitle_Label.setFont(new java.awt.Font("Tahoma", 1, 36)); // NOI18N
        SignUpTitle_Label.setForeground(new java.awt.Color(0, 0, 0));
        SignUpTitle_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        SignUpTitle_Label.setText("Registrační stránka");
        SignUpPanel.add(SignUpTitle_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 40, 430, 50));

        SignUpLogin_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SignUpLogin_Label.setForeground(new java.awt.Color(0, 0, 0));
        SignUpLogin_Label.setText("Login:");
        SignUpPanel.add(SignUpLogin_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 130, 100, 30));

        SIgnUpName_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SIgnUpName_Label.setForeground(new java.awt.Color(0, 0, 0));
        SIgnUpName_Label.setText("Jméno:");
        SignUpPanel.add(SIgnUpName_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 180, 100, 30));

        SIgnUpSurname_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SIgnUpSurname_Label.setForeground(new java.awt.Color(0, 0, 0));
        SIgnUpSurname_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        SIgnUpSurname_Label.setText("Příjmení:");
        SignUpPanel.add(SIgnUpSurname_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 220, 130, 40));

        SignUpPassword_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SignUpPassword_Label.setForeground(new java.awt.Color(0, 0, 0));
        SignUpPassword_Label.setText("Heslo:");
        SignUpPanel.add(SignUpPassword_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 270, 120, 40));

        SignUpPasswordAgain_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SignUpPasswordAgain_Label.setForeground(new java.awt.Color(0, 0, 0));
        SignUpPasswordAgain_Label.setText("Heslo znovu:");
        SignUpPanel.add(SignUpPasswordAgain_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 320, 180, 40));

        SignUpEmail_Label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SignUpEmail_Label.setForeground(new java.awt.Color(0, 0, 0));
        SignUpEmail_Label.setText("Emailová adresa:");
        SignUpPanel.add(SignUpEmail_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 370, 230, 50));
        SignUpPanel.add(SignUpLogin_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 130, 160, 30));
        SignUpPanel.add(SignUpName_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 180, 160, 30));
        SignUpPanel.add(SignUpSurname_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 230, 160, 30));
        SignUpPanel.add(SignUpEmail_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 380, 160, 30));

        SignUpConfirm_Button.setBackground(new java.awt.Color(255, 255, 255));
        SignUpConfirm_Button.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        SignUpConfirm_Button.setForeground(new java.awt.Color(0, 0, 0));
        SignUpConfirm_Button.setText("Provést registraci");
        SignUpConfirm_Button.setBorder(null);
        SignUpConfirm_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SignUpConfirm_ButtonActionPerformed(evt);
            }
        });
        SignUpPanel.add(SignUpConfirm_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 460, 240, 40));

        SignUpBackToLogin_Button.setBackground(new java.awt.Color(255, 255, 255));
        SignUpBackToLogin_Button.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        SignUpBackToLogin_Button.setForeground(new java.awt.Color(0, 0, 0));
        SignUpBackToLogin_Button.setText("Zpět");
        SignUpBackToLogin_Button.setBorder(null);
        SignUpBackToLogin_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SignUpBackToLogin_ButtonActionPerformed(evt);
            }
        });
        SignUpPanel.add(SignUpBackToLogin_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 490, 60, 30));
        SignUpPanel.add(SignUpPassword_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 280, 160, 30));
        SignUpPanel.add(SignUpPasswordAgain_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 330, 160, 30));

        Minimize_Label1.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Minimize_Label1.setForeground(new java.awt.Color(0, 0, 0));
        Minimize_Label1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Minimize_Label1.setText("_");
        Minimize_Label1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 3, 1));
        Minimize_Label1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Minimize_Label1MouseClicked(evt);
            }
        });
        SignUpPanel.add(Minimize_Label1, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 10, 26, 30));

        Close_Label1.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Close_Label1.setForeground(new java.awt.Color(0, 0, 0));
        Close_Label1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Close_Label1.setText("X");
        Close_Label1.setAlignmentY(0.0F);
        Close_Label1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Close_Label1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Close_Label1MouseClicked(evt);
            }
        });
        SignUpPanel.add(Close_Label1, new org.netbeans.lib.awtextra.AbsoluteConstraints(970, 10, 20, 40));

        Background_Label1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Background_Label1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/semestralnipraceoopiiclient/background_image.png"))); // NOI18N
        SignUpPanel.add(Background_Label1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 1040, 570));

        MainPanel.add(SignUpPanel, "SignUpPanel");

        ContactBookPanel.setOpaque(false);
        ContactBookPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ContactBookInternalFrameSharedWith_InternalFrame.setClosable(true);
        ContactBookInternalFrameSharedWith_InternalFrame.setVisible(false);
        ContactBookInternalFrameSharedWith_InternalFrame.addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
                ContactBookInternalFrameSharedWith_InternalFrameInternalFrameClosed(evt);
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });

        InternalFrameSharedWithDeleteButton_Button.setText("Odebrat");
        InternalFrameSharedWithDeleteButton_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InternalFrameSharedWithDeleteButton_ButtonActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("<html>Změna práv:<br>1.odeberte uživatele<br>2.nastavte sdílení znovu</html>");
        jLabel3.setToolTipText("Pokud si přejete odebrat práva, klikněte na tlačítko odebrat práva. \nPokud si přejete práva změnit. Odeberte současná práva a znovu je nastavné přes sdílení kontaktů.");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        InternalFrameSharedWithUserTable_Table.setAutoCreateRowSorter(true);
        InternalFrameSharedWithUserTable_Table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Id", "Jméno", "Příjmení", "Práva"
            }
        ));
        jScrollPane1.setViewportView(InternalFrameSharedWithUserTable_Table);

        javax.swing.GroupLayout ContactBookInternalFrameSharedWith_InternalFrameLayout = new javax.swing.GroupLayout(ContactBookInternalFrameSharedWith_InternalFrame.getContentPane());
        ContactBookInternalFrameSharedWith_InternalFrame.getContentPane().setLayout(ContactBookInternalFrameSharedWith_InternalFrameLayout);
        ContactBookInternalFrameSharedWith_InternalFrameLayout.setHorizontalGroup(
            ContactBookInternalFrameSharedWith_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContactBookInternalFrameSharedWith_InternalFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(InternalFrameSharedWithDeleteButton_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        ContactBookInternalFrameSharedWith_InternalFrameLayout.setVerticalGroup(
            ContactBookInternalFrameSharedWith_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContactBookInternalFrameSharedWith_InternalFrameLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ContactBookInternalFrameSharedWith_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ContactBookInternalFrameSharedWith_InternalFrameLayout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 4, Short.MAX_VALUE))
                    .addComponent(InternalFrameSharedWithDeleteButton_Button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        ContactBookPanel.add(ContactBookInternalFrameSharedWith_InternalFrame, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 150, 300, 310));

        ContactBookInternalFrame_InternalFrame.setClosable(true);
        ContactBookInternalFrame_InternalFrame.setVisible(false);
        ContactBookInternalFrame_InternalFrame.addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
                ContactBookInternalFrame_InternalFrameInternalFrameClosed(evt);
            }
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }
            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
            }
        });

        InternalFrameUserTable_Table.setAutoCreateRowSorter(true);
        InternalFrameUserTable_Table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Jméno", "Příjmení"
            }
        ));
        jScrollPane3.setViewportView(InternalFrameUserTable_Table);

        InternalFrameShareButton_Button.setText("Sdílet");
        InternalFrameShareButton_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InternalFrameShareButton_ButtonActionPerformed(evt);
            }
        });

        InternalFrameReadOnlyPrivilege_RadioButton.setText("Číst (pouze)");

        InternalFrameDeletePrivilege_RadioButton.setText("Odebrat");

        InternalFrameUpdatePrivilege_RadioButton.setText("Upravit");

        jLabel2.setText("Zvolte práva:");

        javax.swing.GroupLayout ContactBookInternalFrame_InternalFrameLayout = new javax.swing.GroupLayout(ContactBookInternalFrame_InternalFrame.getContentPane());
        ContactBookInternalFrame_InternalFrame.getContentPane().setLayout(ContactBookInternalFrame_InternalFrameLayout);
        ContactBookInternalFrame_InternalFrameLayout.setHorizontalGroup(
            ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContactBookInternalFrame_InternalFrameLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 341, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ContactBookInternalFrame_InternalFrameLayout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(InternalFrameShareButton_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(ContactBookInternalFrame_InternalFrameLayout.createSequentialGroup()
                        .addGap(79, 79, 79)
                        .addGroup(ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(InternalFrameUpdatePrivilege_RadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(InternalFrameReadOnlyPrivilege_RadioButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(InternalFrameDeletePrivilege_RadioButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        ContactBookInternalFrame_InternalFrameLayout.setVerticalGroup(
            ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ContactBookInternalFrame_InternalFrameLayout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addGroup(ContactBookInternalFrame_InternalFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 371, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(ContactBookInternalFrame_InternalFrameLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(InternalFrameReadOnlyPrivilege_RadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(InternalFrameDeletePrivilege_RadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(InternalFrameUpdatePrivilege_RadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(56, 56, 56)
                        .addComponent(InternalFrameShareButton_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(26, 26, 26))
        );

        ContactBookPanel.add(ContactBookInternalFrame_InternalFrame, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 60, 650, 440));

        jLabel12.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(0, 0, 0));
        jLabel12.setText("Jméno:");
        ContactBookPanel.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 260, 110, 40));

        jSeparator1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
        ContactBookPanel.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 78, 450, 5));

        ContactBookTitle_Label.setFont(new java.awt.Font("Tahoma", 1, 27)); // NOI18N
        ContactBookTitle_Label.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookTitle_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ContactBookTitle_Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ContactBookPanel.add(ContactBookTitle_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 30, 450, 40));

        ContactBookUserImage_Label.setBackground(new java.awt.Color(204, 204, 204));
        ContactBookUserImage_Label.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookUserImage_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ContactBookUserImage_Label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        ContactBookUserImage_Label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ContactBookUserImage_Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ContactBookUserImage_Label.setOpaque(true);
        ContactBookUserImage_Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ContactBookUserImage_LabelMouseClicked(evt);
            }
        });
        ContactBookPanel.add(ContactBookUserImage_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 20, 90, 70));

        ContactBookInsert_Button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ContactBookInsert_Button.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookInsert_Button.setText("Přidat kontakt");
        ContactBookInsert_Button.setOpaque(false);
        ContactBookInsert_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookInsert_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookInsert_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 490, -1, -1));

        jLabel14.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(0, 0, 0));
        jLabel14.setText("Příjmení:");
        ContactBookPanel.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 300, 110, 40));

        jLabel15.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(0, 0, 0));
        jLabel15.setText("Telefonní číslo:");
        ContactBookPanel.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 350, 140, 20));

        jLabel16.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(0, 0, 0));
        jLabel16.setText("Emailová adresa:");
        ContactBookPanel.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 370, 160, 60));
        ContactBookPanel.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jLabel18.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(0, 0, 0));
        jLabel18.setText("Skupina:");
        ContactBookPanel.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 420, 90, 40));

        ContactBookDelete_Button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ContactBookDelete_Button.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookDelete_Button.setText("Odebrat kontakt");
        ContactBookDelete_Button.setOpaque(false);
        ContactBookDelete_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookDelete_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookDelete_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 490, -1, -1));

        ContactBookReload_Label.setBackground(new java.awt.Color(204, 204, 204));
        ContactBookReload_Label.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        ContactBookReload_Label.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookReload_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ContactBookReload_Label.setText("⟳");
        ContactBookReload_Label.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 5, 1));
        ContactBookReload_Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ContactBookReload_Label.setOpaque(true);
        ContactBookReload_Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ContactBookReload_LabelMouseClicked(evt);
            }
        });
        ContactBookPanel.add(ContactBookReload_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(960, 100, 20, 20));

        ContactBookUpdate_Button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ContactBookUpdate_Button.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookUpdate_Button.setText("Upravit kontakt");
        ContactBookUpdate_Button.setOpaque(false);
        ContactBookUpdate_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookUpdate_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookUpdate_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 490, -1, -1));

        ContactBookShareButton_Button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ContactBookShareButton_Button.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookShareButton_Button.setText("Sdílet kontakt(y)");
        ContactBookShareButton_Button.setOpaque(false);
        ContactBookShareButton_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookShareButton_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookShareButton_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 470, -1, -1));

        ContactBookName_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookName_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookName_TextField.setOpaque(false);
        ContactBookPanel.add(ContactBookName_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 270, 100, -1));

        ContactBookSurname_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookSurname_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookSurname_TextField.setOpaque(false);
        ContactBookPanel.add(ContactBookSurname_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 310, 100, -1));

        ContactBookPhone_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookPhone_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookPhone_TextField.setOpaque(false);
        ContactBookPanel.add(ContactBookPhone_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 350, 100, -1));

        ContactBookEmail_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookEmail_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookEmail_TextField.setOpaque(false);
        ContactBookPanel.add(ContactBookEmail_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 390, 100, -1));

        ContactBookGroup_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookGroup_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookGroup_TextField.setOpaque(false);
        ContactBookPanel.add(ContactBookGroup_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 430, 100, -1));

        jScrollPane2.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane2.setForeground(new java.awt.Color(0, 0, 0));

        ContactBookTable_Table.setAutoCreateRowSorter(true);
        ContactBookTable_Table.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookTable_Table.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookTable_Table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Jméno", "Příjmení", "Tel. číslo", "Email", "Skupina", "Práva"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ContactBookTable_Table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ContactBookTable_TableMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(ContactBookTable_Table);
        if (ContactBookTable_Table.getColumnModel().getColumnCount() > 0) {
            ContactBookTable_Table.getColumnModel().getColumn(0).setHeaderValue("Jméno");
            ContactBookTable_Table.getColumnModel().getColumn(1).setHeaderValue("Příjmení");
            ContactBookTable_Table.getColumnModel().getColumn(2).setHeaderValue("Tel. číslo");
            ContactBookTable_Table.getColumnModel().getColumn(3).setHeaderValue("Email");
            ContactBookTable_Table.getColumnModel().getColumn(4).setHeaderValue("Skupina");
            ContactBookTable_Table.getColumnModel().getColumn(5).setHeaderValue("Práva");
        }

        ContactBookPanel.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 120, 600, 340));

        ContactBookFilter_TextField.setBackground(new java.awt.Color(255, 255, 255));
        ContactBookFilter_TextField.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookFilter_TextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        ContactBookFilter_TextField.setActionCommand("<Not Set>");
        ContactBookFilter_TextField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 7, 2));
        ContactBookFilter_TextField.setMargin(new java.awt.Insets(1, 5, 3, 2));
        ContactBookFilter_TextField.setMinimumSize(new java.awt.Dimension(10, 20));
        ContactBookFilter_TextField.setOpaque(false);
        ContactBookFilter_TextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ContactBookFilter_TextFieldKeyReleased(evt);
            }
        });
        ContactBookPanel.add(ContactBookFilter_TextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 100, 520, 30));

        ContactBookLogout_Button.setText("Odhlásit");
        ContactBookLogout_Button.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        ContactBookLogout_Button.setOpaque(false);
        ContactBookLogout_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookLogout_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookLogout_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 10, 70, 30));

        ContactBookSharedWithButton_Button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ContactBookSharedWithButton_Button.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookSharedWithButton_Button.setText("Sdíleno s");
        ContactBookSharedWithButton_Button.setOpaque(false);
        ContactBookSharedWithButton_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContactBookSharedWithButton_ButtonActionPerformed(evt);
            }
        });
        ContactBookPanel.add(ContactBookSharedWithButton_Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(821, 520, 120, 30));

        ContactBookContactImage_Label.setBackground(new java.awt.Color(204, 204, 204));
        ContactBookContactImage_Label.setForeground(new java.awt.Color(0, 0, 0));
        ContactBookContactImage_Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ContactBookContactImage_Label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        ContactBookContactImage_Label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ContactBookContactImage_Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        ContactBookContactImage_Label.setOpaque(true);
        ContactBookContactImage_Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ContactBookContactImage_LabelMouseClicked(evt);
            }
        });
        ContactBookPanel.add(ContactBookContactImage_Label, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 70, 200, 180));

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 0));
        jLabel1.setText(" Vyhledat:");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel1.setAlignmentY(0.0F);
        jLabel1.setBorder(javax.swing.BorderFactory.createEtchedBorder(java.awt.Color.lightGray, java.awt.Color.lightGray));
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        jLabel1.setMaximumSize(new java.awt.Dimension(70, 19));
        jLabel1.setMinimumSize(new java.awt.Dimension(70, 19));
        jLabel1.setOpaque(true);
        jLabel1.setPreferredSize(new java.awt.Dimension(70, 19));
        jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        ContactBookPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 100, 600, 360));

        Minimize_Label2.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Minimize_Label2.setForeground(new java.awt.Color(0, 0, 0));
        Minimize_Label2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Minimize_Label2.setText("_");
        Minimize_Label2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 3, 1));
        Minimize_Label2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Minimize_Label2MouseClicked(evt);
            }
        });
        ContactBookPanel.add(Minimize_Label2, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 10, 26, 30));

        Close_Label2.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        Close_Label2.setForeground(new java.awt.Color(0, 0, 0));
        Close_Label2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Close_Label2.setText("X");
        Close_Label2.setAlignmentY(0.0F);
        Close_Label2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Close_Label2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Close_Label2MouseClicked(evt);
            }
        });
        ContactBookPanel.add(Close_Label2, new org.netbeans.lib.awtextra.AbsoluteConstraints(970, 10, 20, 40));

        Background_Label2.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        Background_Label2.setForeground(new java.awt.Color(0, 0, 0));
        Background_Label2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Background_Label2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/semestralnipraceoopiiclient/background_image.png"))); // NOI18N
        Background_Label2.setText("jLabel21");
        ContactBookPanel.add(Background_Label2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 1040, 570));

        MainPanel.add(ContactBookPanel, "ContactBookPanel");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addComponent(MainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 1063, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(MainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 572, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Checks user privilege to share(only owner can). Opens new internal frame
     * with table of users to share with.
     *
     * @param evt
     */
    private void ContactBookShareButton_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookShareButton_ButtonActionPerformed
        if (checkEmptySelection()) {
            if (checkPrivileges("owner")) {
                new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        int indeces[] = ContactBookTable_Table.getSelectedRows();
                        defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
                        sharedContactList = new ArrayList<Contact>();
                        for (int i = 0; i < indeces.length; i++) {
                            String firstName = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(indeces[i]), 0);
                            String lastName = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(indeces[i]), 1);
                            System.out.println(firstName);
                            System.out.println(lastName);
                            int id = 0;
                            for (Contact c : contactList) {
                                if (c.getFirstName().equals(firstName) && c.getSurname().equals(lastName)) {
                                    id = c.getId();
                                    System.out.println(id);
                                    Contact contact = new Contact(firstName, lastName, id);
                                    sharedContactList.add(contact);
                                }
                            }
                        }
                        socket.write("SHARE");
                        userList = (ArrayList<User>) socket.readObj();
                        displayTableContentInternalFrame();
                        socket.writeObj(sharedContactList);
                        return null;
                    }
                }.execute();
                ContactBookInternalFrame_InternalFrame.setVisible(true);
            }
        }
    }//GEN-LAST:event_ContactBookShareButton_ButtonActionPerformed
    /**
     * Checks user privilege to update (U). Send request to update selected
     * contact to server side.
     *
     * @param evt
     */
    private void ContactBookUpdate_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookUpdate_ButtonActionPerformed
        if (checkEmptySelection()) {
            if (checkPrivileges("U")) {
                new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        String result;
                        int i = ContactBookTable_Table.getSelectedRow();
                        defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
                        for (int k = 0; k < defaultModel.getRowCount(); k++) {
                            if (defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 0).toString().equals(ContactBookName_TextField.getText()) && defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 1).toString().equals(ContactBookSurname_TextField.getText()) && i != k) {
                                System.out.println(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 0).toString() + defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 1).toString());
                                return result = "ILLEGALUPDATE";
                            }
                        }
                        socket.write("UPDATE");
                        socket.write("update contact set FirstName = '" + ContactBookName_TextField.getText() + "', LastName = '" + ContactBookSurname_TextField.getText() + "',"
                                + " PhoneNumber = '" + ContactBookPhone_TextField.getText() + "', EmailAddress = '" + ContactBookEmail_TextField.getText() + "', GroupName='" + ContactBookGroup_TextField.getText() + ""
                                + "'where ID = " + currentContactId + ";");
                        try {
                            result = socket.read();
                        } catch (IOException ex) {
                            Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                            result = "ERROR";
                        }
                        return result;
                    }

                    @Override
                    protected void done() {
                        try {
                            String result = (String) get();
                            clearContactBookTextField();
                            if (result.equals("ILLEGALUPDATE")) {
                                JOptionPane.showMessageDialog(rootPane, "Kontakt s daným jménem a příjmením již exisuje!");
                            }
                            if (result.equals("OK")) {
                                JOptionPane.showMessageDialog(rootPane, "Úprava proběhla v pořádku");
                                displayTable();
                            }
                            if (!result.equals("ILLEGALUPDATE") && !result.equals("OK")) {
                                JOptionPane.showMessageDialog(rootPane, "Chyba!");
                            }

                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }.execute();
            }
        }
    }//GEN-LAST:event_ContactBookUpdate_ButtonActionPerformed

    /**
     * Checks user privileges to delete(D). Send request to delete selected
     * contact to server side.
     *
     * @param evt
     */
    private void ContactBookDelete_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookDelete_ButtonActionPerformed
        if (checkEmptySelection()) {
            if (checkPrivileges("D")) {
                String[] options = new String[]{"Ano", "Ne"};
                int response = JOptionPane.showOptionDialog(rootPane, "Opravdu chcete smazat kontakt?", "", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                if (response == 0) {
                    new SwingWorker<Object, Object>() {
                        @Override
                        protected Object doInBackground() throws Exception {
                            String result;
                            socket.write("DELETE");
                            socket.write(String.valueOf(currentContactId));
                            try {
                                result = socket.read();
                                return result;
                            } catch (IOException ex) {
                                Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                                result = "ERROR";
                                return result;
                            }
                        }

                        @Override
                        protected void done() {
                            try {
                                String result = (String) get();
                                clearContactBookTextField();
                                if (result.equals("OK")) {
                                    JOptionPane.showMessageDialog(rootPane, "Kontakt smazán!");
                                    displayTable();
                                } else {
                                    JOptionPane.showMessageDialog(rootPane, "Chyba!");
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }.execute();
                }
            }
        }
    }//GEN-LAST:event_ContactBookDelete_ButtonActionPerformed
    /**
     * Send request to insert specified contact to server side.
     *
     * @param evt
     */
    private void ContactBookInsert_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookInsert_ButtonActionPerformed
        if (ContactBookName_TextField.getText().trim().equals("") || ContactBookSurname_TextField.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(rootPane, "Vyplňte alespoň jméno a příjmení pro přidání nového kontaktu.");
            return;
        }
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                String result;
                int i = ContactBookTable_Table.getSelectedRow();
                defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
                for (int k = 0; k < defaultModel.getRowCount(); k++) {
                    if (defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 0).toString().equals(ContactBookName_TextField.getText()) && defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 1).toString().equals(ContactBookSurname_TextField.getText())) {
                        System.out.println(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 0).toString() + defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(k), 1).toString());
                        return result = "ILLEGALINSERT";
                    }
                }
                socket.write("INSERT");
                String querry = "insert into contact(FirstName, LastName, PhoneNumber, EmailAddress, GroupName) "
                        + "values ('" + ContactBookName_TextField.getText() + "', '" + ContactBookSurname_TextField.getText() + "',"
                        + " '" + ContactBookPhone_TextField.getText() + "', '" + ContactBookEmail_TextField.getText() + "', '" + ContactBookGroup_TextField.getText() + "')";
                socket.write(querry);
                result = socket.read();
                return result;
            }

            @Override
            protected void done() {
                try {
                    String result = (String) get();
                    clearContactBookTextField();
                    if (result.equals("ILLEGALINSERT")) {
                        JOptionPane.showMessageDialog(rootPane, "Kontakt s daným jménem a příjmením již exisuje!");
                    }
                    if (result.equals("OK")) {
                        JOptionPane.showMessageDialog(rootPane, "Kontakt přidán!");
                        displayTable();
                    }
                    if (!result.equals("ILLEGALINSERT") && !result.equals("OK")) {
                        JOptionPane.showMessageDialog(rootPane, "Chyba!");
                    }

                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(rootPane, "Chyba!");
                }
            }
        }.execute();
    }//GEN-LAST:event_ContactBookInsert_ButtonActionPerformed

    /**
     * Changes card in cardlayout.
     *
     * @param evt
     */
    private void SignUpBackToLogin_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SignUpBackToLogin_ButtonActionPerformed
        card.show(MainPanel, "LoginPanel");
    }//GEN-LAST:event_SignUpBackToLogin_ButtonActionPerformed
    /**
     * Changes card in cardlayout.
     *
     * @param evt
     */
    private void LoginToSignUp_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginToSignUp_ButtonActionPerformed
        card.show(MainPanel, "SignUpPanel");
    }//GEN-LAST:event_LoginToSignUp_ButtonActionPerformed
    /**
     * Authentication of user. Sends given login credentials to server to be
     * validated.
     *
     * @param evt
     */
    private void LoginConfirm_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginConfirm_ButtonActionPerformed
        if (LoginLogin_TextField.getText().equals("") || (LoginPassword_PasswordField.getPassword().length == 0)) {
            JOptionPane.showMessageDialog(rootPane, "Neúplné údaje");
            return;
        }
        if (!connected) {
            JOptionPane.showMessageDialog(rootPane, "Nepodařilo se navázat spojení se serverem.\nZkuste to, prosím, později.");
            connect();
        } else {
            new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    socket.write("LOGIN");
                    socket.write(LoginLogin_TextField.getText());
                    loginName = LoginLogin_TextField.getText();
                    socket.write(String.valueOf(hashPassword(String.valueOf(LoginPassword_PasswordField.getPassword()))));
                    if (socket.read().equals("OK")) {
                        for (User u : userList) {
                            if ((u.getLoginName().equals(loginName) || u.getLoginName().toLowerCase().equals(loginName)) && u.getImage() != null) {
                                ImageIcon image = new ImageIcon(new ImageIcon(u.getImage()).getImage().getScaledInstance(90, 70, Image.SCALE_SMOOTH));
                                ContactBookUserImage_Label.setIcon(image);
                                return 1;
                            }
                        }
                        ContactBookUserImage_Label.setText("Přidejte fotografii");
                        return 1;
                    }
                    return 2;
                }

                @Override
                protected void done() {
                    try {
                        int i = (Integer) get();
                        if (i == 1) {
                            ContactBookSharedWithButton_Button.setVisible(false);
                            JOptionPane.showMessageDialog(rootPane, "Přihlášení proběhlo úspěšně");
                            card.show(MainPanel, "ContactBookPanel");
                            ContactBookTitle_Label.setText("ContactBook uživatele " + loginName.toUpperCase());
                            displayTableContentInternalFrame();
                            displayTable();
                        } else {
                            JOptionPane.showMessageDialog(rootPane, "Nesprávné jméno či heslo!");
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(rootPane, "Chyba na síti! Zkuste se, prosím, přihlásit později.");
                        connect();
                    }

                }
            }
                    .execute();
        }
    }//GEN-LAST:event_LoginConfirm_ButtonActionPerformed

    /**
     * Allows user to show/hide his password in passwordfield.
     *
     * @param evt
     */
    private void LoginShowPassword_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginShowPassword_CheckBoxActionPerformed
        if (LoginShowPassword_CheckBox.isSelected()) {
            LoginPassword_PasswordField.setEchoChar((char) 0);
        } else {
            LoginPassword_PasswordField.setEchoChar('*');
        }
    }//GEN-LAST:event_LoginShowPassword_CheckBoxActionPerformed
    /**
     * Closes client side socket and signals to server side to close the
     * dedicated ServerThread as well. Closes the application.
     *
     * @param evt
     */
    private void Close_LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Close_LabelMouseClicked
        if (connected) {
            socket.write("END");
            socket.close();
        }
        System.exit(0);
    }//GEN-LAST:event_Close_LabelMouseClicked
    /**
     * Minimizes the frame
     *
     * @param evt
     */
    private void Minimize_LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Minimize_LabelMouseClicked
        this.setState(Frame.ICONIFIED);
    }//GEN-LAST:event_Minimize_LabelMouseClicked
    /**
     * Minimizes the frame
     *
     * @param evt
     */
    private void Minimize_Label1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Minimize_Label1MouseClicked
        this.setState(Frame.ICONIFIED);
    }//GEN-LAST:event_Minimize_Label1MouseClicked
    /**
     * Closes client side socket and signals to server side to close the
     * dedicated ServerThread as well. Closes the application.
     *
     * @param evt
     */
    private void Close_Label1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Close_Label1MouseClicked
        if (connected) {
            socket.write("END");
            socket.close();
        }
        System.exit(0);
    }//GEN-LAST:event_Close_Label1MouseClicked
    /**
     * Gets current mouse position.
     *
     * @param evt
     */
    private void MainPanelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MainPanelMousePressed
        mousePositionX = evt.getX();
        mousePositionY = evt.getY();
    }//GEN-LAST:event_MainPanelMousePressed
    /**
     * Registers mouse current movement while being pressed. Moves frame
     * accordingly.
     *
     * @param evt
     */
    private void MainPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MainPanelMouseDragged
        int x = evt.getXOnScreen();
        int y = evt.getYOnScreen();
        this.setLocation(x - mousePositionX, y - mousePositionY);
    }//GEN-LAST:event_MainPanelMouseDragged
    /**
     * Validates user input. Consequently sends given data to server to be
     * stored to database.
     *
     * @param evt
     */
    private void SignUpConfirm_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SignUpConfirm_ButtonActionPerformed
        boolean signUpFormFilled = true;
        if (SignUpLogin_TextField.getText().equals("") || SignUpName_TextField.getText().equals("") || SignUpSurname_TextField.getText().equals("") || SignUpEmail_TextField.getText().equals("") || SignUpPassword_TextField.getText().equals("")) {
            signUpFormFilled = false;
            JOptionPane.showMessageDialog(rootPane, "Vyplňte prosím všechna pole");
            return;
        }
        if (!String.valueOf(SignUpPassword_TextField.getPassword()).equals(String.valueOf(SignUpPasswordAgain_TextField.getPassword()))) {
            signUpFormFilled = false;
            JOptionPane.showMessageDialog(rootPane, "Zadaná hesla se neshodují");
            return;
        }
        if (signUpFormFilled) {
            new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    socket.write("SIGNUP");
                    socket.write(SignUpLogin_TextField.getText());
                    if (socket.read().equals("OK")) {
                        String querry = "insert into user (LoginName, FirstName, LastName, EmailAddress, Password)"
                                + "values ('" + SignUpLogin_TextField.getText() + "','" + SignUpName_TextField.getText() + "','" + SignUpSurname_TextField.getText() + "','"
                                + SignUpEmail_TextField.getText() + "','" + String.valueOf(hashPassword(String.valueOf(SignUpPasswordAgain_TextField.getPassword()))) + "');";
                        System.out.println(querry);
                        socket.write(querry);
                        if (socket.read().equals("OK")) {
                            return 1;
                        } else {
                            return 2;
                        }
                    } else {
                        return 3;
                    }
                }

                @Override
                protected void done() {
                    try {
                        int result = (Integer) get();
                        if (result == 1) {
                            JOptionPane.showMessageDialog(rootPane, "Registrace proběhla úspěšně\nNyní se můžete přihlásit");
                            card.show(MainPanel, "LoginPanel");
                        }
                        if (result == 2) {
                            JOptionPane.showMessageDialog(rootPane, "Něco se porouchalo, zkuste se registrovat později");
                        }
                        if (result == 3) {
                            JOptionPane.showMessageDialog(rootPane, "Zadaný Login již existuje");
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(MainWindow_JFrame.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.execute();
        }
    }//GEN-LAST:event_SignUpConfirm_ButtonActionPerformed

    /**
     * Minimizes the frame
     *
     * @param evt
     */
    private void Minimize_Label2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Minimize_Label2MouseClicked
        this.setState(Frame.ICONIFIED);
    }//GEN-LAST:event_Minimize_Label2MouseClicked
    /**
     * Closes client side socket and signals to server side to close the
     * dedicated ServerThread as well. Closes the application.
     *
     * @param evt
     */
    private void Close_Label2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_Close_Label2MouseClicked
        if (connected) {
            socket.write("END");
        }
        socket.close();
        System.exit(0);
    }//GEN-LAST:event_Close_Label2MouseClicked
    /**
     * Displays selected row, hence selected contact, details to the the
     * appropriate textfield. Allows user to execute delete, update and share
     * operation as well as change contact´s image.
     *
     * @param evt
     */
    private void ContactBookTable_TableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ContactBookTable_TableMouseClicked
        int i = ContactBookTable_Table.getSelectedRow();
        defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
        String s = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 5);
        if (s.contains("shared")) {
            ContactBookSharedWithButton_Button.setVisible(true);
        } else {
            ContactBookSharedWithButton_Button.setVisible(false);
        }
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                for (Contact c : contactList) {
                    if (c.getFirstName().equals(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 0).toString()) && c.getSurname().equals(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 1).toString())) {
                        currentContactId = c.getId();
                        System.out.println(currentContactId);
                        if (c.getImage() != null) {
                            ImageIcon image = new ImageIcon(new ImageIcon(c.getImage()).getImage().getScaledInstance(200, 180, Image.SCALE_SMOOTH));
                            ContactBookContactImage_Label.setText("");
                            return image;
                        } else {
                            ContactBookContactImage_Label.setText("Přidejte fotografii");
                            return null;
                        }
                    }
                }
                return null;
            }

            protected void done() {
                try {
                    ContactBookName_TextField.setText(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 0).toString());
                    ContactBookSurname_TextField.setText(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 1).toString());
                    ContactBookPhone_TextField.setText(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 2).toString());
                    ContactBookEmail_TextField.setText(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 3).toString());
                    ContactBookGroup_TextField.setText(defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(i), 4).toString());
                    ImageIcon image = (ImageIcon) get();
                    ContactBookContactImage_Label.setIcon(image);
                } catch (InterruptedException | ExecutionException ex) {
                    ContactBookContactImage_Label.setText("Fotografii se nepodařilo nahrát");
                    Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.execute();
    }//GEN-LAST:event_ContactBookTable_TableMouseClicked
    /**
     * Allows dynamic search within the contact table.
     *
     * @param evt
     */
    private void ContactBookFilter_TextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ContactBookFilter_TextFieldKeyReleased
        TableRowSorter<DefaultTableModel> tr = new TableRowSorter<DefaultTableModel>(defaultModel);
        ContactBookTable_Table.setRowSorter(tr);
        tr.setRowFilter(RowFilter.regexFilter(ContactBookFilter_TextField.getText()));
    }//GEN-LAST:event_ContactBookFilter_TextFieldKeyReleased
    /**
     * Sends list of users to share given contact(s) with. Tied together with
     * ContactBookShareButton_ButtonActionPerformed().
     *
     * @param evt
     */
    private void InternalFrameShareButton_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InternalFrameShareButton_ButtonActionPerformed
        int indices[] = InternalFrameUserTable_Table.getSelectedRows();
        if (indices.length == 0) {
            JOptionPane.showMessageDialog(rootPane, "Nevybráni žádní uživatelé");
            return;
        } else {
            if (!InternalFrameReadOnlyPrivilege_RadioButton.isSelected() && !InternalFrameUpdatePrivilege_RadioButton.isSelected() && !InternalFrameDeletePrivilege_RadioButton.isSelected()) {
                JOptionPane.showMessageDialog(rootPane, "Vyberte práva!");
                return;
            } else {
                new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        String privilege = chosenPrivilege();
                        defaultModelShare = (DefaultTableModel) InternalFrameUserTable_Table.getModel();
                        socket.write("SHARE2");
                        socket.write(String.valueOf(indices.length));
                        for (int i = 0; i < indices.length; i++) {
                            socket.write((String.valueOf(defaultModelShare.getValueAt(InternalFrameUserTable_Table.convertRowIndexToModel(indices[i]), 0))));
                        }
                        socket.write(privilege);
                        try {
                            if (socket.read().equals("OK")) {
                                return "OK";
                            } else {
                                return "ERROR";
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                            return "ERROR";
                        }
                    }

                    protected void done() {
                        try {
                            String result = (String) get();
                            if (result.equals("OK")) {
                                JOptionPane.showMessageDialog(rootPane, "Sdílení úspěšné!");
                                ContactBookInternalFrame_InternalFrame.setVisible(false);
                                displayTable();
                            } else {
                                JOptionPane.showMessageDialog(rootPane, "S některými z vybranných kontaktů již sdílíte!");
                                ContactBookInternalFrame_InternalFrame.setVisible(false);
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }.execute();
            }
        }
    }//GEN-LAST:event_InternalFrameShareButton_ButtonActionPerformed
    /**
     * Sends "abort" signal to server side meaning the user closed given
     * internal frame and will not finish the "share" operation.
     *
     * @param evt
     */
    private void ContactBookInternalFrame_InternalFrameInternalFrameClosed(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_ContactBookInternalFrame_InternalFrameInternalFrameClosed
        socket.write("ABORT");
    }//GEN-LAST:event_ContactBookInternalFrame_InternalFrameInternalFrameClosed
    /**
     * Reloads content of contact table.
     *
     * @param evt
     */
    private void ContactBookReload_LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ContactBookReload_LabelMouseClicked
        displayTable();
        JOptionPane.showMessageDialog(rootPane, "Kontakty aktualizovány!");
    }//GEN-LAST:event_ContactBookReload_LabelMouseClicked
    /**
     * Logs out the user and reloads the list of users with data send from
     * server.
     *
     * @param evt
     */
    private void ContactBookLogout_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookLogout_ButtonActionPerformed
        new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                socket.write("GETUSERS");
                userList = (ArrayList<User>) socket.readObj();
                return null;
            }
        }.execute();
        LoginLogin_TextField.setText("");
        clearContactBookTextField();
        LoginPassword_PasswordField.setText("");
        ContactBookUserImage_Label.setText("");
        ContactBookUserImage_Label.setIcon(null);
        ContactBookContactImage_Label.setIcon(null);
        ContactBookUserImage_Label.revalidate();
        card.show(MainPanel, "LoginPanel");
    }//GEN-LAST:event_ContactBookLogout_ButtonActionPerformed
    /**
     * Opens filechooser and allows user to select profile image. Then sends the
     * image as byte array to server to be stored to database.
     *
     * @param evt
     */
    private void ContactBookUserImage_LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ContactBookUserImage_LabelMouseClicked
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setPreferredSize(new Dimension(600, 400));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);
        int choice = fileChooser.showSaveDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            new SwingWorker<Object, Object>() {
                ImageIcon image;

                @Override
                protected Object doInBackground() throws Exception {
                    File selectedFile = fileChooser.getSelectedFile();
                    String path = selectedFile.getAbsolutePath();
                    String fileName = selectedFile.getName();
                    String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1, selectedFile.getName().length());
                    System.out.println(">> fileExtension " + fileExtension);
                    try {
                        BufferedImage img = ImageIO.read(new File(path));
                        image = new ImageIcon(new ImageIcon(img).getImage().getScaledInstance(90, 70, Image.SCALE_SMOOTH));
                        socket.write("USERIMAGE");
                        socket.writeImage(img, fileExtension);
                        String result = socket.read();
                        if (result.equals("OK")) {
                            return "OK";
                        } else {
                            return "ERROR";
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                        return "ERROR";
                    }
                }

                protected void done() {
                    try {
                        String result = (String) get();
                        if (result.equals("OK")) {
                            ContactBookUserImage_Label.setText("");
                            ContactBookUserImage_Label.setIcon(image);
                            JOptionPane.showMessageDialog(rootPane, "Obrázek přidán!");
                        } else {
                            JOptionPane.showMessageDialog(rootPane, "Chyba");
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }.execute();
        }
    }//GEN-LAST:event_ContactBookUserImage_LabelMouseClicked

    /**
     * Opens filechooser and allows user to select image for given contact. Then
     * sends the image as byte aray to server to be stored to database.
     *
     * @param evt
     */
    private void ContactBookContactImage_LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ContactBookContactImage_LabelMouseClicked
        if (checkEmptySelection()) {
            if (checkPrivileges("U")) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setPreferredSize(new Dimension(600, 400));
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Images Files", "jpg", "gif", "jpeg");
                fileChooser.addChoosableFileFilter(filter);
                fileChooser.setFileFilter(filter);
                int choice = fileChooser.showSaveDialog(null);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    new SwingWorker<Object, Object>() {
                        ImageIcon image;

                        @Override
                        protected Object doInBackground() throws Exception {
                            File selectedFile = fileChooser.getSelectedFile();
                            String path = selectedFile.getAbsolutePath();
                            String fileName = selectedFile.getName();
                            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1, selectedFile.getName().length());
                            System.out.println(">> fileExtension " + fileExtension);
                            try {
                                BufferedImage img = ImageIO.read(new File(path));
                                image = new ImageIcon(new ImageIcon(img).getImage().getScaledInstance(ContactBookContactImage_Label.getWidth(), ContactBookContactImage_Label.getHeight(), Image.SCALE_SMOOTH));
                                socket.write("CONTACTIMAGE");
                                socket.write((String.valueOf(currentContactId)));
                                socket.writeImage(img, fileExtension);
                                String result = socket.read();
                                if (result.equals("OK")) {
                                    return "OK";
                                } else {
                                    return "ERROR";
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                                return "ERROR";
                            }
                        }

                        @Override
                        protected void done() {
                            try {
                                String result = (String) get();
                                if (result.equals("OK")) {
                                    ContactBookContactImage_Label.setText("");
                                    ContactBookContactImage_Label.setIcon(image);
                                    displayTable();
                                    JOptionPane.showMessageDialog(rootPane, "Obrázek přidán!");

                                } else {
                                    JOptionPane.showMessageDialog(rootPane, "Chyba");
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                JOptionPane.showMessageDialog(rootPane, "Chyba");
                                Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }.execute();
                }
            }
        }
    }//GEN-LAST:event_ContactBookContactImage_LabelMouseClicked
    /**
     * Opens new internal frame with table of users which shares the selected
     * contact.
     *
     * @param evt
     */
    private void ContactBookSharedWithButton_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContactBookSharedWithButton_ButtonActionPerformed
        if (checkEmptySelection()) {
            if (ContactBookTable_Table.getSelectedRowCount() == 1) {
                new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        int index = ContactBookTable_Table.getSelectedRow();
                        defaultModel = (DefaultTableModel) ContactBookTable_Table.getModel();
                        String firstName = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(index), 0);
                        String lastName = (String) defaultModel.getValueAt(ContactBookTable_Table.convertRowIndexToModel(index), 1);
                        System.out.println(firstName);
                        System.out.println(lastName);
                        int id = 0;
                        for (Contact c : contactList) {
                            if (c.getFirstName().equals(firstName) && c.getSurname().equals(lastName)) {
                                id = c.getId();
                                System.out.println(id);
                                break;
                            }
                        }
                        socket.write("SHAREDWITH");
                        socket.write(String.valueOf(id));
                        sharedWithUserList = (ArrayList<User>) socket.readObj();
                        return null;
                    }

                    @Override
                    protected void done() {
                        displayTableContentInternalFrameSharedWith();
                        ContactBookInternalFrameSharedWith_InternalFrame.setVisible(true);
                    }
                }.execute();
            } else {
                JOptionPane.showMessageDialog(rootPane, "Vyberte pouze jeden kontakt!");
            }
        }
    }//GEN-LAST:event_ContactBookSharedWithButton_ButtonActionPerformed
    /**
     * Revokes privileges which were granted to the selected user or group of
     * users. Tied together with
     * ContactBookSharedWithButton_ButtonActionPerformed().
     *
     * @param evt
     */
    private void InternalFrameSharedWithDeleteButton_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InternalFrameSharedWithDeleteButton_ButtonActionPerformed
        if (InternalFrameSharedWithUserTable_Table.getSelectedRowCount() != 0) {
            new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    socket.write("DELETEPRIVILEGE");
                    defaultModelSharedWith = (DefaultTableModel) InternalFrameSharedWithUserTable_Table.getModel();
                    int[] indices = InternalFrameSharedWithUserTable_Table.getSelectedRows();
                    socket.write(String.valueOf(indices.length));
                    for (int i = 0; i < indices.length; i++) {
                        System.out.println((String.valueOf(defaultModelSharedWith.getValueAt(InternalFrameSharedWithUserTable_Table.convertRowIndexToModel(indices[i]), 0))));
                        socket.write((String.valueOf(defaultModelSharedWith.getValueAt(InternalFrameSharedWithUserTable_Table.convertRowIndexToModel(indices[i]), 0))));
                    }
                    String result = socket.read();
                    return result;
                }

                @Override
                protected void done() {
                    try {
                        String result = (String) get();
                        if (result.equals("OK")) {
                            JOptionPane.showMessageDialog(rootPane, "Práva odebrána!");
                            ContactBookInternalFrameSharedWith_InternalFrame.setVisible(false);
                            displayTable();
                        } else {
                            JOptionPane.showMessageDialog(rootPane, "Chyba!");
                            ContactBookInternalFrameSharedWith_InternalFrame.setVisible(false);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        JOptionPane.showMessageDialog(rootPane, "Chyba!");
                        ContactBookInternalFrameSharedWith_InternalFrame.setVisible(false);
                        Logger.getLogger(MainWindow_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.execute();
        } else {
            JOptionPane.showMessageDialog(rootPane, "Nevybráni žádní uživatelé!");
        }
    }//GEN-LAST:event_InternalFrameSharedWithDeleteButton_ButtonActionPerformed
    /**
     * Sends "abort" signal to server side meaning the user closed given
     * internal frame and will not finish the "share with" operation.
     *
     * @param evt
     */
    private void ContactBookInternalFrameSharedWith_InternalFrameInternalFrameClosed(javax.swing.event.InternalFrameEvent evt) {//GEN-FIRST:event_ContactBookInternalFrameSharedWith_InternalFrameInternalFrameClosed
        socket.write("ABORT");
    }//GEN-LAST:event_ContactBookInternalFrameSharedWith_InternalFrameInternalFrameClosed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Background_Label;
    private javax.swing.JLabel Background_Label1;
    private javax.swing.JLabel Background_Label2;
    private javax.swing.JLabel Close_Label;
    private javax.swing.JLabel Close_Label1;
    private javax.swing.JLabel Close_Label2;
    private javax.swing.JLabel ContactBookContactImage_Label;
    private javax.swing.JButton ContactBookDelete_Button;
    private javax.swing.JTextField ContactBookEmail_TextField;
    private javax.swing.JTextField ContactBookFilter_TextField;
    private javax.swing.JTextField ContactBookGroup_TextField;
    private javax.swing.JButton ContactBookInsert_Button;
    private javax.swing.JInternalFrame ContactBookInternalFrameSharedWith_InternalFrame;
    private javax.swing.JInternalFrame ContactBookInternalFrame_InternalFrame;
    private javax.swing.JButton ContactBookLogout_Button;
    private javax.swing.JTextField ContactBookName_TextField;
    private javax.swing.JPanel ContactBookPanel;
    private javax.swing.JTextField ContactBookPhone_TextField;
    private javax.swing.JLabel ContactBookReload_Label;
    private javax.swing.JButton ContactBookShareButton_Button;
    private javax.swing.JButton ContactBookSharedWithButton_Button;
    private javax.swing.JTextField ContactBookSurname_TextField;
    private javax.swing.JTable ContactBookTable_Table;
    private javax.swing.JLabel ContactBookTitle_Label;
    private javax.swing.JButton ContactBookUpdate_Button;
    private javax.swing.JLabel ContactBookUserImage_Label;
    private javax.swing.JRadioButton InternalFrameDeletePrivilege_RadioButton;
    private javax.swing.JRadioButton InternalFrameReadOnlyPrivilege_RadioButton;
    private javax.swing.JButton InternalFrameShareButton_Button;
    private javax.swing.JButton InternalFrameSharedWithDeleteButton_Button;
    private javax.swing.JTable InternalFrameSharedWithUserTable_Table;
    private javax.swing.JRadioButton InternalFrameUpdatePrivilege_RadioButton;
    private javax.swing.JTable InternalFrameUserTable_Table;
    private javax.swing.JButton LoginConfirm_Button;
    private javax.swing.JLabel LoginLogin_Label;
    private javax.swing.JTextField LoginLogin_TextField;
    private javax.swing.JPanel LoginPanel;
    private javax.swing.JLabel LoginPassword_Label;
    private javax.swing.JPasswordField LoginPassword_PasswordField;
    private javax.swing.JCheckBox LoginShowPassword_CheckBox;
    private javax.swing.JLabel LoginTitle_Label;
    private javax.swing.JButton LoginToSignUp_Button;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JLabel Minimize_Label;
    private javax.swing.JLabel Minimize_Label1;
    private javax.swing.JLabel Minimize_Label2;
    private javax.swing.JLabel SIgnUpName_Label;
    private javax.swing.JLabel SIgnUpSurname_Label;
    private javax.swing.JButton SignUpBackToLogin_Button;
    private javax.swing.JButton SignUpConfirm_Button;
    private javax.swing.JLabel SignUpEmail_Label;
    private javax.swing.JTextField SignUpEmail_TextField;
    private javax.swing.JLabel SignUpLogin_Label;
    private javax.swing.JTextField SignUpLogin_TextField;
    private javax.swing.JTextField SignUpName_TextField;
    private javax.swing.JPanel SignUpPanel;
    private javax.swing.JLabel SignUpPasswordAgain_Label;
    private javax.swing.JPasswordField SignUpPasswordAgain_TextField;
    private javax.swing.JLabel SignUpPassword_Label;
    private javax.swing.JPasswordField SignUpPassword_TextField;
    private javax.swing.JTextField SignUpSurname_TextField;
    private javax.swing.JLabel SignUpTitle_Label;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables
}
