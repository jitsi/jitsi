/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * ProtocolProviderFactorySSH.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.protocol.*;

import org.osgi.framework.*;

/**
 *
 * @author Shobhit Jindal
 */
public abstract class ProtocolProviderFactorySSH
        extends ProtocolProviderFactory
{
    /**
     * The name of a property representing the IDENTITY_FILE of the protocol for
     * a ProtocolProviderFactory.
     */
    public static final String IDENTITY_FILE = "IDENTITY_FILE";
    
    /**
     * The name of a property representing the KNOWN_HOSTS_FILE of the protocol
     * for a ProtocolProviderFactory.
     */
    public static final String KNOWN_HOSTS_FILE = "KNOWN_HOSTS_FILE";

    protected ProtocolProviderFactorySSH(BundleContext bundleContext,
        String protocolName)
    {
        super(bundleContext, protocolName);
    }
}
