/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>OneToOneCallPeerPanel</tt> is the panel containing data for a call
 * peer in a given call. It contains information like call peer
 * name, photo, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class OneToOneCallPeerPanel
    extends TransparentPanel
    implements CallPeerRenderer
{
    private static final Logger logger
        = Logger.getLogger(OneToOneCallPeerPanel.class);

    private CallPeerAdapter callPeerAdapter;

    /**
     * The component showing the name of the underlying call peer.
     */
    private final JLabel nameLabel = new JLabel("", JLabel.CENTER);

    /**
     * The component showing the status of the underlying call peer.
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel = new SecurityStatusLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the call is on or off hold.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The component showing the avatar of the underlying call peer.
     */
    private final JLabel photoLabel
        = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.DEFAULT_USER_PHOTO)));

    /**
     * The panel containing security related components.
     */
    private SecurityPanel securityPanel;

    /**
     * The name of the peer.
     */
    private final String peerName;

    /**
     * The list containing all video containers.
     */
    private final java.util.List<Container> videoContainers =
        new ArrayList<Container>();

    /**
     * The operation set through which we do all video operations.
     */
    private OperationSetVideoTelephony videoTelephony;

    /**
     * The parent dialog, where this panel is contained.
     */
    private final CallDialog callDialog;

    /**
     * The component showing the local video.
     */
    private Component localVideo;

    /**
     * The current <code>Window</code> being displayed in full-screen. Because
     * the AWT API with respect to the full-screen support doesn't seem
     * sophisticated enough, the field is used sparingly i.e. when there are no
     * other means (such as a local variable) of acquiring the instance.
     */
    private Window fullScreenWindow;

    private CallPeer callPeer;

    /**
     * Creates a <tt>CallPeerPanel</tt> for the given call peer.
     *
     * @param callDialog the parent dialog containing this call peer panel
     * @param callPeer the <tt>CallPeer</tt> represented in this panel
     */
    public OneToOneCallPeerPanel(   CallDialog callDialog,
                                    CallPeer callPeer)
    {
        this.callDialog = callDialog;
        this.callPeer = callPeer;
        this.peerName = callPeer.getDisplayName();

        /* Create the main Components of the UI. */
        nameLabel.setText(peerName);
        nameLabel.setAlignmentX(JLabel.CENTER);

        Component center = createCenter();
        Component statusBar = createStatusBar();

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        if (nameLabel != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;

            add(nameLabel, constraints);
        }
        if (center != null)
        {
            /*
             * Don't let the center dictate the preferred size because it may
             * display large videos. Otherwise, the large video will make this
             * panel expand and then the panel's container will show scroll
             * bars.
             */
            center.setPreferredSize(new Dimension(1, 1));

            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 1;

            add(center, constraints);
        }
        if (statusBar != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            add(statusBar, constraints);
        }

        this.createSoundLevelIndicators();

        addVideoListener();
    }

    /**
     * Creates the <code>Component</code> hierarchy of the central area of this
     * <code>CallPeerPanel</code> which displays the photo of the
     * <code>CallPeer</code> or the video if any.
     *
     * @return the root of the <code>Component</code> hierarchy of the central
     *         area of this <code>CallPeerPanel</code> which displays the
     *         photo of the <code>CallPeer</code> or the video if any
     */
    private Component createCenter()
    {
        photoLabel.setPreferredSize(new Dimension(90, 90));

        final Container videoContainer = createVideoContainer(photoLabel);
        videoContainer.addHierarchyListener(new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent event)
            {
                int changeFlags = HierarchyEvent.DISPLAYABILITY_CHANGED;

                if ((event.getChangeFlags() & changeFlags) == changeFlags)
                {
                    synchronized (videoContainers)
                    {
                        boolean changed = false;

                        if (videoContainer.isDisplayable())
                        {
                            if (!videoContainers.contains(videoContainer))
                                changed = videoContainers.add(videoContainer);
                        }
                        else
                            changed = videoContainers.remove(videoContainer);
                        if (changed)
                            handleVideoEvent(null);
                    }
                }
            }
        });
        return videoContainer;
    }

    /**
     * Creates a new AWT <code>Container</code> which can display a single
     * <code>Component</code> at a time (supposedly, one which represents video)
     * and, in the absence of such a <code>Component</code>, displays a
     * predefined default <code>Component</code> (in accord with the previous
     * supposition, one which is the default when there is no video). The
     * returned <code>Container</code> will track the <code>Components</code>s
     * added to and removed from it in order to make sure that
     * <code>noVideoContainer</code> is displayed as described.
     *
     * @param noVideoComponent the predefined default <code>Component</code> to
     *            be displayed in the returned <code>Container</code> when there
     *            is no other <code>Component</code> in it
     * @return a new <code>Container</code> which can display a single
     *         <code>Component</code> at a time and, in the absence of such a
     *         <code>Component</code>, displays <code>noVideoComponent</code>
     */
    private Container createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent);
    }

    /**
     * Creates the <code>Component</code> hierarchy of the area of
     * status-related information such as <code>CallPeer</code> display
     * name, call duration, security status.
     *
     * @return the root of the <code>Component</code> hierarchy of the area of
     *         status-related information such as <code>CallPeer</code>
     *         display name, call duration, security status
     */
    private Component createStatusBar()
    {
        // stateLabel
        callStatusLabel.setForeground(Color.WHITE);
        callStatusLabel.setText(callPeer.getState().getStateString());

        PeerStatusPanel statusPanel = new PeerStatusPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0));

        TransparentPanel statusIconsPanel
            = new TransparentPanel(
                new FlowLayout(FlowLayout.CENTER, 5, 0));

        statusIconsPanel.add(securityStatusLabel);
        statusIconsPanel.add(holdStatusLabel);
        statusIconsPanel.add(muteStatusLabel);
        statusIconsPanel.add(callStatusLabel);

        statusPanel.add(statusIconsPanel);

        Component[] buttons =
            new Component[]
            {
                CallPeerRendererUtils.createTransferCallButton(callPeer),
                CallPeerRendererUtils.createEnterFullScreenButton(this)
            };

        Component buttonBar
            = CallPeerRendererUtils.createButtonBar(false, buttons);

        statusPanel.add(buttonBar);

        return statusPanel;
    }

    /**
     * Creates sound level related components.
     */
    private void createSoundLevelIndicators()
    {
        TransparentPanel localLevelPanel
            = new TransparentPanel(new BorderLayout(5, 0));
        TransparentPanel remoteLevelPanel
            = new TransparentPanel(new BorderLayout(5, 0));

        JLabel localLevelLabel
            = new JLabel(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MICROPHONE)));
        JLabel remoteLevelLabel
            = new JLabel(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HEADPHONE)));

        final SoundLevelIndicator localLevelIndicator
            = new SoundLevelIndicator(  SoundLevelChangeEvent.MIN_LEVEL,
                                        SoundLevelChangeEvent.MAX_LEVEL);

        final SoundLevelIndicator remoteLevelIndicator
            = new SoundLevelIndicator(  SoundLevelChangeEvent.MIN_LEVEL,
                                        SoundLevelChangeEvent.MAX_LEVEL);

        localLevelPanel.add(localLevelLabel, BorderLayout.WEST);
        localLevelPanel.add(localLevelIndicator, BorderLayout.CENTER);
        remoteLevelPanel.add(remoteLevelLabel, BorderLayout.WEST);
        remoteLevelPanel.add(remoteLevelIndicator, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);

        add(localLevelPanel, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 0, 10, 0);

        add(remoteLevelPanel, constraints);

        this.callPeer.addStreamSoundLevelListener(new SoundLevelListener()
        {
            public void soundLevelChanged(SoundLevelChangeEvent event)
            {
                remoteLevelIndicator.updateSoundLevel(event.getLevel());
            }
        });

        this.callPeer.getCall().addLocalUserSoundLevelListener(
            new SoundLevelListener()
            {
                public void soundLevelChanged(SoundLevelChangeEvent event)
                {
                    localLevelIndicator.updateSoundLevel(event.getLevel());
                }
            });
    }

    private class VideoTelephonyListener
        implements PropertyChangeListener,
                   VideoListener
    {
        public void propertyChange(PropertyChangeEvent event)
        {
            if (OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING
                    .equals(event.getPropertyName()))
                handleLocalVideoStreamingChange(this);
        }

        public void videoAdded(VideoEvent event)
        {
            handleVideoEvent(event);
        }

        public void videoRemoved(VideoEvent event)
        {
            handleVideoEvent(event);
        }

        public void videoUpdate(VideoEvent event)
        {
            handleVideoEvent(event);
        }
    }

    /**
     * Sets up listening to notifications about adding or removing video for the
     * <code>CallPeer</code> this panel depicts and displays the video in
     * question in the last-known of {@link #videoContainers} (because the video
     * is represented by a <code>Component</code> and it cannot be displayed in
     * multiple <code>Container</code>s at one and the same time) as soon as it
     * arrives.
     * @return the video telephony operation set, where the vide listener was
     * added
     */
    private OperationSetVideoTelephony addVideoListener()
    {
        final Call call = callPeer.getCall();
        if (call == null)
            return null;

        final OperationSetVideoTelephony telephony =
            call.getProtocolProvider()
                .getOperationSet(OperationSetVideoTelephony.class);
        if (telephony == null)
            return null;

        final VideoTelephonyListener videoTelephonyListener
            = new VideoTelephonyListener();

        /*
         * The video is only available while the #callPeer is in a Call
         * and that call is in progress so only listen to VideoEvents during
         * that time.
         */
        CallChangeListener callListener = new CallChangeListener()
        {
            private boolean videoListenerIsAdded;

            private void addVideoListener()
            {
                telephony.addVideoListener(
                        callPeer, videoTelephonyListener);
                telephony.addPropertyChangeListener(
                        call, videoTelephonyListener);
                videoListenerIsAdded = true;

                synchronized (videoContainers)
                {
                    videoTelephony = telephony;

                    handleVideoEvent(null);

                    handleLocalVideoStreamingChange(
                            videoTelephonyListener);
                }
            }

            /*
             * When the #callPeer of this CallPeerPanel gets added
             * to the Call, starts listening for changes in the video in order
             * to display it.
             */
            public synchronized void callPeerAdded(
                CallPeerEvent event)
            {
                if (callPeer.equals(event.getSourceCallPeer())
                        && !videoListenerIsAdded)
                {
                    Call call = callPeer.getCall();

                    if ((call != null)
                            && CallState.CALL_IN_PROGRESS.equals(
                                    call.getCallState()))
                        addVideoListener();
                }
            }

            /*
             * When the #callPeer of this CallPeerPanel leaves the
             * Call, stops listening for changes in the video because it should
             * no longer be updated anyway.
             */
            public synchronized void callPeerRemoved(
                CallPeerEvent event)
            {
                if (callPeer.equals(event.getSourceCallPeer())
                    && videoListenerIsAdded)
                {
                    Call call = callPeer.getCall();

                    if (call != null)
                        removeVideoListener();
                }
            }

            /*
             * When the Call of #callPeer ends, stops tracking the
             * updates in the video because there should no longer be any video
             * anyway. When the Call in question starts, starts tracking any
             * changes to the video because it's negotiated and it should be
             * displayed in this CallPeerPanel.
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
                    if (videoListenerIsAdded)
                        removeVideoListener();
                    call.removeCallChangeListener(this);
                }
                else if (CallState.CALL_IN_PROGRESS.equals(newCallState))
                {
                    if (!videoListenerIsAdded)
                        addVideoListener();
                }
            }

            private void removeVideoListener()
            {
                telephony.removeVideoListener(
                        callPeer, videoTelephonyListener);
                telephony.removePropertyChangeListener(
                        call, videoTelephonyListener);
                videoListenerIsAdded = false;

                if (localVideo != null)
                {
                    telephony.disposeLocalVisualComponent(
                            callPeer, localVideo);
                    localVideo = null;
                }

                synchronized (videoContainers)
                {
                    if (telephony.equals(videoTelephony))
                        videoTelephony = null;
                }

                exitFullScreen(fullScreenWindow);
            }
        };
        call.addCallChangeListener(callListener);
        callListener.callStateChanged(new CallChangeEvent(call,
            CallChangeEvent.CALL_STATE_CHANGE, null, call.getCallState()));

        return telephony;
    }

    /**
     * When a video is added or removed for the <tt>callPeer</tt>, makes sure to
     * display or hide it respectively.
     *
     * @param event a <tt>VideoEvent</tt> describing the added visual
     * <tt>Component</tt> representing video and the provider it was added into
     * or <tt>null</tt> if such information is not available
     */
    private void handleVideoEvent(final VideoEvent event)
    {
        synchronized (videoContainers)
        {
            if ((event != null)
                    && !event.isConsumed()
                    && (event.getOrigin() == VideoEvent.LOCAL))
            {
                Component localVideo = event.getVisualComponent();

                switch (event.getType())
                {
                case VideoEvent.VIDEO_ADDED:
                    this.localVideo = localVideo;

                    /*
                     * Let the creator of the local visual Component know it
                     * shouldn't be disposed of because we're going to use it.
                     */
                    event.consume();
                    break;

                case VideoEvent.VIDEO_REMOVED:
                    if (this.localVideo == localVideo)
                        this.localVideo = null;
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
                    handleVideoEvent(event);
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

                handleVideoEvent(event, videoContainer);
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
                    visualComponent.setPreferredSize(new Dimension(width, height));
                else if (isAncestor(videoContainer, visualComponent))
                    ensureSize(visualComponent, width, height);
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

        int zOrder = 0;

        videoContainer.removeAll();

        // LOCAL
        if (localVideo != null)
        {
            videoContainer.add(localVideo, VideoLayout.LOCAL, zOrder++);

            /*
             * If the local video is turned on, we ensure that the button is
             * selected.
             */
            if (!callDialog.isVideoButtonSelected())
                callDialog.setVideoButtonSelected(true);
        }

        // REMOTE
        Component video = videoTelephony.getVisualComponent(callPeer);

        if (video != null)
            videoContainer.add(video, VideoLayout.CENTER_REMOTE, zOrder++);

        videoContainer.validate();

        /*
         * Without explicit repainting, the remote visual Component will not
         * stay small after entering fullscreen, the Component shown when there
         * is no video will be shown beneath the video Component though the
         * former has already been removed...
         */
        videoContainer.repaint();
    }

    private void handleLocalVideoStreamingChange(
            VideoTelephonyListener listener)
    {
        synchronized (videoContainers)
        {
            if (videoTelephony == null)
                return;

            if (videoTelephony.isLocalVideoStreaming(callPeer.getCall()))
            {
                try
                {
                    videoTelephony.createLocalVisualComponent(
                            callPeer, listener);
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                            "Failed to create local video/visual Component.",
                            ex);
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
     * Returns the name of the peer, contained in this panel.
     *
     * @return the name of the peer, contained in this panel
     */
    public String getPeerName()
    {
        return peerName;
    }

    private Component createFullScreenButtonBar()
    {
        CallPeerState peerState = callPeer.getState();

        Component[] buttons =
            new Component[]
            {   new HoldButton( callPeer.getCall(),
                                true,
                                CallPeerState.isOnHold(peerState)),
                new MuteButton( callPeer.getCall(),
                                true,
                                callPeer.isMute()),
                CallPeerRendererUtils.createExitFullScreenButton(this) };

        Component fullScreenButtonBar
            = CallPeerRendererUtils.createButtonBar(true, buttons);

        return fullScreenButtonBar;
    }

    /**
     * Attempts to give a specific <tt>Component</tt> a visible rectangle with a
     * specific width and a specific height if possible and sane.
     *
     * @param component the <tt>Component</tt> to be given a visible rectangle
     * with the specified width and height
     * @param width the width of the visible rectangle to be given to the
     * specified <tt>Component</tt>
     * @param height the height of the visible rectangle to be given to the
     * specified <tt>Component</tt>
     * @return <tt>true</tt> if an actual attempt has been made because it
     * seemed possible and sounded sane; otherwise, <tt>false</tt>
     */
    private boolean ensureSize(Component component, int width, int height)
    {
        Frame frame = TransferCallButton.getFrame(component);

        if (frame == null)
            return false;
        else if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH)
                == Frame.MAXIMIZED_BOTH)
        {
            /*
             * Forcing the size of a Component which is displayed in a maximized
             * window does not sound like anything we want to do.
             */
            return false;
        }
        else if (frame
                .equals(
                    frame
                        .getGraphicsConfiguration()
                            .getDevice().getFullScreenWindow()))
        {
            /*
             * Forcing the size of a Component which is displayed in a
             * full-screen window does not sound like anything we want to do.
             */
            return false;
        }
        else
        {
            Dimension frameSize = frame.getSize();
            Dimension componentSize = component.getSize();

            int widthDelta = width - componentSize.width;
            int heightDelta = height - componentSize.height;
            int newFrameWidth
                = (widthDelta > 0)
                    ? (frameSize.width + widthDelta)
                    : frameSize.width;
            int newFrameHeight
                = (heightDelta > 0)
                    ? (frameSize.height + heightDelta)
                    : frameSize.height;

            // Don't get bigger than the screen.
            Rectangle screenBounds
                = frame.getGraphicsConfiguration().getBounds();

            if (newFrameWidth > screenBounds.width)
                newFrameWidth = screenBounds.width;
            if (newFrameHeight > screenBounds.height)
                newFrameHeight = screenBounds.height;

            // Don't go out of the screen.
            Point frameLocation = frame.getLocation();
            int newFrameX = frameLocation.x;
            int newFrameY = frameLocation.y;
            int xDelta
                = (newFrameX + newFrameWidth)
                    - (screenBounds.x + screenBounds.width);
            int yDelta
                = (newFrameY + newFrameHeight)
                    - (screenBounds.y + screenBounds.height);

            if (xDelta > 0)
            {
                newFrameX -= xDelta;
                if (newFrameX < screenBounds.x)
                    newFrameX = screenBounds.x;
            }
            if (yDelta > 0)
            {
                newFrameY -= yDelta;
                if (newFrameY < screenBounds.y)
                    newFrameY = screenBounds.y;
            }

            component.setPreferredSize(new Dimension(width, height));
            frame
                .setBounds(newFrameX, newFrameY, newFrameWidth, newFrameHeight);
            return true;
        }
    }

    /**
     * Enters full screen mode. Initializes all components for the full screen
     * user interface.
     */
    public void enterFullScreen()
    {
        // Create the main Components of the UI.
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setTitle(getPeerName());
        frame.setUndecorated(true);

        Component center = createCenter();
        final Component buttonBar = createFullScreenButtonBar();

        // Lay out the main Components of the UI.
        final Container contentPane = frame.getContentPane();
        contentPane.setLayout(new FullScreenLayout(true));
        if (buttonBar != null)
            contentPane.add(buttonBar, FullScreenLayout.SOUTH);
        if (center != null)
            contentPane.add(center, FullScreenLayout.CENTER);

        // Full-screen windows usually have black backgrounds.
        Color background = Color.black;
        contentPane.setBackground(background);
        CallPeerRendererUtils.setBackground(center, background);

        class FullScreenListener
            implements ContainerListener, KeyListener, WindowStateListener
        {
            public void componentAdded(ContainerEvent event)
            {
                Component child = event.getChild();

                child.addKeyListener(this);
            }

            public void componentRemoved(ContainerEvent event)
            {
                Component child = event.getChild();

                child.removeKeyListener(this);
            }

            public void keyPressed(KeyEvent event)
            {
                if (!event.isConsumed()
                    && (event.getKeyCode() == KeyEvent.VK_ESCAPE))
                {
                    event.consume();
                    exitFullScreen(frame);
                }
            }

            public void keyReleased(KeyEvent event)
            {
            }

            public void keyTyped(KeyEvent event)
            {
            }

            public void windowStateChanged(WindowEvent event)
            {
                switch (event.getID())
                {
                case WindowEvent.WINDOW_CLOSED:
                case WindowEvent.WINDOW_DEACTIVATED:
                case WindowEvent.WINDOW_ICONIFIED:
                case WindowEvent.WINDOW_LOST_FOCUS:
                    exitFullScreen(frame);
                    break;
                }
            }
        }
        FullScreenListener listener = new FullScreenListener();

        // Exit on Escape.
        CallPeerRendererUtils.addKeyListener(frame, listener);
        // Activate the above features for the local and remote videos.
        if (center instanceof Container)
            ((Container) center).addContainerListener(listener);
        // Exit when the "full screen" looses its focus.
        frame.addWindowStateListener(listener);

        getGraphicsConfiguration().getDevice().setFullScreenWindow(frame);
        this.fullScreenWindow = frame;
    }

    /**
     * Exits the full screen mode.
     * @param fullScreenWindow the window shown in the full screen mode
     */
    public void exitFullScreen(Window fullScreenWindow)
    {
        GraphicsConfiguration graphicsConfig = getGraphicsConfiguration();
        if (graphicsConfig != null)
            graphicsConfig.getDevice().setFullScreenWindow(null);

        if (fullScreenWindow != null)
        {
            if (fullScreenWindow.isVisible())
                fullScreenWindow.setVisible(false);
            fullScreenWindow.dispose();

            if (this.fullScreenWindow == fullScreenWindow)
                this.fullScreenWindow = null;
        }
    }

    private static class PeerStatusPanel
        extends TransparentPanel
    {
        /*
         * Silence the serial warning. Though there isn't a plan to serialize
         * the instances of the class, there're no fields so the default
         * serialization routine will work.
         */
        private static final long serialVersionUID = 0L;

        public PeerStatusPanel(LayoutManager layout)
        {
            super(layout);
            this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.setColor(Color.DARK_GRAY);
                g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        this.nameLabel.setText(name);
    }

    /**
     * Set the image of the peer
     *
     * @param peerImage new image
     */
    public void setPeerImage(ImageIcon peerImage)
    {
        this.photoLabel.setIcon(peerImage);
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name and icon.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state)
    {
        this.callStatusLabel.setText(state);
    }

    /**
     * Sets the mute status icon to the status panel.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute)
    {
        if(isMute)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
        else
            muteStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold)
    {
        if(isOnHold)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));
        else
            holdStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified by the underlying <tt>CallPeer</tt>
     */
    public void securityOn( String securityString,
                            boolean isSecurityVerified)
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_ON)));

        if (securityPanel == null)
        {
            securityPanel = new SecurityPanel(callPeer);

            GridBagConstraints constraints = new GridBagConstraints();

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);


            this.add(securityPanel, constraints);
        }

        securityPanel.refreshStates(securityString, isSecurityVerified);

        this.revalidate();
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
     * Indicates that the security has gone off.
     */
    public void securityOff()
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF)));
    }

    /**
     * Updates all related components to fit the new value.
     * @param isAudioSecurityOn indicates if the audio security is turned on
     * or off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        securityStatusLabel.setAudioSecurityOn(isAudioSecurityOn);
    }

    /**
     * Updates all related components to fit the new value.
     * @param encryptionCipher the encryption cipher to show
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        securityStatusLabel.setEncryptionCipher(encryptionCipher);
    }

    /**
     * Updates all related components to fit the new value.
     * @param isVideoSecurityOn indicates if the video security is turned on
     * or off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        securityStatusLabel.setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * Sets the call peer adapter managing all related listeners.
     * @param adapter the adapter to set
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        this.callPeerAdapter = adapter;
    }

    /**
     * Returns the call peer adapter managing all related listeners.
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter()
    {
        return callPeerAdapter;
    }

    /**
     * Returns the parent <tt>CallDialog</tt> containing this renderer.
     * @return the parent <tt>CallDialog</tt> containing this renderer
     */
    public CallDialog getCallDialog()
    {
        return callDialog;
    }
}
