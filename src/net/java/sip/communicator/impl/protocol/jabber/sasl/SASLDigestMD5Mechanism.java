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
package net.java.sip.communicator.impl.protocol.jabber.sasl;

import java.io.*;
import java.util.*;

import javax.security.auth.callback.*;
import javax.security.sasl.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.util.Base64; // disambiguation

/**
 * Creates our custom SASLDigestMD5Mechanism with some changes in order
 * to be compatible with some jabber servers.
 * @author Damian Minkov
 */
public class SASLDigestMD5Mechanism
    extends org.jivesoftware.smack.sasl.SASLDigestMD5Mechanism
{
    /**
     * Creates our mechanism.
     * @param saslAuthentication
     */
    public SASLDigestMD5Mechanism(SASLAuthentication saslAuthentication)
    {
        super(saslAuthentication);
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. Note that this method of
     * authentication is not recommended, since it is very inflexible.  Use
     * {@link #authenticate(String, String, CallbackHandler)} whenever possible.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param password the password for this account.
     * @throws IOException If a network error occurs while authenticating.
     * @throws XMPPException If a protocol error occurs or the user is not authenticated.
     */
    @Override
    public void authenticate(String username, String host, String password)
        throws IOException, XMPPException
    {
        //Since we were not provided with a CallbackHandler, we will use our own with the given
        //information

        //Set the authenticationID as the username, since they must be the same in this case.
        this.authenticationId = username;
        this.password = password;
        this.hostname = host;

        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String,String>();
        sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
        authenticate();
    }

    /**
     * Builds and sends the <tt>auth</tt> stanza to the server. The callback handler will handle
     * any additional information, such as the authentication ID or realm, if it is needed.
     *
     * @param username the username of the user being authenticated.
     * @param host     the hostname where the user account resides.
     * @param cbh      the CallbackHandler to obtain user information.
     * @throws IOException If a network error occurs while authenticating.
     * @throws XMPPException If a protocol error occurs or the user is not authenticated.
     */
    @Override
    public void authenticate(String username, String host, CallbackHandler cbh)
        throws IOException, XMPPException
    {
        String[] mechanisms = { getName() };
        Map<String,String> props = new HashMap<String,String>();
        sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
        authenticate();
    }

    /**
     * The server is challenging the SASL mechanism for the stanza he just sent. Send a
     * response to the server's challenge.
     *
     * @param challenge a base64 encoded string representing the challenge.
     * @throws IOException if an exception sending the response occurs.
     */
    @Override
    public void challengeReceived(String challenge)
        throws IOException
    {
        byte response[];
        if(challenge != null) {
            response = sc.evaluateChallenge(Base64.decode(challenge));
        } else {
            response = sc.evaluateChallenge(null);
        }

        String authenticationText = null;
        if(null != response) {
            authenticationText = Base64.encodeBytes(response,Base64.DONT_BREAK_LINES);
        }
        if((null == authenticationText) || (authenticationText.equals(""))) {
            authenticationText = "=";
        }

        // Send the authentication to the server
        getSASLAuthentication().send(new Response(authenticationText));
    }
}
