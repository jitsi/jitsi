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
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.util.swing.TransparentPanel;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.util.swing.*;

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
    implements CallPeerRenderer,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>CallPeer</tt>, which is rendered in this panel.
     */
    private final CallPeer callPeer;

    /**
     * The <tt>CallPeerAdapter</tt> which implements common
     * <tt>CallPeer</tt>-related listeners on behalf of this instance.
     */
    private final CallPeerAdapter callPeerAdapter;

    /**
     * The renderer of the call.
     */
    private final CallRenderer callRenderer;

    /**
     * The component showing the status of the underlying call peer.
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The center component.
     */
    private final VideoContainer center;

    /**
     * The AWT <tt>Component</tt> which implements a button which allows
     * closing/hiding the visual <tt>Component</tt> which depicts the video
     * streaming from the local peer/user to the remote peer(s).
     */
    private Component closeLocalVisualComponentButton;

    /**
     * The DTMF label.
     */
    private final JLabel dtmfLabel = new JLabel();

    /**
     * The component responsible for displaying an error message.
     */
    private JTextComponent errorMessageComponent;

    /**
     * The label showing whether the call is on or off hold.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * Sound local level label.
     */
    private InputVolumeControlButton localLevel;

    /**
     * The <tt>Component</tt> which
     * {@link #updateViewFromModelInEventDispatchThread()} last added to
     * {@link #center} as the visual <tt>Component</tt> displaying the video
     * streaming from the local peer/user to the remote peer(s).
     * <p>
     * <b>Warning</b>: It is not to be used for any other purposes because it
     * may not represent the current state of the model of this view.
     * </p>
     */
    private Component localVideo;

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

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
     * The label containing the user photo.
     */
    private final JLabel photoLabel;

    /**
     * Sound remote level label.
     */
    private Component remoteLevel;

    /**
     * The <tt>Component</tt> which
     * {@link #updateViewFromModelInEventDispatchThread()} last added to
     * {@link #center} as the visual <tt>Component</tt> displaying the video
     * streaming from the remote peer(s) to the local peer/user.
     * <p>
     * <b>Warning</b>: It is not to be used for any other purposes because it
     * may not represent the current state of the model of this view.
     * </p>
     */
    private Component remoteVideo;

    /**
     * Current id for security image.
     */
    private ImageID securityImageID = ImageLoader.SECURE_BUTTON_OFF;

    /**
     * The panel containing security related components.
     */
    private SecurityPanel<?> securityPanel;

    /**
     * The security status of the peer
     */
    private final SecurityStatusLabel securityStatusLabel
        = new SecurityStatusLabel();

    /**
     * The status bar component.
     */
    private final Component statusBar;

    /**
     * The facility which aids this instance in the dealing with the
     * video-related information.
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * The <tt>Observer</tt> which listens to changes in the video-related
     * information detected and reported by {@link #uiVideoHandler}.
     */
    private final Observer uiVideoHandlerObserver
        = new Observer()
        {
            public void update(Observable o, Object arg)
            {
                updateViewFromModel();
            }
        };

    /**
     * The <tt>Runnable</tt> which is scheduled by
     * {@link #updateViewFromModel()} for execution in the AWT event dispatching
     * thread in order to invoke
     * {@link #updateViewFromModelInEventDispatchThread()}.
     */
    private final Runnable updateViewFromModelInEventDispatchThread
        = new Runnable()
        {
            public void run()
            {
                updateViewFromModelInEventDispatchThread();
            }
        };

    /**
     * Creates a <tt>CallPeerPanel</tt> for the given call peer.
     *
     * @param callRenderer the renderer of the call
     * @param callPeer the <tt>CallPeer</tt> represented in this panel
     * @param uiVideoHandler the facility which is to aid the new instance in
     * the dealing with the video-related information
     */
    public OneToOneCallPeerPanel(
            CallRenderer callRenderer,
            CallPeer callPeer,
            UIVideoHandler2 uiVideoHandler)
    {
        this.callRenderer = callRenderer;
        this.callPeer = callPeer;
        this.uiVideoHandler = uiVideoHandler;

        peerName = callPeer.getDisplayName();
        securityPanel = SecurityPanel.create(this, callPeer, null);

        photoLabel = new JLabel(getPhotoLabelIcon());
        center = createCenter();
        statusBar = createStatusBar();

        setPeerImage(CallManager.getPeerImage(callPeer));

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints cnstrnts = new GridBagConstraints();

        if (center != null)
        {
            /*
             * Don't let the center dictate the preferred size because it may
             * display large videos. Otherwise, the large video will make this
             * panel expand and then the panel's container will show scroll
             * bars.
             */
            center.setPreferredSize(new Dimension(1, 1));

            cnstrnts.fill = GridBagConstraints.BOTH;
            cnstrnts.gridx = 0;
            cnstrnts.gridy = 1;
            cnstrnts.weightx = 1;
            cnstrnts.weighty = 1;
            add(center, cnstrnts);
        }
        if (statusBar != null)
        {
            cnstrnts.fill = GridBagConstraints.NONE;
            cnstrnts.gridx = 0;
            cnstrnts.gridy = 3;
            cnstrnts.weightx = 0;
            cnstrnts.weighty = 0;
            cnstrnts.insets = new Insets(5, 0, 0, 0);
            add(statusBar, cnstrnts);
        }

        createSoundLevelIndicators();
        initSecuritySettings();

        /*
         * Add the listeners which will be notified about changes in the model
         * and which will update this view.
         */
        callPeerAdapter = new CallPeerAdapter(callPeer, this);
        uiVideoHandler.addObserver(uiVideoHandlerObserver);

        updateViewFromModel();
    }

    /**
     * Creates the <tt>Component</tt> hierarchy of the central area of this
     * <tt>CallPeerPanel</tt> which displays the photo of the <tt>CallPeer</tt>
     * or the video if any.
     */
    private VideoContainer createCenter()
    {
        photoLabel.setPreferredSize(new Dimension(90, 90));

        return createVideoContainer(photoLabel);
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
                ImageLoader.HEADPHONE, false, false).getComponent();

        final SoundLevelIndicator localLevelIndicator
            = new SoundLevelIndicator(
                    SoundLevelChangeEvent.MIN_LEVEL,
                    SoundLevelChangeEvent.MAX_LEVEL);
        final SoundLevelIndicator remoteLevelIndicator
            = new SoundLevelIndicator(
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
     * Creates the <tt>Component</tt> hierarchy of the area of
     * status-related information such as <tt>CallPeer</tt> display
     * name, call duration, security status.
     *
     * @return the root of the <tt>Component</tt> hierarchy of the area of
     *         status-related information such as <tt>CallPeer</tt>
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
        initSecurityStatusLabel();

        constraints.gridx++;
        statusPanel.add(holdStatusLabel, constraints);

        constraints.gridx++;
        statusPanel.add(muteStatusLabel, constraints);

        constraints.gridx++;
        callStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 3, 2, 12));
        statusPanel.add(callStatusLabel, constraints);

        constraints.gridx++;
        constraints.weightx = 1f;
        statusPanel.add(dtmfLabel, constraints);

        return statusPanel;
    }

    /**
     * Creates a new AWT <tt>Container</tt> which can display a single
     * <tt>Component</tt> at a time (supposedly, one which represents video)
     * and, in the absence of such a <tt>Component</tt>, displays a
     * predefined default <tt>Component</tt> (in accord with the previous
     * supposition, one which is the default when there is no video). The
     * returned <tt>Container</tt> will track the <tt>Components</tt>s
     * added to and removed from it in order to make sure that
     * <tt>noVideoContainer</tt> is displayed as described.
     *
     * @param noVideoComponent the predefined default <tt>Component</tt> to
     *            be displayed in the returned <tt>Container</tt> when there
     *            is no other <tt>Component</tt> in it
     * @return a new <tt>Container</tt> which can display a single
     *         <tt>Component</tt> at a time and, in the absence of such a
     *         <tt>Component</tt>, displays <tt>noVideoComponent</tt>
     */
    private VideoContainer createVideoContainer(Component noVideoComponent)
    {
        Container oldParent = noVideoComponent.getParent();

        if (oldParent != null)
            oldParent.remove(noVideoComponent);

        return new VideoContainer(noVideoComponent, false);
    }

    /**
     * Releases the resources acquired by this instance which require explicit
     * disposal (e.g. any listeners added to the depicted <tt>CallPeer</tt>.
     * Invoked by <tt>OneToOneCallPanel</tt> when it determines that this
     * <tt>OneToOneCallPeerPanel</tt> is no longer necessary. 
     */
    public void dispose()
    {
        callPeerAdapter.dispose();
        uiVideoHandler.deleteObserver(uiVideoHandlerObserver);
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
     * Returns the name of the peer, contained in this panel.
     *
     * @return the name of the peer, contained in this panel
     */
    public String getPeerName()
    {
        return peerName;
    }

    /**
     * Gets the <tt>Icon</tt> to be displayed in {@link #photoLabel}.
     *
     * @return the <tt>Icon</tt> to be displayed in {@link #photoLabel}
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
     * Initializes the security status label, shown in the call status bar.
     */
    private void initSecurityStatusLabel()
    {
        securityStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 5, 2, 5));

        securityStatusLabel.addMouseListener(new MouseAdapter()
        {
            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e)
            {
                CallPeerSecurityStatusEvent securityEvt
                    = callPeer.getCurrentSecuritySettings();

                // Only show the security details if the security is on.
                if (securityEvt instanceof CallPeerSecurityOnEvent
                    && ((CallPeerSecurityOnEvent) securityEvt)
                        .getSecurityController() instanceof ZrtpControl)
                {
                    setSecurityPanelVisible(!callRenderer.getCallContainer()
                        .getCallWindow().getFrame().getGlassPane().isVisible());
                }
            }
        });
    }

    /**
     * Determines whether the visual <tt>Component</tt> depicting the video
     * streaming from the local peer/user to the remote peer(s) is currently
     * visible.
     *
     * @return <tt>true</tt> if the visual <tt>Component</tt> depicting the
     * video streaming from the local peer/user to the remote peer(s) is
     * currently visible; otherwise, <tt>false</tt>
     */
    public boolean isLocalVideoVisible()
    {
        return uiVideoHandler.isLocalVideoVisible();
    }

    /**
     * Reloads all icons.
     */
    public void loadSkin()
    {
        if(localLevel != null)
            localLevel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MICROPHONE)));

        if(remoteLevel != null && remoteLevel instanceof Skinnable)
            ((Skinnable) remoteLevel).loadSkin();

        if(muteStatusLabel.getIcon() != null)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));

        if(holdStatusLabel.getIcon() != null)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));

        securityStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(securityImageID)));

        if(peerImage == null)
        {
            photoLabel.setIcon(
                    new ImageIcon(
                            ImageLoader.getImage(
                                    ImageLoader.DEFAULT_USER_PHOTO)));
        }
    }

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar)
    {
        dtmfLabel.setText(dtmfLabel.getText() + dtmfChar);
        if (dtmfLabel.getBorder() == null)
            dtmfLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 1, 2, 5));
    }

    /**
     * Re-dispatches glass pane mouse events only in case they occur on the
     * security panel.
     *
     * @param glassPane the glass pane
     * @param e the mouse event in question
     */
    private void redispatchMouseEvent(Component glassPane, MouseEvent e)
    {
        Point glassPanePoint = e.getPoint();

        Point securityPanelPoint = SwingUtilities.convertPoint(
                                        glassPane,
                                        glassPanePoint,
                                        securityPanel);

        Component component;
        Point componentPoint;

        if (securityPanelPoint.y > 0)
        {
            component = securityPanel;
            componentPoint = securityPanelPoint;
        }
        else
        {
            Container contentPane
                = callRenderer.getCallContainer().getCallWindow()
                                    .getFrame().getContentPane();

            Point containerPoint = SwingUtilities.convertPoint(
                                                    glassPane,
                                                    glassPanePoint,
                                                    contentPane);

            component = SwingUtilities.getDeepestComponentAt(contentPane,
                    containerPoint.x, containerPoint.y);

            componentPoint = SwingUtilities.convertPoint(contentPane,
                glassPanePoint, component);
        }

        if (component != null)
            component.dispatchEvent(new MouseEvent( component,
                                                    e.getID(),
                                                    e.getWhen(),
                                                    e.getModifiers(),
                                                    componentPoint.x,
                                                    componentPoint.y,
                                                    e.getClickCount(),
                                                    e.isPopupTrigger()));

        e.consume();
    }

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param evt the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent evt)
    {
        if(Boolean.parseBoolean(GuiActivator.getResources()
                .getSettingsString("impl.gui.PARANOIA_UI")))
        {
            SrtpControl srtpControl = null;
            if (callPeer instanceof MediaAwareCallPeer)
                srtpControl = evt.getSecurityController();

            securityPanel
                = new ParanoiaTimerSecurityPanel<SrtpControl>(srtpControl);

            setSecurityPanelVisible(true);
        }
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        securityStatusLabel.setText("");
        securityStatusLabel.setSecurityOff();
        if (securityStatusLabel.getBorder() == null)
            securityStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 5, 2, 3));

        securityPanel.securityOff(evt);
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
        // If the securityOn is called without a specific event, we'll just set
        // the security label status to on.
        if (evt == null)
        {
            securityStatusLabel.setSecurityOn();
            return;
        }

        SrtpControl srtpControl = evt.getSecurityController();

        if ((srtpControl.requiresSecureSignalingTransport()
            && callPeer.getProtocolProvider().isSignalingTransportSecure())
            || !srtpControl.requiresSecureSignalingTransport())
        {
            if (srtpControl instanceof ZrtpControl)
            {
                securityStatusLabel.setText("zrtp");

                if (!((ZrtpControl) srtpControl).isSecurityVerified())
                    securityStatusLabel.setSecurityPending();
                else
                    securityStatusLabel.setSecurityOn();
            }
            else
                securityStatusLabel.setSecurityOn();
        }

        // if we have some other panel, using other control
        if(!srtpControl.getClass().isInstance(
                securityPanel.getSecurityControl())
            || (securityPanel instanceof ParanoiaTimerSecurityPanel))
        {
            setSecurityPanelVisible(false);

            securityPanel
                = SecurityPanel.create(this, callPeer, srtpControl);

            if (srtpControl instanceof ZrtpControl)
                ((ZrtpSecurityPanel) securityPanel)
                    .setSecurityStatusLabel(securityStatusLabel);
        }

        securityPanel.securityOn(evt);

        boolean isSecurityLowPriority = Boolean.parseBoolean(
            GuiActivator.getResources().getSettingsString(
                "impl.gui.I_DONT_CARE_THAT_MUCH_ABOUT_SECURITY"));

        // Display ZRTP panel in case SAS was not verified or a AOR mismtach
        // was detected during creation of ZrtpSecurityPanel.
	    // Don't show panel if user does not care about security at all.
        if (srtpControl instanceof ZrtpControl
            && !isSecurityLowPriority
            && (!((ZrtpControl) srtpControl).isSecurityVerified() 
                || ((ZrtpSecurityPanel) securityPanel).isZidAorMismatch()))
        {
            setSecurityPanelVisible(true);
        }

        this.revalidate();
    }

    /**
     * Indicates that the security status is pending confirmation.
     */
    public void securityPending()
    {
        securityStatusLabel.setSecurityPending();
    }

    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
        if (securityPanel != null)
            securityPanel.securityTimeout(evt);
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
     * Shows/hides the visual <tt>Component</tt> depicting the video streaming
     * from the local peer/user to the remote peer(s).
     *
     * @param visible <tt>true</tt> to show the visual <tt>Component</tt>
     * depicting the video streaming from the local peer/user to the remote
     * peer(s); <tt>false</tt>, otherwise
     */
    public void setLocalVideoVisible(boolean visible)
    {
        uiVideoHandler.setLocalVideoVisible(visible);
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
        {
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
            muteStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 3, 2, 3));
        }
        else
        {
            muteStatusLabel.setIcon(null);
            muteStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

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
        {
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));
            holdStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(2, 3, 2, 3));
        }
        else
        {
            holdStatusLabel.setIcon(null);
            holdStatusLabel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        this.revalidate();
        this.repaint();
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
            GuiActivator.getContactList().setSourceContactImage(
                peerName, photoLabel, 100, 100);
        }
        else
        {
            peerImage = ImageUtils.getScaledRoundedIcon(image, 100, 100);
            if (peerImage == null)
                peerImage = getPhotoLabelIcon();

            photoLabel.setIcon(peerImage);
            photoLabel.repaint();
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
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param oldState the previous state of the peer
     * @param newState the new state of the peer
     * @param stateString the state of the contained call peer
     */
    public void setPeerState(   CallPeerState oldState,
                                CallPeerState newState,
                                String stateString)
    {
        this.callStatusLabel.setText(stateString);

        if (newState == CallPeerState.CONNECTED
            && !CallPeerState.isOnHold(oldState)
            && !securityStatusLabel.isSecurityStatusSet())
        {
            securityStatusLabel.setSecurityOff();
        }
    }

    /**
     * Shows/hides the security panel.
     *
     * @param isVisible <tt>true</tt> to show the security panel, <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean isVisible)
    {
        final JFrame callFrame = callRenderer.getCallContainer()
            .getCallWindow().getFrame();

        final JPanel glassPane = (JPanel) callFrame.getGlassPane();

        if (!isVisible)
        {
            // Need to hide the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(false);
            glassPane.setVisible(false);
            glassPane.removeAll();
        }
        else
        {
            glassPane.setLayout(null);
            glassPane.addMouseListener(new MouseListener()
            {
                public void mouseClicked(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseEntered(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseExited(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mousePressed(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseReleased(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }
            });

            Point securityLabelPoint = securityStatusLabel.getLocation();

            Point newPoint
                = SwingUtilities.convertPoint(securityStatusLabel.getParent(),
                securityLabelPoint.x, securityLabelPoint.y,
                callFrame);

            securityPanel.setBeginPoint(
                new Point((int) newPoint.getX() + 15, 0));
            securityPanel.setBounds(
                0, (int) newPoint.getY() - 5, this.getWidth(), 130);

            glassPane.add(securityPanel);
            // Need to show the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(true);
            glassPane.setVisible(true);

            glassPane.addComponentListener(new ComponentAdapter()
            {
                /**
                 * Invoked when the component's size changes.
                 */
                public void componentResized(ComponentEvent e)
                {
                    if (glassPane.isVisible())
                    {
                        glassPane.setVisible(false);
                        callFrame.removeComponentListener(this);
                    }
                }
            });
        }
    }

    /**
     * Updates this view i.e. <tt>OneToOneCallPeerPanel</tt> so that it depicts
     * the current state of its model i.e. <tt>callPeer</tt>.
     */
    private void updateViewFromModel()
    {
        if (SwingUtilities.isEventDispatchThread())
            updateViewFromModelInEventDispatchThread();
        else
        {
            SwingUtilities.invokeLater(
                    updateViewFromModelInEventDispatchThread);
        }
    }

    /**
     * Updates this view i.e. <tt>OneToOneCallPeerPanel</tt> so that it depicts
     * the current state of its model i.e. <tt>callPeer</tt>. The update is
     * performed in the AWT event dispatching thread.
     */
    private void updateViewFromModelInEventDispatchThread()
    {
        /*
         * Update the display of visual <tt>Component</tt>s depicting video
         * streaming between the local peer/user and the remote peer(s).
         */

        OperationSetVideoTelephony videoTelephony
            = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetVideoTelephony.class);
        Component remoteVideo = null;
        Component localVideo = null;

        if (videoTelephony != null)
        {
            List<Component> remoteVideos
                = videoTelephony.getVisualComponents(callPeer);

            if ((remoteVideos != null) && !remoteVideos.isEmpty())
            {
                /*
                 * TODO OneToOneCallPeerPanel displays a one-to-one conversation
                 * between the local peer/user and a specific remote peer. If
                 * the remote peer is the focus of a telephony conference of its
                 * own, it may be sending multiple videos to the local peer.
                 * Switching to a user interface which displays multiple videos
                 * is the responsibility of whoever decided that this
                 * OneToOneCallPeerPanel is to be used to depict the current
                 * state of the CallConference associated with the CallPeer
                 * depicted by this instance. If that switching decides that
                 * this instance is to continue being the user interface, then
                 * we should probably pick up the remote video which is
                 * generated by the remote peer and not one of its
                 * ConferenceMembers.  
                 */
                remoteVideo = remoteVideos.get(0);
            }

            if (uiVideoHandler.isLocalVideoVisible())
            {
                try
                {
                    localVideo
                        = videoTelephony.getLocalVisualComponent(callPeer);
                }
                catch (OperationFailedException ofe)
                {
                    /*
                     * Well, we cannot do much about the exception. We'll just
                     * not display the local video.
                     */
                }
            }

            /*
             * Determine whether there is actually a change in the local and
             * remote videos which requires an update.
             */
            boolean localVideoChanged
                = ((localVideo != this.localVideo)
                    || ((localVideo != null)
                        && !UIVideoHandler2.isAncestor(center, localVideo)));
            boolean remoteVideoChanged
                = ((remoteVideo != this.remoteVideo)
                    || ((remoteVideo != null)
                        && !UIVideoHandler2.isAncestor(center, remoteVideo)));

            if (localVideoChanged || remoteVideoChanged)
            {
                /*
                 * VideoContainer and JAWTRenderer cannot handle random
                 * additions of Components. Removing the localVideo when the
                 * user has requests its hiding though, should work without
                 * removing all Components from the VideoCotainer and adding
                 * them again.
                 */
                if (localVideoChanged
                        && !remoteVideoChanged
                        && (localVideo == null))
                {
                    if (this.localVideo != null)
                    {
                        center.remove(this.localVideo);
                        this.localVideo = null;

                        if (closeLocalVisualComponentButton != null)
                            center.remove(closeLocalVisualComponentButton);
                    }
                }
                else
                {
                    center.removeAll();
                    if (remoteVideo != null)
                        center.add(remoteVideo, VideoLayout.CENTER_REMOTE, -1);
                    this.remoteVideo = remoteVideo;

                    if (localVideo != null)
                    {
                        center.add(localVideo, VideoLayout.LOCAL, -1);

                        if (closeLocalVisualComponentButton == null)
                        {
                            closeLocalVisualComponentButton
                                = new CloseLocalVisualComponentButton(
                                        uiVideoHandler);
                        }
                        center.add(
                                closeLocalVisualComponentButton,
                                VideoLayout.CLOSE_LOCAL_BUTTON,
                                -1);
                    }
                    this.localVideo = localVideo;
                }
            }
        }
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
}
