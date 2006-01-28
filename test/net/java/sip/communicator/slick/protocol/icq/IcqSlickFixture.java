package net.java.sip.communicator.slick.protocol.icq;

import net.java.sip.communicator.service.protocol.*;
import org.osgi.framework.*;
import junit.framework.*;
import java.util.*;

/**
 * Provides utility code, such as locating and obtaining references towards
 * base services that anyother service would need.
 *
 * @author Emil Ivov
 */
public class IcqSlickFixture extends TestCase
{
    /**
     * To be set by the slick itself upon activation.
     */
    public static BundleContext bc = null;

    /**
     * The tested account id obtained during installation.
     */
    public static AccountID icqAccountID = null;

    /**
     * The agent that we use to verify whether the tested implementation is
     * being honest with us. The icq tester agent is instantiated and registered
     * by the icq slick activator.
     */
    static IcqTesterAgent testerAgent = null;

    /**
     * A Hashtable containing group names mapped against array lists of buddy
     * screen names. This is a snapshot of the server stored buddy list for
     * the icq account that is going to be used by the tested implementation.
     * It is filled in by the icq tester agent who'd login with that account
     * and initialise the ss contact list before the tested implementation has
     * actually done so.
     */
    public static Hashtable preInstalledBuddyList = null;

    public ServiceReference        icqServiceRef = null;
    public ProtocolProviderService provider      = null;
    public AccountManager          accManager    = null;
    public String                  ourAccountID  = null;

    public void setUp() throws Exception
    {
        // first obtain a reference to the account manager
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + AccountManager.PROTOCOL_PROPERTY_NAME
                            + "="+ProtocolNames.ICQ+")";
        try{
            serRefs = IcqSlickFixture.bc.getServiceReferences(
                    AccountManager.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find an account manager service for protocol ICQ",
            serRefs != null || serRefs.length >  0);

        //Keep the reference for later usage.
        accManager = (AccountManager)
            IcqSlickFixture.bc.getService(serRefs[0]);

        ourAccountID =
            System.getProperty(
                IcqProtocolProviderSlick.TESTED_IMPL_ACCOUNT_ID_PROP_NAME);


        //find the protocol provider service
        ServiceReference[] icqProviderRefs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+AccountManager.PROTOCOL_PROPERTY_NAME+"="+ProtocolNames.ICQ+")"
                +"("+AccountManager.ACCOUNT_ID_PROPERTY_NAME+"="
                + ourAccountID +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for ICQ UIN:"+ ourAccountID,
                     icqProviderRefs);
        assertTrue("No Protocol Provider was found for ICQ UIN:"+ ourAccountID,
                     icqProviderRefs.length > 0);

        //save the service for other tests to use.
        icqServiceRef = icqProviderRefs[0];
        provider = (ProtocolProviderService)bc.getService(icqServiceRef);
    }

    public void tearDown()
    {
        bc.ungetService(icqServiceRef);
    }

}
