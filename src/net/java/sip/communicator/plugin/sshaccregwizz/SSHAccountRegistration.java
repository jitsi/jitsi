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

package net.java.sip.communicator.plugin.sshaccregwizz;

/**
 * The <tt>SSHAccountRegistration</tt> is used to store all user input data
 * through the <tt>SSHAccountRegistrationWizard</tt>.
 *
 * @author Shobhit Jindal
 */
public class SSHAccountRegistration
{
    private String accountID;

    /**
     * Stored public keys[SSH] of remote server
     */
    private String knownHostsFile;

    /**
     * Identity file is a private[default] key of the user which is one
     * of the methods of authentication
     */
    private String identityFile;

    /**
     * Returns the Account ID of the ssh registration account.
     * @return accountID
     */
    public String getAccountID()
    {
        return accountID;
    }

    /**
     * Sets the Account ID of the ssh registration account.
     *
     * @param accountID the accountID of the ssh registration account.
     */
    public void setUserID(String accountID)
    {
        this.accountID = accountID;
    }

    /**
     * Returns the Known Hosts of the ssh registration account.
     *
     * @return knownHostsFile
     */
    public String getKnownHostsFile()
    {
        return knownHostsFile;
    }

    /**
     * Sets the Known Hosts of the ssh registration account.
     *
     * @param knownHostsFile
     */
    public void setKnownHostsFile(String knownHostsFile)
    {
        this.knownHostsFile = knownHostsFile;
    }

    /**
     * Returns the Identity File of the ssh registration account.
     *
     * @return identityFile
     */
    public String getIdentityFile()
    {
        return identityFile;
    }

    /**
     * Sets the Machine Port of the ssh registration account.
     *
     * @param machinePort
     */
    public void setIdentityFile(String machinePort)
    {
        this.identityFile = machinePort;
    }
}

