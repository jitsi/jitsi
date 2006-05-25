/*
 * Created on 13/10/2003
 */
package net.java.sip.communicator.impl.protocol.icq.message.common;

/**
 * Hold an old ICQ protocol command subtype.
 * These commands are identified by one or two 16-bit integers.
 *
 * @author jkohen
 */
public class IcqType
{
    private final int primary;
    private final int secondary;

    /**
     * Create an ICQ one-element subtype.
     *
     * @param primary the primary part of the subtype
     */
    public IcqType(int primary)
    {
        this(primary, 0);
    }

    /**
     * Create an ICQ two-element subtype.
     *
     * @param primary the primary part of the subtype
     * @param secondary the secondary part of the subtype
     */
    public IcqType(int primary, int secondary)
    {
        this.primary = primary;
        this.secondary = secondary;
    }

    /**
     * Returns the primary part of the subtype.
     *
     * @return the primary part of the subtype.
     */
    public int getPrimary()
    {
        return primary;
    }

    /**
     * Returns the secondary part of the subtype, which may be missing.
     *
     * @return the secondary part of the subtype, or zero if it's not present.
     */
    public int getSecondary()
    {
        return secondary;
    }

    public String toString()
    {
        if (0 != secondary)
        {
            return "(" + primary + ", " + secondary + ")";
        }
        else
        {
            return "(" + primary + ")";
        }
    }

    public boolean equals(Object obj)
    {
        if (null == obj)
        {
            return false;
        }
        if (! (obj instanceof IcqType))
        {
            return false;
        }
        IcqType other = (IcqType) obj;
        return primary == other.primary && secondary == other.secondary;
    }

    public int hashCode()
    {
        return primary ^ (secondary << 16);
    }
}
