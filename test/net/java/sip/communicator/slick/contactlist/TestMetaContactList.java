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
        Logger.getLogger(TestOperationSetPersistentPresence.class);
    
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
     * match those that were in the mock provider.
     */
    public void testContactListRetrieving()
    {
        ContactGroup root 
            = opSetPersPresence.getServerStoredContactListRoot();
    
        logger.debug("============== Predefined contact List ==============");
        
        logger.debug("rootGroup="+root.getGroupName()
                +" rootGroup.childContacts="+root.countContacts()
                + "rootGroup.childGroups="+root.countSubGroups()
                + "Printing rootGroupContents=\n"+root.toString());
    
        MetaContactGroup expectedRoot = fixture.metaClService.getRoot();

        logger.debug("================ Meta Contact List =================");

        logger.debug("rootGroup="+expectedRoot.getGroupName()
                     +" rootGroup.childContacts="+expectedRoot.countChildContacts()
                     + "rootGroup.childGroups="+expectedRoot.countSubgroups()
                     + "Printing rootGroupContents=\n"+expectedRoot.toString());
                
        Iterator groups = root.subGroups();
        while (groups.hasNext() ){
            ContactGroup group = (ContactGroup)groups.next();

            MetaContactGroup expectedGroup
                = expectedRoot
                    .getMetaContactSubgroup(group.getGroupName());

            assertNotNull("Group " + group.getGroupName() + " was returned by "
                          +"the server but was not in the expected contact list."
                          , expectedGroup );

            Iterator contactsIter = group.contacts();
            while (contactsIter.hasNext()){
                String contactID = ((Contact)contactsIter.next()).getAddress();
                MetaContact expectedContact 
                    = expectedGroup.getMetaContact(contactID);
                
                assertNotNull("Contact " + contactID + " was returned by "
                        +"the server but was not in the expected contact list."
                        , expectedContact );
            }
        }
    }
}
