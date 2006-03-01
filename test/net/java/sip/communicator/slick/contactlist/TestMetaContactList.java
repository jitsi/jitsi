package net.java.sip.communicator.slick.contactlist;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactGroup;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.slick.contactlist.mockprovider.MockContactGroup;
import net.java.sip.communicator.slick.protocol.icq.TestOperationSetPersistentPresence;
import net.java.sip.communicator.util.Logger;

/**
 * Test basic meta contact list functionality such as filling in the contact
 * list from existing protocol provider implementations and others.
 * @author Emil Ivov
 */
public class TestMetaContactList
    extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    private static final Logger logger =
        Logger.getLogger(TestMetaContactList.class);

    private OperationSetPersistentPresence opSetPersPresence;

    /**
     * Creates a unit test with the specified name.
     * @param name the name of one of the test methods in this class.
     */
    public TestMetaContactList(String name)
    {
        super(name);
    }

    /**
     * Initialize the environment.
     * @throws Exception if anything goes wrong.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        Map supportedOperationSets =
            MclSlickFixture.mockProvider.getSupportedOperationSets();

        if ( supportedOperationSets == null
            || supportedOperationSets.size() < 1)
            throw new NullPointerException(
                "No OperationSet implementations are supported by "
                +"this ICQ implementation. ");

        //get the operation set presence here.
        opSetPersPresence =
            (OperationSetPersistentPresence)supportedOperationSets.get(
                OperationSetPersistentPresence.class.getName());

        //if still null then the implementation doesn't offer a presence
        //operation set which is unacceptable for icq.
        if (opSetPersPresence == null)
            throw new NullPointerException(
                "An implementation of the ICQ service must provide an "
                + "implementation of at least the one of the Presence "
                + "Operation Sets");
    }

    /**
     * Finalization
     * @throws Exception in case sth goes wrong.
     */
    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    /**
     * Verifies that the contacts retrieved by the meta contact list service,
     * matches the one that were in the mock provider.
     */
    public void testContactListRetrieving()
    {
        ContactGroup expectedRoot
            = opSetPersPresence.getServerStoredContactListRoot();

        logger.debug("============== Predefined contact List ==============");

        logger.debug("rootGroup="+expectedRoot.getGroupName()
                +" rootGroup.childContacts="+expectedRoot.countContacts()
                + "rootGroup.childGroups="+expectedRoot.countSubGroups()
                + " Printing rootGroupContents=\n"+expectedRoot.toString());

        MetaContactGroup actualRoot = fixture.metaClService.getRoot();

        logger.debug("================ Meta Contact List =================");

        logger.debug("rootGroup="+actualRoot.getGroupName()
                     +" rootGroup.childContacts="+actualRoot.countChildContacts()
                     + " rootGroup.childGroups="+actualRoot.countSubgroups()
                     + " Printing rootGroupContents=\n"+actualRoot.toString());

        Iterator expectedSubgroups = expectedRoot.subGroups();

        //loop over mock groups and check whether they've all been added
        //to the meta contact list.
        while (expectedSubgroups.hasNext() ){
            ContactGroup expectedGroup = (ContactGroup)expectedSubgroups.next();

            MetaContactGroup actualGroup
                = actualRoot
                    .getMetaContactSubgroup(expectedGroup.getGroupName());

            assertNotNull("Group " + expectedGroup.getGroupName() + " was "
                      + "returned by the MetaContactListService implementation "
                      + "but was not in the expected contact list."
                      , actualGroup );

            assertEquals("Group " + expectedGroup.getGroupName()
                       + " did not have the expected number of member contacts"
                       , expectedGroup.countContacts()
                       , actualGroup.countChildContacts() );

            assertEquals("Group " + expectedGroup.getGroupName()
                       + " did not have the expected number of member contacts"
                       , expectedGroup.countContacts()
                       , actualGroup.countChildContacts() );
            assertEquals("Group " + expectedGroup.getGroupName()
                       + " did not have the expected number of sub groups"
                       , expectedGroup.countSubGroups()
                       , actualGroup.countSubgroups() );

            Iterator actualContactsIter = actualGroup.getChildContacts();

            //check whether every contact in the meta list exists in the source
            //mock provider contact list.
            while (actualContactsIter.hasNext()){
                MetaContact actualMetaContact
                    = (MetaContact)actualContactsIter.next();

                Contact actualProtoContact
                    = actualMetaContact.getContactForProvider(
                                            MclSlickFixture.mockProvider);

                Contact expectedProtoContact
                    = expectedGroup.getContact(actualProtoContact.getAddress());

                assertNotNull("Contact " + actualMetaContact.getDisplayName()
                          + " was returned by "
                          + "the MetaContactListService implementation but was "
                          + "not in the expected contact list."
                          , expectedProtoContact );
            }
        }
    }
}
