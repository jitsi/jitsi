/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.service.sysactivity.event.*;
import net.java.sip.communicator.util.*;
import org.freedesktop.*;
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.*;

/**
 * Responsible for subscribe and dispatch signals from NetworkManager.
 * Uses dbus to connect.
 *
 * @author Damian Minkov
 */
public class NetworkManagerListenerImpl
    implements DBusSigHandler
{
    /**
     * The logger.
     */
    private Logger logger = Logger.getLogger(
        NetworkManagerListenerImpl.class.getName());

    /**
     * The only instance of this impl.
     */
    private static NetworkManagerListenerImpl networkManagerListenerImpl;

    /**
     * Dbus connection we use.
     */
    private DBusConnection dbusConn;

    /**
     * Make only one instance.
     */
    private NetworkManagerListenerImpl()
    {
        try
        {
            dbusConn = DBusConnection.getConnection(DBusConnection.SYSTEM);
        }
        catch(DBusException e)
        {
            logger.error("Cannot obtain dbus connection", e);
        }
    }

    /**
     * Gets the instance of <tt>NetworkManagerListenerImpl</tt>.
     * @return the NetworkManagerListenerImpl.
     */
    public static NetworkManagerListenerImpl getInstance()
    {
        if(networkManagerListenerImpl == null)
            networkManagerListenerImpl = new NetworkManagerListenerImpl();

        return networkManagerListenerImpl;
    }

    /**
     * Starts
     */
    @SuppressWarnings("unchecked")
    public void start()
    {
        // on error connecting to dbus do nothing
        if(dbusConn == null)
            return;

        try
        {
            dbusConn.addSigHandler(DBus.NameOwnerChanged.class, this);
            dbusConn.addSigHandler(NetworkManager.StateChange.class, this);
        }
        catch(DBusException e)
        {
            logger.error("Error adding dbus signal handlers", e);
        }
    }

    /**
     * Stops.
     */
    @SuppressWarnings("unchecked")
    public void stop()
    {
        // on error connecting to dbus do nothing
        if(dbusConn == null)
            return;

        try
        {
            dbusConn.removeSigHandler(DBus.NameOwnerChanged.class, this);
            dbusConn.removeSigHandler(NetworkManager.StateChange.class, this);
        }
        catch(DBusException e)
        {
            logger.error("Error removing dbus signal handlers", e);
        }
    }

    /**
     * Receives signals and dispatch them.
     * @param dBusSignal signal to handle.
     */
    public void handle(DBusSignal dBusSignal)
    {
        if(dBusSignal instanceof DBus.NameOwnerChanged)
        {
            DBus.NameOwnerChanged nameOwnerChanged =
                (DBus.NameOwnerChanged)dBusSignal;

            if(nameOwnerChanged.name.equals(NetworkManager.class.getName()))
            {
                boolean b1 = nameOwnerChanged.old_owner != null
                    && nameOwnerChanged.old_owner.length() > 0;
                boolean b2 = nameOwnerChanged.new_owner != null
                    && nameOwnerChanged.new_owner.length() > 0;

                if(b1 && !b2)
                {
                    SystemActivityEvent evt = new SystemActivityEvent(
                        SysActivityActivator.getSystemActivityService(),
                        SystemActivityEvent.EVENT_NETWORK_CHANGE);
                    SysActivityActivator.getSystemActivityService()
                        .fireSystemActivityEvent(evt);
                }
            }
        }
        else if(dBusSignal instanceof NetworkManager.StateChange)
        {
            NetworkManager.StateChange stateChange =
                (NetworkManager.StateChange)dBusSignal;

            SystemActivityEvent evt = null;
            switch(stateChange.getStatus())
            {
                case NetworkManager.NM_STATE_CONNECTED:
                case NetworkManager.NM_STATE_DISCONNECTED:
                    evt = new SystemActivityEvent(
                        SysActivityActivator.getSystemActivityService(),
                        SystemActivityEvent.EVENT_NETWORK_CHANGE);
                    break;
                case NetworkManager.NM_STATE_ASLEEP:
                    evt = new SystemActivityEvent(
                        SysActivityActivator.getSystemActivityService(),
                        SystemActivityEvent.EVENT_SLEEP);
                    break;
            }

            if(evt != null)
                SysActivityActivator.getSystemActivityService()
                        .fireSystemActivityEvent(evt);
        }
    }
}
