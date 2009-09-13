/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Extends the <tt>MediaStream</tt> interface and adds methods specific to
 * audio streaming.
 *
 * @author Emil Ivov
 */
public interface AudioMediaStream extends MediaStream
{
    public void addSoundLevelListener();

    public void startSendingDTMF(DTMFTone tone);

    public void stopSendingDTMF();

    /**
     * Registers a listener that would receive notification events if the
     * remote party starts sending DTMF tones to us.
     *
     * @param listener the <tt>DTMFListener</tt> that we'd like to register.
     */
    public void addDTMFListener(DTMFListener listener);

    /**
     *
     * @param listener
     */
    public void removeDTMFListener(DTMFListener listener);

    /**
     * Causes this <tt>AudioMediaStream</tt> to stop transmitting the audio
     * being fed from this stream's <tt>MediaDevice</tt> and transmit silence
     * instead.
     *
     * @param on <tt>true</tt> if we are to start transmitting silence and
     * <tt>false</tt> if we are to use media from this stream's
     * <tt>MediaDevice</tt> again.
     */
    public void setMute(boolean on);
}
