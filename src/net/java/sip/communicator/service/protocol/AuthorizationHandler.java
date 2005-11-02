/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * @todo say that this is not a listener because methods have to have a return
 * value
 *
 * @author Emil Ivov
 */
public interface AuthorizationHandler
{
    public void handleAuthorisationRequest();
}
