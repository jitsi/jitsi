/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.ldap;

import java.util.*;

/**
 * An LdapPersonFound is contained in each LdapEvent
 * sent by an LdapDirectory after a successful LDAP search.
 * Each instance corresponds to a person found in the
 * LDAP directory, as well as its contact addresses.
 *
 * @author Sebastien Mazy
 */
public interface LdapPersonFound
    extends Comparable<LdapPersonFound>
{
    /**
     * Returns the query which this Ldapperson found is a result of
     *
     * @return the initial query
     */
    public LdapQuery getQuery();

    /**
     * Returns the server which this person was found on
     *
     * @return the server
     */
    public LdapDirectory getServer();

    /**
     * Sets the name/pseudo found in the the directory for this person
     *
     * @param name the name/pseudo found in the the directory for this person
     */
    public void setDisplayName(String name);

    /**
     * Returns the name/pseudo found in the the directory for this person
     *
     * @return the name/pseudo found in the the directory for this person
     */
    public String getDisplayName();

    /**
     * Tries to fetch the photo in the the directory for this person. If
     * successful, the binary photo is available from {@link #getPhoto()}
     */
    public void fetchPhoto();

    /**
     * Gets the photo found in the directory for this person.
     * @return the photo found in the directory for this person.
     */
    public byte[] getPhoto();

    /**
     * Set the photo found in the directory for this person.
     * @param photo the photo found in the directory for this person.
     */
    public void setPhoto(byte[] photo);

    /**
     * Sets the first name found in the the directory for this person
     *
     * @param firstName the name/pseudo found in the the directory for this
     *  person
     */
    public void setFirstName(String firstName);

    /**
     * Returns the first name found in the the directory for this person
     *
     * @return the first name found in the the directory for this person
     */
    public String getFirstName();

    /**
     * Sets the surname found in the the directory for this person
     *
     * @param surname the surname found in the the directory for this person
     */
    public void setSurname(String surname);

    /**
     * Returns the surname found in the the directory for this person
     *
     * @return the surname found in the the directory for this person
     */
    public String getSurname();

    /**
     * Sets the organization found in the the directory for this person
     *
     * @param organization the organization found in the the directory for this
     *  person
     */
    public void setOrganization(String organization);

    /**
     * Returns the organization found in the the directory for this person
     *
     * @return the organization found in the the directory for this person
     */
    public String getOrganization();

    /**
     * Sets the department found in the the directory for this person
     *
     * @param department the department found in the the directory for this
     *  person
     */
    public void setDepartment(String department);

    /**
     * Returns the department found in the the directory for this person
     *
     * @return the department found in the the directory for this person
     */
    public String getDepartment();

    /**
     * Adds a mail address to this person
     *
     * @param mail the mail address
     */
    public void addMail(String mail);

    /**
     * Returns mail addresses from this person
     *
     * @return mail addresses from this person
     */
    public Set<String> getMail();

    /**
     * Returns telephone numbers from this person
     *
     * @return telephone numbers from this person
     */
    public Set<String> getAllPhone();

    /**
     * Adds a work telephone number to this person
     *
     * @param telephoneNumber the work telephone number
     */
    public void addWorkPhone(String telephoneNumber);

    /**
     * Returns work telephone numbers from this person
     *
     * @return work telephone numbers from this person
     */
    public Set<String> getWorkPhone();

    /**
     * Adds a mobile telephone number to this person
     *
     * @param telephoneNumber the mobile telephone number
     */
    public void addMobilePhone(String telephoneNumber);

    /**
     * Returns mobile telephone numbers from this person
     *
     * @return mobile telephone numbers from this person
     */
    public Set<String> getMobilePhone();

    /**
     * Adds a home telephone number to this person
     *
     * @param telephoneNumber the home telephone number
     */
    public void addHomePhone(String telephoneNumber);

    /**
     * Returns home telephone numbers from this person
     *
     * @return home telephone numbers from this person
     */
    public Set<String> getHomePhone();

    /**
     * Returns the distinguished name for this person
     *
     * @return the distinguished name for this person
     */
    public String getDN();
}
