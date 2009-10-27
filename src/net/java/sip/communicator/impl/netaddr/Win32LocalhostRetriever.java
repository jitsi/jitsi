/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.net.*;

/**
 * Class to retrieve local address to use for a 
 * specific destination.
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
     * Native function to retrieve source address to use for a specific destination.
     * @param dst destination address
     * @return source address or null if error
     * @note This function is only implemented for Microsoft Windows (>= XP SP1).
     * Do not try to call it from another OS.
     */
    public native static byte[] getSourceForDestination(byte[] dst);
}

