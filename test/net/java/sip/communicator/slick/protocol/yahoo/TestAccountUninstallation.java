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
package net.java.sip.communicator.slick.protocol.yahoo;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.slick.protocol.generic.*;

import org.osgi.framework.*;

/**
 * Tests whether accounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations. Apart from uninstallation
 * tests the class also contains tests that remove and reinstall the protocol
 * provider bundle in order to verify that accounts are persistent.
 *
 * @author Emil Ivov
 * @author Valentin Martinet
 */
public class TestAccountUninstallation
    extends TestCase
{
    private YahooSlickFixture fixture = new YahooSlickFixture();

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
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();
    }

    /**
     * JUnit teardown method.
     * @throws Exception in case anything goes wrong.
     */
    @Override
    protected void tearDown() throws Exception
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
            new TestAccountUninstallation("testInstallationPersistency"));
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
    public void testInstallationPersistency() throws Exception
    {
        Bundle providerBundle
            = AdHocMultiUserChatSlickFixture.findProtocolProviderBundle(fixture.provider1);

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistency)
        YahooSlickFixture.providerBundle = providerBundle;

        assertNotNull("Couldn't find a bundle for the tested provider"
                      , providerBundle);

        providerBundle.stop();

        assertTrue("Couldn't stop the protocol provider bundle. State was "
                   + providerBundle.getState()
                   ,    Bundle.ACTIVE   != providerBundle.getState()
                     && Bundle.STOPPING != providerBundle.getState());

        providerBundle.uninstall();

        assertEquals("Couldn't stop the protocol provider bundle."
                     , Bundle.UNINSTALLED, providerBundle.getState());

        //verify that the provider is no longer available
        ServiceReference[] yahooProviderRefs = null;
        try
        {
            yahooProviderRefs = AdHocMultiUserChatSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.YAHOO + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ fixture.userID1 + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong: " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for yahoo URI:" + fixture.userID1
                      + "After it was explicitly uninstalled"
                      ,yahooProviderRefs == null || yahooProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The yahoo provider factory kept a reference to the provider we just "
          +"uninstalled (uri="+fixture.userID1+")",
          fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              == null);

        //Now reinstall the bundle
        providerBundle = AdHocMultiUserChatSlickFixture.bc.installBundle(providerBundle.getLocation());

        //set the global providerBundle reference that we will be using
        //in the last series of tests (Account uninstallation persistency)
        YahooSlickFixture.providerBundle = providerBundle;

        assertEquals("Couldn't re-install protocol provider bundle."
                     , Bundle.INSTALLED, providerBundle.getState());

        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(AdHocMultiUserChatSlickFixture.bc,
            providerBundle, ProtocolNames.YAHOO);
        assertEquals("Couldn't re-start protocol provider bundle."
                     , Bundle.ACTIVE, providerBundle.getState());

        //Make sure that the provider is there again.
        //verify that the provider is no longer available
        try
        {
            yahooProviderRefs = AdHocMultiUserChatSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.YAHOO + ")"
                + "(" + ProtocolProviderFactory.USER_ID
                      + "="+ fixture.userID1 + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was not restored after being"
                      +"reinstalled. yahoo URI:" + fixture.userID1
                      ,yahooProviderRefs != null && yahooProviderRefs.length > 0);

        ServiceReference[] yahooFactoryRefs = null;
        try
        {
            yahooFactoryRefs = AdHocMultiUserChatSlickFixture.bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.YAHOO + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //we're the ones who've reinstalled the factory so it's our
        //responsibility to update the fixture.
        fixture.providerFactory
            = (ProtocolProviderFactory)AdHocMultiUserChatSlickFixture.bc.getService(yahooFactoryRefs[0]);
        fixture.provider1
            = (ProtocolProviderService)AdHocMultiUserChatSlickFixture.bc.getService(yahooProviderRefs[0]);


        //verify that the provider is also restored in the provider factory
        //itself
        assertTrue(
          "The yahoo provider did not restore its own reference to the provider "
          +"that we just reinstalled (URI="+fixture.userID1+")",
          !fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              != null);

    }

    /**
     * Uninstalls our test account and makes sure it really has been removed.
     *
     */
    public void testUninstallAccount()
    {
        assertFalse("No installed accounts found",
                    fixture.providerFactory.getRegisteredAccounts().isEmpty());

        assertNotNull(
            "Found no provider corresponding to URI " + fixture.userID1
            ,fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID()));

        assertTrue(
            "Failed to remove a provider corresponding to URI "
            + fixture.userID1
            ,fixture.providerFactory.uninstallAccount(
                fixture.provider1.getAccountID()));
        assertTrue(
            "Failed to remove a provider corresponding to URI "
            + fixture.userID2
            ,fixture.providerFactory.uninstallAccount(
                fixture.provider2.getAccountID()));
        assertTrue(
            "Failed to remove a provider corresponding to URI "
            + fixture.userID3
            ,fixture.providerFactory.uninstallAccount(
                fixture.provider3.getAccountID()));

        //make sure no providers have remained installed.
        ServiceReference[] yahooProviderRefs = null;
        try
        {
            yahooProviderRefs = AdHocMultiUserChatSlickFixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(" + ProtocolProviderFactory.PROTOCOL
                      + "=" +ProtocolNames.YAHOO + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi "
                      + "service for yahoo URI:" + fixture.userID1
                      + "After it was explicitly uninstalled"
                      ,yahooProviderRefs == null || yahooProviderRefs.length == 0);

        //verify that the provider factory knows that we have uninstalled the
        //provider.
        assertTrue(
          "The yahoo provider factory kept a reference to the provider we just "
          +"uninstalled (uri="+fixture.userID1+")",
          fixture.providerFactory.getRegisteredAccounts().isEmpty()
          && fixture.providerFactory.getProviderForAccount(
                fixture.provider1.getAccountID())
              == null);

    }
}
