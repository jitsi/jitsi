/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.rss;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Contains constants and methods used by almost all of our tests.
 *
 * @author Mihai Balan
 */
public class RssSlickFixture
    extends TestCase
{
    /**
     * To be set by the slick itself upon activation.
     */
    public static BundleContext bc = null;

    /**
     * And OSGi service reference for the protocol provider corresponding to our
     * testing account.
     */
    public ServiceReference providerServiceReference = null;

    /**
     * Protocol provider corresponding to the testing account.
     */
    public ProtocolProviderService provider = null;

    /**
     * The user ID associated with the testing account.
     */
    //public String userId = null;

    public ProtocolProviderFactory providerFactory = null;

    /**
     * A reference to the bundle containing the tested ProtocolProvider
     * implementation. This reference is set during the account installation
     * testing and used during the account installation persistence testing.
     */
    public static Bundle providerBundle = null;

    /**
     * A <code>HashTable</code> containing group names mapped against array
     * lists of buddy screen names.
     */
    public static Hashtable<Object, Object> preInstalledBuddyList = null;
    //XXX: Do I really need that? :-\

    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     *
     * @throws java.lang.Exception in case we meet problems while retriving
     * protocol providers through OSGI
     */
    public void setUp()
        throws Exception
    {
        //get a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.RSS + ")";

        try {
            serRefs = bc.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        } catch (InvalidSyntaxException ise)
        {
            //shouldn't happen as the filter is static (typos maybe? :D)
            fail(osgiFilter + "is not a valid OSGi filter");
        }

        assertTrue("Failed to find a provider factory service for protocol RSS",
            serRefs != null && serRefs.length > 0);

        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        //find the protocol providers exported for the two accounts
        ServiceReference[] rssProviderRefs = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.RSS+"))");

        //make sure we found a service
        assertNotNull("No protocol provider was found for RSS account ",
            rssProviderRefs);
        assertTrue("No protocol provider was found for RSS account",
            rssProviderRefs.length > 0);

        //save the service for other tests to use.
        providerServiceReference = rssProviderRefs[0];
        provider = (ProtocolProviderService)bc.getService(
            providerServiceReference);
    }

    /**
     * Un get service references used in here.
     */
    public void tearDown()
    {
        bc.ungetService(providerServiceReference);
    }

    /**
     * Returns the bundle that has registered the protocol provider service
     * implementation that we're currently testing. The method would go through
     * all bundles currently installed in the framework and return the first
     * one that exports the same protocol provider instance as the one we test
     * in this slick.
     *
     * @param provider the provider whose bundle we're looking for.
     * @return the Bundle that has registered the protocol provider service
     * we're testing in the slick.
     */
    public static Bundle findProtocolProviderBundle(
        ProtocolProviderService provider)
    {
        Bundle[] bundles = bc.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            ServiceReference[] registeredServices
                = bundles[i].getRegisteredServices();

            if (registeredServices == null)
                continue;

            for (int j = 0; j < registeredServices.length; j++)
            {
                Object service
                    = bc.getService(registeredServices[j]);
                if (service == provider)
                    return bundles[i];
            }
        }

        return null;
    }

    public void clearProvidersList()
        throws Exception
    {
    //XXX: FTM, do_nothing()
    }
}
