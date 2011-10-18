/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.dtmf;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.DTMFTone;

/**
 * Sending DTMFs through media service using rfc4733.
 * 
 * @author Damian Minkov
 */
public class DTMF4733 
{
    /**
     * Start sending DTMF.
     * @param audioStream the stream of this call.
     * @param tone
     */
    public void startSendingDTMF(AudioMediaStream audioStream,
        net.java.sip.communicator.service.protocol.DTMFTone tone)
    {
        DTMFTone t = mapTone(tone);
        if(t != null)
            audioStream.startSendingDTMF(t);
    }

    /**
     * Stop sending current DTMF.
     * @param audioStream
     */
    public void stopSendingDTMF(AudioMediaStream audioStream)
    {
        audioStream.stopSendingDTMF();
    }

    /**
     * Maps between protocol and media DTMF objects.
     * @param tone
     * @return
     */
    private DTMFTone
        mapTone(net.java.sip.communicator.service.protocol.DTMFTone tone)
    {
        if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_0))
            return DTMFTone.DTMF_0;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_1))
            return DTMFTone.DTMF_1;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_2))
            return DTMFTone.DTMF_2;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_3))
            return DTMFTone.DTMF_3;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_4))
            return DTMFTone.DTMF_4;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_5))
            return DTMFTone.DTMF_5;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_6))
            return DTMFTone.DTMF_6;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_7))
            return DTMFTone.DTMF_7;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_8))
            return DTMFTone.DTMF_8;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_9))
            return DTMFTone.DTMF_9;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_A))
            return DTMFTone.DTMF_A;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_B))
            return DTMFTone.DTMF_B;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_C))
            return DTMFTone.DTMF_C;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_D))
            return DTMFTone.DTMF_D;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_SHARP))
            return DTMFTone.DTMF_SHARP;
        else if(tone.equals(
            net.java.sip.communicator.service.protocol.DTMFTone.DTMF_STAR))
            return DTMFTone.DTMF_STAR;

        return null;
    }
}
