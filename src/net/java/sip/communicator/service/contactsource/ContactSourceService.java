/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

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
     * Constants to identify <tt>ContactSource</tt> in call history.
     */
    public static final String CALL_HISTORY = "CallHistory";

    /**
     * Returns the identifier of this contact source. Some of the common
     * identifiers are defined here (For example the CALL_HISTORY identifier
     * should be returned by all call history implementations of this interface)
     * @return the identifier of this contact source
     */
    public String getIdentifier();

    /**
     * Returns a user-friendly string that identifies this contact source.
     * @return the display name of this contact source
     */
    public String getDisplayName();

    /**
     * Queries this search source for the given <tt>searchString</tt>.
     * @param queryString the string to search for
     * @return the created query
     */
    public ContactQuery queryContactSource(String queryString);
}
