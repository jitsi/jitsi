/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.protocol.*;

/**
 * The quality controls we use to control other party video presets.
 * @author Damian Minkov
 */
public interface QualityControl
{
    /**
     * The currently used quality preset announced as receive by remote party.
     * @return the current quality preset.
     */
    public QualityPreset getRemoteReceivePreset();

    /**
     * The minimum preset that the remote party is sending and we are receiving.
     * @return the minimum remote preset.
     */
    public QualityPreset getRemoteSendMinPreset();

    /**
     * The maximum preset that the remote party is sending and we are receiving.
     * @return the maximum preset announced from remote party as send.
     */
    public QualityPreset getRemoteSendMaxPreset();

    /**
     * Changes remote send preset. This doesn't have impact of current stream.
     * But will have on next media changes.
     * With this we can try to change the resolution that the remote part
     * is sending.
     * @param preset the new preset value.
     */
    public void setRemoteSendMaxPreset(QualityPreset preset);

    /**
     * Changes remote send preset and protocols who can handle the changes
     * will implement this for re-inviting the other party or just sending that
     * media has changed.
     * @param preset the new preset.
     * @throws OperationFailedException
     */
    public void setPreferredRemoteSendMaxPreset(QualityPreset preset)
        throws OperationFailedException;
}
