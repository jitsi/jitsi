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
package net.java.sip.communicator.impl.credentialsstorage;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Base64; // disambiguation

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Implements {@link CredentialsStorageService} to load and store user
 * credentials from/to the {@link ConfigurationService}.
 *
 * @author Dmitri Melnikov
 */
public class CredentialsStorageServiceImpl
    implements CredentialsStorageService
{
    /**
     * The <tt>Logger</tt> used by this <tt>CredentialsStorageServiceImpl</tt>
     * for logging output.
     */
    private final Logger logger
        = Logger.getLogger(CredentialsStorageServiceImpl.class);

    /**
     * The name of a property which represents an encrypted password.
     */
    public static final String ACCOUNT_ENCRYPTED_PASSWORD
        = "ENCRYPTED_PASSWORD";

    /**
     * The name of a property which represents an unencrypted password.
     */
    public static final String ACCOUNT_UNENCRYPTED_PASSWORD = "PASSWORD";

    /**
     * The property in the configuration that we use to verify master password
     * existence and correctness.
     */
    private static final String MASTER_PROP
        = "net.java.sip.communicator.impl.credentialsstorage.MASTER";

    /**
     * This value will be encrypted and saved in MASTER_PROP and
     * will be used to verify the key's correctness.
     */
    private static final String MASTER_PROP_VALUE = "true";

    /**
     * The configuration service.
     */
    private ConfigurationService configurationService;

    /**
     * A {@link Crypto} instance that does the actual encryption and decryption.
     */
    private Crypto crypto;

    /**
     * Initializes the credentials service by fetching the configuration service
     * reference from the bundle context. Encrypts and moves all passwords to
     * new properties.
     *
     * @param bc bundle context
     */
    void start(BundleContext bc)
    {
        configurationService
            = ServiceUtils.getService(bc, ConfigurationService.class);

        /*
         * If a master password is set, the migration of the unencrypted
         * passwords will have to wait for the UIService to register in
         * order to be able to ask for the master password. But that is
         * unreasonably late in the case of no master password.
         */
        if (!isUsingMasterPassword())
            moveAllPasswordProperties();
    }

    /**
     * Forget the encryption/decryption key when stopping the service.
     */
    void stop()
    {
        crypto = null;
    }

    /**
     * Stores the password for the specified account. When password is
     * null the property is cleared.
     *
     * Many threads can call this method at the same time, and the
     * first thread may present the user with the master password prompt and
     * create a <tt>Crypto</tt> instance based on the input
     * (<tt>createCrypto</tt> method). This instance will be used later by all
     * other threads.
     *
     * @param accountPrefix account prefix
     * @param password the password to store
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * stored; otherwise, <tt>false</tt>
     * @see CredentialsStorageServiceImpl#storePassword(String, String)
     */
    public synchronized boolean storePassword(
            String accountPrefix,
            String password)
    {
        if (createCrypto())
        {
            String encryptedPassword = null;
            try
            {
                if (password != null)
                    encryptedPassword = crypto.encrypt(password);
                setEncrypted(accountPrefix, encryptedPassword);
                return true;
            }
            catch (Exception ex)
            {
                logger.error("Encryption failed, password not saved", ex);
                return false;
            }
        }
        else
            return false;
    }

    /**
     * Loads the password for the specified account. If the password is stored
     * encrypted, decrypts it with the master password.
     *
     * Many threads can call this method at the same time, and the first thread
     * may present the user with the master password prompt and create a
     * <tt>Crypto</tt> instance based on the input (<tt>createCrypto</tt>
     * method). This instance will be used later by all other threads.
     *
     * @param accountPrefix account prefix
     * @return the loaded password for the <tt>accountPrefix</tt>
     * @see CredentialsStorageServiceImpl#createCrypto()
     */
    public synchronized String loadPassword(String accountPrefix)
    {
        String password = null;
        if (isStoredEncrypted(accountPrefix) && createCrypto())
        {
            try
            {
                password = crypto.decrypt(getEncrypted(accountPrefix));
            }
            catch (Exception ex)
            {
                logger.error("Decryption with master password failed", ex);
                // password stays null
            }
        }
        return password;
    }

    /**
     * Removes the password for the account that starts with the given prefix by
     * setting its value in the configuration to null.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if the password for the specified
     * <tt>accountPrefix</tt> was successfully removed; otherwise,
     * <tt>false</tt>
     */
    public boolean removePassword(String accountPrefix)
    {
        setEncrypted(accountPrefix, null);
        if (logger.isDebugEnabled())
            logger.debug("Password for '" + accountPrefix + "' removed");
        return true;
    }

    /**
     * Checks if master password is used to encrypt saved account passwords.
     *
     * @return true if used, false if not
     */
    public boolean isUsingMasterPassword()
    {
        return null != configurationService.getString(MASTER_PROP);
    }

    /**
     * Verifies the correctness of the master password.
     * Since we do not store the MP itself, if {@link #MASTER_PROP_VALUE}
     * is equal to the decrypted {@link #MASTER_PROP}'s value, then
     * the MP is considered correct.
     *
     * @param master master password
     * @return <tt>true</tt> if the password is correct; <tt>false</tt>,
     * otherwise
     */
    public boolean verifyMasterPassword(String master)
    {
        Crypto localCrypto = new AESCrypto(master);
        try
        {
            // use this value to verify master password correctness
            String encryptedValue = getEncryptedMasterPropValue();
            boolean correct
                = MASTER_PROP_VALUE.equals(localCrypto.decrypt(encryptedValue));

            if (correct)
            {
                // also set the crypto instance to use the correct MP
                setMasterPassword(master);
            }
            return correct;
        }
        catch (CryptoException e)
        {
            if (e.getErrorCode() == CryptoException.WRONG_KEY)
            {
                logger.debug("Incorrect master pass", e);
                return false;
            }
            else
            {
                // this should not happen, so just in case it does..
                throw new RuntimeException("Decryption failed", e);
            }
        }
    }

    /**
     * Changes the master password from the old to the new one.
     * Decrypts all encrypted password properties from the configuration
     * with the oldPassword and encrypts them again with newPassword.
     *
     * @param oldPassword old master password
     * @param newPassword new master password
     * @return <tt>true</tt> if master password was changed successfully;
     * <tt>false</tt>, otherwise
     */
    public boolean changeMasterPassword(String oldPassword, String newPassword)
    {
        // get all encrypted account password properties
        List<String> encryptedAccountProps =
            configurationService
                .getPropertyNamesBySuffix(ACCOUNT_ENCRYPTED_PASSWORD);

        // this map stores propName -> password
        Map<String, String> passwords = new HashMap<String, String>();
        try
        {
            // read from the config and decrypt with the old MP..
            setMasterPassword(oldPassword);
            for (String propName : encryptedAccountProps)
            {
                String propValue = configurationService.getString(propName);
                if (propValue != null)
                {
                    String decrypted = crypto.decrypt(propValue);
                    passwords.put(propName, decrypted);
                }
            }
            // ..and encrypt again with the new, write to the config
            setMasterPassword(newPassword);
            for (Map.Entry<String, String> entry : passwords.entrySet())
            {
                String encrypted = crypto.encrypt(entry.getValue());
                configurationService.setProperty(entry.getKey(), encrypted);
            }
            // save the verification value, encrypted with the new MP,
            // or remove it if the newPassword is null (we are unsetting MP)
            writeVerificationValue(newPassword == null);
        }
        catch (CryptoException ce)
        {
            logger.debug(ce);
            crypto = null;
            passwords = null;
            return false;
        }
        return true;
    }

    /**
     * Sets the master password to the argument value.
     *
     * @param master master password
     */
    private void setMasterPassword(String master)
    {
        crypto = new AESCrypto(master);
    }

    /**
     * Moves all password properties from unencrypted
     * {@link #ACCOUNT_UNENCRYPTED_PASSWORD} to the corresponding encrypted
     * {@link #ACCOUNT_ENCRYPTED_PASSWORD}.
     */
    private void moveAllPasswordProperties()
    {
        List<String> unencryptedProperties
            = configurationService.getPropertyNamesBySuffix(
                    ACCOUNT_UNENCRYPTED_PASSWORD);

        for (String prop : unencryptedProperties)
        {
            int idx = prop.lastIndexOf('.');

            if (idx != -1)
            {
                String prefix = prop.substring(0, idx);
                String encodedPassword = getUnencrypted(prefix);

                /*
                 * If the password is stored unencrypted, we have to migrate it,
                 * of course. But if it is also stored encrypted in addition to
                 * being stored unencrypted, the situation starts to look
                 * unclear and it may be better to just not migrate it.
                 */
                if ((encodedPassword == null)
                        || (encodedPassword.length() == 0)
                        || isStoredEncrypted(prefix))
                {
                    setUnencrypted(prefix, null);
                }
                else if (!movePasswordProperty(
                        prefix,
                        new String(Base64.decode(encodedPassword))))
                {
                    logger.warn("Failed to move password for prefix " + prefix);
                }
            }
        }
    }

    /**
     * Asks for master password if needed, encrypts the password, saves it to
     * the new property and removes the old property.
     *
     * @param accountPrefix prefix of the account
     * @param password unencrypted password
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * moved; otherwise, <tt>false</tt>
     */
    private boolean movePasswordProperty(String accountPrefix, String password)
    {
        if (createCrypto())
        {
            try
            {
                setEncrypted(accountPrefix, crypto.encrypt(password));
                setUnencrypted(accountPrefix, null);
                return true;
            }
            catch (CryptoException cex)
            {
                logger.debug("Encryption failed", cex);
            }
        }
        // properties are not moved
        return false;
    }

    /**
     * Writes the verification value to the configuration for later use or
     * removes it completely depending on the remove flag argument.
     *
     * @param remove to remove the verification value or just overwrite it.
     */
    private void writeVerificationValue(boolean remove)
    {
        if (remove)
            configurationService.removeProperty(MASTER_PROP);
        else
        {
            try
            {
                configurationService.setProperty(
                        MASTER_PROP,
                        crypto.encrypt(MASTER_PROP_VALUE));
            }
            catch (CryptoException cex)
            {
                logger.error(
                        "Failed to encrypt and write verification value",
                        cex);
            }
        }
    }

    /**
     * Creates a Crypto instance only when it's null, either with a user input
     * master password or with null. If the user decided not to input anything,
     * the instance is not created.
     *
     * @return <tt>true</tt> if the Crypto instance was created; <tt>false</tt>,
     * otherwise
     */
    private synchronized boolean createCrypto()
    {
        /*
         * XXX The method #createCrypto() is synchronized in order to not ask
         * for the master password more than once. Without the synchronization,
         * it is possible to have the master password prompt shown twice in a
         * row during application startup when unencypted passwords are to be
         * migrated with the master password already set and the accounts start
         * loading.
         */

        if (crypto == null)
        {
            logger.debug("Crypto instance is null, creating.");
            if (isUsingMasterPassword())
            {
                String master = showPasswordPrompt();

                if (master == null)
                {
                    // User clicked cancel button in the prompt.
                    crypto = null;
                }
                else
                {
                    /*
                     * At this point the master password must be correct, so we
                     * set the crypto instance to use it
                     */
                    setMasterPassword(master);
                }

                moveAllPasswordProperties();
            }
            else
            {
                logger.debug("Master password not set");

                /*
                 * Setting the master password to null means we shall still be
                 * using encryption/decryption but using some default value, not
                 * something specified by the user.
                 */
                setMasterPassword(null);
            }
        }
        return (crypto != null);
    }

    /**
     * Displays a password prompt to the user in a loop until it is correct or
     * the user presses the cancel button.
     *
     * @return the entered password or <tt>null</tt> if none was provided.
     */
    private String showPasswordPrompt()
    {
        String master;

        // Ask for master password until the input is correct or
        // cancel button is pressed and null returned
        boolean correct = true;

        MasterPasswordInputService masterPasswordInputService
            = CredentialsStorageActivator.getMasterPasswordInputService();

        if(masterPasswordInputService == null)
        {
            logger.error(
                    "Missing MasterPasswordInputService to show input dialog");
            return null;
        }

        do
        {
            master = masterPasswordInputService.showInputDialog(correct);
            if (master == null)
                return null;
            correct = ((master.length() != 0) && verifyMasterPassword(master));
        }
        while (!correct);
        return master;
    }

    /**
     * Retrieves the property for the master password from the configuration
     * service.
     *
     * @return the property for the master password
     */
    private String getEncryptedMasterPropValue()
    {
        return configurationService.getString(MASTER_PROP);
    }

    /**
     * Retrieves the encrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @return the encrypted account password.
     */
    private String getEncrypted(String accountPrefix)
    {
        return
            configurationService.getString(
                    accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD);
    }

    /**
     * Saves the encrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @param value the encrypted account password.
     */
    private void setEncrypted(String accountPrefix, String value)
    {
        configurationService.setProperty(
                accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD,
                value);
    }

    /**
     * Check if encrypted account password is saved in the configuration.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if saved, <tt>false</tt> if not
     */
    public boolean isStoredEncrypted(String accountPrefix)
    {
        return
            configurationService.getString(
                    accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD)
                != null;
    }

    /**
     * Retrieves the unencrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @return the unencrypted account password
     */
    private String getUnencrypted(String accountPrefix)
    {
        return
            configurationService.getString(
                    accountPrefix + "." + ACCOUNT_UNENCRYPTED_PASSWORD);
    }

    /**
     * Saves the unencrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @param value the unencrypted account password
     */
    private void setUnencrypted(String accountPrefix, String value)
    {
        configurationService.setProperty(
                accountPrefix + "." + ACCOUNT_UNENCRYPTED_PASSWORD, value);
    }
}
