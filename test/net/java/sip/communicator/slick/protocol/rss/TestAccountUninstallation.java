/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Tests whether accounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations. Apart from uninstallation
 * tests the class also contains tests that remove and reinstall the protocol
 * provider bundle in order to verify that accounts are persistent.
 *
 * @author Mihai Balan
 */
public class TestAccountUninstallation
    extends TestCase
{
    private RssSlickFixture fixture = new RssSlickFixture();

    /**
     * Constructs a test instance
     * @param name The name of the test.
     */
    public TestAccountUninstallation(String name)
    {
        super(name);
    }

    /**
     * JUnit setup method.
     * @throws Exception in case anything goes wrong.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    public void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Returns a suite containing tests in this class in the order that we'd
     * like them executed.
     * @return a Test suite containing tests in this class in the order that
     * we'd like them executed.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(
            new TestAccountUninstallation("testInstallationPersistence"));
        suite.addTest(
            new TestAccountUninstallation("testUninstallAccount"));

        return suite;
    }

    /**
     * Stops and removes the tested bundle, verifies that it has unregistered
     * its provider, then reloads and restarts the bundle and verifies that
     * the protocol provider is reRegistered in the bundle context.
     *
     * @throws java.lang.Exception if an exception occurs during testing.
     */
    public void testInstallationPersistence() throws Exception
    {
        Bundle providerBundle =
            fixture.findProtocolProviderBundle(fixture.provider);

        //set the global providerBundle reference that we will be using
            //in the last series of tests (Account uninstallation persistency)
        RssSlickFixture.providerBundle = providerBundle;

        assertNotNull("Couldn't find a bundle for the protocol provider",
            providerBundle);

        providerBundle.stop();
        assertTrue("Couldn't stop protocol provider bundle. State was "
            + providerBundle.getSymbolicName(),
            providerBundle.getState() != Bundle.ACTIVE
            && providerBundle.getState() != Bundle.STOPPING);

        providerBundle.uninstall();
        assertEquals("Couldn't uninstall protocol provider bundle.",
            providerBundle.getState(), Bundle.UNINSTALLED);

        //verify that the provider is no longer available
        ServiceReference providerRefs[] = null;
        try {
            providerRefs = fixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&(" + ProtocolProviderFactory.PROTOCOL + "="
                    + ProtocolNames.RSS + "))");
        } catch (InvalidSyntaxException ise)
        {
            fail("Invalid OSGi filter. Exception was" + ise.getMessage());
        }

        //verify the provider really uninstalled
        assertTrue("Protocol provider still present after being explicitly" +
                " uninstalled",
            providerRefs == null || providerRefs.length == 0);
        assertTrue("The RSS protocol provider factory didn't completely "
            + "uninstalled the provider service",
            fixture.providerFactory.getRegisteredAccounts().size() == 0 &&
            fixture.providerFactory.getProviderForAccount(
                fixture.provider.getAccountID()) == null);

        //reinstall provider
        providerBundle = fixture.bc.installBundle(providerBundle.getLocation());
        RssSlickFixture.providerBundle = providerBundle;

        assertTrue("Couldn't reinstall provider bundle",
            providerBundle.getState() == Bundle.INSTALLED);


        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(fixture.bc,
            providerBundle, ProtocolNames.RSS);
        assertTrue("Couldn't start provider",
            providerBundle.getState() == Bundle.ACTIVE);

        providerRefs = null;
        try {
            providerRefs = fixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&(" + ProtocolProviderFactory.PROTOCOL + "="
                    + ProtocolNames.RSS + "))");
        } catch (InvalidSyntaxException ise)
        {
            fail("Invalid OSGi filter. Exception was" + ise.getMessage());
        }
        assertTrue("The protocol provider hasn't been restored after being "
                + "reinstalled",
            providerRefs != null && providerRefs.length > 0);

        ServiceReference factoryRefs[] = null;
        try {
            factoryRefs = fixture.bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL + "=RSS)");
        } catch (InvalidSyntaxException ise)
        {
            fail("Invalid OSGi filter. Exception was " + ise.getMessage());
        }

        fixture.providerFactory =
            (ProtocolProviderFactory) fixture.bc.getService(factoryRefs[0]);
        fixture.provider =
            (ProtocolProviderService) fixture.bc.getService(providerRefs[0]);

        assertFalse("RSS provider did not restore its own reference to the"
            + " provider that we just reinstalled.",
            fixture.providerFactory.getRegisteredAccounts().isEmpty()
            && fixture.providerFactory.getProviderForAccount(
                fixture.provider.getAccountID()) == null);
    }

    /**
     * Unistalls our test account and makes sure it really has been removed.
     */
    public void testUninstallAccount() throws Exception
    {
        assertTrue("No accounts found.",
            ! fixture.providerFactory.getRegisteredAccounts().isEmpty());

        assertNotNull("Found no provider corresponding to RSS",
            fixture.providerFactory.getProviderForAccount(
                fixture.provider.getAccountID()));

        assertTrue("Failed to remove provider",
            fixture.providerFactory.uninstallAccount(
                fixture.provider.getAccountID()));

        ServiceReference[] providerRefs = null;
        try {
            providerRefs = fixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL + "=" + ProtocolNames.RSS
                    + ")");
        } catch (InvalidSyntaxException ise)
        {
            fail("Invalid OSGi filter.Exception was: " + ise.getMessage());
        }

        assertTrue("Protocol provider service still registered as OSGi service",
            providerRefs == null || providerRefs.length == 0);

        assertTrue("Provider factory didn't properly uninstalled the provider"
            + "service we just uninstalled",
            fixture.providerFactory.getRegisteredAccounts().isEmpty()
            && fixture.providerFactory.getProviderForAccount(
                fixture.provider.getAccountID()) == null);
    }
}
