/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msofficecomm;

/**
 * Defines the public interface of the out-of-process COM server which
 * integrates Jitsi's presence, IM and VoIP with Microsoft Office/Outlook.
 *
 * @author Lyubomir Marinov
 */
class OutOfProcessServer
{
    static
    {
        System.loadLibrary("jmsofficecomm");
    }

    static native int start();

    static native int stop();

    /**
     * Prevents the initialization of a new <tt>OutOfProcessServer</tt>
     * instance.
     */
    private OutOfProcessServer()
    {
    }
}
