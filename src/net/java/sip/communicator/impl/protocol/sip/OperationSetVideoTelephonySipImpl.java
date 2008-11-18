/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Implements <code>OperationSetVideoTelephony</code> in order to give access to
 * video-specific functionality in the SIP protocol implementation such as
 * visual <code>Component</code>s displaying video and listening to dynamic
 * availability of such <code>Component</code>s. Because the video in the SIP
 * protocol implementation is provided by the <code>CallSession</code>, this
 * <code>OperationSetVideoTelephony</code> just delegates to the
 * <code>CallSession</code> while hiding the <code>CallSession</code> as the
 * provider of the video and pretending this
 * <code>OperationSetVideoTelephony</code> is the provider because other
 * implementation may not provider their video through the
 * <code>CallSession</code>.
 * 
 * @author Lubomir Marinov
 */
public class OperationSetVideoTelephonySipImpl
    implements OperationSetVideoTelephony
{

    /*
     * Delegates to the CallSession of the Call of the specified CallParticipant
     * because the video is provided by the CallSession in the SIP protocol
     * implementation. Because other OperationSetVideoTelephony implementations
     * may not provide their video through the CallSession, this implementation
     * promotes itself as the provider of the video by replacing the CallSession
     * in the VideoEvents it fires.
     */
    public void addVideoListener(CallParticipant participant,
        VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        ((CallSipImpl) participant.getCall()).getMediaCallSession()
            .addVideoListener(
                new InternalVideoListener(this, participant, listener));
    }

    public Component createLocalVisualComponent(CallParticipant participant,
        VideoListener listener) throws OperationFailedException
    {
        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();

        if (callSession != null)
        {
            try
            {
                return callSession.createLocalVisualComponent(listener);
            }
            catch (MediaException ex)
            {
                throw new OperationFailedException(
                    "Failed to create visual Component for local video (capture).",
                    OperationFailedException.INTERNAL_ERROR, ex);
            }
        }
        return null;
    }

    public void disposeLocalVisualComponent(CallParticipant participant,
        Component component)
    {
        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();

        if (callSession != null)
        {
            callSession.disposeLocalVisualComponent(component);
        }
    }

    /*
     * Delegates to the CallSession of the Call of the specified CallParticipant
     * because the video is provided by the CallSession in the SIP protocol
     * implementation.
     */
    public Component[] getVisualComponents(CallParticipant participant)
    {
        CallSession callSession =
            ((CallSipImpl) participant.getCall()).getMediaCallSession();

        return (callSession != null) ? callSession.getVisualComponents()
            : new Component[0];
    }

    /*
     * Delegates to the CallSession of the Call of the specified CallParticipant
     * because the video is provided by the CallSession in the SIP protocol
     * implementation. Because other OperationSetVideoTelephony implementations
     * may not provide their video through the CallSession, this implementation
     * promotes itself as the provider of the video by replacing the CallSession
     * in the VideoEvents it fires.
     */
    public void removeVideoListener(CallParticipant participant,
        VideoListener listener)
    {
        if (listener != null)
        {
            ((CallSipImpl) participant.getCall()).getMediaCallSession()
                .removeVideoListener(
                    new InternalVideoListener(this, participant, listener));
        }
    }

    /**
     * Represents a <code>VideoListener</code> which forwards notifications to a
     * specific delegate <code>VideoListener</code> and hides the original
     * <code>VideoEvent</code> sender from it by pretending the sender is a
     * specific <code>OperationSetVideoTelephony</code>. It's necessary in order
     * to hide from the <code>VideoListener</code>s the fact that the video of
     * the SIP protocol implementation is managed by <code>CallSession</code>.
     */
    private static class InternalVideoListener
        implements VideoListener
    {

        /**
         * The <code>VideoListener</code> this implementation hides the original
         * <code>VideoEvent</code> source from.
         */
        private final VideoListener delegate;

        /**
         * The <code>CallParticipant</code> whose videos {@link #delegate} is
         * interested in.
         */
        private final CallParticipant participant;

        /**
         * The <code>OperationSetVideoTelephony</code> which is to be presented
         * as the source of the <code>VideoEvents</code> forwarded to
         * {@link #delegate}.
         */
        private final OperationSetVideoTelephony telephony;

        /**
         * Initializes a new <code>InternalVideoListener</code> which is to
         * impersonate the sources of <code>VideoEvents</code> with a specific
         * <code>OperationSetVideoTelephony</code> for a specific
         * <code>VideoListener</code> interested in the videos of a specific
         * <code>CallParticipant</code>.
         * 
         * @param telephony the <code>OperationSetVideoTelephony</code> which is
         *            to be stated as the source of the <code>VideoEvent</code>
         *            sent to the specified delegate <code>VideoListener</code>
         * @param participant the <code>CallParticipant</code> whose videos the
         *            specified delegate <code>VideoListener</code> is
         *            interested in
         * @param delegate the <code>VideoListener</code> which shouldn't know
         *            that the videos in the SIP protocol implementation is
         *            managed by the CallSession and not by the specified
         *            <code>telephony</code>
         */
        public InternalVideoListener(OperationSetVideoTelephony telephony,
            CallParticipant participant, VideoListener delegate)
        {
            if (participant == null)
                throw new NullPointerException("participant");

            this.telephony = telephony;
            this.participant = participant;
            this.delegate = delegate;
        }

        /*
         * Two InternalVideoListeners are equal if they impersonate the sources
         * of VideoEvents with equal OperationSetVideoTelephonies for equal
         * delegate VideoListeners added to equal CallParticipants.
         */
        public boolean equals(Object other)
        {
            if (other == this)
                return true;
            if ((other == null) || !other.getClass().equals(getClass()))
                return false;

            InternalVideoListener otherListener = (InternalVideoListener) other;

            return otherListener.telephony.equals(telephony)
                && otherListener.participant.equals(participant)
                && otherListener.delegate.equals(delegate);
        }

        public int hashCode()
        {
            return (telephony.hashCode() << 16) + (delegate.hashCode() >> 16);
        }

        /*
         * Upon receiving a VideoEvent, sends to delegate a new VideoEvent of
         * the same type and with the same visual Component but with the source
         * of the event being set to #telephony. Thus the fact that the
         * CallSession is the original source is hidden from the clients of
         * OperationSetVideoTelephony.
         */
        public void videoAdded(VideoEvent event)
        {
            delegate.videoAdded(new VideoEvent(this, event.getType(), event
                .getVisualComponent(), event.getOrigin()));
        }

        /*
         * Upon receiving a VideoEvent, sends to delegate a new VideoEvent of
         * the same type and with the same visual Component but with the source
         * of the event being set to #telephony. Thus the fact that the
         * CallSession is the original source is hidden from the clients of
         * OperationSetVideoTelephony.
         */
        public void videoRemoved(VideoEvent event)
        {
            delegate.videoAdded(new VideoEvent(this, event.getType(), event
                .getVisualComponent(), event.getOrigin()));
        }
    }
}
