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
package net.java.sip.communicator.slick.protocol.generic;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 * Generic Slick fixture for ad-hoc multi-user chat.
 *
 * @author Valentin Martinet
 */
public abstract class AdHocMultiUserChatSlickFixture extends TestCase
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
    public ProtocolProviderService provider1 = null;

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
     * The protocol provider corresponding to our second testing account.
     */
    public ProtocolProviderService provider2 = null;

    /**
     * The user ID associated with testing account 2.
     */
    public String userID2 = null;

    /**
     * An osgi service reference for the protocol provider corresponding to our
     * third testing account.
     */
    public ServiceReference provider3ServiceRef = null;

    /**
     * The protocol provider corresponding to our third testing account.
     */
    public ProtocolProviderService provider3 = null;

    /**
     * The user ID associated with testing account 3.
     */
    public String userID3 = null;

    /**
     * The tested protocol provider factory.
     */
    public ProtocolProviderFactory providerFactory = null;

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
     * and initialize the ss contact list before the tested implementation has
     * actually done so.
     */
    public static Hashtable<String, List<String>> preInstalledBuddyList  = null;

    /**
     * A reference to the bundle containing the tested pp implementation. This
     * reference is set during the accoung uninstallation testing and used during
     * the account uninstallation persistence testing.
     */
    public static Bundle providerBundle = null;

    /**
     * Constructor
     */
    public AdHocMultiUserChatSlickFixture()
    {
        super();
    }

    /**
     *
     *
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws OperationFailedException
     */
    public void clearProvidersLists()
    throws IllegalArgumentException,
    IllegalStateException,
    OperationFailedException
    {
        Map<String, OperationSet> supportedOperationSets1 =
            provider1.getSupportedOperationSets();

        if ( supportedOperationSets1 == null
            || supportedOperationSets1.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this msn implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence1 =
            (OperationSetPersistentPresence)supportedOperationSets1.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for msn.
        if (opSetPersPresence1 == null)
            throw new NullPointerException(
                "An implementation of the msn service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        // lets do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                + "this msn implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence2 =
            (OperationSetPersistentPresence) supportedOperationSets2.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for msn.
        if (opSetPersPresence2 == null)
            throw new NullPointerException(
                "An implementation of the msn service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        // lets do it once again for the third provider
        Map<String, OperationSet> supportedOperationSets3 =
            provider3.getSupportedOperationSets();

        if (supportedOperationSets3 == null
            || supportedOperationSets3.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                + "this msn implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence3 =
            (OperationSetPersistentPresence) supportedOperationSets3.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for msn.
        if (opSetPersPresence3 == null)
            throw new NullPointerException(
                "An implementation of the msn service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        deleteGroups(opSetPersPresence1);
        deleteGroups(opSetPersPresence2);
        deleteGroups(opSetPersPresence3);
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

    /**
     * Delete all groups and contacts for the given persistent presence op. set.
     *
     * @param opSetPersPresence
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws OperationFailedException
     */
    public void deleteGroups(OperationSetPersistentPresence opSetPersPresence)
    throws IllegalArgumentException, IllegalStateException,
    OperationFailedException
    {
        ContactGroup rootGroup =
            opSetPersPresence.getServerStoredContactListRoot();

        // first delete the groups
        Vector<ContactGroup> groupsToRemove = new Vector<ContactGroup>();
        Iterator<ContactGroup> iter = rootGroup.subgroups();
        while (iter.hasNext())
        {
            groupsToRemove.add(iter.next());
        }

        iter = groupsToRemove.iterator();
        while (iter.hasNext())
        {
            ContactGroup item = iter.next();
            opSetPersPresence.removeServerStoredContactGroup(item);
        }

        //then delete contacts if any in root list
        Vector<Contact> contactsToRemove = new Vector<Contact>();
        Iterator<Contact> iter2 = rootGroup.contacts();
        while (iter2.hasNext())
        {
            contactsToRemove.add(iter2.next());
        }
        iter2 = contactsToRemove.iterator();
        while (iter2.hasNext())
        {
            opSetPersPresence.unsubscribe(iter2.next());
        }
    }

    /**
     * JUnit setUp
     */
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * JUnit tearDown method.
     * Unget service references used in here.
     */
    @Override
    public void tearDown() throws Exception
    {
        bc.ungetService(provider1ServiceRef);
        bc.ungetService(provider2ServiceRef);
        bc.ungetService(provider3ServiceRef);
    }

}
