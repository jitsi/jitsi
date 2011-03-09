/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import net.java.sip.communicator.service.ldap.*;

/**
 * Implementation of LdapPersonFound
 * An LdapPersonFound is contained in each LdapEvent
 * sent by an LdapDirectory after a successful LDAP search.
 * Each instance corresponds to a person found in the
 * LDAP directory, as well as its contact addresses.
 *
 * @author Sebastien Mazy
 */
public class LdapPersonFoundImpl
    implements LdapPersonFound
{
    /**
     * the server on which the person was found
     */
    private LdapDirectoryImpl server;

    /**
     * the query which this LdapPersonFound is a result of
     */
    private final LdapQuery query;

    /**
     * distinguished name for this person in the directory
     */
    private final String dn;

    /**
     * name/pseudo found in the the directory for this person
     */
    private String displayName = null;

    /**
     * first name found in the the directory for this person
     */
    private String firstName = null;

    /**
     * surname found in the the directory for this person
     */
    private String surname = null;

    /**
     * organization found in the the directory for this person
     */
    private String organization = null;

    /**
     * department found in the the directory for this person
     */
    private String department = null;

    /**
     * the set storing the mail addresses
     */
    private final Set<String> mails = new HashSet<String>();

    /**
     * the set storing the work phone numbers
     */
    private final Set<String> workPhoneNumbers = new HashSet<String>();

    /**
     * the set storing the mobile phone numbers
     */
    private final Set<String> mobilePhoneNumbers = new HashSet<String>();

    /**
     * the set storing the home phone numbers
     */
    private final Set<String> homePhoneNumbers = new HashSet<String>();

    /**
     * the constructor for this class
     *
     * @param server the server on which this person was found
     * @param dn distinguished name for this person in the directory
     * @param query the search query
     */
    public LdapPersonFoundImpl(LdapDirectoryImpl server, String dn,
            LdapQuery query)
    {
        if(server == null | query == null | dn==null)
            throw new NullPointerException();
        this.server = server;
        this.query = query;
        this.dn = dn;
    }

    /**
     * Returns the query which this Ldapperson found is a result of
     *
     * @return the initial query
     */
    public LdapQuery getQuery()
    {
        return this.query;
    }

    /**
     * Returns the server which this person was found on
     *
     * @return the server
     */
    public LdapDirectory getServer()
    {
        return (LdapDirectory) this.server;
    }

    /**
     * Sets the name/pseudo found in the the directory for this person
     *
     * @param name the name/pseudo found in the the directory for this person
     */
    public void setDisplayName(String name)
    {
        this.displayName = name;
    }

    /**
     * Returns the name/pseudo found in the the directory for this person
     *
     * @return the name/pseudo found in the the directory for this person
     */
    public String getDisplayName()
    {
        return this.displayName;
    }

    /**
     * Tries to fetch the photo in the the directory for this person
     *
     * @return the photo found in the the directory for this person
     * or null if not found
     */
    public byte[] fetchPhoto()
    {
        byte[] photo;
        photo = this.server.fetchPhotoForPerson(this.dn);
        return photo;
    }

    /**
     * Sets the first name found in the the directory for this person
     *
     * @param firstName the name/pseudo found in the the directory for this
     * person
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * Returns the first name found in the the directory for this person
     *
     * @return the first name found in the the directory for this person
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    /**
     * Sets the surname found in the the directory for this person
     *
     * @param surname the surname found in the the directory for this person
     */
    public void setSurname(String surname)
    {
        this.surname = surname;
    }

    /**
     * Returns the surname found in the the directory for this person
     *
     * @return the surname found in the the directory for this person
     */
    public String getSurname()
    {
        return this.surname;
    }

    /**
     * Sets the organization found in the the directory for this person
     *
     * @param organization the organization found in the the directory for this
     *  person
     */
    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    /**
     * Returns the organization found in the the directory for this person
     *
     * @return the organization found in the the directory for this person
     */
    public String getOrganization()
    {
        return this.organization;
    }

    /**
     * Sets the department found in the the directory for this person
     *
     * @param department the department found in the the directory for this
     *  person
     */
    public void setDepartment(String department)
    {
        this.department = department;
    }

    /**
     * Returns the department found in the the directory for this person
     *
     * @return the department found in the the directory for this person
     */
    public String getDepartment()
    {
        return this.department;
    }

    /**
     * Adds a mail address to this person
     *
     * @param mail the mail address
     */
    public void addMail(String mail)
    {
        this.mails.add(mail);
    }

    /**
     * Returns mail addresss from this person
     *
     * @return mail addresss from this person
     */
    public Set<String> getMail()
    {
        return this.mails;
    }

    /**
     * Returns telephone numbers from this person
     *
     * @return telephone numbers from this person
     */
    public Set<String> getAllPhone()
    {
        Set<String> allPhone = new HashSet<String>();

        allPhone.addAll(this.workPhoneNumbers);
        allPhone.addAll(this.mobilePhoneNumbers);
        allPhone.addAll(this.homePhoneNumbers);

        return allPhone;
    }

    /**
     * Adds a work telephone number to this person
     *
     * @param telephoneNumber the work telephone number
     */
    public void addWorkPhone(String telephoneNumber)
    {
        this.workPhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns work telephone numbers from this person
     *
     * @return work telephone numbers from this person
     */
    public Set<String> getWorkPhone()
    {
        Set<String> workPhone = new HashSet<String>();

        workPhone.addAll(this.workPhoneNumbers);

        return workPhone;
    }

    /**
     * Adds a mobile telephone number to this person
     *
     * @param telephoneNumber the mobile telephone number
     */
    public void addMobilePhone(String telephoneNumber)
    {
        this.mobilePhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns mobile telephone numbers from this person
     *
     * @return mobile telephone numbers from this person
     */
    public Set<String> getMobilePhone()
    {
        Set<String> mobilePhone = new HashSet<String>();

        mobilePhone.addAll(this.mobilePhoneNumbers);

        return mobilePhone;
    }

    /**
     * Adds a home telephone number to this person
     *
     * @param telephoneNumber the home telephone number
     */
    public void addHomePhone(String telephoneNumber)
    {
        this.homePhoneNumbers.add(telephoneNumber);
    }

    /**
     * Returns home telephone numbers from this person
     *
     * @return home telephone numbers from this person
     */
    public Set<String> getHomePhone()
    {
        Set<String> homePhone = new HashSet<String>();

        homePhone.addAll(this.homePhoneNumbers);

        return homePhone;
    }

    /**
     * Returns the distinguished name for this person
     *
     * @return the distinguished name for this person
     */
    public String getDN()
    {
        return this.dn;
    }

    /**
     * A string representation of this LdapPersonFoundImpl
     * (created for debugging purposes)
     *
     * @return a printable String
     */
    public String toString()
    {
        return this.getDisplayName();
    }

    public int compareTo(LdapPersonFound other)
    {
        if(this.toString().equals(other.toString()))
            return this.getDN().compareTo((other).getDN());
        else
            return this.toString().compareTo(other.toString());
    }

    public boolean equals(Object o)
    {
        if(!(o instanceof LdapPersonFound) || o == null)
            return false;
        else
            return this.toString().equals(o.toString()) &&
                this.getDN().equals(((LdapPersonFound) o).getDN());
    }
}
