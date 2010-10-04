/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * This event represents starting or ending reception of a specific
 * <tt>DTMFTone</tt>.
 *
 * @author Emil Ivov
 */
public class DTMFToneEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The tone that this event is pertaining to.
     */
    private final DTMFTone dtmfTone;

    /**
     * Creates an instance of this <tt>DTMFToneEvent</tt> with the specified
     * source stream and DTMF tone.
     *
     * @param source the <tt>AudioMediaSteam</tt> instance that received the
     * tone.
     * @param dtmfTone the tone that we (started/stopped) receiving.
     */
    public DTMFToneEvent(AudioMediaStream source, DTMFTone dtmfTone)
    {
        super(source);

        this.dtmfTone = dtmfTone;
    }

    /**
     * Returns the <tt>DTMFTone</tt> instance that this event pertains to.
     *
     * @return the <tt>DTMFTone</tt> instance that this event pertains to.
     */
    public DTMFTone getDtmfTone()
    {
        return dtmfTone;
    }
}
