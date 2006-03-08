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

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

        mockContact = MetaContactListServiceLick.THE_CONTACT;

        metaContact = fixture.metaClService.findMetaContactByContact(
                                                                mockContact);

    }

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
        Contact actualReturn = metaContact.getContact(
                                mockContact.getAddress(), fixture.mockProvider);

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
        Iterator childContacts = metaContact.getContacts();

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
        Iterator childContacts = metaContact.getContactsForProvider(
                                                        fixture.mockProvider);

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

    public void testGetMetaUID()
    {
        String metaUID = metaContact.getMetaUID();
        assertNotNull( "getMetaUID() did not seem to return a valid UID"
                       , metaUID);

        assertTrue( "getMetaUID() did not seem to return a valid UID"
                       , metaUID.trim().length() > 0);
    }
}
