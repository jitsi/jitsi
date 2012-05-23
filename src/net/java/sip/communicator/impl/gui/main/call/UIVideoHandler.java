/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>UIVideoHandler</tt> is meant to handle all video related events.
 *
 * @author Yana Stamcheva
 */
public class UIVideoHandler
{
    /**
     * The <tt>Logger</tt> used by the <tt>UIVideoHandler</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UIVideoHandler.class);

    /**
     * The component showing the local video.
     */
    private Component localVideo;

    /**
     * The component showing the remote video.
     */
    private Component remoteVideo;

    /**
     * Listener for all key and mouse events. It is used for desktop sharing
     * purposes.
     */
    private MouseAndKeyListener mouseAndKeyListener;

    /**
     * The local video mouse listener.
     */
    private final LocalVideoMouseListener localVideoListener
        = new LocalVideoMouseListener();

    private boolean localVideoVisible = true;

    /**
     * In case of desktop streaming (client-side) if the local peer can control
     * remote peer's computer.
     */
    private boolean allowRemoteControl = false;

    /**
     * The renderer of the call.
     */
    private final CallRenderer callRenderer;

    /**
     * The close local video button.
     */
    private Component closeButton;

    /**
     * The list of video containers.
     */
    private List<Container> videoContainers;

    /**
     * The video telephony listener.
     */
    private VideoTelephonyListener videoTelephonyListener;

    /**
     * The operation set through which we do all video operations.
     */
    private OperationSetVideoTelephony videoTelephony;

    /**
     * A list of peer, video tool bar pair.
     */
    private final Map<CallPeer, Component> videoToolbars;

    /**
     * The local video tool bar.
     */
    private Component localVideoToolbar;

    /**
     * Indicates if the local video listener is added.
     */
    private boolean isLocalVideoListenerAdded = false;

    /**
     * Constructor.
     *
     * @param callRenderer ther <tt>CallRenderer</tt>
     * @param videoContainers the video <tt>Container</tt>
     */
    public UIVideoHandler(  CallRenderer callRenderer,
                            List<Container> videoContainers)
    {
        this.callRenderer = callRenderer;
        this.videoContainers = videoContainers;
        this.videoToolbars = new Hashtable<CallPeer, Component>();
    }

    /**
     * Gets the <tt>Icon</tt> to be displayed in {@link #photoLabels}.
     *
     * @return the <tt>Icon</tt> to be displayed in {@link #photoLabels}
     */
    private ImageIcon getPhotoLabelIcon(CallPeer callPeer)
    {
        byte[] peerImage = CallManager.getPeerImage(callPeer);

        return
            (peerImage == null)
                ? new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO))
                : new ImageIcon(peerImage);
    }

    /**
     * Set the video cotnainers list.
     *
     * @param videoContainers the video <tt>Container</tt> list
     */
    public void setVideoContainersList(List<Container> videoContainers)
    {
        this.videoContainers = videoContainers;
    }

    public void addVideoToolbar(CallPeer callPeer, Component videoToolbar)
    {
        videoToolbars.put(callPeer, videoToolbar);
    }

    public void setLocalVideoToolbar(Component videoToolbar)
    {
        localVideoToolbar = videoToolbar;
    }

    /**
     * Sets up listening to notifications about adding or removing video for the
     * <code>CallPeer</code> this panel depicts and displays the video in
     * question in the last-known of {@link #videoContainers} (because the video
     * is represented by a <code>Component</code> and it cannot be displayed in
     * multiple <code>Container</code>s at one and the same time) as soon as it
     * arrives.
     * @return the video telephony operation set, where the video listener was
     * added
     */
    public OperationSetVideoTelephony addVideoListener(final CallPeer callPeer)
    {
        final Call call = callPeer.getCall();
        if (call == null)
            return null;

        final OperationSetVideoTelephony telephony =
            call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);
        if (telephony == null)
            return null;

        videoTelephonyListener = new VideoTelephonyListener(callPeer);

        /**
         * The video is only available while the #callPeer is in a Call
         * and that call is in progress so only listen to VideoEvents during
         * that time.
         */
        CallChangeListener callListener = new CallChangeListener()
        {
            private void addVideoListener(CallPeer callPeer)
            {
                telephony.addVideoListener(
                        callPeer, videoTelephonyListener);

                if (!isLocalVideoListenerAdded)
                {
                    telephony.addPropertyChangeListener(
                            call, videoTelephonyListener);

                    isLocalVideoListenerAdded = true;
                }

                synchronized (videoContainers)
                {
                    videoTelephony = telephony;

                    handleVideoEvent(callPeer, null);

                    handleLocalVideoStreamingChange(
                            callPeer, videoTelephonyListener);
                }
            }

            /**
             * When the #callPeer of this CallPeerPanel gets added
             * to the Call, starts listening for changes in the video in order
             * to display it.
             *
             * @param event the <tt>CallPeerEvent</tt> received
             */
            public synchronized void callPeerAdded(
                CallPeerEvent event)
            {
                if (callPeer.equals(event.getSourceCallPeer()))
                {
                    Call call = callPeer.getCall();

                    if ((call != null)
                            && CallState.CALL_IN_PROGRESS.equals(
                                    call.getCallState()))
                        addVideoListener(event.getSourceCallPeer());
                }
            }

            /**
             * When the #callPeer of this CallPeerPanel leaves the
             * Call, stops listening for changes in the video because it should
             * no longer be updated anyway.
             *
             * @param event the <tt>CallPeerEvent</tt> received
             */
            public synchronized void callPeerRemoved(
                CallPeerEvent event)
            {
                if (callPeer.equals(event.getSourceCallPeer()))
                {
                    if (callPeer.getCall() != null)
                        removeVideoListener(event.getSourceCallPeer());
                }
            }

            /**
             * When the Call of #callPeer ends, stops tracking the
             * updates in the video because there should no longer be any video
             * anyway. When the Call in question starts, starts tracking any
             * changes to the video because it's negotiated and it should be
             * displayed in this CallPeerPanel.
             *
             * @param event the <tt>CallChangeEvent</tt> received
             */
            public synchronized void callStateChanged(CallChangeEvent event)
            {
                // we are interested only in CALL_STATE_CHANGEs
                if(!event.getEventType().equals(
                        CallChangeEvent.CALL_STATE_CHANGE))
                    return;

                CallState newCallState = (CallState) event.getNewValue();

                if (CallState.CALL_ENDED.equals(newCallState))
                {
                    removeVideoListener(callPeer);
                    call.removeCallChangeListener(this);

                    if(allowRemoteControl)
                    {
                        allowRemoteControl = false;
                        removeMouseAndKeyListeners();
                    }
                }
                else if (CallState.CALL_IN_PROGRESS.equals(newCallState))
                {
                    addVideoListener(callPeer);
                }
            }
        };
        call.addCallChangeListener(callListener);
        callListener.callStateChanged(new CallChangeEvent(call,
            CallChangeEvent.CALL_STATE_CHANGE, null, call.getCallState()));

        return telephony;
    }

    /**
     * Removes the video listener
     */
    public void removeVideoListener(CallPeer callPeer)
    {
        final Call call = callPeer.getCall();
        if (call == null)
            return;

        final OperationSetVideoTelephony telephony =
            call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);
        if (telephony == null)
            return;

        if (videoTelephonyListener == null)
            return;

        telephony.removeVideoListener(
                callPeer, videoTelephonyListener);

        if (!CallManager.isVideoStreaming(call) && isLocalVideoListenerAdded)
        {
            telephony.removePropertyChangeListener(
                    call, videoTelephonyListener);
            isLocalVideoListenerAdded = false;

            if (localVideo != null)
            {
                telephony.disposeLocalVisualComponent(
                        callPeer, localVideo);
                localVideo = null;
            }
        }

        synchronized (videoContainers)
        {
            if (!CallManager.isVideoStreaming(call)
                && telephony.equals(videoTelephony))
                videoTelephony = null;

            int videoContainerCount;

            if ((videoTelephony != null)
                    && ((videoContainerCount = videoContainers.size()) > 0))
            {
                Container videoContainer
                    = videoContainers.get(videoContainerCount - 1);

                handleVideoEvent(callPeer, null, videoContainer);
            }
        }

        videoToolbars.remove(callPeer);

        callRenderer.exitFullScreen();
    }

    /**
     * Listener that will process change related to video such as if local
     * streaming has been turned on/off or a video component has been
     * added/removed.
     */
    private class VideoTelephonyListener
        implements PropertyChangeListener,
                   VideoListener
    {
        private final CallPeer callPeer;

        /**
         * Creates an instance of <tt>VideoTelephonyListener</tt> by specifying
         * the corresponding <tt>Call</tt>.
         *
         * @param callPeer the <tt>CallPeer</tt> to which the listener would be
         * added
         */
        public VideoTelephonyListener(CallPeer callPeer)
        {
            this.callPeer = callPeer;
        }

        /**
         * {@inheritDoc}
         */
        public void propertyChange(final PropertyChangeEvent event)
        {
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        propertyChange(event);
                    }
                });
                return;
            }

            if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING
                    .equals(event.getPropertyName()))
            {
                CallPanel callContainer = callRenderer.getCallContainer();

                // We ensure that when the local video property changes to
                // false our related buttons would be disabled.
                if (event.getNewValue().equals(MediaDirection.RECVONLY))
                {
                    callContainer.setVideoButtonSelected(false);
                    callContainer.setDesktopSharingButtonSelected(false);
                }
                else if (event.getNewValue().equals(MediaDirection.SENDRECV))
                {
                    Call call = callPeer.getCall();

                    if (call != null)
                    {
                        /*
                         * If the local video or desktop sharing is turned on,
                         * we ensure that the button is selected.
                         */
                        if (CallManager.isDesktopSharingEnabled(call))
                        {
                            callContainer.setDesktopSharingButtonSelected(true);
                            if (CallManager.isRegionDesktopSharingEnabled(call))
                            {
                                DesktopSharingFrame
                                    .createTransparentFrame(call, false)
                                        .setVisible(true);
                            }
                        }
                        else if (CallManager.isLocalVideoEnabled(call))
                        {
                            callContainer.setVideoButtonSelected(true);
                        }
                    }
                }

                handleLocalVideoStreamingChange(callPeer, this);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void videoAdded(VideoEvent event)
        {
            CallPanel callContainer = callRenderer.getCallContainer();

            if (callContainer.isConference()
                && !callContainer.isVideoConferenceInterfaceEnabled())
            {
                callContainer.enableVideoConferenceInterface(true);
            }

            handleVideoEvent(callPeer, event);
        }

        /**
         * {@inheritDoc}
         */
        public void videoRemoved(VideoEvent event)
        {
            CallPanel callContainer = callRenderer.getCallContainer();
            if (callContainer.isConference()
                && !CallManager.isVideoStreaming(callPeer.getCall())
                && callContainer.isVideoConferenceInterfaceEnabled())
            {
                callContainer.enableVideoConferenceInterface(false);
            }

            handleVideoEvent(callPeer, event);
        }

        /**
         * {@inheritDoc}
         */
        public void videoUpdate(VideoEvent event)
        {
            handleVideoEvent(callPeer, event);
        }
    }

    /**
     * When a video is added or removed for the <tt>callPeer</tt>, makes sure to
     * display or hide it respectively.
     *
     * @param event a <tt>VideoEvent</tt> describing the added visual
     * <tt>Component</tt> representing video and the provider it was added into
     * or <tt>null</tt> if such information is not available
     */
    public void handleVideoEvent(   final CallPeer callPeer,
                                    final VideoEvent event)
    {
        if (event != null && logger.isTraceEnabled())
            logger.trace("UI video event received originated in: "
                    + event.getOrigin() + " and is of type: " + event.getType());

        if ((event != null) && !event.isConsumed())
        {
            int origin = event.getOrigin();
            Component video = event.getVisualComponent();

            synchronized (videoContainers)
            {
                switch (event.getType())
                {
                case VideoEvent.VIDEO_ADDED:
                    switch (origin)
                    {
                    case VideoEvent.LOCAL:
                        this.localVideo = video;
                        this.closeButton = new CloseButton();
                        break;
                    case VideoEvent.REMOTE:
                        this.remoteVideo = video;
                        break;
                    }

                    addMouseListeners(origin);

                    /*
                     * Let the creator of the local visual Component know it
                     * shouldn't be disposed of because we're going to use it.
                     */
                    event.consume();
                    break;

                case VideoEvent.VIDEO_REMOVED:
                    switch (origin)
                    {
                    case VideoEvent.LOCAL:
                        if (localVideo == video)
                        {
                            this.localVideo = null;
                            this.closeButton = null;
                        }
                        break;
                    case VideoEvent.REMOTE:
                        if (remoteVideo == video)
                            this.remoteVideo = null;
                        break;
                    }
                    break;
                }
            }
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    handleVideoEvent(callPeer, event);
                }
            });
            return;
        }

        synchronized (videoContainers)
        {
            int videoContainerCount;

            if ((videoTelephony != null)
                    && ((videoContainerCount = videoContainers.size()) > 0))
            {
                Container videoContainer
                    = videoContainers.get(videoContainerCount - 1);

                handleVideoEvent(callPeer, event, videoContainer);
            }
        }
    }

    /**
     * Handles a specific <tt>VideoEvent</tt> related to a specific visual
     * <tt>Component</tt> depicting video knowing that it is to be displayed or
     * is already displayed in a specific <tt>Container</tt>.
     *
     * @param videoEvent the <tt>VideoEvent</tt> describing the visual
     * <tt>Component</tt> which was added, removed or updated
     * @param videoContainer the <tt>Container</tt> which is to contain or
     * already contains the visual <tt>Component</tt> described by
     * <tt>videoEvent</tt>
     */
    private void handleVideoEvent(
            CallPeer callPeer,
            VideoEvent videoEvent,
            Container videoContainer)
    {
        if (videoEvent != null)
        {
            if ((videoEvent.getOrigin() == VideoEvent.REMOTE)
                    && (videoEvent instanceof SizeChangeVideoEvent))
            {
                SizeChangeVideoEvent sizeChangeVideoEvent
                    = (SizeChangeVideoEvent) videoEvent;
                Component visualComponent
                    = sizeChangeVideoEvent.getVisualComponent();
                int width = sizeChangeVideoEvent.getWidth();
                int height = sizeChangeVideoEvent.getHeight();

                if (visualComponent.getParent() == null)
                {
                    visualComponent.setPreferredSize(
                            new Dimension(width, height + 30));
                }
                else if (isAncestor(videoContainer, visualComponent))
                {
                    // We should ensure window size only if we are not in a
                    // single window mode.
                    if (!callRenderer.getCallContainer().getCallWindow().equals(
                                GuiActivator
                                    .getUIService()
                                        .getSingleWindowContainer()))
                    {
                        callRenderer.ensureSize(visualComponent, width, height);
                    }

                    /*
                     * Even if ensureSize hasn't changed the Frame size,
                     * videoContainer may still need to lay out visualComponent
                     * again because the size-related properties of the latter
                     * have likely changed.
                     */
                    videoContainer.doLayout();
                }
                return;
            }

            /*
             * We only care about VIDEO_ADDED and VIDEO_REMOVED from now on till
             * the end of this method. And null, of course.
             */
            switch (videoEvent.getType())
            {
                case VideoEvent.VIDEO_ADDED:
                case VideoEvent.VIDEO_REMOVED:
                    break;
                default:
                    return;
            }
        }

        videoContainer.removeAll();

        // REMOTE
        /*
         * UIVideoHandlers share a single VideoContainer in a Call i.e. there is
         * a single video area even in conference calls. So rather than adding
         * the videos of a single CallPeer, each UIVideoHandler adds all videos
         * of a Call.
         */
        Call call = callPeer.getCall();
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();
        List<Component> remoteVideos = new LinkedList<Component>();

        while (callPeerIter.hasNext())
        {
            CallPeer peer = callPeerIter.next();

            List<Component> visualComponents
                = videoTelephony.getVisualComponents(peer);

            Component videoToolbar = videoToolbars.get(peer);

            if (videoToolbar != null)
            {
                Container toolbarParent = videoToolbar.getParent();

                if (toolbarParent != null)
                    toolbarParent.remove(videoToolbar);
            }

            if (visualComponents != null && visualComponents.size() > 0)
            {
                for (int i = 0; i < visualComponents.size(); i++)
                {
                    Component visualComponent = visualComponents.get(i);
 
                    if (videoToolbar != null
                        && callRenderer.getCallContainer()
                            .isVideoConferenceInterfaceEnabled())
                    {
                        Container remoteVideoParent
                            = visualComponent.getParent();

                        if (remoteVideoParent != null)
                            remoteVideoParent.remove(visualComponent);

                        JPanel remoteVideoPanel
                            = new TransparentPanel(new BorderLayout());
                        remoteVideoPanel.add(visualComponent);
                        remoteVideoPanel.add(videoToolbar, BorderLayout.SOUTH);
                        remoteVideos.add(remoteVideoPanel);
                    }
                    else
                        remoteVideos.add(visualComponent);
                }
            }
            else if (videoToolbar != null && callRenderer.getCallContainer()
                        .isVideoConferenceInterfaceEnabled())
            {
                Component defaultPhotoPanel
                    = createDefaultPhotoPanel(callPeer, videoToolbar);

                remoteVideos.add(defaultPhotoPanel);
            }
        }

        if (remoteVideos.isEmpty())
        {
            callRenderer
                .getCallContainer()
                    .removeRemoteVideoSpecificComponents();
        }
        else
        {
            for (Component remoteVideo : remoteVideos)
            {
                Container remoteVideoParent = remoteVideo.getParent();

                if (remoteVideoParent != null)
                {
                    remoteVideoParent.remove(remoteVideo);
                    Container parent = remoteVideoParent.getParent();

                    if (parent != null)
                        parent.remove(remoteVideoParent);
                }

                videoContainer.add(
                    remoteVideo, VideoLayout.CENTER_REMOTE, 0);
            }

            callRenderer.getCallContainer().addRemoteVideoSpecificComponents(
                    callPeer);
        }

        // LOCAL
        if (localVideo != null)
        {
            Container localVideoParent = localVideo.getParent();

            if (localVideoParent != null)
                localVideoParent.remove(localVideo);

            Container closeButtonParent = closeButton.getParent();

            if (closeButtonParent != null)
                closeButtonParent.remove(closeButton);

            if (localVideoToolbar != null)
            {
                Container toolbarParent = localVideoToolbar.getParent();
                if (toolbarParent != null)
                    toolbarParent.remove(localVideoToolbar);
            }

            if (localVideoVisible)
            {
                if (localVideoToolbar != null && callRenderer.getCallContainer()
                        .isVideoConferenceInterfaceEnabled())
                {
                    JPanel localVideoPanel
                        = new TransparentPanel(new BorderLayout());

                    localVideoPanel.add(localVideo);
                    localVideoPanel.add(localVideoToolbar, BorderLayout.SOUTH);

                    videoContainer.add(localVideoPanel, VideoLayout.LOCAL, 0);
                }
                else
                    videoContainer.add(localVideo, VideoLayout.LOCAL, 0);

                if (!CallManager.isDesktopSharingEnabled(call))
                {
                    videoContainer.add(
                            closeButton,
                            VideoLayout.CLOSE_LOCAL_BUTTON,
                            0);
                }
            }
        }

        videoContainer.validate();

        /*
         * Without explicit repainting, the remote visual Component will not
         * stay small after entering fullscreen, the Component shown when there
         * is no video will be shown beneath the video Component though the
         * former has already been removed...
         */
        videoContainer.repaint();
    }

    /**
     * Handles the change when we turn on/off local video streaming such as
     * creating/releasing visual component.
     *
     * @param listener Listener that will be called back
     */
    private void handleLocalVideoStreamingChange(
            CallPeer callPeer,
            VideoTelephonyListener listener)
    {
        synchronized (videoContainers)
        {
            if (videoTelephony == null)
                return;
            if (callPeer == null || callPeer.getCall() == null)
                return;

            if (videoTelephony.isLocalVideoStreaming(callPeer.getCall()))
            {
                if (localVideo == null)
                {
                    try
                    {
                        videoTelephony.createLocalVisualComponent(
                                callPeer, listener);
                    }
                    catch (OperationFailedException ex)
                    {
                        logger.error(
                                "Failed to create local video Component.",
                                ex);
                    }
                }
            }
            else if (localVideo != null)
            {
                videoTelephony.disposeLocalVisualComponent(
                        callPeer, localVideo);
                localVideo = null;
            }
        }
    }

    /**
     * Add <tt>KeyListener</tt>, <tt>MouseListener</tt>,
     * <tt>MouseWheelListener</tt> and <tt>MouseMotionListener</tt> to remote
     * video component.
     */
    public void addMouseAndKeyListeners()
    {
        if(remoteVideo != null)
        {
            remoteVideo.addKeyListener(mouseAndKeyListener);
            remoteVideo.addMouseListener(mouseAndKeyListener);
            remoteVideo.addMouseMotionListener(mouseAndKeyListener);
            remoteVideo.addMouseWheelListener(mouseAndKeyListener);
        }
    }

    /**
     * Remove <tt>KeyListener</tt>, <tt>MouseListener</tt>,
     * <tt>MouseWheelListener</tt> and <tt>MouseMotionListener</tt> to remote
     * video component.
     */
    public void removeMouseAndKeyListeners()
    {
        if(remoteVideo != null)
        {
            remoteVideo.removeKeyListener(mouseAndKeyListener);
            remoteVideo.removeMouseListener(mouseAndKeyListener);
            remoteVideo.removeMouseMotionListener(mouseAndKeyListener);
            remoteVideo.removeMouseWheelListener(mouseAndKeyListener);
        }
    }

    /**
     * Listener for all key and mouse events and will transfer them to
     * the <tt>OperationSetDesktopSharingClient</tt>.
     *
     * @author Sebastien Vincent
     */
    private class MouseAndKeyListener
        implements RemoteControlListener,
                   KeyListener,
                   MouseListener,
                   MouseMotionListener,
                   MouseWheelListener
    {
        /**
         * Desktop sharing client-side <tt>OperationSet</tt>.
         */
        private final OperationSetDesktopSharingClient desktopSharingClient;

        /**
         * The remote-controlled <tt>CallPeer</tt>.
         */
        private final CallPeer callPeer;

        /**
         * Last time the mouse has moved inside remote video. It is used mainly
         * to avoid sending too much <tt>MouseEvent</tt> which can take a lot of
         * bandwidth.
         */
        private long lastMouseMovedTime = 0;

        /**
         * Constructor.
         *
         * @param opSet <tt>OperationSetDesktopSharingClient</tt> object
         * @param callPeer the remote-controlled <tt>CallPeer</tt>
         */
        public MouseAndKeyListener(OperationSetDesktopSharingClient opSet,
            CallPeer callPeer)
        {
            desktopSharingClient = opSet;
            this.callPeer = callPeer;
        }

        /**
         * {@inheritDoc}
         */
        public void mouseMoved(MouseEvent event)
        {
            if(System.currentTimeMillis() > lastMouseMovedTime + 50)
            {
                desktopSharingClient.sendMouseEvent(callPeer, event,
                        remoteVideo.getSize());
                lastMouseMovedTime = System.currentTimeMillis();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mousePressed(MouseEvent event)
        {
            desktopSharingClient.sendMouseEvent(callPeer, event);
        }

        /**
         * {@inheritDoc}
         */
        public void mouseReleased(MouseEvent event)
        {
            desktopSharingClient.sendMouseEvent(callPeer, event);
        }

        /**
         * {@inheritDoc}
         */
        public void mouseClicked(MouseEvent event) {}

        /**
         * {@inheritDoc}
         */
        public void mouseEntered(MouseEvent event) {}

        /**
         * {@inheritDoc}
         */
        public void mouseExited(MouseEvent event) {}

        /**
         * {@inheritDoc}
         */
        public void mouseWheelMoved(MouseWheelEvent event)
        {
            desktopSharingClient.sendMouseEvent(callPeer, event);
        }

        /**
         * {@inheritDoc}
         */
        public void mouseDragged(MouseEvent event)
        {
             desktopSharingClient.sendMouseEvent(callPeer, event,
                     remoteVideo.getSize());
        }

        /**
         * {@inheritDoc}
         */
        public void keyPressed(KeyEvent event)
        {
            char key = event.getKeyChar();
            int code = event.getKeyCode();

            if(key == KeyEvent.CHAR_UNDEFINED ||
                    code == KeyEvent.VK_CLEAR ||
                    code == KeyEvent.VK_DELETE ||
                    code == KeyEvent.VK_BACK_SPACE ||
                    code == KeyEvent.VK_ENTER)
            {
                desktopSharingClient.sendKeyboardEvent(callPeer, event);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void keyReleased(KeyEvent event)
        {
            char key = event.getKeyChar();
            int code = event.getKeyCode();

            if(key == KeyEvent.CHAR_UNDEFINED ||
                    code == KeyEvent.VK_CLEAR ||
                    code == KeyEvent.VK_DELETE ||
                    code == KeyEvent.VK_BACK_SPACE ||
                    code == KeyEvent.VK_ENTER)
            {
                desktopSharingClient.sendKeyboardEvent(callPeer, event);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void keyTyped(KeyEvent event)
        {
            char key = event.getKeyChar();

            if(key != '\n' && key != '\b')
                desktopSharingClient.sendKeyboardEvent(callPeer, event);
        }

        /**
         * This method is called when remote control has been granted.
         *
         * @param event <tt>RemoteControlGrantedEvent</tt>
         */
        public void remoteControlGranted(RemoteControlGrantedEvent event)
        {
            if(getCallPeer() != event.getCallPeer())
                return;

            allowRemoteControl = true;
            if(remoteVideo != null)
                addMouseAndKeyListeners();
        }

        /**
         * This method is called when remote control has been revoked.
         *
         * @param event <tt>RemoteControlRevokedEvent</tt>
         */
        public void remoteControlRevoked(RemoteControlRevokedEvent event)
        {
            if(getCallPeer() != event.getCallPeer())
                return;

            if(allowRemoteControl)
            {
                allowRemoteControl = false;
                removeMouseAndKeyListeners();
            }
        }

        /**
         * Returns the remote-controlled <tt>CallPeer</tt>.
         *
         * @return the remote-controlled <tt>CallPeer</tt>
         */
        public CallPeer getCallPeer()
        {
            return callPeer;
        }
    }

    /**
     * The Mouse listener for local video. It is responsible for dragging local
     * video.
     */
    private class LocalVideoMouseListener
        implements  MouseListener,
                    MouseMotionListener
    {
        /**
         * Indicates if we're currently during a drag operation.
         */
        private boolean inDrag = false;

        /**
         * The previous x coordinate of the drag.
         */
        private int previousX = 0;

        /**
         * The previous y coordinate of the drag.
         */
        private int previousY = 0;

        /**
         * Indicates that the mouse has been dragged.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mouseDragged(MouseEvent event)
        {
            Point p = event.getPoint();

            if (inDrag)
            {
                Component c = (Component) event.getSource();

                int newX = c.getX() + p.x - previousX;
                int newY = c.getY() + p.y - previousY;

                Component remoteContainer;
                if (remoteVideo != null)
                    remoteContainer = remoteVideo;
                else
                    synchronized (videoContainers)
                    {
                        // If there's no remote video we limit the drag to the
                        // parent video container.
                        if (videoContainers != null
                                && videoContainers.size() > 0)
                            remoteContainer = videoContainers.get(
                                videoContainers.size() - 1);
                        else
                            // In the case no video container is available
                            // we just limit the drag to this panel.
                            remoteContainer = callRenderer.getCallContainer();
                    }

                int minX = remoteContainer.getX();
                int maxX = remoteContainer.getX() + remoteContainer.getWidth();
                int minY = remoteContainer.getY();
                int maxY = remoteContainer.getY() + remoteContainer.getHeight();

                if (newX < minX)
                    newX = minX;

                if (newX + c.getWidth() > maxX)
                    newX = maxX - c.getWidth();

                if (newY < minY)
                    newY = minY;

                if (newY + c.getHeight() > maxY)
                    newY = maxY - c.getHeight();

                c.setLocation(newX, newY);
                if (closeButton.isVisible())
                    closeButton.setVisible(false);
            }
        }

        public void mouseMoved(MouseEvent event) {}

        public void mouseClicked(MouseEvent event) {}

        public void mouseEntered(MouseEvent event) {}

        public void mouseExited(MouseEvent event) {}

        /**
         * Indicates that the mouse has been pressed.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mousePressed(MouseEvent event)
        {
            Point p = event.getPoint();

            previousX = p.x;
            previousY = p.y;
            inDrag = true;
        }

        /**
         * Indicates that the mouse has been released.
         *
         * @param event the <tt>MouseEvent</tt> that notified us
         */
        public void mouseReleased(MouseEvent event)
        {
            inDrag = false;
            previousX = 0;
            previousY = 0;

            if (!closeButton.isVisible())
            {
                Component c = (Component) event.getSource();
                closeButton.setLocation(
                    c.getX() + c.getWidth() - closeButton.getWidth() - 3,
                    c.getY() + 3);
                closeButton.setVisible(true);
            }
        }
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
    private static boolean isAncestor(Container container, Component component)
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
     * Sets up listening to remote-control notifications (granted or revoked).
     *
     * @return reference to <tt>OperationSetDesktopSharingClient</tt>
     */
    public OperationSetDesktopSharingClient addRemoteControlListener(
            CallPeer callPeer)
    {
        Call call = callPeer.getCall();

        if (call == null)
            return null;

        OperationSetDesktopSharingClient desktopSharingClient
            = call.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingClient.class);

        if(desktopSharingClient != null)
        {
            mouseAndKeyListener = new MouseAndKeyListener(desktopSharingClient,
                callPeer);
            desktopSharingClient.addRemoteControlListener(mouseAndKeyListener);
        }
        return desktopSharingClient;
    }

    /**
     * Removes the setup for listening to remote-control notifications (granted
     * or revoked).
     */
    public void removeRemoteControlListener(CallPeer callPeer)
    {
        if (mouseAndKeyListener != null)
        {
            Call call = callPeer.getCall();

            if (call != null)
            {
                OperationSetDesktopSharingClient desktopSharingClient
                    = call.getProtocolProvider().getOperationSet(
                            OperationSetDesktopSharingClient.class);

                if (desktopSharingClient != null)
                {
                    desktopSharingClient.removeRemoteControlListener(
                        mouseAndKeyListener);
                }
            }
        }
    }

    /**
     * Adds mouse listeners in the event dispatch thread.
     *
     * @param videoType the type of the video.
     */
    private void addMouseListeners(final int videoType)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addMouseListeners(videoType);
                }
            });
            return;
        }

        if(videoType == VideoEvent.LOCAL && localVideo != null)
        {
            localVideo.addMouseMotionListener(
                localVideoListener);
            localVideo.addMouseListener(
                localVideoListener);
        }
        else if(videoType == VideoEvent.REMOTE)
        {
            if(allowRemoteControl)
            {
                addMouseAndKeyListeners();
            }
        }
    }

    /**
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible)
    {
        synchronized (videoContainers)
        {
            this.localVideoVisible = isVisible;

            if (isVisible
                != callRenderer.getCallContainer()
                    .isShowHideVideoButtonSelected())
            {
                callRenderer.getCallContainer()
                    .setShowHideVideoButtonSelected(isVisible);
            }

            int videoContainerCount;

            if ((videoTelephony != null)
                    && ((videoContainerCount = videoContainers.size()) > 0))
            {
                Container videoContainer
                    = videoContainers.get(videoContainerCount - 1);

                if (localVideo != null)
                {
                    if (isVisible)
                    {
                        Container localVideoParent = localVideo.getParent();

                        if (localVideoParent != null)
                            localVideoParent.remove(localVideo);

                        Container closeButtonParent
                            = closeButton.getParent();

                        if (closeButtonParent != null)
                            closeButtonParent.remove(closeButton);

                        videoContainer.add(localVideo, VideoLayout.LOCAL, 0);
                        videoContainer.add(
                                closeButton,
                                VideoLayout.CLOSE_LOCAL_BUTTON,
                                0);
                    }
                    else
                    {
                        videoContainer.remove(localVideo);
                        videoContainer.remove(closeButton);
                    }

                    /*
                     * Just like #handleVideoEvent(VideoEvent, Container) says,
                     * we have to be explicit in order to achieve a proper
                     * layout and an up-to-date painting.
                     */
                    videoContainer.validate();
                    videoContainer.repaint();
                }
            }
        }
    }

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible()
    {
        return localVideoVisible;
    }

    private class CloseButton
        extends Button
        implements MouseListener
    {
        private static final long serialVersionUID = 0L;

        Image image = ImageLoader.getImage(ImageLoader.CLOSE_VIDEO);

        public CloseButton()
        {
            int buttonWidth = image.getWidth(this) + 5;
            int buttonHeight = image.getHeight(this) + 5;

            setPreferredSize(new Dimension(buttonWidth, buttonHeight));
            setSize(new Dimension(buttonWidth, buttonHeight));

            this.addMouseListener(this);
        }

        public void paint(Graphics g)
        {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(image,
                getWidth()/2 - image.getWidth(this)/2,
                getHeight()/2 - image.getHeight(this)/2, this);
        }

        public void mouseClicked(MouseEvent event)
        {
            setLocalVideoVisible(false);

            callRenderer.getCallContainer()
                .setShowHideVideoButtonSelected(false);
        }

        public void mouseEntered(MouseEvent event) {}

        public void mouseExited(MouseEvent event) {}

        public void mousePressed(MouseEvent event) {}

        public void mouseReleased(MouseEvent event) {}
    }

    /**
     * Get the local video <tt>Component</tt>.
     *
     * @return the local video <tt>Component</tt>
     */
    public Component getLocalVideoComponent()
    {
        return localVideo;
    }

    /**
     * Get the remote video <tt>Component</tt>.
     *
     * @return the remote video <tt>Component</tt>.
     */
    @Deprecated
    public Component getRemoteVideoComponent()
    {
        return remoteVideo;
    }

    /**
     * Creates the default photo panel.
     *
     * @param callPeer the corresponding call peer
     * @param videoToolbar the corresponding video tool bar
     * @return 
     */
    private Component createDefaultPhotoPanel(  CallPeer callPeer,
                                                Component videoToolbar)
    {
        JPanel defaultPanel = new TransparentPanel(new BorderLayout());

        JPanel photoPanel = new TransparentPanel(new BorderLayout())
        {
            /**
             * @{inheritDoc}
             */
            @Override
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);

                g = g.create();

                try
                {
                    AntialiasingManager.activateAntialiasing(g);

                    g.setColor(Color.GRAY);
                    g.fillRoundRect(
                        0, 0, this.getWidth(), this.getHeight(), 6, 6);
                }
                finally
                {
                    g.dispose();
                }
            }
        };

        JLabel photoLabel = new JLabel(getPhotoLabelIcon(callPeer));
        photoPanel.add(photoLabel);
        photoPanel.setPreferredSize(new Dimension(320, 240));

        defaultPanel.add(photoPanel, BorderLayout.CENTER);
        defaultPanel.add(videoToolbar, BorderLayout.SOUTH);

        return defaultPanel;
    }
}
