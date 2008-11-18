/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.awt.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents an <code>OperationSet</code> giving access to video-specific
 * functionality in telephony such as visual <code>Component</code>s displaying
 * video and listening to dynamic availability of such <code>Component</code>s.
 * 
 * @author Lubomir Marinov
 */
public interface OperationSetVideoTelephony
    extends OperationSet
{

    /**
     * Adds a specific <code>VideoListener</code> to this telephony in order to
     * receive notifications when visual/video <code>Component</code>s are being
     * added and removed for a specific <code>CallParticipant</code>.
     * 
     * @param participant the <code>CallParticipant</code> whose video the
     *            specified listener is to be notified about
     * @param listener the <code>VideoListener</code> to be notified when
     *            visual/video <code>Component</code>s are being added or
     *            removed for <code>participant</code>
     */
    void addVideoListener(CallParticipant participant, VideoListener listener);

    Component createLocalVisualComponent(CallParticipant participant,
        VideoListener listener) throws OperationFailedException;

    void disposeLocalVisualComponent(CallParticipant participant,
        Component component);

    /**
     * Gets the visual/video <code>Component</code>s available in this telephony
     * for a specific <code>CallParticipant</code>.
     * 
     * @param participant the <code>CallParticipant</code> whose videos are to
     *            be retrieved
     * @return an array of the visual <code>Component</code>s available in this
     *         telephony for the specified <code>participant</code>
     */
    Component[] getVisualComponents(CallParticipant participant);

    /**
     * Removes a specific <code>VideoListener</code> from this telephony in
     * order to no longer have it receive notifications when visual/video
     * <code>Component</code>s are being added and removed for a specific
     * <code>CallParticipant</code>.
     * 
     * @param participant the <code>CallParticipant</code> whose video the
     *            specified listener is to no longer be notified about
     * @param listener the <code>VideoListener</code> to no longer be notified
     *            when visual/video <code>Component</code>s are being added or
     *            removed for <code>participant</code>
     */
    void removeVideoListener(CallParticipant participant, VideoListener listener);
}
