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
package net.java.sip.communicator.impl.protocol.jabber;

import java.security.cert.*;
import javax.net.ssl.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of the OperationSetTLS for the Jabber protocol.
 *
 * @author Markus Kilas
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

    /**
     * @see OperationSetTLS#getCipherSuite()
     */
    @Override
    public String getCipherSuite()
    {
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

    /**
     * @see OperationSetTLS#getProtocol() 
     */
    @Override
    public String getProtocol()
    {
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

    /**
     * @see OperationSetTLS#getServerCertificates() 
     */
    @Override
    public Certificate[] getServerCertificates()
    {
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
