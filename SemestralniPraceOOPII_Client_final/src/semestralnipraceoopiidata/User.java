/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiidata;

import java.io.Serializable;

/**
 * User class. Same as server side User class. Used for transfer over socket,
 * hence implements interface Serializable.
 *
 * @author Dominik
 */
public class User implements Serializable {

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getPrivilege() {
        return privilege;
    }

    private int id;
    private String firstName;
    private String lastName;
    private byte[] image;
    private String loginName;
    private String privilege;
    private static final long serialVersionUID = 32L;

    /**
     * Creates instance of user class.
     *
     * @param Id user id
     * @param LoginName user login name
     * @param FirstName user first name
     * @param LastName user last name
     * @param Image user profile image
     */
    public User(int Id, String LoginName, String FirstName, String LastName, byte[] Image) {
        this.id = Id;
        this.loginName = LoginName;
        this.firstName = FirstName;
        this.lastName = LastName;
        this.image = Image;
    }

    /**
     * Creates instance of user class.
     *
     * @param Id user id
     * @param FirstName user first name
     * @param LastName user last name
     * @param Privilege user profile image
     */
    public User(int Id, String FirstName, String LastName, String Privilege) {
        this.id = Id;
        this.firstName = FirstName;
        this.lastName = LastName;
        this.privilege = Privilege;
    }
}
