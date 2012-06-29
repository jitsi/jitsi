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
@SuppressWarnings("rawtypes")
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
            dbusConn.addSigHandler(DBusNetworkManager.StateChange.class, this);
            dbusConn.addSigHandler(DBusNetworkManager.StateChanged.class, this);
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
            dbusConn.removeSigHandler(
                DBusNetworkManager.StateChange.class, this);
            dbusConn.removeSigHandler(
                DBusNetworkManager.StateChanged.class, this);
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

            if(nameOwnerChanged.name.equals("org.freedesktop.NetworkManager"))
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
        else if(dBusSignal instanceof DBusNetworkManager.StateChange)
        {
            DBusNetworkManager.StateChange stateChange =
                (DBusNetworkManager.StateChange)dBusSignal;

            SystemActivityEvent evt = null;
            switch(stateChange.getStatus())
            {
                case DBusNetworkManager.NM_STATE_CONNECTED:
                case DBusNetworkManager.NM_STATE_DISCONNECTED:
                case DBusNetworkManager.NM9_STATE_DISCONNECTED:
                case DBusNetworkManager.NM9_STATE_CONNECTED_LOCAL:
                case DBusNetworkManager.NM9_STATE_CONNECTED_SITE:
                case DBusNetworkManager.NM9_STATE_CONNECTED_GLOBAL:
                    evt = new SystemActivityEvent(
                        SysActivityActivator.getSystemActivityService(),
                        SystemActivityEvent.EVENT_NETWORK_CHANGE);
                    break;
                case DBusNetworkManager.NM_STATE_ASLEEP:
                case DBusNetworkManager.NM9_STATE_ASLEEP:
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

    /**
     * Whether we are connected to the network manager through dbus.
     * @return whether we are connected to the network manager.
     */
    public boolean isConnected()
    {
        return dbusConn != null;
    }
}
