/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.control;

import java.util.*;

/**
 * Represents a control over the key frame-related logic of a
 * <tt>VideoMediaStream</tt>.
 *
 * @author Lyubomir Marinov
 */
public interface KeyFrameControl
{
    /**
     * Adds a <tt>KeyFrameRequestee</tt> to be made available through this
     * <tt>KeyFrameControl</tt>.
     *
     * @param index the zero-based index at which <tt>keyFrameRequestee</tt> is
     * to be added to the list of <tt>KeyFrameRequestee</tt>s made available or
     * <tt>-1</tt> to have this <tt>KeyFrameControl</tt> choose at which index
     * it is to be added in accord with its internal logic
     * through this <tt>KeyFrameControl</tt>
     * @param keyFrameRequestee the <tt>KeyFrameRequestee</tt> to be added to
     * this <tt>KeyFrameControl</tt> so that it is made available through it
     */
    public void addKeyFrameRequestee(
            int index,
            KeyFrameRequestee keyFrameRequestee);

    /**
     * Adds a <tt>KeyFrameRequester</tt> to be made available through this
     * <tt>KeyFrameControl</tt>.
     *
     * @param index the zero-based index at which <tt>keyFrameRequester</tt> is
     * to be added to the list of <tt>KeyFrameRequester</tt>s made available or
     * <tt>-1</tt> to have this <tt>KeyFrameControl</tt> choose at which index
     * it is to be added in accord with its internal logic
     * through this <tt>KeyFrameControl</tt>
     * @param keyFrameRequester the <tt>KeyFrameRequester</tt> to be added to
     * this <tt>KeyFrameControl</tt> so that it is made available through it
     */
    public void addKeyFrameRequester(
            int index,
            KeyFrameRequester keyFrameRequester);

    /**
     * Gets the <tt>KeyFrameRequestee</tt>s made available through this
     * <tt>KeyFrameControl</tt>.
     *
     * @return an unmodifiable list of <tt>KeyFrameRequestee</tt>s made
     * available through this <tt>KeyFrameControl</tt>
     */
    public List<KeyFrameRequestee> getKeyFrameRequestees();

    /**
     * Gets the <tt>KeyFrameRequester</tt>s made available through this
     * <tt>KeyFrameControl</tt>.
     *
     * @return an unmodifiable list of <tt>KeyFrameRequester</tt>s made
     * available through this <tt>KeyFrameControl</tt>
     */
    public List<KeyFrameRequester> getKeyFrameRequesters();

    /**
     * Notifies this <tt>KeyFrameControl</tt> that the remote peer of the
     * associated <tt>VideoMediaStream</tt> has requested a key frame from the
     * local peer.
     *
     * @return <tt>true</tt> if the local peer has honored the request from the
     * remote peer for a key frame; otherwise, <tt>false</tt>
     */
    public boolean keyFrameRequest();

    /**
     * Removes a <tt>KeyFrameRequestee</tt> to no longer be made available
     * through this <tt>KeyFrameControl</tt>.
     *
     * @param keyFrameRequestee the <tt>KeyFrameRequestee</tt> to be removed
     * from this <tt>KeyFrameControl</tt> so that it is no longer made available
     * through it
     * @return <tt>true</tt> if <tt>keyFrameRequestee</tt> was found in this
     * <tt>KeyFrameControl</tt>; otherwise, <tt>false</tt>
     */
    public boolean removeKeyFrameRequestee(KeyFrameRequestee keyFrameRequestee);

    /**
     * Removes a <tt>KeyFrameRequester</tt> to no longer be made available
     * through this <tt>KeyFrameControl</tt>.
     *
     * @param keyFrameRequester the <tt>KeyFrameRequester</tt> to be removed
     * from this <tt>KeyFrameControl</tt> so that it is no longer made available
     * through it
     * @return <tt>true</tt> if <tt>keyFrameRequester</tt> was found in this
     * <tt>KeyFrameControl</tt>; otherwise, <tt>false</tt>
     */
    public boolean removeKeyFrameRequester(KeyFrameRequester keyFrameRequester);

    /**
     * Requests a key frame from the remote peer of the associated
     * <tt>VideoMediaStream</tt>.
     *
     * @return <tt>true</tt> if a key frame was indeed requested from the remote
     * peer of the associated <tt>VideoMediaStream</tt> in response to the call;
     * otherwise, <tt>false</tt>
     */
    public boolean requestKeyFrame();

    /**
     * Represents a way for the remote peer of a <tt>VideoMediaStream</tt> to
     * request a key frame from its local peer.
     *
     * @author Lyubomir Marinov
     */
    public interface KeyFrameRequestee
    {
        /**
         * Notifies this <tt>KeyFrameRequestee</tt> that the remote peer of the
         * associated <tt>VideoMediaStream</tt> requests a key frame from the
         * local peer.
         *
         * @return <tt>true</tt> if this <tt>KeyFrameRequestee</tt> has honored
         * the request for a key frame; otherwise, <tt>false</tt>
         */
        public boolean keyFrameRequest();
    }

    /**
     * Represents a way for a <tt>VideoMediaStream</tt> to request a key frame
     * from its remote peer.
     *
     * @author Lyubomir Marinov
     */
    public interface KeyFrameRequester
    {
        /**
         * The name of the <tt>ConfigurationService</tt> property which
         * specifies the preferred <tt>KeyFrameRequester</tt> to be used. 
         */
        public static final String PREFERRED_PNAME
            = "net.java.sip.communicator.service.media.codec.video.h264."
                + "preferredKeyFrameRequester";

        /**
         * The value of the {@link #PREFERRED_PNAME}
         * <tt>ConfigurationService</tt> property which indicates that the
         * RTCP <tt>KeyFrameRequester</tt> is preferred.
         */
        public static final String RTCP = "rtcp";

        /**
         * The value of the {@link #PREFERRED_PNAME}
         * <tt>ConfigurationService</tt> property which indicates that the
         * signaling/protocol <tt>KeyFrameRequester</tt> is preferred.
         */
        public static final String SIGNALING = "signaling";

        /**
         * The default value of the {@link #PREFERRED_PNAME}
         * <tt>ConfigurationService</tt> property.
         */
        public static final String DEFAULT_PREFERRED = RTCP;

        /**
         * Requests a key frame from the remote peer of the associated
         * <tt>VideoMediaStream</tt>.
         *
         * @return <tt>true</tt> if this <tt>KeyFrameRequester</tt> has
         * indeed requested a key frame from the remote peer of the associated
         * <tt>VideoMediaStream</tt> in response to the call; otherwise,
         * <tt>false</tt>
         */
        public boolean requestKeyFrame();
    }
}
