/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.searchsource;

import java.util.regex.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ContactSearchSourceService</tt> interface is meant to be implemented
 * by modules supporting large lists of contacts and wanting to enable searching
 * from other modules.
 *
 * @author Yana Stamcheva
 */
public interface ContactSearchSourceService
{
    /**
     * Queries this search source for the given <tt>searchString</tt>.
     * @param searchString the string to search for
     * @return the created query
     */
    public ContactQuery querySearchSource(String searchString);

    /**
     * Queries this search source for the given <tt>searchPattern</tt>.
     * @param searchPattern the pattern to search for
     * @return the created query
     */
    public ContactQuery querySearchSource(Pattern searchPattern);

    /**
     * Returns the telephony provider preferred for calling items from this
     * source.
     * @return the preferred telephony provider
     */
    public ProtocolProviderService getPreferredTelephonyProvider();
}
