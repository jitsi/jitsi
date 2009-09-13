/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Extends the <tt>MediaStream</tt> interface and adds methods specific to
 * audio streaming.
 *
 * @author Emil Ivov
 */
public interface AudioMediaStream extends MediaStream
{
    public void addSoundLevelListener();

    public void startSendingDTMF();

    public void stopSendingDTMF();

    public void addDTMFListener(DTMFListener);

    public void removeDTMFListener();

    public void setMute();
}
