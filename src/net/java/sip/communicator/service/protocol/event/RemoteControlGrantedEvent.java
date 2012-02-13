package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Event that notify that remote control feature has been granted. This is used
 * in desktop sharing related usage. After rights being granted, local
 * peer should notify keyboard and mouse events to remote peer.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlGrantedEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new <tt>RemoteControlGrantedEvent</tt> object.
     *
     * @param source source object
     */
    public RemoteControlGrantedEvent(Object source)
    {
        super(source);
    }
}
