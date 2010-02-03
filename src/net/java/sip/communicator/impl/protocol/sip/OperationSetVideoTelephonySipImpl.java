/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.awt.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>OperationSetVideoTelephony</tt> in order to give access to
 * video-specific functionality in the SIP protocol implementation such as
 * visual <tt>Component</tt>s displaying video and listening to dynamic
 * availability of such <tt>Component</tt>s. Because the video in the SIP
 * protocol implementation is provided by the <tt>CallSession</tt>, this
 * <tt>OperationSetVideoTelephony</tt> just delegates to the
 * <tt>CallSession</tt> while hiding the <tt>CallSession</tt> as the
 * provider of the video and pretending this
 * <tt>OperationSetVideoTelephony</tt> is the provider because other
 * implementation may not provider their video through the
 * <tt>CallSession</tt>.
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
     * Initializes a new <tt>OperationSetVideoTelephonySipImpl</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephonySipImpl</tt>.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephonySipImpl</tt>
     *            the new extension should build upon
     */
    public OperationSetVideoTelephonySipImpl(
        OperationSetBasicTelephonySipImpl basicTelephony)
    {
        this.basicTelephony = basicTelephony;
    }

    /**
     * Delegates to the <tt>CallPeerMediaHandler</tt> of the specified
     * <tt>CallPeer</tt> because the video is provided by it. Because other
     * <tt>OperationSetVideoTelephony</tt> implementations may not provide their
     * video through the <tt>CallPeerMediaHandler</tt>, this implementation
     * promotes itself as the provider of the video by replacing the
     * <tt>CallPeerMediaHandler</tt> in the <tt>VideoEvents</tt> it fires.
     *
     * @param peer the <tt>CallPeer</tt> that we will be registering
     * <tt>listener</tt> with.
     * @param listener the <tt>VideoListener</tt> that we'd like to register.
     */
    public void addVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        ((CallPeerSipImpl) peer)
            .getMediaHandler()
                .addVideoListener(
                    new InternalVideoListener(this, peer, listener));
    }

    /**
     * Implements
     * {@link OperationSetVideoTelephony#createLocalVisualComponent(CallPeer,
     * VideoListener)}.
     *
     * @param peer the <tt>CallPeer</tt> that we are sending our local video to.
     * @param listener the <tt>VideoListener</tt> where we'd like to retrieve
     * the <tt>Component</tt> containing the local video.
     * @return the <tt>Component</tt> containing the local video.
     * @throws OperationFailedException if we fail extracting the local video.
     */
    public Component createLocalVisualComponent(
            CallPeer peer,
            VideoListener listener)
        throws OperationFailedException
    {
        CallPeerMediaHandler mediaHandler = ((CallPeerSipImpl) peer).getMediaHandler();
        return mediaHandler.createLocalVisualComponent();
    }

    /**
     * Implements
     * {@link OperationSetVideoTelephony#disposeLocalVisualComponent(CallPeer,
     * Component)}.
     *
     * @param peer the <tt>CallPeer</tt> whose local video component we'd like
     * to dispose of.
     * @param component the <tt>Component</tt> that we'll be disposing of.
     */
    public void disposeLocalVisualComponent(CallPeer peer, Component component)
    {
        CallPeerMediaHandler mediaHandler = ((CallPeerSipImpl) peer).getMediaHandler();
        mediaHandler.disposeLocalVisualComponent();
    }

    /**
     * Gets the visual/video <tt>Component</tt> available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video is to be retrieved
     * @return the visual/video <tt>Component</tt> available in this telephony
     * for the specified <tt>peer</tt> if any; otherwise, <tt>null</tt>
     */
    public Component getVisualComponent(CallPeer peer)
    {
        return ((CallPeerSipImpl) peer).getMediaHandler().getVisualComponent();
    }

    /**
     * Delegates to the <tt>CallPeerMediaHandler</tt> of the specified
     * <tt>CallPeer</tt> because the video is provided by it.
     *
     * @param peer the <tt>CallPeer</tt> that we'd like to unregister our
     * <tt>VideoListener</tt> from.
     * @param listener the <tt>VideoListener</tt> that we'd like to unregister.
     */
    public void removeVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener != null)
            ((CallPeerSipImpl) peer)
                .getMediaHandler()
                    .removeVideoListener(
                        new InternalVideoListener(this, peer, listener));
    }

    /**
     * Implements OperationSetVideoTelephony#setLocalVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     *
     * @param call    the call where we'd like to allow sending local video.
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        ((CallSipImpl)call).setLocalVideoAllowed(allowed);
    }

    /**
     * Determines whether the streaming of local video in a specific
     * <tt>Call</tt> is currently allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     *
     * @return <tt>true</tt> if the streaming of local video for the specified
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((CallSipImpl) call).isLocalVideoAllowed();
    }

    /**
     * Determines whether a specific <tt>Call</tt> is currently streaming the
     * local video (to a remote destination).
     *
     * @param call the <tt>Call</tt> whose video transmission we are interested
     * in.
     *
     * @return <tt>true</tt> if the specified <tt>Call</tt> is currently
     * streaming the local video (to a remote destination); otherwise,
     * <tt>false</tt>
     */
    public boolean isLocalVideoStreaming(Call call)
    {
        return ((CallSipImpl) call).isLocalVideoStreaming();
    }

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to start listening to the changes of
     * the property values of
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with the specified <tt>Call</tt> change
     * their values
     */
    public void addPropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((CallSipImpl) call).addVideoPropertyChangeListener(listener);
    }

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to stop listening to the changes of the
     * property values of
     * @param listener the <tt>PropertyChangeListener</tt> to no longer be
     * notified when the properties associated with the specified <tt>Call</tt>
     * change their values
     */
    public void removePropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((CallSipImpl) call).removeVideoPropertyChangeListener(listener);
    }

    /**
     * Represents a <tt>VideoListener</tt> which forwards notifications to a
     * specific delegate <tt>VideoListener</tt> and hides the original
     * <tt>VideoEvent</tt> sender from it by pretending the sender is a
     * specific <tt>OperationSetVideoTelephony</tt>. It's necessary in order
     * to hide from the <tt>VideoListener</tt>s the fact that the video of
     * the SIP protocol implementation is managed by <tt>CallSession</tt>.
     */
    private static class InternalVideoListener
        implements VideoListener
    {

        /**
         * The <tt>VideoListener</tt> this implementation hides the original
         * <tt>VideoEvent</tt> source from.
         */
        private final VideoListener delegate;

        /**
         * The <tt>CallPeer</tt> whose videos {@link #delegate} is
         * interested in.
         */
        private final CallPeer peer;

        /**
         * The <tt>OperationSetVideoTelephony</tt> which is to be presented
         * as the source of the <tt>VideoEvents</tt> forwarded to
         * {@link #delegate}.
         */
        private final OperationSetVideoTelephony telephony;

        /**
         * Initializes a new <tt>InternalVideoListener</tt> which is to
         * impersonate the sources of <tt>VideoEvents</tt> with a specific
         * <tt>OperationSetVideoTelephony</tt> for a specific
         * <tt>VideoListener</tt> interested in the videos of a specific
         * <tt>CallPeer</tt>.
         *
         * @param telephony the <tt>OperationSetVideoTelephony</tt> which is
         * to be stated as the source of the <tt>VideoEvent</tt> sent to the
         * specified delegate <tt>VideoListener</tt>
         * @param peer the <tt>CallPeer</tt> whose videos the specified delegate
         * <tt>VideoListener</tt> is interested in
         * @param delegate the <tt>VideoListener</tt> which shouldn't know
         * that the videos in the SIP protocol implementation is managed by the
         * <tt>CallSession</tt> and not by the specified <tt>telephony</tt>
         */
        public InternalVideoListener(
                OperationSetVideoTelephony telephony,
                CallPeer peer,
                VideoListener delegate)
        {
            if (peer == null)
                throw new NullPointerException("peer cannot be null");

            this.telephony = telephony;
            this.peer = peer;
            this.delegate = delegate;
        }

        /**
         * Compares two InternalVideoListeners and determines they are equal
         * if they impersonate the sources of VideoEvents with equal
         * OperationSetVideoTelephonies for equal delegate VideoListeners added
         * to equal CallPeer-s.
         *
         * @param other the object that we'd be compared to.
         *
         * @return true if the underlying peer, telephony operation set and
         * delegate are equal to those of the <tt>other</tt> instance.
         */
        @Override
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

        /**
         * Returns a hashcode based on the hash codes of the wrapped telephony
         * operation set and <tt>VideoListener</tt> delegate.
         *
         * @return a hashcode based on the hash codes of the wrapped telephony
         * operation set and <tt>VideoListener</tt> delegate.
         */
        @Override
        public int hashCode()
        {
            return (telephony.hashCode() << 16) + (delegate.hashCode() >> 16);
        }

        /**
         * Upon receiving a VideoEvent, sends to delegate a new VideoEvent of
         * the same type and with the same visual Component but with the source
         * of the event being set to #telephony. Thus the fact that the
         * CallSession is the original source is hidden from the clients of
         * OperationSetVideoTelephony.
         *
         * @param event the <tt>VideoEvent</tt> containing the visual component
         */
        public void videoAdded(VideoEvent event)
        {
            VideoEvent delegateEvent
                = new VideoEvent(
                        this,
                        event.getType(),
                        event.getVisualComponent(),
                        event.getOrigin());

            delegate.videoAdded(delegateEvent);
            if (delegateEvent.isConsumed())
                event.consume();
        }

        /**
         * Upon receiving a VideoEvent, sends to delegate a new VideoEvent of
         * the same type and with the same visual Component but with the source
         * of the event being set to #telephony. Thus the fact that the
         * CallSession is the original source is hidden from the clients of
         * OperationSetVideoTelephony.
         *
         * @param event the <tt>VideoEvent</tt> containing video details and
         * the event component.
         */
        public void videoRemoved(VideoEvent event)
        {
            VideoEvent delegateEvent
                = new VideoEvent(
                        this,
                        event.getType(),
                        event.getVisualComponent(),
                        event.getOrigin());

            delegate.videoRemoved(delegateEvent);
            if (delegateEvent.isConsumed())
                event.consume();
        }

        /**
         * Forwards a specific <tt>VideoEvent</tt> to the associated delegate.
         *
         * @param event the <tt>VideoEvent</tt> to be forwarded to the
         * associated delegate
         */
        public void videoUpdate(VideoEvent event)
        {
            /*
             * TODO Since the specified event is directly fired, the sender is
             * the original one and not this.
             */
            delegate.videoUpdate(event);
        }
    }
}
