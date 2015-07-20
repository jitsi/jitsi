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
package net.java.sip.communicator.service.dns;

import java.net.*;

/**
 * Runtime exception that is thrown when a DNSSEC validation failure occurred.
 * This is not a checked exception or a derivative of
 * {@link UnknownHostException} so that existing code does not retry the lookup
 * (potentially in a loop).
 *
 * @author Ingo Bauersachs
 */
public class DnssecRuntimeException
    extends RuntimeException
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a new instance of this class.
     * @param message The reason why this exception is thrown.
     */
    public DnssecRuntimeException(String message)
    {
        super(message);
    }
}
