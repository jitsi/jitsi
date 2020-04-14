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
package net.java.sip.communicator.plugin.gibberishaccregwizz;

/**
 * The <tt>GibberishAccountRegistration</tt> is used to store all user input data
 * through the <tt>GibberishAccountRegistrationWizard</tt>.
 *
 * @author Emil Ivov
 */
public class GibberishAccountRegistration
{
    private String userID;
    private String password;
    private boolean rememberPassword;

    /**
     * Returns the User ID of the gibberish registration account.
     * @return the User ID of the gibberish registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the gibberish registration account.
     * @param userID the userID of the gibberish registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the Gibberish registration account.
     *
     * @return the password of the Gibberish registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the Gibberish registration account.
     *
     * @param password the password of the Gibberish registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this Gibberish account registration.
     *
     * @param rememberPassword <tt>true</tt> if password has to remembered,
     * <tt>false</tt> otherwise.
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

}
