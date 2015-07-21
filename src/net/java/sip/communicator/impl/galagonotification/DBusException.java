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

/**
 * Implements <tt>Exception</tt> for D-Bus errors reported through the native
 * <tt>DBusError</tt> structure.
 *
 * @author Lubomir Marinov
 */
public class DBusException
    extends Exception
{

    /**
     * Silences a serialization warning. Besides, we don't have fields of our
     * own so the default serialization routine will always work for us.
     */
    private static final long serialVersionUID = 0;

    /**
     * Initializes a new <tt>DBusException</tt> instance with the specified
     * detail message.
     *
     * @param message the detail message to later be reported by the new
     * instance through its {@link #getMessage()}
     */
    public DBusException(String message)
    {
        super(message);
    }
}
