/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * SSHAccountRegistration.java
 *
 * Created on 22 May, 2007, 8:49 AM
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
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

