/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.net.*;

/**
 * The operation set is a very simplified version of the server stored info
 * operation sets, allowing protocol providers to implement a quick way of
 * showing user information, by simply returning a URL where the information
 * of a specific user is to be found.
 */
public interface OperationSetWebContactInfo
    extends OperationSet
{
    /**
     * Returns the URL of a page containing information on <tt>contact</tt>
     * @param contact the <tt>Contact</tt> that we'd like to get information
     * about.
     * @return the URL of a page containing information on the specified
     * contact.
     */
    public URL getWebContactInfo(Contact contact);

    /**
     * Returns the URL of a page containing information on the contact with the
     * specified <tt>contactAddress</tt>.
     * @param contactAddress the <tt>contactAddress</tt> that we'd like to get
     * information about.
     * @return the URL of a page containing information on the specified
     * contact.
     */
    public URL getWebContactInfo(String contactAddress);
}
