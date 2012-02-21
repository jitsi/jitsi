/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.List;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a default implementation of <tt>OperationSetVideoTelephony</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on implementation-specific details.
 *
 * @param <T> the implementation specific telephony operation set class like for
 * example <tt>OperationSetBasicTelephonySipImpl</tt>.
 * @param <U> the implementation specific provider class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt>.
 * @param <V> the <tt>MediaAwareCall</tt> implementation like
 * <tt>CallSipImpl</tt> or <tt>CallJabberImpl</tt>.
 * @param <W> the <tt>MediaAwarePeerCall</tt> implementation like
 * <tt>CallPeerSipImpl</tt> or <tt>CallPeerJabberImpl</tt>.
 *
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public abstract class AbstractOperationSetVideoTelephony<
                                    T extends OperationSetBasicTelephony<U>,
                                    U extends ProtocolProviderService,
                                    V extends MediaAwareCall<W, T, U>,
                                    W extends MediaAwareCallPeer<V, ?, U> >
    implements OperationSetVideoTelephony
{
    /**
     * The SIP <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    protected final U parentProvider;

    /**
     * The telephony-related functionality this extension builds upon.
     */
    protected final T basicTelephony;

    /**
     * Initializes a new <tt>AbstractOperationSetVideoTelephony</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephony</tt> implementation.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephony</tt>
     * the new extension should build upon
     */
    public AbstractOperationSetVideoTelephony(T basicTelephony)
    {
        this.basicTelephony = basicTelephony;
        this.parentProvider = basicTelephony.getProtocolProvider();
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void addVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        ((W)peer)
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public Component createLocalVisualComponent(
            CallPeer peer,
            VideoListener listener)
        throws OperationFailedException
    {
        return ((W)peer).getMediaHandler().createLocalVisualComponent();
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void disposeLocalVisualComponent(CallPeer peer, Component component)
    {
        ((W)peer).getMediaHandler().disposeLocalVisualComponent(component);
    }

    /**
     * Gets the visual/video <tt>Component</tt> available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video is to be retrieved
     * @return the visual/video <tt>Component</tt> available in this telephony
     * for the specified <tt>peer</tt> if any; otherwise, <tt>null</tt>
     */
    @Deprecated
    public Component getVisualComponent(CallPeer peer)
    {
        List<Component> visualComponents = getVisualComponents(peer);

        return visualComponents.isEmpty() ? null : visualComponents.get(0);
    }

    /**
     * Gets the visual/video <tt>Component</tt>s available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose videos are to be retrieved
     * @return the visual/video <tt>Component</tt>s available in this telephony
     * for the specified <tt>peer</tt>
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public List<Component> getVisualComponents(CallPeer peer)
    {
        return ((W)peer).getMediaHandler().getVisualComponents();
    }

    /**
     * Delegates to the <tt>CallPeerMediaHandler</tt> of the specified
     * <tt>CallPeer</tt> because the video is provided by it.
     *
     * @param peer the <tt>CallPeer</tt> that we'd like to unregister our
     * <tt>VideoListener</tt> from.
     * @param listener the <tt>VideoListener</tt> that we'd like to unregister.
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void removeVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener != null)
            ((W)peer)
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
     * @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        MediaAwareCall<?, ?, ?> mediaAwareCall = (MediaAwareCall<?, ?, ?>) call;

        mediaAwareCall.setVideoDevice(null);
        mediaAwareCall.setLocalVideoAllowed(allowed, MediaUseCase.CALL);
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((V)call).isLocalVideoAllowed(MediaUseCase.CALL);
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public boolean isLocalVideoStreaming(Call call)
    {
        return ((V)call).isLocalVideoStreaming();
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void addPropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((V)call).addVideoPropertyChangeListener(listener);
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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void removePropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((V)call).removeVideoPropertyChangeListener(listener);
    }

    /**
     * Get the <tt>MediaUseCase</tt> of a video telephony operation set.
     *
     * @return <tt>MediaUseCase.CALL</tt>
     */
    public MediaUseCase getMediaUseCase()
    {
        return MediaUseCase.CALL;
    }

    /**
     * Returns the quality control for video calls if any.
     * Return null so protocols who supports it to override it.
     * @param peer the peer which this control operates on.
     * @return the implemented quality control.
     */
    public QualityControl getQualityControl(CallPeer peer)
    {
        return null;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it with
     * initial video setting.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws java.text.ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    public Call createVideoCall(String uri, QualityPreset qualityPreferences)
        throws OperationFailedException,
            ParseException
    {
        return createVideoCall(uri);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it with
     * initial video setting.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    public Call createVideoCall(
            Contact callee, QualityPreset qualityPreferences)
        throws OperationFailedException
    {
        return createVideoCall(callee);
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
