/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

/**
 * Enumeration of all possible type field value for Google Talk session IQ.
 *
 * @author Sebastien Vincent
 */
public enum GTalkType
{
    /**
     * Initiate type.
     */
    INITIATE("initiate"),

    /**
     * Accept type.
     */
    ACCEPT("accept"),

    /**
     * Candidate type.
     */
    CANDIDATES("candidates"),

    /**
     * Reject type.
     */
    REJECT("reject"),

    /**
     * Transport-info type.
     */
    TRANSPORT_INFO("transport-info"),

    /**
     * Transport-accept type.
     */
    TRANSPORT_ACCEPT("transport-accept"),

    /**
     * Terminate type.
     */
    TERMINATE("terminate");

    /**
     * The name of this type.
     */
    private final String typeName;

    /**
     * Creates a <tt>GTalkType</tt> instance with the specified name.
     *
     * @param typeName the name of the <tt>GTalkType</tt> we'd like
     * to create.
     */
    private GTalkType(String typeName)
    {
        this.typeName = typeName;
    }

    /**
     * Returns the name of this <tt>GTalkType</tt> (e.g. "initiate"
     * or "accept"). The name returned by this method is meant for
     * use directly in the XMPP XML string.
     *
     * @return Returns the name of this <tt>GTalkType</tt>
     */
    @Override
    public String toString()
    {
        return typeName;
    }

    /**
     * Returns a <tt>GTalkType</tt> value corresponding to the specified
     * <tt>gtalkTypeStr</tt>.
     *
     * @param gtalkTypeStr the action <tt>String</tt> that we'd like to
     * parse.
     * @return a <tt>GTalkType</tt> value corresponding to the specified
     * <tt>GTalkTypeStr</tt>.
     *
     * @throws IllegalArgumentException in case <tt>GTalkTypeStr</tt> is
     * not a valid media direction.
     */
    public static GTalkType parseString(String gtalkTypeStr)
        throws IllegalArgumentException
    {
        for (GTalkType value : values())
            if (value.toString().equals(gtalkTypeStr))
                return value;

        throw new IllegalArgumentException(
             gtalkTypeStr + " is not a valid Google Talk type");
    }
}
