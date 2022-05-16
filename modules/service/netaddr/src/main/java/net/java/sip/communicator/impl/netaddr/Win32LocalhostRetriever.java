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

import static net.java.sip.communicator.impl.netaddr.IPHlpAPI.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.*;
import java.io.*;
import java.net.*;
import lombok.extern.slf4j.*;
import net.java.sip.communicator.impl.netaddr.IPHlpAPI.*;

/**
 * Class to retrieve local address to use for a specific destination. This class
 * works only on Microsoft Windows system.
 */
@Slf4j
public class Win32LocalhostRetriever
{
    /**
     * Native method to retrieve source address to use for a specific
     * destination.
     * <p>
     * This function is only implemented for Microsoft Windows. Do not try to
     * call it from another OS.
     *
     * @param dst destination address
     * @return source address or null if not found.
     * @throws IOException    if an unknown ip address type is encountered
     * @throws Win32Exception if the underlying OS functions fail
     */
    public static InetAddress getSourceForDestination(InetAddress dst)
        throws IOException
    {
        Structure sockaddr;
        short sa_family;
        if (dst instanceof Inet4Address)
        {
            sockaddr_in sock4 = new sockaddr_in();
            sock4.sin_addr = dst.getAddress();
            sock4.sin_family = sa_family = AF_INET;
            sockaddr = sock4;
        }
        else if (dst instanceof Inet6Address)
        {
            sockaddr_in6 sock6 = new sockaddr_in6();
            sock6.sin6_addr = dst.getAddress();
            sock6.sin6_family = sa_family = AF_INET6;
            sock6.sin6_scope_id = ((Inet6Address) dst).getScopeId();
            sockaddr = sock6;
        }
        else
        {
            throw new IOException("Unknown IP address type");
        }

        IntByReference refBestIfIndex = new IntByReference(0);
        int error = INSTANCE.GetBestInterfaceEx(sockaddr, refBestIfIndex);
        if (error != 0)
        {
            throw new Win32Exception(error);
        }

        int bestIfIndex = refBestIfIndex.getValue();

        // The recommended method of calling the GetAdaptersAddresses function
        // is to pre-allocate a 15KB working buffer
        Memory buffer = new Memory(15 * 1024);
        IntByReference size = new IntByReference(0);
        int flags =
            GAA_FLAG_INCLUDE_ALL_INTERFACES
                | GAA_FLAG_INCLUDE_PREFIX
                | GAA_FLAG_SKIP_DNS_SERVER
                | GAA_FLAG_SKIP_FRIENDLY_NAME
                | GAA_FLAG_SKIP_MULTICAST;
        error = INSTANCE
            .GetAdaptersAddresses(sa_family, flags, Pointer.NULL, buffer, size);
        if (error == WinError.ERROR_BUFFER_OVERFLOW)
        {
            buffer = new Memory(size.getValue());
            error = INSTANCE
                .GetAdaptersAddresses(sa_family, flags, Pointer.NULL, buffer,
                    size);
            if (error != WinError.ERROR_SUCCESS)
            {
                throw new Win32Exception(error);
            }
        }

        InetAddress result = null;
        IP_ADAPTER_ADDRESSES_LH adapter = new IP_ADAPTER_ADDRESSES_LH(buffer);
        do
        {
            if (sa_family == AF_INET && adapter.IfIndex == bestIfIndex)
            {
                result = adapter.FirstUnicastAddress.Address.toAddress();
                break;

            }
            else if (sa_family == AF_INET6
                && adapter.Ipv6IfIndex == bestIfIndex)
            {
                result = adapter.FirstUnicastAddress.Address.toAddress();
                break;
            }
            adapter = adapter.Next;
        }
        while (adapter != null);

        return result;
    }
}

