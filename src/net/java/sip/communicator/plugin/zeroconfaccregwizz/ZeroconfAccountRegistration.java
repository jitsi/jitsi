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
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

/**
 * The <tt>ZeroconfAccountRegistration</tt> is used to store
 * all user input data
 * through the <tt>ZeroconfAccountRegistrationWizard</tt>.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class ZeroconfAccountRegistration
{
    private String userID;
    private String first;
    private String last;
    private String mail;
    private boolean rememberContacts;

    /**
     * Returns the User ID of the zeroconf registration account.
     * @return the User ID of the zeroconf registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the zeroconf registration account.
     * @param userID the userID of the zeroconf registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the Zeroconf registration account.
     * @return the password of the Zeroconf registration account.
     */
    public String getFirst()
    {
        return first;
    }

    /**
     * Sets the password of the Zeroconf registration account.
     * @param first first name
     */
    public void setFirst(String first)
    {
        this.first = first;
    }

    /**
     * Returns <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     * @return <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     */
    public boolean isRememberContacts()
    {
        return rememberContacts;
    }

    /**
     * Sets the rememberPassword value of this Zeroconf account registration.
     * @param rememberContacts true if we want to remember the
     *        contacts we meet, false otherwise
     */
    public void setRememberContacts(boolean rememberContacts)
    {
        this.rememberContacts = rememberContacts;
    }

    /**
     * Returns the last name
     * @return last name
     */
    public String getLast()
    {
        return last;
    }

    /**
     * Sets the last name
     * @param last last name
     */
    public void setLast(String last)
    {
        this.last = last;
    }

    /**
     * Returns the mail address
     * @return mail address
     */
    public String getMail()
    {
        return mail;
    }

    /**
     * Sets the mail address
     * @param mail mail address
     */
    public void setMail(String mail)
    {
        this.mail = mail;
    }


}
