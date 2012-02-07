/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.dtmf;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Sending DTMFs inband into the audio stream.
 *
 * @author Vincent Lucas
 */
public class DTMFInband
{
    /**
     * Adds a new inband DTMF tone to send.
     *
     * @param audioStream The stream of this call.
     * @param tone The tone (audio signal) to send via the audioStream.
     */
    public void addInbandDTMF(
            AudioMediaStream audioStream,
            net.java.sip.communicator.service.protocol.DTMFTone tone)
    {
        audioStream.addInbandDTMF(DTMFInbandTone.mapTone(tone));
    }
}
