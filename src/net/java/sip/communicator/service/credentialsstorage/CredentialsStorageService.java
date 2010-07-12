/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Loads and saves user credentials from/to the persistent storage 
 * (configuration file in the default implementation).
 *
 * @author Dmitri Melnikov
 */
public interface CredentialsStorageService
{
    /**
     * Store the password for the account that starts with the given prefix.
     * 
     * @param accountPrefix
     * @param password
     */
    public void storePassword(String accountPrefix, String password);

    /**
     * Load the password for the account that starts with the given prefix.
     * 
     * @param accountPrefix
     * @return
     */
    public String loadPassword(String accountPrefix);

    /**
     * Remove the password for the account that starts with the given prefix.
     * 
     * @param accountPrefix
     */
    public void removePassword(String accountPrefix);

    /**
     * Checks if master password was set by the user and 
     * it is used to encrypt saved account passwords. 
     * 
     * @return true if used, false if not
     */
    public boolean isUsingMasterPassword();
    
    /**
     * Changes the old master password to the new one. 
     * For all saved account passwords it decrypts them with the old MP and then
     * encrypts them with the new MP. 
     * @param oldPassword
     * @param newPassword
     * @return true if MP was changed successfully, false otherwise
     */
    public boolean changeMasterPassword(String oldPassword, String newPassword);

    /**
     * Verifies the correctness of the master password.
     * @param master
     * @return true if the password is correct, false otherwise
     */
    public boolean verifyMasterPassword(String master);
    
    /**
     * Checks if the account password that starts with the given prefix is saved in encrypted form.
     * 
     * @return true if saved, false if not
     */
    public boolean isStoredEncrypted(String accountPrefix);
}
