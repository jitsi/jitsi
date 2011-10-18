/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.protocol.generic;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation for the authorization handler.
 */
public class AuthHandler
implements AuthorizationHandler
{

    private static final Logger logger =
        Logger.getLogger(AuthHandler.class);

    public AuthorizationResponse processAuthorisationRequest(
        AuthorizationRequest req, Contact sourceContact)
    {
        logger.trace("processAuthorisationRequest " + req + " " +
            sourceContact);

        return new AuthorizationResponse(AuthorizationResponse.ACCEPT, "");
    }

    public AuthorizationRequest createAuthorizationRequest(Contact contact)
    {
        logger.trace("createAuthorizationRequest " + contact);
        return new AuthorizationRequest();
    }

    public void processAuthorizationResponse(
        AuthorizationResponse response, Contact sourceContact)
    {
        logger.debug("auth response from: " +
            sourceContact.getAddress() + " " +
            response.getResponseCode().getCode());
    }
}