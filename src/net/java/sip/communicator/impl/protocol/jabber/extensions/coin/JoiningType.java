/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

/**
 * Joining type.
 *
 * @author Sebastien Vincent
 */
public enum JoiningType
{
    /**
     * Dialed-in.
     */
    dialed_in("dialed-in"),

    /**
     * Dialed-out.
     */
    dialed_out("dialed-out"),

    /**
     * Focus owner.
     */
    focus_owner("focus-owner");

    /**
     * The name of this type.
     */
    private final String type;

    /**
     * Creates a <tt>JoiningType</tt> instance with the specified name.
     *
     * @param type type name.
     */
    private JoiningType(String type)
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
     * Returns a <tt>JoiningType</tt>.
     *
     * @param typeStr the <tt>String</tt> that we'd like to
     * parse.
     * @return an JoiningType.
     *
     * @throws IllegalArgumentException in case <tt>typeStr</tt> is
     * not a valid <tt>EndPointType</tt>.
     */
    public static JoiningType parseString(String typeStr)
        throws IllegalArgumentException
    {
        for (JoiningType value : values())
            if (value.toString().equals(typeStr))
                return value;

        throw new IllegalArgumentException(
            typeStr + " is not a valid reason");
    }
}
