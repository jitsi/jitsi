/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;
import org.osgi.framework.*;

/**
 * Tests whether accaounts are uninstalled properly. It is important that
 * tests from this class be called last since they will install the accounts
 * that have been used to test the implementations.
 *
 * @author Emil Ivov
 */
public class TestAccountUninstallation
    extends TestCase
{
    IcqSlickFixture fixture = new IcqSlickFixture();

    public TestAccountUninstallation(String name)
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

    /**
     * Uinstalls our test account and makes sure it really has been removed.
     */
    public void testUninstallAccount()
    {
        assertFalse("No installed accaounts found",
                    fixture.accManager.getRegisteredAcounts().isEmpty());

        assertNotNull(
            "Found no provider corresponding to account ID "
            + fixture.icqAccountID,
            fixture.accManager.getProviderForAccount(fixture.icqAccountID));

        assertTrue(
            "Failed to remove a provider corresponding to acc id "
            + fixture.icqAccountID,
            fixture.accManager.uninstallAccount(fixture.icqAccountID));

        ServiceReference[] icqProviderRefs = null;
        try
        {
            icqProviderRefs = fixture.bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                + "(" + AccountManager.PROTOCOL_PROPERTY_NAME + "=" +ProtocolNames.ICQ + ")"
                + "(" + AccountManager.ACCOUNT_ID_PROPERTY_NAME + "="+ fixture.icqAccountID + ")"
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            fail("We apparently got our filter wrhong");
        }

        //make sure we didn't see a service
        assertTrue("A Protocol Provider Service was still regged as an osgi service "
                      +"for ICQ UIN:" + fixture.icqAccountID
                      + "After it was explicitly uninstalled"
                      ,icqProviderRefs == null || icqProviderRefs.length == 0);

        //verify that the provider knows that we have uninstalled the service.
        assertTrue(
            "The ICQ account manager kept a reference to the provider we just "
            +"uninstalled (accID="+fixture.icqAccountID+")",
            fixture.accManager.getRegisteredAcounts().isEmpty()
            && fixture.accManager.getProviderForAccount(fixture.icqAccountID) == null
            );
    }

}
