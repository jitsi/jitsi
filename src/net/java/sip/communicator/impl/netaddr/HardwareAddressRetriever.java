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
 * Class to retrieve hardware address of a specific interface.
 *
 * We know that starting Java 6, NetworkInterface has getHardwareAddress method
 * but as we still support Java 5 we have to do it ourself.
 *
 * @author Sebastien Vincent
 */
public class HardwareAddressRetriever
{
    /* load library */
    static
    {
        System.loadLibrary("hwaddressretriever");
    }

    /**
     * Returns the hardware address of a particular interface.
     *
     * @param ifName name of the interface
     * @return byte array representing the hardware address of the interface or
     * null if interface is not found or other system related errors
     */
    public static native byte[] getHardwareAddress(String ifName);
}
