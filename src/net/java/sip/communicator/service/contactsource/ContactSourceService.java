/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactSourceService</tt> interface is meant to be implemented
 * by modules supporting large lists of contacts and wanting to enable searching
 * from other modules.
 *
 * @author Yana Stamcheva
 */
public interface ContactSourceService
{
    /**
     * Queries this search source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString);

    /**
     * Returns the telephony provider preferred for calling items from this
     * source.
     * @return the preferred telephony provider
     */
    public ProtocolProviderService getPreferredTelephonyProvider();
}
