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
package net.java.sip.communicator.slick.protocol.gibberish;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Contains tests verifying persistence of account uninstallation. In other
 * words we try to make sure that once uninstalled an account remains
 * uninstalled.
 *
 * @author Emil Ivov
 */
public class TestAccountUninstallationPersistence
        extends TestCase
{
    /**
     * Creates a new test instance wrapper around the test with the specified
     * name.
     * @param testName the name of the test that we will be executing.
     */
    public TestAccountUninstallationPersistence(String testName)
    {
        super(testName);
    }

    /**
     * Retrieves a reference to the Gibberish bundle, stops it and uninstalls
     * it and then reinstalls it in order to make sure that accounts are not
     * reloaded once removed.
     *
     * @throws java.lang.Exception if something goes wrong while manipulating
     * the bundles.
     */
    public void testAccountUninstallationPersistence()
        throws Exception
    {
        Bundle providerBundle = GibberishSlickFixture.providerBundle;

        providerBundle.stop();

        assertTrue("Couldn't stop the protocol provider bundle. State was "
                   + providerBundle.getState()
                   ,    Bundle.ACTIVE   != providerBundle.getState()
                     && Bundle.STOPPING != providerBundle.getState());

        providerBundle.uninstall();

        assertEquals("Couldn't stop the protocol provider bundle."
                     , Bundle.UNINSTALLED, providerBundle.getState());

        //Now reinstall the bundle and restart the provider
        providerBundle
            = GibberishSlickFixture.bc.installBundle(providerBundle
                                                        .getLocation());

        assertEquals("Couldn't re-install protocol provider bundle."
                     , Bundle.INSTALLED, providerBundle.getState());

        AccountManagerUtils.startBundleAndWaitStoredAccountsLoaded(
            GibberishSlickFixture.bc, providerBundle, "Gibberish");
        assertEquals("Couldn't re-start protocol provider bundle."
                     , Bundle.ACTIVE, providerBundle.getState());


        //verify that the provider is not reinstalled
        ServiceReference[] gibberishProviderRefs = null;
        try
        {
            gibberishProviderRefs = GibberishSlickFixture.bc
                .getServiceReferences(ProtocolProviderService.class.getName()
                ,"(" + ProtocolProviderFactory.PROTOCOL
                     + "=Gibberish)");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrong " + ex.getMessage());
        }

        //make sure we didn't retrieve a service
        assertTrue("A Gibberish Protocol Provider Service was still regged "
                      +"as an osgi service after it was explicitly uninstalled"
                      ,gibberishProviderRefs == null
                        || gibberishProviderRefs.length == 0);

        //and a nasty hack at the end - delete the configuration file so that
        //we get a fresh start on next run.
        ServiceReference confReference
            = GibberishSlickFixture.bc.getServiceReference(
                ConfigurationService.class.getName());
        ConfigurationService configurationService
            = (ConfigurationService) GibberishSlickFixture
                .bc.getService(confReference);

        configurationService.purgeStoredConfiguration();
    }
}
