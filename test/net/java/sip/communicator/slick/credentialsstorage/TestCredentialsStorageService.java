/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.credentialsstorage;

import junit.framework.*;
import net.java.sip.communicator.service.credentialsstorage.*;

import org.osgi.framework.*;

/**
 * Tests for the @link{CredentialsStorageService}.
 *
 * @author Dmitri Melnikov
 */
public class TestCredentialsStorageService
    extends TestCase
{
    /**
     * The service we are testing.
     */
    private CredentialsStorageService credentialsService = null;
    
    /**
     * Prefix for the test account.
     */
    private final String accountPrefix = "my.account.prefix";
    
    /**
     * Password for the test account.
     */
    private final String accountPassword = "pa$$W0rt123.";
    
    /**
     * The master password.
     */
    private final String masterPassword = "MasterPazz321";

    /**
     * Another master password. 
     */
    private final String otherMasterPassword = "123$ecretPSWRD";
    
    
    /**
     * Generic JUnit Constructor.
     * 
     * @param name the name of the test
     */
    public TestCredentialsStorageService(String name)
    {
        super(name);
        BundleContext context = CredentialsStorageServiceLick.bc;
        ServiceReference ref =
            context.getServiceReference(CredentialsStorageService.class
                .getName());
        credentialsService =
            (CredentialsStorageService) context.getService(ref);
    }
    
    /**
     * Generic JUnit setUp method.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        // set the master password
        boolean passSet =
            credentialsService.changeMasterPassword(null, masterPassword);
        if (!passSet)
        {
            throw new Exception("Failed to set the master password");
        }
        credentialsService.storePassword(accountPrefix, accountPassword);
        super.setUp();
    }
    
    /**
     * Generic JUnit tearDown method.
     * @throws Exception if anything goes wrong.
     */
    protected void tearDown() throws Exception
    {
        // remove the password
        boolean passRemoved =
            credentialsService.changeMasterPassword(masterPassword, null);
        if (!passRemoved)
        {
            throw new Exception("Failed to remove the master password");
        }
        credentialsService.removePassword(accountPrefix);
    }

    /**
     * Tests if a master password can be verified.
     */
    public void testIsVerified()
    {
        // try to verify a wrong password
        boolean verify1 =
            credentialsService.verifyMasterPassword(otherMasterPassword);
        assertFalse("Wrong password cannot be correct", verify1);
        
        // try to verify a correct password
        boolean verify2 =
            credentialsService.verifyMasterPassword(masterPassword);
        assertTrue("Correct password cannot be wrong", verify2);
    }
    
    /**
     * Tests whether the loaded password is the same as the stored one.
     */
    public void testLoadPassword()
    {
        String loadedPassword = credentialsService.loadPassword(accountPrefix);
        
        assertEquals("Loaded and stored passwords do not match", accountPassword,
            loadedPassword);
    }

    /**
     * Tests whether the service knows that we are using a master password.
     */
    public void testIsUsingMasterPassword()
    {
        boolean isUsing = credentialsService.isUsingMasterPassword();
        
        assertTrue("Master password is used, true expected", isUsing);
    }

    /**
     * Changes the master password to the new value and back again.
     */
    public void testChangeMasterPassword()
    {
        // change MP to a new value
        boolean change1 =
            credentialsService.changeMasterPassword(masterPassword,
                otherMasterPassword);
        assertTrue("Changing master password failed", change1);
        
        // account passwords must remain the same
        String loadedPassword = credentialsService.loadPassword(accountPrefix);
        assertEquals("Account passwords must not differ", loadedPassword,
            accountPassword);
        
        // change MP back
        boolean change2 =
            credentialsService.changeMasterPassword(otherMasterPassword,
                masterPassword);
        assertTrue("Changing master password back failed", change2);
    }

    /**
     * Test that the service is aware that the account password is stored in an
     * encrypted form.
     */
    public void testIsStoredEncrypted()
    {
        boolean storedEncrypted =
            credentialsService.isStoredEncrypted(accountPrefix);
        assertTrue("Account password is not stored encrypted", storedEncrypted);
    }

    /**
     * Tests whether removing the saved password really removes it.
     */
    public void testRemoveSavedPassword()
    {
        // remove the saved password
        credentialsService.removePassword(accountPrefix);
        
        // try to load the password
        String loadedPassword = credentialsService.loadPassword(accountPrefix);
        assertNull("Password was not removed", loadedPassword);
        
        // save it back again
        credentialsService.storePassword(accountPrefix, accountPassword);
    }
}
