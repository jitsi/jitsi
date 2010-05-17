/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.netaddr.event;

/**
 * A ChangeEvent is fired on change of the network configuration of the computer.
 *
 * @author Damian Minkov
 */
public class ChangeEvent
    extends java.util.EventObject
{
    /**
     * Event type for interface going up.
     */
    public static final int IFACE_DOWN = 0;

    /**
     * Event type for interface going down.
     */
    public static final int IFACE_UP = 1;   

    /**
     * The type of the current event.
     */
    private int type = -1;

    /**
     * Whether this event is after computer have been suspended.
     */
    private boolean standby = false;

    /**
     * Creates event.
     * @param source the source of the event.
     * @param type the type of the event.
     */
    public ChangeEvent(Object source, int type)
    {
        super(source);

        this.type = type;
    }

    /**
     * Creates event.
     * @param source the source of the event.
     * @param type the type of the event.
     * @param standby is the event after a suspend of the computer.
     */
    public ChangeEvent(Object source, int type, boolean standby)
    {
        this(source, type);

        this.standby = standby;
    }

    /**
     * The type of this event.
     * @return the type
     */
    public int getType()
    {
        return type;
    }

    /**
     * Whether this event is after suspend of the computer.
     * @return the standby
     */
    public boolean isStandby()
    {
        return standby;
    }

    /**
     * Overrides toString method.
     * @return string representing the event.
     */
    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("ChangeEvent ");

        switch(type)
        {
            case IFACE_DOWN: buff.append("Interface down"); break;
            case IFACE_UP: buff.append("Interface up"); break;
        }

        buff.append(", standby=" + standby);

        return buff.toString();
    }
}
