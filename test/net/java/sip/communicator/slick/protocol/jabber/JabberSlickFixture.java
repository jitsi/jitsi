/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.jabber;

import java.util.*;

import org.osgi.framework.*;

import junit.framework.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Contains fields and methods used by most or all tests in the Jabber slick.
 *
 * @author Damian Minkov
 */
public class JabberSlickFixture
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
     * The name of the chat room that we are using for testing of multi user
     * chatting.
     */
    public String chatRoomName = null;



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
     * and initialize the ss contact list before the tested implementation has
     * actually done so.
     */
    public static Hashtable<String, List<String>> preInstalledBuddyList  = null;


    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     *
     * @throws java.lang.Exception in case we meet problems while retrieving
     * protocol providers through OSGI
     */
    public void setUp()
        throws Exception
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.JABBER+")";
        try{
            serRefs = bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol Jabber",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        userID1
            = System.getProperty(
                JabberProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID2
           = System.getProperty(
                JabberProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.USER_ID);

        chatRoomName
            = System.getProperty(
                JabberProtocolProviderServiceLick.CHAT_ROOM_NAME);


        //find the protocol providers exported for the two accounts
        ServiceReference[] jabberProvider1Refs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.JABBER+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + userID1 +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for Jabber account1:"
                      + userID1
                      , jabberProvider1Refs);
        assertTrue("No Protocol Provider was found for Jabber account1:"+ userID1,
                     jabberProvider1Refs.length > 0);

        ServiceReference[] jabberProvider2Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.JABBER+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID2 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for Jabber account2:"
                      + userID2
                      , jabberProvider2Refs);
        assertTrue("No Protocol Provider was found for Jabber account2:"+ userID2,
                     jabberProvider2Refs.length > 0);

        //save the service for other tests to use.
        provider1ServiceRef = jabberProvider1Refs[0];
        provider1 = (ProtocolProviderService)bc.getService(provider1ServiceRef);
        provider2ServiceRef = jabberProvider2Refs[0];
        provider2 = (ProtocolProviderService)bc.getService(provider2ServiceRef);
    }

    /**
     * Un get service references used in here.
     */
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
                +"this Jabber implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence1 =
            (OperationSetPersistentPresence)supportedOperationSets1.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for jabber.
        if (opSetPersPresence1 == null)
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");

        // lets do it once again for the second provider
        Map<String, OperationSet> supportedOperationSets2 =
            provider2.getSupportedOperationSets();

        if (supportedOperationSets2 == null
            || supportedOperationSets2.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                + "this Jabber implementation. ");

        //get the operation set presence here.
        OperationSetPersistentPresence opSetPersPresence2 =
            (OperationSetPersistentPresence) supportedOperationSets2.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for jabber.
        if (opSetPersPresence2 == null)
            throw new NullPointerException(
                "An implementation of the Jabber service must provide an "
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
