/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.lang.ref.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a default/base implementation of
 * <tt>OperationSetDesktopSharingClient</tt> which attempts to make it easier
 * for implementers to provide complete solutions while focusing on
 * implementation-specific functionality.
 * 
 * @param <T>
 *
 * @author Sebastien Vincent
 * @author Lyubomir Marinov
 */
public abstract class AbstractOperationSetDesktopSharingClient
        <T extends ProtocolProviderService>
    implements OperationSetDesktopSharingClient
{

    /**
     * The list of <tt>RemoteControlListener</tt>s to be notified when a change
     * in remote control access occurs.
     */
    private final List<WeakReference<RemoteControlListener>> listeners
        = new ArrayList<WeakReference<RemoteControlListener>>();

    /**
     * The <tt>ProtocolProviderService</tt> implementation which created this
     * instance and for which telephony conferencing services are being provided
     * by this instance.
     */
    protected final T parentProvider;

    /**
     * Initializes a new <tt>AbstractOperationSetDesktopSharing</tt> instance
     * which is to be provided by a specific <tt>ProtocolProviderService.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> implementation
     * which is creating the new instance and for which telephony conferencing
     * services are being provided by this instance
     */
    protected AbstractOperationSetDesktopSharingClient(T parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    /**
     * Adds a <tt>RemoteControlListener</tt> to be notified when the remote peer
     * accepts to give us full control of their desktop.
     * <p>
     * The default implementation of
     * <tt>AbstractOperationSetDesktopSharingClient</tt> adds a
     * <tt>WeakReference</tt> to the specified <tt>RemoteControlListener</tt> in
     * order to avoid memory leaks because of code which calls
     * <tt>addRemoteControlListener</tt> and never calls
     * <tt>removeRemoteControlListener</tt>.
     * </p>
     *
     * @param listener the <tt>RemoteControlListener</tt> to add
     */
    public void addRemoteControlListener(RemoteControlListener listener)
    {
        synchronized (listeners)
        {
            Iterator<WeakReference<RemoteControlListener>> i
                = listeners.iterator();
            boolean contains = false;

            while (i.hasNext())
            {
                RemoteControlListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    contains = true;
            }
            if (!contains)
            {
                listeners.add(
                        new WeakReference<RemoteControlListener>(listener));
            }
        }
    }

    /**
     * Fires a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlGranted()
    {
        List<RemoteControlListener> listeners = getListeners();

        if (!listeners.isEmpty())
        {
            RemoteControlGrantedEvent event
                = new RemoteControlGrantedEvent(this);

            for(RemoteControlListener l : listeners)
                l.remoteControlGranted(event);
        }
    }

    /**
     * Fires a <tt>RemoteControlGrantedEvent</tt> to all registered listeners.
     */
    public void fireRemoteControlRevoked()
    {
        List<RemoteControlListener> listeners = getListeners();

        if (!listeners.isEmpty())
        {
            RemoteControlRevokedEvent event
                = new RemoteControlRevokedEvent(this);

            for(RemoteControlListener l : listeners)
                l.remoteControlRevoked(event);
        }
    }

    /**
     * Gets a list of <tt>RemoteControlListener</tt>s to be notified of remote
     * control access changes.
     *
     * @return a list of <tt>RemoteControlListener</tt>s to be notifed of remote
     * control access changes
     */
    protected List<RemoteControlListener> getListeners()
    {
        List<RemoteControlListener> listeners;

        synchronized (this.listeners)
        {
            Iterator<WeakReference<RemoteControlListener>> i
                = this.listeners.iterator();
            listeners
                = new ArrayList<RemoteControlListener>(this.listeners.size());

            while (i.hasNext())
            {
                RemoteControlListener l = i.next().get();

                if (l == null)
                    i.remove();
                else
                    listeners.add(l);
            }
        }
        return listeners;
    }

    /**
     * Removes a <tt>RemoteControlListener</tt> to be notified when remote peer
     * accept/revoke to give us full control.
     *
     * @param listener <tt>RemoteControlListener</tt> to remove
     */
    public void removeRemoteControlListener(RemoteControlListener listener)
    {
        synchronized (listeners)
        {
            Iterator<WeakReference<RemoteControlListener>> i
                = listeners.iterator();

            while (i.hasNext())
            {
                RemoteControlListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
        }
    }
}
