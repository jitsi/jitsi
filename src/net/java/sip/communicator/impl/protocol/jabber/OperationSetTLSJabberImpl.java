/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.security.cert.*;
import javax.net.ssl.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of the OperationSetTLS for the Jabber protocol.
 *
 * @author Markus Kilås
 */
public class OperationSetTLSJabberImpl
    implements OperationSetTLS
{
    private final ProtocolProviderServiceJabberImpl jabberService;

    public OperationSetTLSJabberImpl(
        ProtocolProviderServiceJabberImpl jabberService)
    {
        this.jabberService = jabberService;
    }

    public String getCipherSuite() {
        final String result;
        final SSLSocket socket = jabberService.getSSLSocket();
        if (socket == null)
        {
            result = null;
        }
        else
        {
            result = socket.getSession().getCipherSuite();
        }
        return result;
    }

    public String getProtocol() {
        final String result;
        final SSLSocket socket = jabberService.getSSLSocket();
        if (socket == null)
        {
            result = null;
        }
        else
        {
            result = socket.getSession().getProtocol();
        }
        return result;
    }

    public Certificate[] getServerCertificates() {
        Certificate[] result = null;
        final SSLSocket socket = jabberService.getSSLSocket();
        if (socket != null)
        {
            try
            {
                result = socket.getSession().getPeerCertificates();
            } 
            catch (SSLPeerUnverifiedException ignored) // NOPMD
            {
                // result will be null
            }
        }
        return result;
    }
    
}
