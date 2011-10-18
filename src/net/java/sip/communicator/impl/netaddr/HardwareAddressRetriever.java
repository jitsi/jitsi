/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
