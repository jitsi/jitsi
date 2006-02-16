package net.java.sip.communicator.slick.contactlist;

import junit.framework.*;

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

    public TestMetaContactList(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        fixture.setUp();

    }

    protected void tearDown() throws Exception
    {
        fixture.tearDown();
        super.tearDown();
    }

    public void testContactListRetrieving()
    {

    }
}
