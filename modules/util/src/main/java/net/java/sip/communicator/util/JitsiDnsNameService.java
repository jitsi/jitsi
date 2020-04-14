/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2016 Atlassian Pty Ltd
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
package net.java.sip.communicator.util;

import java.lang.reflect.Method;
import java.net.*;

import org.xbill.DNS.*;

import sun.net.spi.nameservice.*;

/**
 * JNDI DNS service to send DNS lookup through the dnsjava <tt>Lookup</tt>
 * infrastructure. This is needed to catch lookups that are not made
 * through the DNSSEC-aware <tt>NetworkUtils</tt>.
 * 
 * @author Ingo Bauersachs
 */
public class JitsiDnsNameService
    implements NameService
{
    private static boolean v6first;
    private static Name localhostName = null;
    private static InetAddress[] localhostAddresses = null;
    private static InetAddress[] localhostNamedAddresses = null;
    private static boolean addressesLoaded = false;

    static
    {
        v6first = Boolean.getBoolean("java.net.preferIPv6Addresses");
        try
        {
            // retrieve the name from the system that is used as localhost
            Class<?> inClass = Class.forName("java.net.InetAddressImplFactory");
            Method create = inClass.getDeclaredMethod("create");
            create.setAccessible(true);

            Object impl = create.invoke(null);
            Class<?> clazz = Class.forName("java.net.InetAddressImpl");
            Method hostname = clazz.getMethod("getLocalHostName");
            hostname.setAccessible(true);
            localhostName = new Name((String) hostname.invoke(impl));
            Method lookup = clazz.getMethod("lookupAllHostAddr", String.class);
            lookup.setAccessible(true);

            localhostNamedAddresses = (InetAddress[])lookup.invoke(impl,
                localhostName.toString());
            localhostAddresses = (InetAddress[])lookup.invoke(impl,
                "localhost");

            addressesLoaded = true;
        }
        catch (Exception e)
        {
            System.err.println("Could not obtain localhost: " + e);
        }
    }

    @Override
    public String getHostByAddr(final byte[] bytes)
        throws UnknownHostException
    {
        InetAddress addr = InetAddress.getByAddress(bytes);
        if (addr.isLoopbackAddress())
        {
            return "localhost";
        }

        Name name = ReverseMap.fromAddress(addr);
        Lookup l = new Lookup(name, Type.PTR);
        Record[] records = l.run();
        if (records == null)
        {
            throw new UnknownHostException();
        }

        return ((PTRRecord) records[0]).getTarget().toString();
    }

    @Override
    public InetAddress[] lookupAllHostAddr(final String host)
        throws UnknownHostException
    {
        Name n;
        try
        {
            n = new Name(host);
            // Avoid sending queries to a nameserver for localhost
            // and the machine's hostname. Unless in an enterprise environment
            // the nameserver wouldn't know about that machine name anyway.
            if (addressesLoaded)
            {
                if (n.equals(localhostName))
                {
                    return localhostNamedAddresses;
                }
                else if (host.equals("localhost"))
                {
                    return localhostAddresses;
                }
            }
        }
        catch (TextParseException e)
        {
            throw new UnknownHostException(host);
        }

        Lookup l = new Lookup(n, v6first ? Type.AAAA : Type.A);
        Record[] r = l.run();
        if (r == null || r.length == 0)
        {
            l = new Lookup(n, v6first ? Type.A : Type.AAAA);
            r = l.run();
        }

        if (r == null || r.length == 0)
        {
            throw new UnknownHostException(host);
        }

        InetAddress[] results = new InetAddress[r.length];
        for (int i = 0; i < r.length; i++)
        {
            if (r[i] instanceof AAAARecord)
            {
                results[i] = ((AAAARecord) r[i]).getAddress();
            }
            else if (r[i] instanceof ARecord)
            {
                results[i] = ((ARecord) r[i]).getAddress();
            }
        }

        return results;
    }
}
