/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

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

    /**
     * The telephony-related functionality this extension builds upon.
     */
    private final OperationSetBasicTelephonySipImpl basicTelephony;

    /**
     * Initializes a new <code>OperationSetVideoTelephonySipImpl</code> instance
     * which builds upon the telephony-related functionality of a specific
     * <code>OperationSetBasicTelephonySipImpl</code>.
     *
     * @param basicTelephony the <code>OperationSetBasicTelephonySipImpl</code>
     *            the new extension should build upon
     */
    public OperationSetVideoTelephonySipImpl(
        OperationSetBasicTelephonySipImpl basicTelephony)
    {
        this.basicTelephony = basicTelephony;
    }

    /*
     * Delegates to the CallSession of the Call of the specified CallPeer
     * because the video is provided by the CallSession in the SIP protocol
     * implementation. Because other OperationSetVideoTelephony implementations
     * may not provide their video through the CallSession, this implementation
     * promotes itself as the provider of the video by replacing the CallSession
     * in the VideoEvents it fires.
     */
    public void addVideoListener(CallPeer peer,
        VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        ((CallSipImpl) peer.getCall()).getMediaCallSession()
            .addVideoListener(
                new InternalVideoListener(this, peer, listener));
    }

    /*
     * Implements OperationSetVideoTelephony#createLocalVisualComponent(
     * CallPeer, VideoListener). Delegates to CallSession#createLocalVisualComponent(
     * VideoListener) of the Call of the specified CallPeer because the
     * CallSession manages the visual components which represent local video.
     */
    public Component createLocalVisualComponent(CallPeer peer,
        VideoListener listener) throws OperationFailedException
    {
        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();

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

    /*
     * Implements OperationSetVideoTelephony#disposeLocalVisualComponent(
     * CallPeer, Component). Delegates to CallSession#disposeLocalVisualComponent(
     * Component) of the Call of the specified CallPeer because the
     * CallSession manages the visual components which represent local video.
     */
    public void disposeLocalVisualComponent(CallPeer peer,
        Component component)
    {
        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();

        if (callSession != null)
            callSession.disposeLocalVisualComponent(component);
    }

    /*
     * Delegates to the CallSession of the Call of the specified CallPeer
     * because the video is provided by the CallSession in the SIP protocol
     * implementation.
     */
    public Component[] getVisualComponents(CallPeer peer)
    {
        CallSession callSession =
            ((CallSipImpl) peer.getCall()).getMediaCallSession();

        return (callSession != null) ? callSession.getVisualComponents()
            : new Component[0];
    }

    /*
     * Delegates to the CallSession of the Call of the specified CallPeer
     * because the video is provided by the CallSession in the SIP protocol
     * implementation. Because other OperationSetVideoTelephony implementations
     * may not provide their video through the CallSession, this implementation
     * promotes itself as the provider of the video by replacing the CallSession
     * in the VideoEvents it fires.
     */
    public void removeVideoListener(CallPeer peer,
        VideoListener listener)
    {
        if (listener != null)
        {
            ((CallSipImpl) peer.getCall()).getMediaCallSession()
                .removeVideoListener(
                    new InternalVideoListener(this, peer, listener));
        }
    }

    /*
     * Implements OperationSetVideoTelephony#setLocalVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {

        /*
         * Modify the local media setup to reflect the requested setting for the
         * streaming of the local video.
         */
        CallSipImpl sipCall = (CallSipImpl) call;
        CallSession callSession = sipCall.getMediaCallSession();

        try
        {
            callSession.setLocalVideoAllowed(allowed);
        }
        catch (MediaException ex)
        {
            throw new OperationFailedException(
                    "Failed to allow/disallow the streaming of local video.",
                    OperationFailedException.INTERNAL_ERROR, ex);
        }

        /*
         * Once the local state has been modified, re-invite all
         * CallPeers to re-negotiate the modified media setup.
         */
        Iterator<CallPeer> peers = call.getCallPeers();
        while (peers.hasNext())
        {
            CallPeerSipImpl peer
                = (CallPeerSipImpl) peers.next();
            String sdpOffer = null;

            try
            {
                sdpOffer
                    = callSession.createSdpOffer(
                        peer.getSdpDescription());
            }
            catch (MediaException ex)
            {
                throw new OperationFailedException(
                        "Failed to create re-invite offer for peer "
                            + peer,
                        OperationFailedException.INTERNAL_ERROR,
                        ex);
            }

            basicTelephony.sendInviteRequest(peer, sdpOffer);
        }
    }

    /*
     * Implements OperationSetVideoTelephony#isLocalVideoAllowed(Call).
     * Delegates to CallSession#isLocalVideoAllowed() of the specified Call.
     */
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((CallSipImpl) call).getMediaCallSession().isLocalVideoAllowed();
    }

    /*
     * Implements OperationSetVideoTelephony#isLocalVideoStreaming(Call).
     * Delegates to CallSession#isLocalVideoStreaming() of the specified Call.
     */
    public boolean isLocalVideoStreaming(Call call)
    {
        return ((CallSipImpl) call)
            .getMediaCallSession().isLocalVideoStreaming();
    }

    /*
     * Implements OperationSetVideoTelephony#addPropertyChangeListener(Call,
     * PropertyChangeListener). Delegates to CallSession#addPropertyChangeListener(
     * PropertyChangeListener) of the specified Call because CallSession
     * contains the properties associated with a Call.
     */
    public void addPropertyChangeListener(
            Call call, PropertyChangeListener listener)
    {
        ((CallSipImpl) call)
            .getMediaCallSession().addPropertyChangeListener(listener);
    }

    /*
     * Implements OperationSetVideoTelephony#removePropertyChangeListener(Call,
     * PropertyChangeListener). Delegates to CallSession#removePropertyChangeListener(
     * PropertyChangeListener) of the specified Call because CallSession
     * contains the properties associated with a Call.
     */
    public void removePropertyChangeListener(
            Call call, PropertyChangeListener listener)
    {
        ((CallSipImpl) call)
            .getMediaCallSession().removePropertyChangeListener(listener);
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
         * The <code>CallPeer</code> whose videos {@link #delegate} is
         * interested in.
         */
        private final CallPeer peer;

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
         * <code>CallPeer</code>.
         *
         * @param telephony the <code>OperationSetVideoTelephony</code> which is
         *            to be stated as the source of the <code>VideoEvent</code>
         *            sent to the specified delegate <code>VideoListener</code>
         * @param peer the <code>CallPeer</code> whose videos the
         *            specified delegate <code>VideoListener</code> is
         *            interested in
         * @param delegate the <code>VideoListener</code> which shouldn't know
         *            that the videos in the SIP protocol implementation is
         *            managed by the CallSession and not by the specified
         *            <code>telephony</code>
         */
        public InternalVideoListener(OperationSetVideoTelephony telephony,
            CallPeer peer, VideoListener delegate)
        {
            if (peer == null)
                throw new NullPointerException("peer cannot be null");

            this.telephony = telephony;
            this.peer = peer;
            this.delegate = delegate;
        }

        /*
         * Two InternalVideoListeners are equal if they impersonate the sources
         * of VideoEvents with equal OperationSetVideoTelephonies for equal
         * delegate VideoListeners added to equal CallPeer-s.
         */
        public boolean equals(Object other)
        {
            if (other == this)
                return true;
            if ((other == null) || !other.getClass().equals(getClass()))
                return false;

            InternalVideoListener otherListener = (InternalVideoListener) other;

            return otherListener.telephony.equals(telephony)
                && otherListener.peer.equals(peer)
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
