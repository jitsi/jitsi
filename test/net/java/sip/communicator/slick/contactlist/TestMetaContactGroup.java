/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist;

import junit.framework.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.slick.contactlist.mockprovider.*;
import net.java.sip.communicator.service.protocol.*;
import java.util.*;

/**
 * Tests methods and behaviour of MetaContactGroup implementations.
 *
 * @author Emil Ivov
 */
public class TestMetaContactGroup extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    /**
     * The MetaContactGroup that we're doing the testing aginst.
     */
    MetaContactGroup metaGroup = null;

    /**
     * The mock contact group that we're doing the testing against.
     */
    MockContactGroup mockGroup = null;

    public TestMetaContactGroup(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        OperationSetPersistentPresence opSetPresence
            = (OperationSetPersistentPresence)fixture.mockProvider
                .getSupportedOperationSets().get(
                    OperationSetPersistentPresence.class.getName());

        mockGroup = (MockContactGroup)opSetPresence
                                            .getServerStoredContactListRoot();

        mockGroup = (MockContactGroup)mockGroup
                    .getGroup(MetaContactListServiceLick.THE_NAME_OF_A_GROUP);

        metaGroup = fixture.metaClService.getRoot()
                            .getMetaContactSubgroup(mockGroup.getGroupName());


    }

    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        fixture = null;

        super.tearDown();
    }

    /**
     * Verifies whehter the returned number of contacts corresponds to the
     * children of the corresponding mock contact group.
     */
    public void testCountChildContacts()
    {
        assertEquals("MetaContactGroup.countChildContacts failed"
                    , metaGroup.countChildContacts()
                    , mockGroup.countContacts());
    }

    /**
     * Verifies whehter the returned number of subgroups corresponds to the
     * subgroups of the corresponding mock contact group.
     */
    public void testCountSubgroups()
    {
        assertEquals("MetaContactGroup.countChildContacts failed"
                    , metaGroup.countSubgroups()
                    , mockGroup.countSubgroups());

    }

    /**
     * Goes through the iterator returned by the getChildContacts() method
     * and tries to make sure that it looks sane.
     */
    public void testGetChildContacts()
    {
        Iterator childContactsIter = metaGroup.getChildContacts();

        assertNotNull("getChildContacts() returned a null iterator."
                      , childContactsIter);

        assertTrue("getChildContacts() returned an empty iterator."
                   , childContactsIter.hasNext());

        //i don't think we could test anything else here without becoming
        //redundant with TestMetaContactList
    }

    /**
     * Tests getContactGroup()
     */
    public void testGetContactGroup()
    {
        ContactGroup actualReturn = metaGroup.getContactGroup(
                                mockGroup.getGroupName(), fixture.mockProvider);

        assertNotNull("getContactGroup() return null.", actualReturn);

        assertSame("getContactGroup() did not return the right proto group."
                   , actualReturn, mockGroup);
    }

    /**
     * Tests getContactGroups()
     */
    public void testGetContactGroups()
    {
        Iterator contactGroups = metaGroup.getContactGroups();

        assertNotNull("contact groups iterator", contactGroups);

        assertTrue("The contact groups iterator was empty."
                   , contactGroups.hasNext());

        MockContactGroup actualMockGroup
            = (MockContactGroup)contactGroups.next();

        assertSame("Iterator did not contain the right contact group"
                   , mockGroup
                   , actualMockGroup);
    }

    /**
     * Tests testGetContactGroupsForProvider()
     */
    public void testGetContactGroupsForProvider()
    {
        Iterator contactGroups = metaGroup.getContactGroups();

        assertNotNull("contact groups for provider iterator", contactGroups);

        assertTrue("The contact groups iterator was empty for a mock provider."
                   , contactGroups.hasNext());

        MockContactGroup actualMockGroup
            = (MockContactGroup)contactGroups.next();

        assertSame("A prov. iterator did not contain the right contact group"
                   , mockGroup
                   , actualMockGroup);

    }

    /**
     * Checks whether the name of the meta group has been properly initialized.
     */
    public void testGetGroupName()
    {
        assertEquals("grp: " + metaGroup + " had the wrong name."
                     , MetaContactListServiceLick.THE_NAME_OF_A_GROUP
                     , metaGroup.getGroupName());
    }

    /**
     * Verifies whether getMetaContact() returns proper results.
     */
    public void testGetMetaContact()
    {
        //firt obtain references to 2 contacts.
        MetaContact firstContact = metaGroup.getMetaContact(0);
        MetaContact lastContact = metaGroup.getMetaContact(
                                            metaGroup.countChildContacts() - 1);

        //make sure that what we just got is not null.
        assertNotNull("getMetaContact(int) returned null for contact 0"
                      , firstContact);

        assertNotNull("getMetaContact(int) returned null for its last contact "
                      + (metaGroup.countChildContacts() - 1)
                      , lastContact);

        //I don't see what else we could add here without copying code from
        //test meta contact
    }

    /**
     * Tests the method.
     */
    public void testGetMetaContact2()
    {
        //firt obtain a reference to a contact through iteration.
        Iterator childContactsIter = metaGroup.getChildContacts();

        //make sure the returned ref is ok.
        assertNotNull("getChildContacts() returned a null iterator."
                      , childContactsIter);

        assertTrue("getChildContacts() returned an empty iterator."
                   , childContactsIter.hasNext());

        MetaContact expectedChild = (MetaContact)childContactsIter.next();

        MetaContact actualChild = metaGroup.getMetaContact(
                                                expectedChild.getMetaUID());

        assertSame("getMetaContact(metaUID) did not return as expected."
                   , expectedChild, actualChild);
    }

    /**
     * test.
     */
    public void testGetMetaContactSubgroup()
    {
        //firt obtain references to a group.
        MetaContactGroup actualGroup = metaGroup.getMetaContactSubgroup(0);

        //make sure that what we just got is not null.
        assertNotNull("getMetaContact(int) returned null for group 0"
                      , actualGroup);

        //check whether this
        assertNotNull("The returned group does not appear to really exist"
                     ,mockGroup.getGroup(actualGroup.getGroupName()));
        assertNotNull("Group encapsulated in the returned group did not match"
                    , mockGroup.getGroup(((MockContactGroup)actualGroup
                        .getContactGroups().next()).getGroupName()));
    }

    /**
     * test.
     */
    public void testGetMetaContactSubgroup2()
    {
        //firt obtain references to a group.
        MetaContactGroup actualGroup = metaGroup.getMetaContactSubgroup(
            ((MetaContactGroup)metaGroup.getSubgroups().next()).getGroupName());

        //make sure that what we just got is not null.
        assertNotNull("getMetaContact(String) returned null for group 0"
                      , actualGroup);

    }

    public void testGetSubgroups()
    {
        Iterator subgroupsIter = metaGroup.getSubgroups();

        assertNotNull("getSubgroup() returned a null iterator."
                      , subgroupsIter);

        assertTrue("getSubgroups() returned an empty iterator."
                   , subgroupsIter.hasNext());

        //i don't think we could test anything else here without becoming
        //redundant with TestMetaContactList

    }
}
