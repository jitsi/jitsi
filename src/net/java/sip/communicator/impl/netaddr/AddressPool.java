/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.net.*;
import java.util.*;

/**
 * The class scans all local interfaces discovering all addresses, and starts
 * constantly testing them one by one in order to verify their properties. It
 * also orders the addresses, putting first those that it has determined to be
 * more easily usable and that offer better chances of connection success.
 *
 * @author Emil Ivov
 */
public class AddressPool
{
    private final Map<AddressPoolEntry, AddressDiagnosticsKit> diagnosticsKits =
        new Hashtable<AddressPoolEntry, AddressDiagnosticsKit>();

    private final List<AddressPoolEntry> addressEntries =
        new ArrayList<AddressPoolEntry>();

    public AddressPool()
    {
    }

    private void initPool()
    {
        Enumeration<NetworkInterface> localIfaces = null;
        try
        {
            localIfaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException ex)
        {
            throw new RuntimeException(
                "Failed to retrieve local interfaces!");
        }

        //loop over all local network interfaces
        while (localIfaces.hasMoreElements())
        {
            NetworkInterface iFace = localIfaces.nextElement();

            Enumeration<InetAddress> addresses = iFace.getInetAddresses();

            //addresses loop
            while (addresses.hasMoreElements())
            {
                InetAddress address = addresses.nextElement();

                //we don't care about loopback addresses
                if(address.isLoopbackAddress())
                    continue;

                AddressPoolEntry addrEntry =
                    new AddressPoolEntry(address, iFace);
                AddressDiagnosticsKit diagKit =
                        new AddressDiagnosticsKit(addrEntry);

                addressEntries.add(addrEntry);
                diagnosticsKits.put(addrEntry, diagKit);
                diagKit.start();
            } //addresses loop
        } //interfaces loop
    }

    public static void main(String[] args)
    {
        AddressPool pool = new AddressPool();
        pool.initPool();
    }
}
