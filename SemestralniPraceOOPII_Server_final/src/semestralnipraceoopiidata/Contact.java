/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semestralnipraceoopiidata;

import java.io.Serializable;

/**
 * Contact class. Same as client sode Contact class. Used for transfer over
 * socket, hence implements interface Serializable.
 *
 * @author Dominik
 */
public class Contact implements Serializable {

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getGroup() {
        return group;
    }

    public String getPrivilege() {
        return privilege;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPrivilege(String privilege) {
        this.privilege = privilege;
    }

    public byte[] getImage() {
        return image;
    }

    private int id;
    private String firstName;
    private String surname;
    private String phone;
    private String email;
    private String group;
    private byte[] image;
    private String privilege;
    private static final long serialVersionUID = 42L;

    /**
     * Creates instance of contact class.
     *
     * @param Id contact id
     * @param FirstName contact firstname
     * @param Surname contact lastname
     * @param Phone contact phonenumber
     * @param Email contact email address
     * @param Group contact group
     * @param Image contact image
     * @param Privilege privileges of given user upon contact
     */
    public Contact(int Id, String FirstName, String Surname, String Phone, String Email, String Group, byte[] Image, String Privilege) {
        this.id = Id;
        this.firstName = FirstName;
        this.surname = Surname;
        this.phone = Phone;
        this.email = Email;
        this.group = Group;
        this.image = Image;
        this.privilege = Privilege;
    }

    /**
     * Creates instance of contact class.
     *
     * @param FirstName contact firstname
     * @param Surname contact lastname
     * @param Id contact id
     */
    public Contact(String FirstName, String Surname, int Id) {
        this.firstName = FirstName;
        this.surname = Surname;
        this.id = Id;
    }
}
