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
 * @todo comment
 * @author Emil Ivov
 */
public class TestMetaContact extends TestCase
{
    /**
     * A reference to the SLICK fixture.
     */
    MclSlickFixture fixture = new MclSlickFixture(getClass().getName());

    /**
     * The MetaContact that we're doing the testing aginst.
     */
    MetaContact metaContact = null;

    /**
     * The mock contact that we're doing the testing against.
     */
    MockContact mockContact = null;


    public TestMetaContact(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        mockContact = MetaContactListServiceLick.subLevelContact;

        metaContact = fixture.metaClService.findMetaContactByContact(
                                                                mockContact);

    }

    @Override
    protected void tearDown() throws Exception
    {
        fixture.tearDown();

        fixture = null;
        super.tearDown();

    }

    /**
     * Tests getContact()
     */
    public void testGetContact()
    {
        Contact actualReturn
            = metaContact.getContact(
                    mockContact.getAddress(),
                    MclSlickFixture.mockProvider);

        assertNotNull("getContact() return null.", actualReturn);

        assertSame("getContact() did not return the right proto group."
                   , mockContact, actualReturn);

    }

    /**
     * Test getContactCount()
     */
    public void testGetContactCount()
    {
        //we only have mock provider registered so the count should be one.
        assertEquals("getContactCount()", 1, metaContact.getContactCount());
    }

    /**
     * Test getContacts()
     */
    public void testGetContacts()
    {
        Iterator<Contact> childContacts = metaContact.getContacts();

        assertNotNull("getContacts() returned a null iterator."
                      , childContacts);

        assertTrue("getContacts() returned an empty iterator."
                   , childContacts.hasNext());

        assertSame("The iterator returned by getContacts() ("
                   + mockContact.getAddress()
                   +")did not contain the "
                   +"right mock contact"
                   , mockContact, childContacts.next());
    }

    /**
     * Test getContactsForProvider
     */
    public void testGetContactsForProvider()
    {
        Iterator<Contact> childContacts
            = metaContact.getContactsForProvider(MclSlickFixture.mockProvider);

        assertNotNull("getContactsForProvider() returned a null iterator."
                      , childContacts);

        assertTrue("getContactsForProvider() returned an empty iterator."
                   , childContacts.hasNext());

        assertSame("The iterator returned by getContactsForProvider() ("
                   + mockContact.getAddress()
                   +")did not contain the "
                   +"right mock contact"
                   , mockContact, childContacts.next());
    }

    /**
     * Tests that getDefaultContact() returns the contact that is currently the
     * best choice for communication with the tested meta contact.
     */
    public void testGetDefaultContact()
    {
        Contact actualReturn = metaContact.getDefaultContact();

        assertNotNull("getDefaultContact() return null.", actualReturn);

        assertSame("getDefaultContact() did not return the right proto group."
                   , actualReturn, mockContact);
    }

    /**
     * Checks whether the display name matches the one in th mock contact.
     */
    public void testGetDisplayName()
    {
        assertEquals("getDisplayName()",
                     mockContact.getDisplayName(),
                     metaContact.getDisplayName());
    }

    /**
     * Very light test of the existance and the uniqueness of meta UIDs
     */
    public void testGetMetaUID()
    {
        String metaUID = metaContact.getMetaUID();
        assertNotNull( "getMetaUID() did not seem to return a valid UID"
                       , metaUID);

        assertTrue( "getMetaUID() did not seem to return a valid UID"
                       , metaUID.trim().length() > 0);
    }

    /**
     * Verifies whether the compare method in meta contacts takes into account
     * all important details: i.e. contact status, alphabetical order.
     */
    public void testCompareTo()
    {
        verifyCompareToForAllContactsInGroupAndSubgroups(
                fixture.metaClService.getRoot());
    }

    /**
     * compare all neighbour contacts in <tt>group</tt> and its subgroups and
     * try to determine whether they'reproperly ordered.
     *
     * @param group the <tt>MetaContactGroup</tt> to walk through
     */
    public void verifyCompareToForAllContactsInGroupAndSubgroups(
                                MetaContactGroup group)
    {
        //first check order of contacts in this group
        Iterator<MetaContact> contacts = group.getChildContacts();

        MetaContact previousContact = null;
        int previousContactIsOnlineStatus = 0;

        while(contacts.hasNext())
        {
            MetaContact currentContact  = contacts.next();

            //calculate the total status for this contact
            Iterator<Contact> protoContacts = currentContact.getContacts();
            int currentContactIsOnlineStatus = 0;

            while(protoContacts.hasNext())
            {
                if (protoContacts.next().getPresenceStatus().isOnline())
                {
                    currentContactIsOnlineStatus = 1;
                }
            }

            if (previousContact != null)
            {
                assertTrue( previousContact + " with status="
                        + previousContactIsOnlineStatus
                        + " was wrongfully before "
                        + currentContact+ " with status="
                        + currentContactIsOnlineStatus
                        , previousContactIsOnlineStatus >= currentContactIsOnlineStatus);

                //if both were equal then assert alphabetical order.
                if (previousContactIsOnlineStatus == currentContactIsOnlineStatus)
                    assertTrue( "The display name: "
                               + previousContact.getDisplayName()
                               + " should be considered less than "
                               + currentContact.getDisplayName()
                               ,previousContact.getDisplayName()
                                    .compareToIgnoreCase(
                                        currentContact.getDisplayName())
                               <= 0);
            }
            previousContact = currentContact;
            previousContactIsOnlineStatus = currentContactIsOnlineStatus;
        }

        //now go over the subgroups
        Iterator<MetaContactGroup> subgroups = group.getSubgroups();

        while(subgroups.hasNext())
        {
            verifyCompareToForAllContactsInGroupAndSubgroups(
                    subgroups.next());
        }
    }

    /**
     * Test creating, changing and removing metacontact details.
     */
    public void testDetails()
    {
        String name = "test_detail_name";
        String detail_1 = "detail_1";
        String detail_2 = "detail_2";
        String detail_3 = "detail_3";

        metaContact.addDetail(name, detail_1);
        List<String> ds = metaContact.getDetails(name);
        assertTrue( "Must contain one detail",
                       1 == ds.size());
        assertTrue("The result details does not contain the desired",
                ds.contains(detail_1));

        metaContact.changeDetail(name, detail_1, detail_2);
        ds = metaContact.getDetails(name);
        assertEquals( "Must contain one detail",
                       1 , ds.size());
        assertTrue("The result details does not contain the desired",
                ds.contains(detail_2));

        metaContact.removeDetail(name, detail_2);
        ds = metaContact.getDetails(name);
        assertEquals( "Must contain no details",
                       0 , ds.size());

        metaContact.addDetail(name, detail_1);
        metaContact.addDetail(name, detail_2);
        metaContact.addDetail(name, detail_3);
        ds = metaContact.getDetails(name);
        assertEquals( "Must contain three detail",
                       3 , ds.size());
        assertTrue("The result details does not contain the desired",
                ds.contains(detail_1));
        assertTrue("The result details does not contain the desired",
                ds.contains(detail_2));
        assertTrue("The result details does not contain the desired",
                ds.contains(detail_3));

        metaContact.removeDetails(name);
        ds = metaContact.getDetails(name);
        assertEquals( "Must contain no details",
                       0 , ds.size());
    }
}
