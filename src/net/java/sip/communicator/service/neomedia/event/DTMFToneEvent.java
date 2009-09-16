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
public class DTMFToneEvent extends EventObject
{
    /**
     * The tone that this event is pertaining to.
     */
    private DTMFTone dtmfTone = null;

    /**
     * Creates an instance of this <tt>DTMFToneEvent</tt> with the specified
     * source stream and dtmf tone.
     *
     * @param source the <tt>AudioMediaSteam</tt> instance that received the
     * tone.
     * @param tone the tone that we (started/stopped) receing.
     */
    public DTMFToneEvent(AudioMediaStream source, DTMFTone tone)
    {
        super(source);

        this.dtmfTone = tone;
    }

    /**
     * Returns the <tt>DTMFTone</tt> instance that this event is pertaining to.
     *
     * @return the <tt>DTMFTone</tt> instance that this event is pertaining to.
     */
    public DTMFTone getDtmfTone()
    {
        return dtmfTone;
    }

}
