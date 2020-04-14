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
package net.java.sip.communicator.plugin.icqaccregwizz;

/**
 * The <tt>IcqAccountRegistration</tt> is used to store all user input data
 * through the <tt>IcqAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class IcqAccountRegistration
{
    private String uin;

    private String password;

    private boolean rememberPassword = true;

    /**
     * Returns the password of the icq registration account.
     * @return the password of the icq registration account.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the icq registration account.
     * @param password the password of the icq registration account.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword() {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this icq account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword) {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the UIN of the icq registration account.
     * @return the UIN of the icq registration account.
     */
    public String getUin() {
        return uin;
    }

    /**
     * Sets the UIN of the icq registration account.
     * @param uin the UIN of the icq registration account.
     */
    public void setUin(String uin) {
        this.uin = uin;
    }
}
