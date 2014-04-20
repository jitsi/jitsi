/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.java.sip.communicator.service.protocol.OperationSetConnectionInfo;

/**
 * An <tt>OperationSet</tt> that allows access to connection information used
 * by the protocol provider.
 *
 * @author Markus Kilas
 */
public class OperationSetConnectionInfoJabberImpl
    implements OperationSetConnectionInfo
{
    private final ProtocolProviderServiceJabberImpl jabberService;

    public OperationSetConnectionInfoJabberImpl(
            ProtocolProviderServiceJabberImpl jabberService)
    {
        this.jabberService = jabberService;
    }

    public String getServerAddress() {
        SocketAddress address
                = jabberService.getSocket().getRemoteSocketAddress();
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getHostName();
        }
        else
        {
            return address.toString();
        }
    }

    public int getServerPort() {
        return jabberService.getSocket().getPort();
    }
}
