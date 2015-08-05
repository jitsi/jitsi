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
package net.java.sip.communicator.impl.galagonotification;

import java.awt.image.*;

/**
 * Declares the native functions required by the galagonotification bundle.
 *
 * @author Lubomir Marinov
 */
public final class GalagoNotification
{
    static
    {
        System.loadLibrary("galagonotification");
    }

    /**
     * Connects to the <tt>DBUS_BUS_SESSION</tt> D-Bus bus daemon and registers
     * with it.
     *
     * @return a new reference to a <tt>DBusConnection</tt> to the
     * <tt>DBUS_BUS_SESSION</tt> D-Bus bus daemon
     * @throws DBusException if connecting to and registering with the
     * <tt>DBUS_BUS_SESSION</tt> D-Bus bus daemon fails
     */
    public static native long dbus_bus_get_session()
        throws DBusException;

    /**
     * Decrements the reference count of the specified <tt>DBusConnection</tt>
     * and finalizes it if the count reaches zero.
     *
     * @param connection the <tt>DBusConnection</tt> to decrement the reference
     * count of
     */
    public static native void dbus_connection_unref(long connection);

    /**
     * Invokes <tt>org.freedesktop.Notifications.GetCapabilities</tt> through
     * the specified <tt>DBusConnection</tt> in order to retrieve the optional
     * capabilities supported by the freedesktop.org Desktop Notifications
     * server.
     *
     * @param connection the <tt>DBusConnection</tt> with the freedesktop.org
     * Desktop Notifications server
     * @return an array of <tt>String</tt>s listing the optional capabilities
     * supported by the freedesktop.org Desktop Notifications server
     * @throws DBusException if retrieving the optional capabilities of the
     * freedesktop.org Desktop Notifications server fails
     */
    public static native String[] getCapabilities(long connection)
        throws DBusException;

    /**
     * Invokes <tt>org.freedesktop.Notifications.Notify</tt> through the
     * specified <tt>DBusConnection</tt> in order to send a notification to the
     * freedesktop.org Desktop Notifications server.
     *
     * @param connection the <tt>DBusConnection</tt> with the freedesktop.org
     * Desktop Notifications server
     * @param appName the optional name of the application sending the
     * notification
     * @param replacesId the optional notification identifier of an existing
     * notification to be replaced by the notification being sent; <tt>0</tt> to
     * not replace any existing notification
     * @param icon the optional icon to be displayed by the notification if the
     * server supports such display. Not supported by this implementation at
     * this time.
     * @param summary the summary text briefly describing the notification
     * @param body the optional detailed body text of the notification
     * @param expireTimeout the time in milliseconds since the display of the
     * notification after which the notification should automatically close. If
     * <tt>-1</tt>, the notification's expiration time is dependent on the
     * notification server's settings. If <tt>0</tt>, never expires.
     * @return the unique identifier of the sent notification if
     * <tt>replacesId</tt> is <tt>0</tt>; <tt>replacesId</tt> if
     * <tt>replacesId</tt> is not <tt>0</tt>
     * @throws DBusException if sending the notification to the freedesktop.org
     * Desktop Notifications server fails
     */
    public static native long notify(
            long connection,
            String appName,
            long replacesId,
            BufferedImage icon,
            String summary,
            String body,
            int expireTimeout)
        throws DBusException;

    /**
     * Prevents the creation of <tt>GalagoNotification</tt> instances.
     */
    private GalagoNotification()
    {
    }
}
