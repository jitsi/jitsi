/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.msn;

import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.slick.protocol.generic.*;

/**
 * Contains fields and methods used by most or all tests in the msn slick.
 *
 * @author Damian Minkov
 * @author Valentin Martinet
 */
public class MsnSlickFixture
    extends AdHocMultiUserChatSlickFixture
{

    /**
     * Constructor
     */
    public MsnSlickFixture()
    {
        super();
    }
    
    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     * 
     * @throws InvalidSyntaxException  in case we meet problems while retrieving
     * protocol providers through OSGI
     */
    public void setUp() throws InvalidSyntaxException
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.MSN+")";
        try{
            serRefs = bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol msn",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        userID1 =
            System.getProperty(
                MsnProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID2 =
            System.getProperty(
                MsnProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.USER_ID);
        
        userID3 =
            System.getProperty(
                MsnProtocolProviderServiceLick.ACCOUNT_3_PREFIX
                + ProtocolProviderFactory.USER_ID);

        //find the protocol providers exported for the two accounts
        ServiceReference[] msnProvider1Refs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.MSN+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + userID1 +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for msn account1:"
                      + userID1
                      , msnProvider1Refs);
        assertTrue("No Protocol Provider was found for msn account1:"+ userID1,
                     msnProvider1Refs.length > 0);

        ServiceReference[] msnProvider2Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.MSN+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID2 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for msn account2:"
                      + userID2
                      , msnProvider2Refs);
        assertTrue("No Protocol Provider was found for msn account2:"+ userID2,
                     msnProvider2Refs.length > 0);
        
        ServiceReference[] msnProvider3Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.MSN+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID3 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for msn account3:"
                      + userID3
                      , msnProvider3Refs);
        assertTrue("No Protocol Provider was found for msn account3:"+ userID3,
                     msnProvider3Refs.length > 0);

        //save the service for other tests to use.
        provider1ServiceRef = msnProvider1Refs[0];
        provider1 = (ProtocolProviderService)bc.getService(provider1ServiceRef);
        provider2ServiceRef = msnProvider2Refs[0];
        provider2 = (ProtocolProviderService)bc.getService(provider2ServiceRef);
        provider3ServiceRef = msnProvider3Refs[0];
        provider3 = (ProtocolProviderService)bc.getService(provider3ServiceRef);
    }
}
