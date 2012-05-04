/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

/**
 * Implements an <tt>AudioSystem</tt> without any devices which allows the user
 * to select to use no audio capture, notification and playback.
 *
 * @author Lyubomir Marinov
 */
public class NoneAudioSystem
    extends AudioSystem
{
    public static final String LOCATOR_PROTOCOL = "none";

    public NoneAudioSystem()
        throws Exception
    {
        super(LOCATOR_PROTOCOL);
    }

    protected void doInitialize()
        throws Exception
    {
    }

    @Override
    public String toString()
    {
        return "None";
    }
}
