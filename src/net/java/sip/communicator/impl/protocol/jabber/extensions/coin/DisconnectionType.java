/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

/**
 * Disconnection type.
 *
 * @author Sebastien Vincent
 */
public enum DisconnectionType
{
    /**
     * Departed.
     */
    departed("departed"),

    /**
     * Booted.
     */
    booted("booted"),

    /**
     * Failed.
     */
    failed("failed"),

    /**
     * Busy
     */
    busy("busy");

    /**
     * The name of this type.
     */
    private final String type;

    /**
     * Creates a <tt>DisconnectionType</tt> instance with the specified name.
     *
     * @param type type name.
     */
    private DisconnectionType(String type)
    {
        this.type = type;
    }

    /**
     * Returns the type name.
     *
     * @return type name
     */
    @Override
    public String toString()
    {
        return type;
    }

    /**
     * Returns a <tt>DisconnectionType</tt>.
     *
     * @param typeStr the <tt>String</tt> that we'd like to
     * parse.
     * @return an DisconnectionType.
     *
     * @throws IllegalArgumentException in case <tt>typeStr</tt> is
     * not a valid <tt>EndPointType</tt>.
     */
    public static DisconnectionType parseString(String typeStr)
        throws IllegalArgumentException
    {
        for (DisconnectionType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(
            typeStr + " is not a valid reason");
    }
}
