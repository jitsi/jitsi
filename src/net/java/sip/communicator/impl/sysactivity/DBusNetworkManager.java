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

import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.*;

/**
 * NetworkManager D-Bus Interface
 *
 * @author Damian Minkov
 * @author Ingo Bauersachs
 */
@DBusInterfaceName("org.freedesktop.NetworkManager")
public interface DBusNetworkManager
    extends DBusInterface
{
    /*
     * Types of NetworkManager states for versions < 0.9
     */
    public static final int NM_STATE_UNKNOWN        = 0;
    public static final int NM_STATE_ASLEEP         = 1;
    public static final int NM_STATE_CONNECTING     = 2;
    public static final int NM_STATE_CONNECTED      = 3;
    public static final int NM_STATE_DISCONNECTED   = 4;

    /*
     * Types of NetworkManager states for versions >= 0.9
     */
    public static final int NM9_STATE_UNKNOWN          = 0;
    public static final int NM9_STATE_ASLEEP           = 10;
    public static final int NM9_STATE_DISCONNECTED     = 20;
    public static final int NM9_STATE_DISCONNECTING    = 30;
    public static final int NM9_STATE_CONNECTING       = 40;
    public static final int NM9_STATE_CONNECTED_LOCAL  = 50;
    public static final int NM9_STATE_CONNECTED_SITE   = 60;
    public static final int NM9_STATE_CONNECTED_GLOBAL = 70;

    /**
     * State change signal.
     */
    public class StateChange extends DBusSignal
    {
        /**
         * The name of the signal.
         */
        public final String name;

        /**
         * The current status it holds.
         */
        public final UInt32 status;

        /**
         * Creates status change.
         * @param path the path
         * @param status the status
         * @throws DBusException
         */
        public StateChange(String path, UInt32 status)
            throws DBusException
        {
            super(path, status);
            name = path;
            this.status = status;
        }

        /**
         * The current status.
         * @return the current status
         */
        public int getStatus()
        {
            return status.intValue();
        }

        /**
         * Returns status description
         * @return the status description
         */
        public String getStatusName()
        {
            switch(status.intValue())
            {
                case NM_STATE_ASLEEP : return "Asleep";
                case NM_STATE_CONNECTING : return "Connecting";
                case NM_STATE_CONNECTED : return "Connected";
                case NM_STATE_DISCONNECTED : return "Disconnected";
                default : return "Unknown";
            }
        }
    }

    /**
     * State changed signal.
     */
    public static class StateChanged extends StateChange
    {
        /**
         * Creates status changed.
         * @param path the path
         * @param status the status
         * @throws DBusException
         */
        public StateChanged(String path, UInt32 status)
            throws DBusException
        {
            super(path, status);
        }

        /**
         * Returns status description
         * @return the status name
         */
        @Override
        public String getStatusName()
        {
            switch(status.intValue())
            {
                case NM9_STATE_UNKNOWN: return "Unknown";
                case NM9_STATE_ASLEEP: return "Asleep";
                case NM9_STATE_DISCONNECTED: return "Disconnected";
                case NM9_STATE_DISCONNECTING: return "Disconnecting";
                case NM9_STATE_CONNECTING: return "Connecting";
                case NM9_STATE_CONNECTED_LOCAL: return "LocalConnectivity";
                case NM9_STATE_CONNECTED_SITE: return "SiteConnectivity";
                case NM9_STATE_CONNECTED_GLOBAL: return "GlobalConnectivity";
                default : return "Unknown";
            }
        }
    }
}
