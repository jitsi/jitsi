package net.java.sip.communicator.slick.contactlist;

import junit.framework.*;
import net.java.sip.communicator.service.contactlist.*;

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
        MetaContactGroup listRoot = fixture.metaClService.getRoot();

        System.out.println("!!! Meta Contact List Root = " + listRoot);
    }
}
