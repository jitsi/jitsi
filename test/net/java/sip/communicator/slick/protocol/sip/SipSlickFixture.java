/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.sip;

import junit.framework.*;
import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Contains fields and methods used by most or all tests in the SIP slick.
 */
public class SipSlickFixture
    extends TestCase
{
    /**
     * To be set by the slick itself upon activation.
     */
    public static BundleContext bc = null;

    /**
     * An osgi service reference for the protocol provider corresponding to our
     * first testing account.
     */
    public ServiceReference provider1ServiceRef = null;

    /**
      * The protocol provider corresponding to our first testing account.
      */
    public ProtocolProviderService provider1        = null;

    /**
     * The user ID associated with testing account 1.
     */
    public String userID1 = null;

    /**
     * An osgi service reference for the protocol provider corresponding to our
     * second testing account.
     */
    public ServiceReference provider2ServiceRef = null;

    /**
     * The protocol provider corresponding to our first testing account.
     */
    public ProtocolProviderService provider2        = null;

    /**
     * The user ID associated with testing account 2.
     */
    public String userID2 = null;


    /**
     * The tested protocol provider factory.
     */
    public ProtocolProviderFactory providerFactory = null;

    /**
     * A reference to the bundle containing the tested pp implementation. This
     * reference is set during the accoung uninstallation testing and used during
     * the account uninstallation persistence testing.
     */
    public static Bundle providerBundle = null;

    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     *
     * @throws java.lang.Exception in case we meet problems while retriving
     * protocol providers through OSGI
     */
    public void setUp()
        throws Exception
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.SIP+")";
        try{
            serRefs = bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol SIP",
            serRefs != null || serRefs.length >  0);

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        userID1 =
            System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID2 =
            System.getProperty(
                SipProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.USER_ID);

        //find the protocol providers exported for the two accounts
        ServiceReference[] sipProvider1Refs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.SIP+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + userID1 +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for SIP account1:"
                      + userID1
                      , sipProvider1Refs);
        assertTrue("No Protocol Provider was found for SIP account1:"+ userID1,
                     sipProvider1Refs.length > 0);

        ServiceReference[] sipProvider2Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.SIP+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID2 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for SIP account2:"
                      + userID2
                      , sipProvider2Refs);
        assertTrue("No Protocol Provider was found for SIP account2:"+ userID2,
                     sipProvider2Refs.length > 0);

        //save the service for other tests to use.
        provider1ServiceRef = sipProvider1Refs[0];
        provider1 = (ProtocolProviderService)bc.getService(provider1ServiceRef);
        provider2ServiceRef = sipProvider2Refs[0];
        provider2 = (ProtocolProviderService)bc.getService(provider2ServiceRef);
    }

    /**
     * Un get service references used in here.
     */
    public void tearDown()
    {
        bc.ungetService(provider1ServiceRef);
        bc.ungetService(provider2ServiceRef);
    }

}
