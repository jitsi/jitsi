package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Event that notify that remote control feature has been revoked. This is used
 * in desktop sharing related usage. After rights beeing revoked, local
 * peer must not notify keyboard and mouse events to remote peer.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlRevokedEvent extends EventObject
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
    public RemoteControlRevokedEvent(Object source)
    {
        super(source);
    }
}
