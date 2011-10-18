/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.net.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Returns the url which can be used to register new account fo the Icq server
 *
 * @author Damian Minkov
 */
public class OperationSetWebAccountRegistrationIcqImpl
    implements OperationSetWebAccountRegistration
{
    /**
     * Returns a URL that points to a page which allows for on-line
     * registration of accounts belonging to the service supported by this
     * protocol provider.
     *
     * @return a URL pointing to a web page where one could register their
     *   account for the current protocol.
     */
    public URL getAccountRegistrationURL()
    {
        try
        {
            return new URL("http://www.icq.com/register/");
        }
        catch (MalformedURLException ex)
        {
            return null;
        }
    }
}
