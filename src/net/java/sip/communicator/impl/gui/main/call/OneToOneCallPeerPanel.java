/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.main.call.conference.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>OneToOneCallPeerPanel</tt> is the panel containing data for a call
 * peer in a given call. It contains information like call peer
 * name, photo, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 * @author Adam Netocny
 */
public class OneToOneCallPeerPanel
    extends TransparentPanel
    implements  CallPeerRenderer,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>OneToOneCallPeerPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OneToOneCallPeerPanel.class);

    /**
     * The <tt>CallPeerAdapter</tt> that implements all common tt>CallPeer</tt>
     * related listeners.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * The component showing the status of the underlying call peer.
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel;

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the call is on or off hold.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The DTMF label.
     */
    private final JLabel dtmfLabel = new JLabel();

    /**
     * Current id for security image.
     */
    private ImageID securityImageID = ImageLoader.SECURE_BUTTON_OFF;

    /**
     * The component responsible for displaying an error message.
     */
    private JTextComponent errorMessageComponent;

    /**
     * The <tt>Component</tt>s showing the avatar of the underlying call peer.
     * <p>
     * Because the <tt>Component</tt>s showing the avatar of the underlying call
     * peer are managed by their respective <tt>VideoContainer</tt>s and are
     * automatically displayed whenever there is no associated remote video,
     * each <tt>VideoContainer</tt> must have its own. Otherwise, the various
     * <tt>VideoContainer</tt>s might start fighting over which one is to
     * contain the one and only photoLabel.
     * </p>
     */
    private final List<JLabel> photoLabels = new LinkedList<JLabel>();

    /**
     * The panel containing security related components.
     */
    private SecurityPanel securityPanel;

    /**
     * The <tt>Icon</tt> which represents the avatar of the associated call
     * peer.
     */
    private ImageIcon peerImage;

    /**
     * The name of the peer.
     */
    private String peerName;

    /**
     * The list containing all video containers.
     */
    private final List<Container> videoContainers;

    /**
     * The renderer of the call.
     */
    private final CallRenderer callRenderer;

    /**
     * The component showing the remote video.
     */
    private Component remoteVideo;

    /**
     * The <tt>CallPeer</tt>, which is rendered in this panel.
     */
    private CallPeer callPeer;

    /**
     * In case of desktop streaming (client-side) if the local peer can control
     * remote peer's computer.
     */
    private boolean allowRemoteControl = false;

    /**
     * Listener for all key and mouse events. It is used for desktop sharing
     * purposes.
     */
    private MouseAndKeyListener mouseAndKeyListener = null;

    /**
     * Sound local level label.
     */
    private InputVolumeControlButton localLevel;

    /**
     * Sound remote level label.
     */
    private OutputVolumeControlButton remoteLevel;

    /**
     * The center component.
     */
    private final Component center;

    /**
     * The status bar component.
     */
    private final Component statusBar;

    /**
     * The label containing the user photo.
     */
    private final JLabel photoLabel;

    private boolean localVideoVisible = true;

    private final UIVideoHandler videoHandler;

    /**
     * Creates a <tt>CallPeerPanel</tt> for the given call peer.
     *
     * @param callRenderer the renderer of the call
     * @param callPeer the <tt>CallPeer</tt> represented in this panel
     */
    public OneToOneCallPeerPanel(   CallRenderer callRenderer,
                                    CallPeer callPeer,
                                    List<Container> videoContainers,
                                    UIVideoHandler vHandler)
    {
        this.callRenderer = callRenderer;
        this.callPeer = callPeer;
        this.peerName = callPeer.getDisplayName();
        this.videoContainers = videoContainers;

        if (vHandler == null)
            videoHandler
                = new UIVideoHandler(callPeer, callRenderer, videoContainers);
        else
        {
            videoHandler = vHandler;
            videoHandler.setVideoContainersList(videoContainers);
        }

        videoHandler.addVideoListener();
        videoHandler.addRemoteControlListener();

        securityStatusLabel = new SecurityStatusLabel(
            callRenderer.getCallContainer().getCallWindow().getFrame(),
            new ImageIcon(
                ImageLoader.getImage(ImageLoader.SECURE_BUTTON_OFF)));

        photoLabel = new JLabel(getPhotoLabelIcon());
        center = createCenter(videoContainers);
        statusBar = createStatusBar();

        this.setPeerImage(CallManager.getPeerImage(callPeer));

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

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

        createSoundLevelIndicators();
        initSecuritySettings();
    }

    /**
     * Creates a <tt>CallPeerPanel</tt> for the given call peer.
     *
     * @param callRenderer the renderer of the call
     * @param callPeer the <tt>CallPeer</tt> represented in this panel
     */
    public OneToOneCallPeerPanel(   CallRenderer callRenderer,
                                    CallPeer callPeer,
                                    List<Container> videoContainers)
    {
        this(callRenderer, callPeer, videoContainers, null);
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
    Component createCenter(final List<Container> videoContainers)
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
                            if (!photoLabels.contains(photoLabel))
                                photoLabels.add(photoLabel);
                        }
                        else
                        {
                            changed = videoContainers.remove(videoContainer);
                            photoLabels.remove(photoLabel);
                        }
                        if (changed)
                            videoHandler.handleVideoEvent(null);
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
        VideoContainer oldParent
            = (VideoContainer) noVideoComponent.getParent();
        if (oldParent != null)
            oldParent.remove(noVideoComponent);

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
        dtmfLabel.setForeground(Color.WHITE);
        callStatusLabel.setText(callPeer.getState().getLocalizedStateString());

        PeerStatusPanel statusPanel = new PeerStatusPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        statusPanel.add(securityStatusLabel, constraints);

        constraints.gridx++;
        statusPanel.add(holdStatusLabel, constraints);

        constraints.gridx++;
        statusPanel.add(muteStatusLabel, constraints);

        constraints.gridx++;
        statusPanel.add(callStatusLabel, constraints);

        constraints.gridx++;
        constraints.weightx = 1f;
        statusPanel.add(dtmfLabel, constraints);

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

        localLevel = new InputVolumeControlButton(
                callPeer.getCall(),
                ImageLoader.MICROPHONE,
                ImageLoader.MUTE_BUTTON,
                false, false, false);

        remoteLevel = new OutputVolumeControlButton(
                ImageLoader.HEADPHONE, false, false);

        final SoundLevelIndicator localLevelIndicator
            = new SoundLevelIndicator(  callRenderer,
                                        SoundLevelChangeEvent.MIN_LEVEL,
                                        SoundLevelChangeEvent.MAX_LEVEL);

        final SoundLevelIndicator remoteLevelIndicator
            = new SoundLevelIndicator(  callRenderer,
                                        SoundLevelChangeEvent.MIN_LEVEL,
                                        SoundLevelChangeEvent.MAX_LEVEL);

        localLevelPanel.add(localLevel, BorderLayout.WEST);
        localLevelPanel.add(localLevelIndicator, BorderLayout.CENTER);
        remoteLevelPanel.add(remoteLevel, BorderLayout.WEST);
        remoteLevelPanel.add(remoteLevelIndicator, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);

        add(localLevelPanel, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 0, 10, 0);

        add(remoteLevelPanel, constraints);

        this.callPeer.addStreamSoundLevelListener(
                new SoundLevelListener()
                {
                    public void soundLevelChanged(Object source, int level)
                    {
                        remoteLevelIndicator.updateSoundLevel(level);
                    }
                });

        this.callPeer.getCall().addLocalUserSoundLevelListener(
                new SoundLevelListener()
                {
                    public void soundLevelChanged(Object source, int level)
                    {
                        localLevelIndicator.updateSoundLevel(level);
                    }
                });
    }

    /**
     * Initializes the security settings for this call peer.
     */
    private void initSecuritySettings()
    {
        CallPeerSecurityStatusEvent securityEvent
            = callPeer.getCurrentSecuritySettings();

        if (securityEvent != null
            && securityEvent instanceof CallPeerSecurityOnEvent)
        {
            securityOn((CallPeerSecurityOnEvent) securityEvent);
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

    /**
     * The <tt>TransparentPanel</tt> that will display the peer status.
     */
    private static class PeerStatusPanel
        extends TransparentPanel
    {
        /**
         * Silence the serial warning. Though there isn't a plan to serialize
         * the instances of the class, there're no fields so the default
         * serialization routine will work.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Constructs a new <tt>PeerStatusPanel</tt>.
         *
         * @param layout the <tt>LayoutManager</tt> to use
         */
        public PeerStatusPanel(LayoutManager layout)
        {
            super(layout);
            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

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
        peerName = name;

        ((OneToOneCallPanel) callRenderer).setPeerName(name);
    }

    /**
     * Set the image of the peer
     *
     * @param image new image
     */
    public void setPeerImage(byte[] image)
    {
        if (image == null || image.length <= 0)
        {
            TreeContactList.setSourceContactImage(
                peerName, photoLabel, 100, 100);
        }
        else
        {
            this.peerImage = ImageUtils.getScaledRoundedIcon(image, 100, 100);

            this.peerImage = getPhotoLabelIcon();

            synchronized (videoContainers)
            {
                if (photoLabels.size() > 0)
                    for (JLabel photoLabel : photoLabels)
                    {
                        photoLabel.setIcon(this.peerImage);
                        photoLabel.repaint();
                    }
                else
                {
                    photoLabel.setIcon(peerImage);
                    photoLabel.repaint();
                }
            }
        }
    }

    /**
     * Gets the <tt>Icon</tt> to be displayed in {@link #photoLabels}.
     *
     * @return the <tt>Icon</tt> to be displayed in {@link #photoLabels}
     */
    private ImageIcon getPhotoLabelIcon()
    {
        return
            (peerImage == null)
                ? new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO))
                : peerImage;
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
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        if((evt.getSecurityController().requiresSecureSignalingTransport()
            && callPeer.getProtocolProvider().isSignalingTransportSecure())
            || !evt.getSecurityController().requiresSecureSignalingTransport())
        {
            securityImageID = ImageLoader.SECURE_BUTTON_ON;
            securityStatusLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(securityImageID)));
        }

        //set common encryption properties
        securityStatusLabel.setEncryptionCipher(evt.getCipher());
        switch (evt.getSessionType())
        {
            case CallPeerSecurityStatusEvent.AUDIO_SESSION:
                securityStatusLabel.setAudioSecurityOn(true);
                break;
            case CallPeerSecurityStatusEvent.VIDEO_SESSION:
                securityStatusLabel.setVideoSecurityOn(true);
                break;
        }

        //set encryption specific options
        if (securityPanel == null)
        {
            securityPanel = SecurityPanel.create(evt.getSecurityController());

            GridBagConstraints constraints = new GridBagConstraints();

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);
            this.add(securityPanel, constraints);
        }
        securityPanel.refreshStates();

        this.revalidate();
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        securityImageID = ImageLoader.SECURE_BUTTON_OFF;
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(securityImageID)));

        switch (evt.getSessionType())
        {
            case CallPeerSecurityStatusEvent.AUDIO_SESSION:
                securityStatusLabel.setAudioSecurityOn(false);
                break;
            case CallPeerSecurityStatusEvent.VIDEO_SESSION:
                securityStatusLabel.setVideoSecurityOn(false);
                break;
        }

        if (securityPanel != null
            && !securityStatusLabel.isAudioSecurityOn()
            && !securityStatusLabel.isVideoSecurityOn())
        {
            securityPanel.getParent().remove(securityPanel);
            securityPanel = null;
        }
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
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel()
    {
        return callRenderer.getCallContainer();
    }

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar)
    {
        dtmfLabel.setText(dtmfLabel.getText() + dtmfChar);
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
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    public void setErrorReason(String reason)
    {
        if (errorMessageComponent == null)
        {
            errorMessageComponent = new JTextPane();

            JTextPane textPane = (JTextPane) errorMessageComponent;
            textPane.setEditable(false);
            textPane.setOpaque(false);

            StyledDocument doc = textPane.getStyledDocument();

            MutableAttributeSet standard = new SimpleAttributeSet();
            StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
            StyleConstants.setFontFamily(standard,
                                        textPane.getFont().getFamily());
            StyleConstants.setFontSize(standard, 12);
            doc.setParagraphAttributes(0, 0, standard, true);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            add(errorMessageComponent, constraints);
            this.revalidate();
        }

        errorMessageComponent.setText(reason);

        if (isVisible())
            errorMessageComponent.repaint();
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
         * Desktop sharing clien-side <tt>OperationSet</tt>.
         */
        private final OperationSetDesktopSharingClient desktopSharingClient;

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
         */
        public MouseAndKeyListener(OperationSetDesktopSharingClient opSet)
        {
            desktopSharingClient = opSet;
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
        public void mouseClicked(MouseEvent event)
        {
            /* do nothing */
        }

        /**
         * {@inheritDoc}
         */
        public void mouseEntered(MouseEvent event)
        {
            /* do nothing */
        }

        /**
         * {@inheritDoc}
         */
        public void mouseExited(MouseEvent event)
        {
            /* do nothing */
        }

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
            {
                desktopSharingClient.sendKeyboardEvent(callPeer, event);
            }
        }

        /**
         * This method is called when remote control has been granted.
         *
         * @param event <tt>RemoteControlGrantedEvent</tt>
         */
        public void remoteControlGranted(RemoteControlGrantedEvent event)
        {
            allowRemoteControl = true;

            if(remoteVideo != null)
            {
                addMouseAndKeyListeners();
            }
        }

        /**
         * This method is called when remote control has been revoked.
         *
         * @param event <tt>RemoteControlRevokedEvent</tt>
         */
        public void remoteControlRevoked(RemoteControlRevokedEvent event)
        {
            if(allowRemoteControl)
            {
                allowRemoteControl = false;
                removeMouseAndKeyListeners();
            }
        }
    }

    /**
     * The Mouse listener for local video. It is responsible for dragging local
     * video.
     */
//    private class LocalVideoMouseListener
//        implements  MouseListener,
//                    MouseMotionListener
//    {
//        /**
//         * Indicates if we're currently during a drag operation.
//         */
//        private boolean inDrag = false;
//
//        /**
//         * The previous x coordinate of the drag.
//         */
//        private int previousX = 0;
//
//        /**
//         * The previous y coordinate of the drag.
//         */
//        private int previousY = 0;
//
//        /**
//         * Indicates that the mouse has been dragged.
//         *
//         * @param event the <tt>MouseEvent</tt> that notified us
//         */
//        public void mouseDragged(MouseEvent event)
//        {
//            Point p = event.getPoint();
//
//            if (inDrag)
//            {
//                Component c = (Component) event.getSource();
//
//                int newX = c.getX() + p.x - previousX;
//                int newY = c.getY() + p.y - previousY;
//
//                Component remoteContainer;
//                if (remoteVideo != null)
//                    remoteContainer = remoteVideo;
//                else
//                    synchronized (videoContainers)
//                    {
//                        // If there's no remote video we limit the drag to the
//                        // parent video container.
//                        if (videoContainers != null
//                                && videoContainers.size() > 0)
//                            remoteContainer = videoContainers.get(
//                                videoContainers.size() - 1);
//                        else
//                            // In the case no video container is available
//                            // we just limit the drag to this panel.
//                            remoteContainer = OneToOneCallPeerPanel.this;
//                    }
//
//                int minX = remoteContainer.getX();
//                int maxX = remoteContainer.getX() + remoteContainer.getWidth();
//                int minY = remoteContainer.getY();
//                int maxY = remoteContainer.getY() + remoteContainer.getHeight();
//
//                if (newX < minX)
//                    newX = minX;
//
//                if (newX + c.getWidth() > maxX)
//                    newX = maxX - c.getWidth();
//
//                if (newY < minY)
//                    newY = minY;
//
//                if (newY + c.getHeight() > maxY)
//                    newY = maxY - c.getHeight();
//
//                c.setLocation(newX, newY);
//                if (closeButton.isVisible())
//                    closeButton.setVisible(false);
//            }
//        }
//
//        public void mouseMoved(MouseEvent event) {}
//
//        public void mouseClicked(MouseEvent event) {}
//
//        public void mouseEntered(MouseEvent event) {}
//
//        public void mouseExited(MouseEvent event) {}
//
//        /**
//         * Indicates that the mouse has been pressed.
//         *
//         * @param event the <tt>MouseEvent</tt> that notified us
//         */
//        public void mousePressed(MouseEvent event)
//        {
//            Point p = event.getPoint();
//
//            previousX = p.x;
//            previousY = p.y;
//            inDrag = true;
//        }
//
//        /**
//         * Indicates that the mouse has been released.
//         *
//         * @param event the <tt>MouseEvent</tt> that notified us
//         */
//        public void mouseReleased(MouseEvent event)
//        {
//            inDrag = false;
//            previousX = 0;
//            previousY = 0;
//
//            if (!closeButton.isVisible())
//            {
//                Component c = (Component) event.getSource();
//                closeButton.setLocation(
//                    c.getX() + c.getWidth() - closeButton.getWidth() - 3,
//                    c.getY() + 3);
//                closeButton.setVisible(true);
//            }
//        }
//    }

//    private class CloseButton
//        extends Label
//        implements MouseListener
//    {
//        private static final long serialVersionUID = 0L;
//
//        Image image = ImageLoader.getImage(ImageLoader.CLOSE_VIDEO);
//
//        public CloseButton()
//        {
//            int buttonWidth = image.getWidth(this) + 5;
//            int buttonHeight = image.getHeight(this) + 5;
//
//            setPreferredSize(new Dimension(buttonWidth, buttonHeight));
//            setSize(new Dimension(buttonWidth, buttonHeight));
//
//            this.addMouseListener(this);
//        }
//
//        public void paint(Graphics g)
//        {
//            g.setColor(Color.GRAY);
//            g.fillRect(0, 0, getWidth(), getHeight());
//            g.drawImage(image,
//                getWidth()/2 - image.getWidth(this)/2,
//                getHeight()/2 - image.getHeight(this)/2, this);
//        }
//
//        public void mouseClicked(MouseEvent event)
//        {
//            setLocalVideoVisible(false);
//
//            callRenderer.getCallContainer()
//                .setShowHideVideoButtonSelected(false);
//        }
//
//        public void mouseEntered(MouseEvent event) {}
//
//        public void mouseExited(MouseEvent event) {}
//
//        public void mousePressed(MouseEvent event) {}
//
//        public void mouseReleased(MouseEvent event) {}
//    }

    /**
     * Reloads all icons.
     */
    public void loadSkin()
    {
        if(localLevel != null)
            localLevel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MICROPHONE)));

        if(remoteLevel != null)
            remoteLevel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HEADPHONE)));

        if(muteStatusLabel.getIcon() != null)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));

        if(holdStatusLabel.getIcon() != null)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));

        securityStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(securityImageID)));

        if(this.peerImage == null)
        {
            synchronized (videoContainers)
            {
                for (JLabel photoLabel : photoLabels)
                    photoLabel.setIcon(new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)));
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
        videoHandler.setLocalVideoVisible(isVisible);
//        synchronized (videoContainers)
//        {
//            this.localVideoVisible = isVisible;
//
//            if (isVisible
//                != callRenderer.getCallContainer()
//                    .isShowHideVideoButtonSelected())
//            {
//                callRenderer.getCallContainer()
//                    .setShowHideVideoButtonSelected(isVisible);
//            }
//
//            int videoContainerCount;
//
//            if ((videoTelephony != null)
//                    && ((videoContainerCount = videoContainers.size()) > 0))
//            {
//                Container videoContainer
//                    = videoContainers.get(videoContainerCount - 1);
//
//                if (localVideo != null)
//                {
//                    if (isVisible)
//                    {
//                        Container localVideoParent = localVideo.getParent();
//
//                        if (localVideoParent != null)
//                            localVideoParent.remove(localVideo);
//
//                        Container closeButtonParent
//                            = closeButton.getParent();
//
//                        if (closeButtonParent != null)
//                            closeButtonParent.remove(closeButton);
//
//                        videoContainer.add(localVideo, VideoLayout.LOCAL, 0);
//                        videoContainer.add(
//                                closeButton,
//                                VideoLayout.CLOSE_LOCAL_BUTTON,
//                                0);
//                    }
//                    else
//                    {
//                        videoContainer.remove(localVideo);
//                        videoContainer.remove(closeButton);
//                    }
//
//                    /*
//                     * Just like #handleVideoEvent(VideoEvent, Container) says,
//                     * we have to be explicit in order to achieve a proper
//                     * layout and an up-to-date painting.
//                     */
//                    videoContainer.validate();
//                    videoContainer.repaint();
//                }
//            }
//        }
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

    /**
     * Returns the parent call renderer.
     *
     * @return the parent call renderer
     */
    public CallRenderer getCallRenderer()
    {
        return callRenderer;
    }

    /**
     * Returns the component associated with this renderer.
     *
     * @return the component associated with this renderer
     */
    public Component getComponent()
    {
        return this;
    }

    /**
     * Returns the video handler associated with this call peer renderer.
     *
     * @return the video handler associated with this call peer renderer
     */
    public UIVideoHandler getVideoHandler()
    {
        return videoHandler;
    }
}
