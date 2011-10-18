/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.net.*;

/**
 * The operation set is there to propose an extremely simple and light way of
 * registering new accounts for a given protocol. The
 * getAccountRegistrationURL() returns a URL that when opened in a web browser
 * allows you to register your own account with the corresponding protocol.
 * <p>
 *
 * @author Emil Ivov
 */
public interface OperationSetWebAccountRegistration
    extends OperationSet
{
    /**
     * Returns a URL that points to a page which allows for on-line registration
     * of accounts belonging to the service supported by this protocol provider.
     * <p>
     * @return a URL pointing to a web page where one could register their
     * account for the current protocol.
     */
    public URL getAccountRegistrationURL();
}
