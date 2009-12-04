/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * Extends the <tt>MediaStream</tt> interface and adds methods specific to
 * audio streaming.
 *
 * @author Emil Ivov
 */
public interface AudioMediaStream
    extends MediaStream
{
    /**
     * Sets <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt>
     * registered to receive notifications for changes in the levels of the
     * party that's at the other end of this stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we want to stop stream audio level
     * measurements.
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener);

    /**
     * Registers <tt>listener</tt> as the <tt>SoundLevelListener</tt> that will
     * receive notifications for changes in the levels of conference
     * participants that the remote party could be mixing.
     *
     * @param listener the <tt>SoundLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we'd like to stop receiving notifications.
     */
    public void setConferenceMemberAudioLevelListener(
                                            SimpleAudioLevelListener listener);

    /**
     * Sets <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt>
     * registered to receive notifications for changes in the levels of the
     * audio that this stream is sending out.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> that we'd like to
     * register or <tt>null</tt> if we want to stop local audio level
     * measurements.
     */
    public void setLocalUserAudioLevelListener(
                                            SimpleAudioLevelListener listener);

    /**
     * Starts sending the specified <tt>DTMFTone</tt> until the
     * <tt>stopSendingDTMF()</tt> method is called. Callers should keep in mind
     * the fact that calling this method would most likely interrupt all audio
     * transmission until the corresponding stop method is called. Also, calling
     * this method successively without invoking the corresponding stop method
     * between the calls, would simply replace the <tt>DTMFTone</tt> from the
     * first call with that from the second.
     *
     * @param tone the <tt>DTMFTone</tt> that we'd like to start sending.
     */
    public void startSendingDTMF(DTMFTone tone);

    /**
     * Interrupts transmission of a <tt>DTMFTone</tt> started with the
     * <tt>startSendingDTMF</tt> method. This method has no effect if no tone
     * is being currently sent.
     */
    public void stopSendingDTMF();

    /**
     * Registers a listener that would receive notification events if the
     * remote party starts sending DTMF tones to us.
     *
     * @param listener the <tt>DTMFListener</tt> that we'd like to register.
     */
    public void addDTMFListener(DTMFListener listener);

    /**
     * Removes <tt>listener</tt> from the list of <tt>DTMFListener</tt>s
     * registered to receive events for incoming DTMF tones.
     *
     * @param listener the listener that we'd like to unregister
     */
    public void removeDTMFListener(DTMFListener listener);
}
