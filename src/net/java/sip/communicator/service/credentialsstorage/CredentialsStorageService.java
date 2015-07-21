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
     * @param accountPrefix account prefix
     * @param password the password to store
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * stored; otherwise, <tt>false</tt>
     */
    public boolean storePassword(String accountPrefix, String password);

    /**
     * Load the password for the account that starts with the given prefix.
     *
     * @param accountPrefix account prefix
     * @return the loaded password for the <tt>accountPrefix</tt>
     */
    public String loadPassword(String accountPrefix);

    /**
     * Remove the password for the account that starts with the given prefix.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if the password for the specified
     * <tt>accountPrefix</tt> was successfully removed; otherwise,
     * <tt>false</tt>
     */
    public boolean removePassword(String accountPrefix);

    /**
     * Checks if master password was set by the user and
     * it is used to encrypt saved account passwords.
     *
     * @return <tt>true</tt> if used, <tt>false</tt> if not
     */
    public boolean isUsingMasterPassword();

    /**
     * Changes the old master password to the new one.
     * For all saved account passwords it decrypts them with the old MP and then
     * encrypts them with the new MP.
     *
     * @param oldPassword the old master password
     * @param newPassword the new master password
     * @return <tt>true</tt> if master password was changed successfully;
     * <tt>false</tt>, otherwise
     */
    public boolean changeMasterPassword(String oldPassword, String newPassword);

    /**
     * Verifies the correctness of the master password.
     *
     * @param master the master password to verify
     * @return <tt>true</tt> if the password is correct; <tt>false</tt>,
     * otherwise
     */
    public boolean verifyMasterPassword(String master);

    /**
     * Checks if the account password that starts with the given prefix is saved
     * in encrypted form.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if saved, <tt>false</tt> if not
     */
    public boolean isStoredEncrypted(String accountPrefix);
}
