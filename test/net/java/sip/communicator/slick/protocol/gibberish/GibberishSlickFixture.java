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

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Contains fields and methods used by most or all tests in the Gibberish slick.
 *
 * @author Emil Ivov
 */
public class GibberishSlickFixture
    extends TestCase
{
    /**
     * To be set by the slick itself upon activation.
     */
    public static BundleContext bc = null;

    /**
     * An osgi service reference for the protocol provider corresponding to our
     * first testing account.
     */
    public ServiceReference provider1ServiceRef = null;

    /**
      * The protocol provider corresponding to our first testing account.
      */
    public ProtocolProviderService provider1        = null;

    /**
     * The user ID associated with testing account 1.
     */
    public String userID1 = null;

    /**
     * An osgi service reference for the protocol provider corresponding to our
     * second testing account.
     */
    public ServiceReference provider2ServiceRef = null;

    /**
     * The protocol provider corresponding to our first testing account.
     */
    public ProtocolProviderService provider2        = null;

    /**
     * The user ID associated with testing account 2.
     */
    public String userID2 = null;


    /**
     * The tested protocol provider factory.
     */
    public ProtocolProviderFactory providerFactory = null;

    /**
     * A reference to the bundle containing the tested pp implementation. This
     * reference is set during the accoung uninstallation testing and used during
     * the account uninstallation persistence testing.
     */
    public static Bundle providerBundle = null;

    /**
     * Indicates whether the user has requested for onlline tests not to be run.
     * (e.g. due to lack of network connectivity or ... time constraints ;)).
     */
    public static boolean onlineTestingDisabled = false;

    /**
     * A Hashtable containing group names mapped against array lists of buddy
     * screen names. This is a snapshot of the server stored buddy list for
     * the account that is going to be used by the tested implementation.
     * It is filled in by the tester agent who'd login with that account
     * and initialise the ss contact list before the tested implementation has
     * actually done so.
     */
    public static Hashtable<String, List<String>> preInstalledBuddyList  = null;


    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     *
     * @throws java.lang.Exception in case we meet problems while retriving
     * protocol providers through OSGI
     */
    @Override
    public void setUp()
        throws Exception
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "=Gibberish)";
        try{
            serRefs = bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol Gibberish",
            (serRefs != null) && (serRefs.length > 0));

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        userID1 =
            System.getProperty(
                GibberishProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID2 =
            System.getProperty(
                GibberishProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.USER_ID);

        //find the protocol providers exported for the two accounts
        ServiceReference[] gibberishProvider1Refs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"=Gibberish)"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + userID1 +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for Gibberish account1:"
                      + userID1
                      , gibberishProvider1Refs);
        assertTrue("No Protocol Provider was found for Gibberish account1:"
                   + userID1
                   , gibberishProvider1Refs.length > 0);

        ServiceReference[] gibberishProvider2Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"=Gibberish)"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID2 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for Gibberish account2:"
                      + userID2
                      , gibberishProvider2Refs);
        assertTrue("No Protocol Provider was found for Gibberish account2:"
                   + userID2
                   , gibberishProvider2Refs.length > 0);

        //save the service for other tests to use.
        provider1ServiceRef = gibberishProvider1Refs[0];
        provider1 = (ProtocolProviderService)bc.getService(provider1ServiceRef);
        provider2ServiceRef = gibberishProvider2Refs[0];
        provider2 = (ProtocolProviderService)bc.getService(provider2ServiceRef);
    }

    /**
     * Un get service references used in here.
     */
    @Override
    public void tearDown()
    {
        bc.ungetService(provider1ServiceRef);
        bc.ungetService(provider2ServiceRef);
    }

    /**
     * Returns the bundle that has registered the protocol provider service
     * implementation that we're currently testing. The method would go through
     * all bundles currently installed in the framework and return the first
     * one that exports the same protocol provider instance as the one we test
     * in this slick.
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

    public void clearProvidersLists()
        throws Exception
    {
        Map<String, OperationSet> supportedOperationSets1 =
            provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this Gibberish implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence1 =
            (OperationSetPersistentPresence)supportedOperationSets1.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for gibberish.
        if (opSetPersPresence1 == null)
            throw new NullPointerException(
                "An implementation of the Gibberish service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        // lets do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                + "this Gibberish implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence2 =
            (OperationSetPersistentPresence) supportedOperationSets2.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for gibberish.
        if (opSetPersPresence2 == null)
            throw new NullPointerException(
                "An implementation of the Gibberish service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        ContactGroup rootGroup1 = opSetPersPresence1.getServerStoredContactListRoot();

        // first delete the groups
        Vector<ContactGroup> groupsToRemove = new Vector<ContactGroup>();
        Iterator<ContactGroup> iter = rootGroup1.subgroups();
        while (iter.hasNext())
        {
            groupsToRemove.add(iter.next());
        }

        iter = groupsToRemove.iterator();
        while (iter.hasNext())
        {
            ContactGroup item = iter.next();
            opSetPersPresence1.removeServerStoredContactGroup(item);
        }

        //then delete contacts if any in root list
        Vector<Contact> contactsToRemove = new Vector<Contact>();
        Iterator<Contact> iter2 = rootGroup1.contacts();
        while (iter2.hasNext())
        {
            contactsToRemove.add(iter2.next());
        }
        iter2 = contactsToRemove.iterator();
        while (iter2.hasNext())
        {
            opSetPersPresence1.unsubscribe(iter2.next());
        }

        ContactGroup rootGroup2 = opSetPersPresence2.getServerStoredContactListRoot();

        // delete groups
        groupsToRemove = new Vector<ContactGroup>();
        iter = rootGroup2.subgroups();
        while (iter.hasNext())
        {
            groupsToRemove.add(iter.next());
        }

        iter = groupsToRemove.iterator();
        while (iter.hasNext())
        {
            ContactGroup item = iter.next();
            opSetPersPresence2.removeServerStoredContactGroup(item);
        }

        //then delete contacts if any in root list
        contactsToRemove = new Vector<Contact>();
        iter2 = rootGroup2.contacts();
        while (iter2.hasNext())
        {
            contactsToRemove.add(iter2.next());
        }
        iter2 = contactsToRemove.iterator();
        while (iter2.hasNext())
        {
            opSetPersPresence2.unsubscribe(iter2.next());
        }
    }
}
