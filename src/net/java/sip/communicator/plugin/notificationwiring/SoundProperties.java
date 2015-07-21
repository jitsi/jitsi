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
package net.java.sip.communicator.plugin.notificationwiring;

import org.jitsi.service.resources.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 *
 * @author Yana Stamcheva
 */
public final class SoundProperties
{
    /**
     * The incoming message sound id.
     */
    public static final String INCOMING_MESSAGE;

    /**
     * The incoming file sound id.
     */
    public static final String INCOMING_FILE;

    /**
     * The outgoing call sound id.
     */
    public static final String OUTGOING_CALL;

    /**
     * The incoming call sound id.
     */
    public static final String INCOMING_CALL;

    /**
     * The busy sound id.
     */
    public static final String BUSY;

    /**
     * The dialing sound id.
     */
    public static final String DIALING;

    /**
     * The sound id of the sound played when call security is turned on.
     */
    public static final String CALL_SECURITY_ON;

    /**
     * The sound id of the sound played when a call security error occurs.
     */
    public static final String CALL_SECURITY_ERROR;

    /**
     * The hang up sound id.
     */
    public static final String HANG_UP;

    static
    {

        /*
         * Call NotificationActivator.getResources() once because (1) it's not a trivial
         * getter, it caches the reference so it always checks whether the cache
         * has already been built and (2) accessing a local variable is supposed
         * to be faster than calling a method (even if the method is a trivial
         * getter and it's inlined at runtime, it's still supposed to be slower
         * because it will be accessing a field, not a local variable).
         */
        ResourceManagementService resources =
            NotificationWiringActivator.getResources();

        INCOMING_MESSAGE = resources.getSoundPath("INCOMING_MESSAGE");
        INCOMING_FILE = resources.getSoundPath("INCOMING_FILE");
        OUTGOING_CALL = resources.getSoundPath("OUTGOING_CALL");
        INCOMING_CALL = resources.getSoundPath("INCOMING_CALL");
        BUSY = resources.getSoundPath("BUSY");
        DIALING = resources.getSoundPath("DIAL");
        CALL_SECURITY_ON = resources.getSoundPath("CALL_SECURITY_ON");
        CALL_SECURITY_ERROR = resources.getSoundPath("CALL_SECURITY_ERROR");
        HANG_UP = resources.getSoundPath("HANG_UP");
    }

    private SoundProperties() {
    }
}
