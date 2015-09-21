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
package net.java.sip.communicator.slick.contactlist;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.impl.protocol.mock.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

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

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        OperationSetPersistentPresence opSetPresence
            = MclSlickFixture.mockProvider.getOperationSet(
                    OperationSetPersistentPresence.class);

        mockGroup = (MockContactGroup)opSetPresence
                                            .getServerStoredContactListRoot();

        mockGroup = (MockContactGroup)mockGroup
                    .getGroup(MetaContactListServiceLick.topLevelGroupName);

        metaGroup = fixture.metaClService.getRoot()
                            .getMetaContactSubgroup(mockGroup.getGroupName());


    }

    @Override
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
        Iterator<MetaContact> childContactsIter = metaGroup.getChildContacts();

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
        ContactGroup actualReturn
            = metaGroup.getContactGroup(
                    mockGroup.getGroupName(),
                    MclSlickFixture.mockProvider);

        assertNotNull("getContactGroup() return null.", actualReturn);

        assertSame("getContactGroup() did not return the right proto group."
                   , actualReturn, mockGroup);
    }

    /**
     * Tests getContactGroups()
     */
    public void testGetContactGroups()
    {
        Iterator<ContactGroup> contactGroups = metaGroup.getContactGroups();

        assertNotNull("contact groups iterator", contactGroups);

        assertTrue("The contact groups iterator was empty."
                   , contactGroups.hasNext());

        ContactGroup actualMockGroup = contactGroups.next();

        assertSame("Iterator did not contain the right contact group"
                   , mockGroup
                   , actualMockGroup);
    }

    /**
     * Tests testGetContactGroupsForProvider()
     */
    public void testGetContactGroupsForProvider()
    {
        Iterator<ContactGroup> contactGroups = metaGroup.getContactGroups();

        assertNotNull("contact groups for provider iterator", contactGroups);

        assertTrue("The contact groups iterator was empty for a mock provider."
                   , contactGroups.hasNext());

        ContactGroup actualMockGroup = contactGroups.next();

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
                     , MetaContactListServiceLick.topLevelGroupName
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
        Iterator<MetaContact> childContactsIter = metaGroup.getChildContacts();

        //make sure the returned ref is ok.
        assertNotNull("getChildContacts() returned a null iterator."
                      , childContactsIter);

        assertTrue("getChildContacts() returned an empty iterator."
                   , childContactsIter.hasNext());

        MetaContact expectedChild = childContactsIter.next();

        MetaContact actualChild = metaGroup.getMetaContact(
                                                expectedChild.getMetaUID());

        assertSame("getMetaContact(metaUID) did not return as expected."
                   , expectedChild, actualChild);
    }

    /**
     * Verifies whether getMetaContact(string, provider) returns proper results.
     */
    public void testGetMetaContact3()
    {
        MetaContactGroup metaContactGroup1 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);

        MetaContact metaContact
            = metaContactGroup1.getMetaContact(
                    MclSlickFixture.mockProvider,
                    MetaContactListServiceLick.subLevelContactName);

        //do as best as we can to determine whether this is the right meta
        //contact
        assertNotNull(
                "getMetaCont(prov, contactID) returned a MetaC that didn't "
                    + "contain our contact",
                metaContact.getContact(
                        MetaContactListServiceLick.subLevelContactName,
                        MclSlickFixture.mockProvider));

        assertEquals(
            "getMetaCont(prov, contactID) returned a MetaC with a wrong name "
            , metaContact.getDisplayName()
            , MetaContactListServiceLick.subLevelContactName);
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
                    , mockGroup.getGroup(actualGroup
                        .getContactGroups().next().getGroupName()));
    }

    /**
     * test.
     */
    public void testGetMetaContactSubgroup2()
    {
        //firt obtain references to a group.
        MetaContactGroup actualGroup = metaGroup.getMetaContactSubgroup(
            (metaGroup.getSubgroups().next()).getGroupName());

        //make sure that what we just got is not null.
        assertNotNull("getMetaContact(String) returned null for group 0"
                      , actualGroup);
    }

    /**
     * Tests MetaContact.contains(MetaContact)
     */
    public void testContains()
    {
        MetaContactGroup metaContactGroup1 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);

        MetaContact metaContact = fixture.metaClService.findMetaContactByContact(
            MetaContactListServiceLick.subLevelContact);

        assertTrue(metaContactGroup1.contains(metaContact));
    }

    /**
     * Tests MetaContact.contains(MetaContact)
     */
    public void testContains2()
    {
        MetaContactGroup metaContactGroup1 = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);

        MetaContactGroup metaContactGroup2 = metaContactGroup1
            .getMetaContactSubgroup(MetaContactListServiceLick
                                        .subLevelGroup.getGroupName());

        assertTrue(metaContactGroup1.contains(metaContactGroup2));

    }

    /**
     * Tests MetaContactGroup.getSubgroups();
     */
    public void testGetSubgroups()
    {
        Iterator<MetaContactGroup> subgroupsIter = metaGroup.getSubgroups();

        assertNotNull("getSubgroup() returned a null iterator."
                      , subgroupsIter);

        assertTrue("getSubgroups() returned an empty iterator."
                   , subgroupsIter.hasNext());

        //i don't think we could test anything else here without becoming
        //redundant with TestMetaContactList
    }

    /**
     * Goes over the contacts in one of the groups and verifies that indexOf
     * returns properly for every one of them.
     */
    public void testIndexOf1()
    {
        MetaContactGroup metaContactGroup = fixture.metaClService
            .findMetaContactGroupByContactGroup(MetaContactListServiceLick
                                      .topLevelMockGroup);
        for ( int i = 0; i < metaContactGroup.countChildContacts(); i++)
        {
            MetaContact currentMetaContact = metaContactGroup.getMetaContact(i);

            assertEquals("indexOf failed for " + currentMetaContact
                         , i, metaContactGroup.indexOf(currentMetaContact));
        }
    }

    /**
     * Goes over the subgroups in one the root group and verifies that indexOf
     * returns properly for every one of them
     */
    public void testIndexOf2()
    {
        MetaContactGroup metaContactGroup = fixture.metaClService.getRoot();
        for ( int i = 0; i < metaContactGroup.countSubgroups(); i++)
        {
            MetaContactGroup currentMetaContactGroup
                = metaContactGroup.getMetaContactSubgroup(i);

            assertEquals("indexOf failed for " + currentMetaContactGroup
                         , i, metaContactGroup.indexOf(currentMetaContactGroup));
        }

    }
}
