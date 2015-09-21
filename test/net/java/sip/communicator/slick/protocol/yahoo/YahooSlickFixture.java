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
package net.java.sip.communicator.slick.protocol.yahoo;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.slick.protocol.generic.*;

import org.osgi.framework.*;

/**
 * Contains fields and methods used by most or all tests in the yahoo slick.
 *
 * @author Damian Minkov
 * @author Valentin Martinet
 */
public class YahooSlickFixture
    extends AdHocMultiUserChatSlickFixture
{
    /**
     * Constructor
     */
    public YahooSlickFixture()
    {
        super();
    }

    /**
     * Initializes protocol provider references and whatever else there is to
     * initialize.
     *
     * @throws InvalidSyntaxException in case we meet problems while retrieving
     * protocol providers through OSGI
     */
    @Override
    public void setUp() throws InvalidSyntaxException
    {
        // first obtain a reference to the provider factory
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
                            + "="+ProtocolNames.YAHOO+")";
        try{
            serRefs = bc.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            //this really shouldhn't occur as the filter expression is static.
            fail(osgiFilter + " is not a valid osgi filter");
        }

        assertTrue(
            "Failed to find a provider factory service for protocol yahoo",
            (serRefs != null) && (serRefs.length >  0));

        //Keep the reference for later usage.
        providerFactory = (ProtocolProviderFactory)bc.getService(serRefs[0]);

        userID1 =
            System.getProperty(
                YahooProtocolProviderServiceLick.ACCOUNT_1_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID2 =
            System.getProperty(
                YahooProtocolProviderServiceLick.ACCOUNT_2_PREFIX
                + ProtocolProviderFactory.USER_ID);

        userID3 =
            System.getProperty(
                YahooProtocolProviderServiceLick.ACCOUNT_3_PREFIX
                + ProtocolProviderFactory.USER_ID);

        //find the protocol providers exported for the three accounts
        ServiceReference[] yahooProvider1Refs
            = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                "(&"
                +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.YAHOO+")"
                +"("+ProtocolProviderFactory.USER_ID+"="
                + userID1 +")"
                +")");

        //make sure we found a service
        assertNotNull("No Protocol Provider was found for yahoo account1:"
                      + userID1
                      , yahooProvider1Refs);
        assertTrue("No Protocol Provider was found for yahoo account1:"+userID1,
                     yahooProvider1Refs.length > 0);

        ServiceReference[] yahooProvider2Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.YAHOO+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID2 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for yahoo account2:"
                      + userID2
                      , yahooProvider2Refs);
        assertTrue("No Protocol Provider was found for yahoo account2:"+userID2,
                     yahooProvider2Refs.length > 0);

        ServiceReference[] yahooProvider3Refs
        = bc.getServiceReferences(
            ProtocolProviderService.class.getName(),
            "(&"
            +"("+ProtocolProviderFactory.PROTOCOL+"="+ProtocolNames.YAHOO+")"
            +"("+ProtocolProviderFactory.USER_ID+"="
            + userID3 +")"
            +")");

        //again make sure we found a service.
        assertNotNull("No Protocol Provider was found for yahoo account3:"
                      + userID3
                      , yahooProvider3Refs);
        assertTrue("No Protocol Provider was found for yahoo account2:"+userID3,
                     yahooProvider3Refs.length > 0);

        //save the service for other tests to use.
        provider1ServiceRef = yahooProvider1Refs[0];
        provider1 = (ProtocolProviderService)bc.getService(provider1ServiceRef);
        provider2ServiceRef = yahooProvider2Refs[0];
        provider2 = (ProtocolProviderService)bc.getService(provider2ServiceRef);
        provider3ServiceRef = yahooProvider3Refs[0];
        provider3 = (ProtocolProviderService)bc.getService(provider3ServiceRef);
    }
}
