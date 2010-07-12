/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.credentialsstorage;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

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
     * The logger for this class.
     */
    private final Logger logger =
        Logger.getLogger(CredentialsStorageServiceImpl.class);

    /**
     * The name of a property which represents an encrypted password.
     */
    public static final String ACCOUNT_ENCRYPTED_PASSWORD =
        "ENCRYPTED_PASSWORD";

    /**
     * The name of a property which represents an unencrypted password.
     */
    public static final String ACCOUNT_UNENCRYPTED_PASSWORD = "PASSWORD";

    /**
     * The property in the configuration that we use to verify master password
     * existence and correctness.
     */
    private static final String MASTER_PROP =
        "net.java.sip.communicator.impl.credentialsstorage.MASTER";
    
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
     * reference from the bundle context.
     * 
     * @param bc bundle context
     */
    void start(BundleContext bc)
    {
        ServiceReference confServiceReference =
            bc.getServiceReference(ConfigurationService.class.getName());
        this.configurationService =
            (ConfigurationService) bc.getService(confServiceReference);
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
     * @see CredentialsStorageServiceImpl#createCrypto()
     */
    public synchronized void storePassword(String accountPrefix, String password)
    {
        boolean createdOrExisting = createCrypto();
        if (createdOrExisting)
        {
            String encryptedPassword = null;
            try
            {
                if (password != null)
                    encryptedPassword = crypto.encrypt(password);
                setEncrypted(accountPrefix, encryptedPassword);
            }
            catch (Exception e)
            {
                logger.warn("Encryption failed, password not saved", e);
            }
        }
    }

    /**
     * Loads the password for the specified account.
     * First check if the password is stored in the configuration unencrypted
     * and if so, encrypt it and store in the new property. Otherwise, if the
     * password is stored encrypted, decrypt it with the master password.
     * 
     * Many threads can call this method at the same time, and the
     * first thread may present the user with the master password prompt and
     * create a <tt>Crypto</tt> instance based on the input 
     * (<tt>createCrypto</tt> method). This instance will be used later by all
     * other threads.
     * 
     * @see CredentialsStorageServiceImpl#createCrypto()
     */
    public synchronized String loadPassword(String accountPrefix)
    {
        String password = null;
        if (isStoredUnencrypted(accountPrefix))
        {
            password = new String(Base64.decode(getUnencrypted(accountPrefix)));
            movePasswordProperty(accountPrefix, password);
        }

        if (password == null && isStoredEncrypted(accountPrefix))
        {
            boolean createdOrExisting = createCrypto();
            if (createdOrExisting)
            {
                try
                {
                    String ciphertext = getEncrypted(accountPrefix);
                    password = crypto.decrypt(ciphertext);
                }
                catch (Exception e)
                {
                    logger.warn("Decryption with master password failed", e);
                    // password stays null
                }
            }
        }
        return password;
    }

    /**
     * Removes the password for the account that starts with the given prefix by
     * setting its value in the configuration to null.
     */
    public void removePassword(String accountPrefix)
    {
        setEncrypted(accountPrefix, null);
        if (logger.isDebugEnabled())
        {
            logger.debug("Password for '" + accountPrefix + "' removed");
        }
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
     * Since we do not store the MP itself, if MASTER_PROP_VALUE 
     * is equal to the decrypted MASTER_PROP value, then
     * the MP is considered correct. 
     * 
     * @param master master password
     * @return true if the password is correct, false otherwise
     */
    public boolean verifyMasterPassword(String master)
    {
        Crypto localCrypto = new AESCrypto(master);
        try
        {
            // use this value to verify master password correctness
            String encryptedValue = getEncryptedMasterPropValue();
            return MASTER_PROP_VALUE
                .equals(localCrypto.decrypt(encryptedValue));
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
     * Asks for master password if needed, encrypts the password, saves it to
     * the new property and removes the old property.
     * 
     * @param accountPrefix prefix of the account
     * @param password unencrypted password
     */
    private void movePasswordProperty(String accountPrefix, String password)
    {
        boolean createdOrExisting = createCrypto();
        if (createdOrExisting)
        {
            try
            {
                String encryptedPassword = crypto.encrypt(password);
                setEncrypted(accountPrefix, encryptedPassword);
                setUnencrypted(accountPrefix, null);
            }
            catch (CryptoException e)
            {
                logger.debug("Encryption failed", e);
                // properties are not moved
            }
        }
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
        {
            configurationService.removeProperty(MASTER_PROP);
        }
        else
        {
            try
            {
                String encryptedValue = crypto.encrypt(MASTER_PROP_VALUE);
                configurationService.setProperty(MASTER_PROP, encryptedValue);
            }
            catch (CryptoException e)
            {
                logger.warn("Failed to encrypt and write verification value");
            }
        }
    }

    /**
     * Creates a Crypto instance only when it's null, either with a user input
     * master password or with null. If the user decided not to input anything,
     * the instance is not created.
     * 
     * @return true if Crypto instance was created, false otherwise
     */
    private boolean createCrypto()
    {
        if (crypto == null)
        {
            logger.debug("Crypto instance is null, creating.");
            if (isUsingMasterPassword())
            {
                String master = showPasswordPrompt();
                if (master == null)
                {
                    // user clicked cancel button in the prompt
                    crypto = null;
                    return false;
                }
                // at this point the master password must be correct,
                // so we set the crypto instance to use it
                setMasterPassword(master);
            }
            else
            {
                logger.debug("Master password not set");
                
                // setting the master password to null means 
                // we shall still be using encryption/decryption
                // but using some default value, not something specified
                // by the user
                setMasterPassword(null);
            }
        }
        return true;
    }

    /**
     * Displays a password prompt to the user in a loop until it is correct or
     * the user presses the cancel button.
     * 
     * @return the entered password or null if none was provided.
     */
    private String showPasswordPrompt()
    {
        String master = null;
        JPasswordField passwordField = new JPasswordField();
        String inputMsg =
            CredentialsStorageActivator
                .getString("plugin.securityconfig.masterpassword.MP_INPUT");
        String errorMsg =
            "<html><font color=\"red\">"
                + CredentialsStorageActivator
                    .getString("plugin.securityconfig.masterpassword.MP_VERIFICATION_FAILURE_MSG")
                + "</font></html>";

        // Ask for master password until the input is correct or
        // cancel button is pressed
        boolean correct = true;
        do
        {
            Object[] msg = null;
            if (correct)
            {
                msg = new Object[]
                { inputMsg, passwordField };
            }
            else
            {
                msg = new Object[]
                { errorMsg, inputMsg, passwordField };
            }
            // clear the password field
            passwordField.setText("");

            if (JOptionPane
                .showOptionDialog(
                    null,
                    msg,
                    CredentialsStorageActivator
                        .getString("plugin.securityconfig.masterpassword.MP_TITLE"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]
                    {
                        CredentialsStorageActivator.getString("service.gui.OK"),
                        CredentialsStorageActivator
                            .getString("service.gui.CANCEL") },
                    CredentialsStorageActivator.getString("service.gui.OK")) == JOptionPane.YES_OPTION)
            {

                master = new String(passwordField.getPassword());
                correct = !master.isEmpty() && verifyMasterPassword(master);
            }
            else
            {
                return null;
            }
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
        return configurationService.getString(accountPrefix + "."
            + ACCOUNT_ENCRYPTED_PASSWORD);
    }

    /**
     * Saves the encrypted account password using configuration service.
     * 
     * @param accountPrefix account prefix
     * @param value the encrypted account password.
     */
    private void setEncrypted(String accountPrefix, String value)
    {
        configurationService.setProperty(accountPrefix + "."
            + ACCOUNT_ENCRYPTED_PASSWORD, value);
    }
    
    /**
     * Check if encrypted account password is saved in the configuration.
     * 
     * @return true if saved, false if not
     */
    public boolean isStoredEncrypted(String accountPrefix)
    {
        return null != configurationService.getString(accountPrefix + "."
            + ACCOUNT_ENCRYPTED_PASSWORD);
    }

    /**
     * Retrieves the unencrypted account password using configuration service.
     * 
     * @param accountPrefix account prefix
     * @return the unencrypted account password 
     */
    private String getUnencrypted(String accountPrefix)
    {
        return configurationService.getString(accountPrefix + "."
            + ACCOUNT_UNENCRYPTED_PASSWORD);
    }

    /**
     * Saves the unencrypted account password using configuration service.
     * 
     * @param accountPrefix account prefix
     * @param value the unencrypted account password 
     */
    private void setUnencrypted(String accountPrefix, String value)
    {
        configurationService.setProperty(accountPrefix + "."
            + ACCOUNT_UNENCRYPTED_PASSWORD, value);
    }

    /**
     * Check if unencrypted account password is saved in the configuration.
     * 
     * @param accountPrefix account prefix 
     * @return true if saved, false if not
     */
    private boolean isStoredUnencrypted(String accountPrefix)
    {
        configurationService.getPropertyNamesByPrefix("", false);
        return null != configurationService.getString(accountPrefix + "."
            + ACCOUNT_UNENCRYPTED_PASSWORD);
    }
}
