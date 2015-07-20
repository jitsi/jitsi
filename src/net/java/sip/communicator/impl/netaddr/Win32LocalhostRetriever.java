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
package net.java.sip.communicator.impl.netaddr;

/**
 * Class to retrieve local address to use for a specific destination.
 * This class works only on Microsoft Windows system.
 *
 * @author Sebastien Vincent
 */
public class Win32LocalhostRetriever
{
    /* load library */
    static
    {
        System.loadLibrary("LocalhostRetriever");
    }

    /**
     * Constructor.
     */
    public Win32LocalhostRetriever()
    {
    }

    /**
     * Native method to retrieve source address to use for a specific
     * destination.
     *
     * @param dst destination address
     * @return source address or null if error
     * @note This function is only implemented for Microsoft Windows
     * (>= XP SP1). Do not try to call it from another OS.
     */
    public native static byte[] getSourceForDestination(byte[] dst);
}

