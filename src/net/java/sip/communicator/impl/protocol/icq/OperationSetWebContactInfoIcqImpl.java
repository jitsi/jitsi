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
 *
 * @author Damian Minkov
 */
public class OperationSetWebContactInfoIcqImpl
    implements OperationSetWebContactInfo
{
    public OperationSetWebContactInfoIcqImpl()
    {
    }

    /**
     * Returns the URL of a page containing information on <tt>contact</tt>
     *
     * @param contact the <tt>Contact</tt> that we'd like to get information
     *   about.
     * @return the URL of a page containing information on the specified
     *   contact.
     */
    public URL getWebContactInfo(Contact contact)
    {
        return getWebContactInfo(contact.getAddress());
    }

    /**
     * Returns the URL of a page containing information on the contact with
     * the specified <tt>contactAddress</tt>.
     *
     * @param contactAddress the <tt>contactAddress</tt> that we'd like to
     *   get information about.
     * @return the URL of a page containing information on the specified
     *   contact.
     */
    public URL getWebContactInfo(String contactAddress)
    {
        try
        {
            return new URL(
                "http://www.icq.com/people/about_me.php?uin=" +
                contactAddress);
        }
        catch (MalformedURLException ex)
        {
            return null;
        }
    }
}
