/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.sysactivity;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.impl.sysactivity.DBusNetworkManager.*;
import net.java.sip.communicator.service.sysactivity.event.*;
import org.apache.commons.lang3.*;
import org.freedesktop.dbus.connections.impl.*;
import org.freedesktop.dbus.exceptions.*;
import org.freedesktop.dbus.interfaces.*;
import org.freedesktop.dbus.interfaces.DBus.*;

/**
 * Responsible for subscribe and dispatch signals from NetworkManager. Uses dbus
 * to connect.
 *
 * @author Damian Minkov
 */
@Slf4j
public class NetworkManagerListenerImpl
    implements SystemActivityManager
{
    /**
     * Dbus connection we use.
     */
    private DBusConnection dbusConn;

    private SystemActivityNotificationsServiceImpl sysActivitiesService;

    /**
     * Make only one instance.
     */
    public NetworkManagerListenerImpl(SystemActivityNotificationsServiceImpl
        sysActivitiesService)
    {
        this.sysActivitiesService = sysActivitiesService;
        try
        {
            dbusConn = DBusConnection.getConnection(
                DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS);
        }
        catch (DBusException e)
        {
            logger.error("Cannot obtain dbus connection", e);
        }
    }

    /**
     * Starts
     */
    public void start()
    {
        // on error connecting to dbus do nothing
        if (dbusConn == null)
        {
            return;
        }

        try
        {
            dbusConn.addSigHandler(NameOwnerChanged.class,
                nameOwnerChangedHandler);
            dbusConn.addSigHandler(StateChange.class,
                stateChangeHandler);
        }
        catch (DBusException e)
        {
            logger.error("Error adding dbus signal handlers", e);
        }
    }

    /**
     * Stops.
     */
    public void stop()
    {
        // on error connecting to dbus do nothing
        if (dbusConn == null)
        {
            return;
        }

        try
        {
            dbusConn.removeSigHandler(NameOwnerChanged.class,
                nameOwnerChangedHandler);
            dbusConn.removeSigHandler(StateChange.class,
                stateChangeHandler);
        }
        catch (DBusException e)
        {
            logger.error("Error removing dbus signal handlers", e);
        }
    }

    private final DBusSigHandler<NameOwnerChanged>
        nameOwnerChangedHandler = nameOwnerChanged ->
    {
        if (nameOwnerChanged.name.equals("org.freedesktop.NetworkManager"))
        {
            if (StringUtils.isNotEmpty(nameOwnerChanged.oldOwner)
                && StringUtils.isEmpty(nameOwnerChanged.newOwner))
            {
                SystemActivityEvent evt = new SystemActivityEvent(
                    sysActivitiesService,
                    SystemActivityEvent.EVENT_NETWORK_CHANGE);
                sysActivitiesService.fireSystemActivityEvent(evt);
            }
        }
    };

    private final DBusSigHandler<StateChange>
        stateChangeHandler = stateChange ->
    {
        SystemActivityEvent evt = null;
        switch (stateChange.getStatus())
        {
        case DBusNetworkManager.NM_STATE_CONNECTED:
        case DBusNetworkManager.NM_STATE_DISCONNECTED:
        case DBusNetworkManager.NM9_STATE_DISCONNECTED:
        case DBusNetworkManager.NM9_STATE_CONNECTED_LOCAL:
        case DBusNetworkManager.NM9_STATE_CONNECTED_SITE:
        case DBusNetworkManager.NM9_STATE_CONNECTED_GLOBAL:
            evt = new SystemActivityEvent(
                sysActivitiesService,
                SystemActivityEvent.EVENT_NETWORK_CHANGE);
            break;
        case DBusNetworkManager.NM_STATE_ASLEEP:
        case DBusNetworkManager.NM9_STATE_ASLEEP:
            evt = new SystemActivityEvent(
                sysActivitiesService,
                SystemActivityEvent.EVENT_SLEEP);
            break;
        }

        if (evt != null)
        {
            sysActivitiesService.fireSystemActivityEvent(evt);
        }
    };

    /**
     * Whether we are connected to the network manager through dbus.
     *
     * @return whether we are connected to the network manager.
     */
    public boolean isConnected()
    {
        return dbusConn != null;
    }
}
