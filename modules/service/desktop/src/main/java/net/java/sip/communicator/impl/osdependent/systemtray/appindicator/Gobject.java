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
package net.java.sip.communicator.impl.osdependent.systemtray.appindicator;

import com.sun.jna.*;

/**
 * JNA mappings for GTK GObject types that are required for the tray menu.
 *
 * @author Ingo Bauersachs
 */
interface Gobject extends Library
{
    Gobject INSTANCE =
        Native.load("gobject-2.0", Gobject.class);

    interface SignalHandler extends Callback
    {
        void signal(Pointer widget, Pointer data);
    }

    /**
     * Connects a GCallback function to a signal for a particular object.
     * Similar to g_signal_connect(), but allows to provide a GClosureNotify for
     * the data which will be called when the signal handler is disconnected and
     * no longer used. Specify connect_flags if you need ..._after() or
     * ..._swapped() variants of this function.
     *
     * @param instance the instance to connect to.
     * @param detailed_signal a string of the form "signal-name::detail".
     * @param c_handler the GCallback to connect.
     * @param data data to pass to c_handler calls.
     * @param destroy_data a GClosureNotify for data.
     * @param connect_flags a combination of GConnectFlags.
     *
     * @return the handler id (always greater than 0 for successful connections)
     */
    long g_signal_connect_data(Pointer instance, String detailed_signal,
        SignalHandler c_handler, Pointer data, Pointer destroy_data,
        int connect_flags);

    /**
     * Disconnects a handler from an instance so it will not be called during
     * any future or currently ongoing emissions of the signal it has been
     * connected to. The handler_id becomes invalid and may be reused. The
     * handler_id has to be a valid signal handler id, connected to a signal of
     * instance .
     *
     * @param instance The instance to remove the signal handler from.
     * @param handler_id Handler id of the handler to be disconnected.
     */
    void g_signal_handler_disconnect(Pointer instance, long handler_id);

    /**
     * Decreases the reference count of object. When its reference count drops
     * to 0, the object is finalized (i.e. its memory is freed). If the pointer
     * to the GObject may be reused in future (for example, if it is an instance
     * variable of another object), it is recommended to clear the pointer to
     * NULL rather than retain a dangling pointer to a potentially invalid
     * GObject instance. Use g_clear_object() for this.
     *
     * @param object a GObject.
     */
    void g_object_unref(Pointer object);

    /**
     * Increases the reference count of object.
     *
     * @param object a GObject.
     * @return the same object.
     */
    Pointer g_object_ref(Pointer object);
}
