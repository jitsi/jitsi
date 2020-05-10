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

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import net.java.sip.communicator.impl.netaddr.*;
import org.junit.*;

public class Win32LocalhostTest
{
    @Test
    public void testBestAdapterRoute() throws IOException
    {
        InetAddress localhost = Win32LocalhostRetriever
            .getSourceForDestination(InetAddress.getLoopbackAddress());
        assertEquals(InetAddress.getLoopbackAddress(), localhost);

        System.setProperty("java.net.preferIPv6Addresses", "true");
        localhost = Win32LocalhostRetriever
            .getSourceForDestination(InetAddress.getLoopbackAddress());
        assertEquals(InetAddress.getLoopbackAddress(), localhost);
    }
}
