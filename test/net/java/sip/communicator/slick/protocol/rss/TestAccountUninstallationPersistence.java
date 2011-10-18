/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import junit.framework.*;
import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Contains tests verifying persistence of account uninstallation. In other
 * words we try to make sure that once uninstalled an account remains
 * uninstalled.
 *
 * @author Mihai Balan
 */
public class TestAccountUninstallationPersistence
    extends TestCase
{
    /**
     * Creates a new test instance wrapper around the test with the specified
     * name.
     *
     * @param testName the name of the test that we will be executing.
     */
    public TestAccountUninstallationPersistence(String testName)
    {
        super(testName);
    }

    /**
     * Retrieves a reference to the RSS bundle, stops and uninstalls it and then
     * reinstalls it in order to make sure that accounts are not reloaded once
     * removed.
     *
     * @throws Exception if anything goes wrong while manipulating the bundles.
     */
    public void testAccountUninstallationPersistence()
        throws Exception
    {
        Bundle providerBundle = RssSlickFixture.providerBundle;
        providerBundle.stop();

        assertTrue("Couldn't stop the protocol provider bundle. State was"
            + providerBundle.getState(),
            providerBundle.getState() != Bundle.ACTIVE
            && providerBundle.getState() != Bundle.STOPPING);

        providerBundle.uninstall();
        assertEquals("Couldn't stop the protocol provider bundle.",
            providerBundle.getState(), Bundle.UNINSTALLED);

        //now reinstall the bundle and restart the provider
        providerBundle = RssSlickFixture.bc.installBundle(
            providerBundle.getLocation());
        assertEquals("Couldn't reinstall protocol provider bundle.",
            providerBundle.getState(), Bundle.INSTALLED);

        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(
            RssSlickFixture.bc, providerBundle, ProtocolNames.RSS);
        assertEquals("Couldn't restart protocol provider bundle.",
            providerBundle.getState(), Bundle.ACTIVE);

        //verify the provider is not reinstalled
        ServiceReference[] providerRefs = null;
        try {
            providerRefs = RssSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL + "=" + ProtocolNames.RSS
                    + ")");
        } catch (InvalidSyntaxException ise)
        {
            fail("OSGi filter is wrong. Error was: " + ise.getMessage());
        }

        //make sure we didn't retrieve a service
        assertTrue("An RSS protocol provider service was still registered as an"
            + " OSGi service even after being explicitly uninstalled",
            providerRefs == null || providerRefs.length == 0);

        //now delete configuration file for a fresh start for the next test
        ServiceReference confRef = RssSlickFixture.bc.getServiceReference(
            ConfigurationService.class.getName());
        ConfigurationService confServ =
            (ConfigurationService) RssSlickFixture.bc.getService(confRef);

        confServ.purgeStoredConfiguration();
    }
}
