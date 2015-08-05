/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.util.event.*;

/**
 * Facilitates the handling of the various video-related events occurring in a
 * <tt>CallConference</tt> in the UI as employed by <tt>OneToOneCallPanel</tt>
 * and <tt>VideoConferenceCallPanel</tt>. The intention at the time of this
 * writing is to have <tt>UIVideoHandler2</tt> initialized by <tt>CallPanel</tt>
 * and shared by it with <tt>OneToOneCallPanel</tt> and
 * <tt>VideoConferenceCallPanel</tt> as <tt>CallPanel</tt> switches between them
 * as needed.
 *
 * @author Lyubomir Marinov
 */
public class UIVideoHandler2
    extends Observable
{
    /**
     * The name of the <tt>UIVideoHandler2</tt> property which indicates whether
     * the visual <tt>Component</tt> displaying the video of the local peer/user
     * streaming to the remote peer(s) is to be made visible in the user
     * interface.
     */
    public static final String LOCAL_VIDEO_VISIBLE_PROPERTY_NAME
        = "localVideoVisible";

    /**
     * The <tt>CallConference</tt> in which the handling of the various
     * video-related events is to be facilitated by this
     * <tt>UIVideoHandler2</tt>.
     */
    private final CallConference callConference;

    /**
     * The listener implementations which get notified by
     * {@link #callConference}, the <tt>Call</tt>s participating in it, the
     * <tt>CallPeer</tt>s associated with them, and the
     * <tt>ConferenceMember</tt>s participating in their telephony conferences
     * about events related to the handling of video which this instance
     * facilitates.
     */
    private final CallConferenceListener callConferenceListener;

    /**
     * The indicator which determines whether the visual <tt>Component</tt>
     * depicting the video of the local peer/user streaming to the remote
     * peer(s) is to be made visible in the user interface.
     */
    private boolean localVideoVisible = true;

    /**
     * Initializes a new <tt>UIVideoHandler2</tt> instance which is to
     * facilitate the handling of the various video-related events occurring in
     * a specific <tt>CallConference</tt>.
     *
     * @param callConference the <tt>CallConference</tt> in which the handling
     * of the various video-related events is to be facilitated by the new
     * instance
     */
    public UIVideoHandler2(CallConference callConference)
    {
        this.callConference = callConference;

        callConferenceListener = new CallConferenceListener();
    }

    /**
     * Notifies this instance about a change in the value of the <tt>calls</tt>
     * property of {@link #callConference} i.e. a <tt>Call</tt> was added to or
     * removed from the list of <tt>Call</tt>s participating in
     * <tt>callConference</tt>. Adding or removing <tt>Call</tt>s modifies the
     * list of <tt>CallPeer</tt>s associated with <tt>callConference</tt> which
     * in turn may result in the adding or removing of visual
     * <tt>Component</tt>s depicting video.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the
     * <tt>Call</tt> which was added to or removed from the list of
     * <tt>Call</tt>s participating in <tt>callConference</tt>
     */
    protected void callConferenceCallsPropertyChange(PropertyChangeEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * Notifies this instance about a change in the value of a video-related
     * property of a <tt>ConferenceMember</tt>. Changing such a value means that
     * a visual <tt>Component</tt> displaying video may be associated or
     * dissociated with the <tt>ConferenceMember</tt>.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the
     * <tt>ConferenceMember</tt> whose video-related property value changed, the
     * name of the property whose value changed, and the old and new values of
     * the property in question
     */
    protected void conferenceMemberVideoPropertyChange(PropertyChangeEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * Releases the resources (which require explicit disposal such as listeners
     * added to notifiers) acquired by this instance throughout its lifetime and
     * prepares it for garbage collection.
     */
    void dispose()
    {
        callConferenceListener.dispose();
    }

    /**
     * Determines whether a specific <tt>Container</tt> is an ancestor of a
     * specific <tt>Component</tt> (in the UI hierarchy).
     *
     * @param container the <tt>Container</tt> which is to be tested as an
     * ancestor of <tt>component</tt>
     * @param component the <tt>Component</tt> which is to be tested as having
     * <tt>container</tt> as its ancestor
     * @return <tt>true</tt> if <tt>container</tt> is an ancestor of
     * <tt>component</tt> (in the UI hierarchy); otherwise, <tt>false</tt>
     */
    public static boolean isAncestor(Container container, Component component)
    {
        do
        {
            Container parent = component.getParent();

            if (parent == null)
                return false;
            else if (parent.equals(container))
                return true;
            else
                component = parent;
        }
        while (true);
    }

    /**
     * Gets the indicator which determines whether the visual <tt>Component</tt>
     * depicting the video of the local peer/user streaming to the remote
     * peer(s) is to be made visible in the user interface. The indicator does
     * not determine whether the local peer/user is actually streaming video to
     * the remote peer(s).
     *
     * @return <tt>true</tt> to have the visual <tt>Component</tt> depicting the
     * video of the local peer/user streaming to the remote peer(s) visible in
     * the user interface; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoVisible()
    {
        return localVideoVisible;
    }

    /**
     * Notifies this instance that the value of the property which indicates
     * whether the local peer is streaming video to the remote peer(s) changed.
     * It is not very clear who is the source of the
     * <tt>PropertyChangeEvent</tt> because a <tt>PropertyChangeListener</tt> is
     * added through <tt>OperationSetVideoTelephony</tt> by specifying a
     * <tt>Call</tt>. But it is likely that a change in the value of the
     * property in question is related to the video and, consequently, this
     * instance.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the source
     * notifying about the change and the old and new values of the property.
     */
    protected void localVideoStreamingPropertyChange(PropertyChangeEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * {@inheritDoc}
     *
     * Overrides {@link Observable#notifyObservers(Object)} to force the super
     * to notify the added <tt>Observer</tt>s regardless of the <tt>changed</tt>
     * state of this <tt>Observable</tt> which <tt>UIVideoHandler2</tt> does not
     * use at the time of this writing.
     */
    @Override
    public void notifyObservers(Object arg)
    {
        setChanged();

        super.notifyObservers(arg);
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerConferenceEvent</tt>
     * fired by a <tt>CallPeer</tt> associated with a <tt>Call</tt>
     * participating in {@link #callConference}. Adding or removing a
     * <tt>ConferenceMember</tt> may cause a visual <tt>Component</tt>
     * displaying video to be associated or dissociated with the
     * <tt>ConferenceMember</tt>.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> this instance is to be
     * notified about
     */
    protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerEvent</tt> fired by a
     * <tt>Call</tt> participating in {@link #callConference}. Adding or
     * removing a <tt>CallPeer</tt> may modify the list of visual
     * <tt>Component</tt>s displaying video.
     *
     * @param ev the <tt>CallPeerEvent</tt> this instance is to be notified
     * about
     */
    protected void onCallPeerEvent(CallPeerEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * Notifies this instance about a specific <tt>VideoEvent</tt> fired by a
     * <tt>CallPeer</tt> associated with a <tt>Call</tt> participating in
     * {@link #callConference}.
     *
     * @param ev the <tt>VideoEvent</tt> this instance is to be notified about
     */
    protected void onVideoEvent(VideoEvent ev)
    {
        notifyObservers(ev);
    }

    /**
     * Sets the indicator which determines whether the visual <tt>Component</tt>
     * depicting the video of the local peer/user streaming to the remote
     * peer(s) is to be made visible in the user interface. The indicator does
     * not determine whether the local peer/user is actually streaming video to
     * the remote peer(s).
     *
     * @param localVideoVisible <tt>true</tt> to have the visual
     * <tt>Component</tt> depicting the video of the local peer/user streaming
     * to the remote peer(s) visible in the user interface; otherwise,
     * <tt>false</tt>
     */
    public void setLocalVideoVisible(boolean localVideoVisible)
    {
        if (this.localVideoVisible != localVideoVisible)
        {
            boolean oldValue = this.localVideoVisible;

            this.localVideoVisible = localVideoVisible;

            notifyObservers(
                    new PropertyChangeEvent(
                            this,
                            LOCAL_VIDEO_VISIBLE_PROPERTY_NAME,
                            oldValue,
                            this.localVideoVisible));
        }
    }

    /**
     * Implements the listeners which get notified by
     * {@link UIVideoHandler2#callConference}, the <tt>Call</tt>s participating
     * in it, the <tt>CallPeer</tt>s associated with them, and the
     * <tt>ConferenceMember</tt>s participating in their telephony conferences
     * about events related to the handling of video which this
     * <tt>UIVideoHandler2</tt> facilitates.
     */
    private class CallConferenceListener
        extends CallPeerConferenceAdapter
        implements CallChangeListener,
                   PropertyChangeListener,
                   VideoListener
    {
        /**
         * Initializes a new <tt>CallConferenceListener</tt> instance which is
         * to get notified by {@link UIVideoHandler2#callConference} about
         * events related to the handling of video.
         */
        CallConferenceListener()
        {
            callConference.addCallChangeListener(this);
            callConference.addCallPeerConferenceListener(this);
            callConference.addPropertyChangeListener(this);
            for (Call call : callConference.getCalls())
                addListeners(call);
        }

        /**
         * Adds this as a listener to the <tt>CallPeer</tt>s associated with a
         * specific <tt>Call</tt> and to the <tt>ConferenceMember</tt>s
         * participating in their telephony conferences.
         *
         * @param call the <tt>Call</tt> to whose associated <tt>CallPeer</tt>s
         * and <tt>ConferenceMember</tt>s this is to add itself as a listener
         */
        private void addListeners(Call call)
        {
            OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony != null)
                videoTelephony.addPropertyChangeListener(call, this);

            Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

            while (callPeerIter.hasNext())
                addListeners(callPeerIter.next());
        }

        /**
         * Adds this as a listener to a specific <tt>CallPeer</tt> and to the
         * <tt>ConferenceMember</tt>s participating in its telephony conference.
         *
         * @param callPeer the <tt>CallPeer</tt> to which and to whose
         * participating <tt>ConferenceMember</tt>s this is to add itself as a
         * listener
         */
        private void addListeners(CallPeer callPeer)
        {
            OperationSetVideoTelephony videoTelephony
                = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony != null)
                    videoTelephony.addVideoListener(callPeer, this);

            for (ConferenceMember conferenceMember
                    : callPeer.getConferenceMembers())
            {
                addListeners(conferenceMember);
            }
        }

        /**
         * Adds this as a listener to a specific <tt>ConferenceMember</tt>.
         *
         * @param conferenceMember the <tt>ConferenceMember</tt> to which this
         * is to add itself as a listener
         */
        private void addListeners(ConferenceMember conferenceMember)
        {
            conferenceMember.addPropertyChangeListener(this);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to {@link #onCallPeerEvent(CallPeerEvent)} because the
         * specifics can be determined from the <tt>CallPeerEvent</tt>.
         */
        public void callPeerAdded(CallPeerEvent ev)
        {
            onCallPeerEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to {@link #onCallPeerEvent(CallPeerEvent)} because the
         * specifics can be determined from the <tt>CallPeerEvent</tt>.
         */
        public void callPeerRemoved(CallPeerEvent ev)
        {
            onCallPeerEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * <tt>CallConferenceListener</tt> does nothing because changes in the
         * state of a <tt>Call</tt> are not directly related to video or are
         * expressed with other events which are directly related to video.
         */
        public void callStateChanged(CallChangeEvent ev)
        {
        }

        /**
         * Releases the resources (which require explicit disposal such as
         * listeners added to notifiers) acquired by this instance throughout
         * its lifetime and prepares it for garbage collection.
         */
        void dispose()
        {
            callConference.removeCallChangeListener(this);
            callConference.removeCallPeerConferenceListener(this);
            callConference.removePropertyChangeListener(this);
            for (Call call : callConference.getCalls())
                removeListeners(call);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
        {
            switch (ev.getEventID())
            {
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
                addListeners(ev.getConferenceMember());
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
                removeListeners(ev.getConferenceMember());
                break;
            }

            UIVideoHandler2.this.onCallPeerConferenceEvent(ev);
        }

        /**
         * Notifies this instance about a specific <tt>CallPeerEvent</tt> which
         * was fired by a <tt>Call</tt> participating in
         * {@link UIVideoHandler2#callConference}.
         *
         * @param ev the <tt>CallPeerEvent</tt> which this instance is to be
         * notified about and which was fired by a <tt>Call</tt> participating
         * in <tt>UIVideoHandler2.callConference</tt>
         */
        private void onCallPeerEvent(CallPeerEvent ev)
        {
            switch (ev.getEventID())
            {
            case CallPeerEvent.CALL_PEER_ADDED:
                addListeners(ev.getSourceCallPeer());
                break;
            case CallPeerEvent.CALL_PEER_REMOVED:
                removeListeners(ev.getSourceCallPeer());
                break;
            }

            UIVideoHandler2.this.onCallPeerEvent(ev);
        }

        /**
         * Notifies this instance about a specific <tt>VideoEvent</tt> which was
         * fired by a <tt>CallPeer</tt> associated with a <tt>Call</tt>
         * participating in {@link UIVideoHandler2#callConference}.
         *
         * @param ev the <tt>VideoEvent</tt> which this instance is to be
         * notified about and which was fired by a <tt>CallPeer</tt> associated
         * with a <tt>Call</tt> participating in
         * <tt>UIVideoHandler2.callConference</tt>
         */
        private void onVideoEvent(VideoEvent ev)
        {
            UIVideoHandler2.this.onVideoEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * For example, notifies this <tt>UIVideoHandler2</tt> that a
         * <tt>Call</tt> was added/removed to/from the <tt>callConference</tt>.
         */
        public void propertyChange(PropertyChangeEvent ev)
        {
            String propertyName = ev.getPropertyName();

            if (CallConference.CALLS.equals(propertyName))
            {
                if (ev.getSource() instanceof CallConference)
                {
                    Object oldValue = ev.getOldValue();

                    if (oldValue instanceof Call)
                        removeListeners((Call) oldValue);

                    Object newValue = ev.getNewValue();

                    if (newValue instanceof Call)
                        addListeners((Call) newValue);

                    callConferenceCallsPropertyChange(ev);
                }
            }
            else if (ConferenceMember.VIDEO_SSRC_PROPERTY_NAME.equals(
                            propertyName)
                    || ConferenceMember.VIDEO_STATUS_PROPERTY_NAME.equals(
                            propertyName))
            {
                if (ev.getSource() instanceof ConferenceMember)
                    conferenceMemberVideoPropertyChange(ev);
            }
            else if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING.equals(
                    propertyName))
            {
                localVideoStreamingPropertyChange(ev);
            }
        }

        /**
         * Removes this as a listener from the <tt>CallPeer</tt>s associated
         * with a specific <tt>Call</tt> and from the <tt>ConferenceMember</tt>s
         * participating in their telephony conferences.
         *
         * @param call the <tt>Call</tt> from whose associated
         * <tt>CallPeer</tt>s and <tt>ConferenceMember</tt>s this is to remove
         * itself as a listener
         */
        private void removeListeners(Call call)
        {
            OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony != null)
                videoTelephony.addPropertyChangeListener(call, this);

            Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

            while (callPeerIter.hasNext())
                removeListeners(callPeerIter.next());
        }

        /**
         * Removes this as a listener from a specific <tt>CallPeer</tt> and from
         * the <tt>ConferenceMember</tt>s participating in its telephony
         * conference.
         *
         * @param callPeer the <tt>CallPeer<tt> from which and from whose
         * participating <tt>ConferenceMember</tt>s this is to remove itself as
         * a listener
         */
        private void removeListeners(CallPeer callPeer)
        {
            OperationSetVideoTelephony videoTelephony
                = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony != null)
                videoTelephony.removeVideoListener(callPeer, this);

            for (ConferenceMember conferenceMember
                    : callPeer.getConferenceMembers())
            {
                removeListeners(conferenceMember);
            }
        }

        /**
         * Removes this as a listener from a specific <tt>ConferenceMember</tt>.
         *
         * @param conferenceMember the <tt>ConferenceMember</tt> from which this
         * is to remove itself as a listener
         */
        private void removeListeners(ConferenceMember conferenceMember)
        {
            conferenceMember.removePropertyChangeListener(this);
        }

        /**
         * {@inheritDoc}
         *
         * Implements {@link VideoListener#videoAdded(VideoEvent)}. Delegates to
         * {@link #onVideoEvent(VideoEvent) because the specifics can be
         * determined from the <tt>VideoEvent</tt>.
         */
        public void videoAdded(VideoEvent ev)
        {
            onVideoEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Implements {@link VideoListener#videoRemoved(VideoEvent)}. Delegates
         * to {@link #onVideoEvent(VideoEvent) because the specifics can be
         * determined from the <tt>VideoEvent</tt>.
         */
        public void videoRemoved(VideoEvent ev)
        {
            onVideoEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Implements {@link VideoListener#videoUpdate(VideoEvent)}. Delegates
         * to {@link #onVideoEvent(VideoEvent) because the specifics can be
         * determined from the <tt>VideoEvent</tt>.
         */
        public void videoUpdate(VideoEvent ev)
        {
            onVideoEvent(ev);
        }
    }
}
