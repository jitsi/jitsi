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
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.service.gui.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.call.CallPeerAdapter;
import net.java.sip.communicator.util.skin.*;

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
    implements SwingCallPeerRenderer,
               PropertyChangeListener,
               Skinnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>OneToOneCallPeerPanel</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OneToOneCallPeerPanel.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Property to disable showing the dummy picture for the peer when no
     * streams present.
     */
    public static final String HIDE_PLACEHOLDER_PIC_PROP
        = "net.java.sip.communicator.impl.gui.main.call.HIDE_PLACEHOLDER_PIC";

    /**
     * The <tt>CallPeer</tt>, which is rendered in this panel.
     */
    private final CallPeer callPeer;

    /**
     * The <tt>Call</tt>, which is rendered in this panel.
     */
    private final Call call;

    /**
     * The <tt>CallPeerAdapter</tt> which implements common
     * <tt>CallPeer</tt>-related listeners on behalf of this instance.
     */
    private final CallPeerAdapter callPeerAdapter;

    /**
     * The renderer of the call.
     */
    private final SwingCallRenderer callRenderer;

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
     * A listener to desktop sharing granted/revoked events and to mouse and
     * keyboard interaction with the remote video displaying the remote desktop.
     */
    private final DesktopSharingMouseAndKeyboardListener
        desktopSharingMouseAndKeyboardListener;

    /**
     * The indicator which determines whether {@link #dispose()} has already
     * been invoked on this instance. If <tt>true</tt>, this instance is
     * considered non-functional and is to be left to the garbage collector.
     */
    private boolean disposed = false;

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
     * The listener that will listen when retrieving contact details.
     */
    private DisplayNameAndImageChangeListener detailsChangeListener = null;

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
                /*
                 * We receive events/notifications from various threads and we
                 * respond to them in the AWT event dispatching thread. It is
                 * possible to first schedule an event to be brought to the AWT
                 * event dispatching thread, then to have #dispose() invoked on
                 * this instance and, finally, to receive the scheduled event in
                 * the AWT event dispatching thread. In such a case, this
                 * disposed instance should not respond to the event.
                 */
                if (!disposed)
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
            SwingCallRenderer callRenderer,
            CallPeer callPeer,
            UIVideoHandler2 uiVideoHandler)
    {
        this.callRenderer = callRenderer;
        this.callPeer = callPeer;
        // we need to obtain call as soon as possible
        // cause if it fails too quickly we may fail to show it
        this.call = callPeer.getCall();
        this.uiVideoHandler = uiVideoHandler;

        detailsChangeListener = new DisplayNameAndImageChangeListener();
        peerName = CallManager.getPeerDisplayName(callPeer,
            detailsChangeListener);
        securityPanel = SecurityPanel.create(this, callPeer, null);

        ImageIcon icon = getPhotoLabelIcon();
        if(icon != null)
        {
            photoLabel = new JLabel(icon);
        }
        else
        {
            photoLabel = new JLabel();
        }
        center = createCenter();
        statusBar = createStatusBar();

        setPeerImage(CallManager.getPeerImage(callPeer));

        /* Lay out the main Components of the UI. */
        setLayout(new GridBagLayout());

        GridBagConstraints cnstrnts = new GridBagConstraints();

        if (center != null)
        {
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

        /*
         * This view adapts to whether it is displayed in full-screen or
         * windowed mode.
         */
        if (callRenderer instanceof Component)
        {
            ((Component) callRenderer).addPropertyChangeListener(
                    CallContainer.PROP_FULL_SCREEN,
                    this);
        }

        OperationSetDesktopSharingClient desktopSharingClient
            = callPeer.getProtocolProvider().getOperationSet(
                    OperationSetDesktopSharingClient.class);
        if (desktopSharingClient != null)
        {
            desktopSharingMouseAndKeyboardListener
                = new DesktopSharingMouseAndKeyboardListener(
                        callPeer,
                        desktopSharingClient);
        }
        else
            desktopSharingMouseAndKeyboardListener = null;

        updateViewFromModel();
    }

    /**
     * Set size of the {@link #photoLabel}
     */
    private void sizePhotoLabel()
    {
        if(photoLabel.getIcon() != null)
        {
            photoLabel.setPreferredSize(new Dimension(90, 90));
        }
        else
        {
            photoLabel.setPreferredSize(new Dimension(0, 0));
        }
    }

    /**
     * Creates the <tt>Component</tt> hierarchy of the central area of this
     * <tt>CallPeerPanel</tt> which displays the photo of the <tt>CallPeer</tt>
     * or the video if any.
     */
    private VideoContainer createCenter()
    {
        sizePhotoLabel();

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

        localLevel
            = new InputVolumeControlButton(
                    call.getConference(),
                    ImageLoader.MICROPHONE,
                    ImageLoader.MUTE_BUTTON,
                    false,
                    false);
        remoteLevel
            = new OutputVolumeControlButton(
                    call.getConference(),
                    ImageLoader.HEADPHONE,
                    false,
                    false)
                .getComponent();

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

        if(!GuiActivator.getConfigurationService().getBoolean(
                "net.java.sip.communicator.impl.gui.main.call."
                    + "DISABLE_SOUND_LEVEL_INDICATORS",
                false))
        {
            callPeer.addStreamSoundLevelListener(
                    new SoundLevelListener()
                    {
                        public void soundLevelChanged(Object source, int level)
                        {
                            remoteLevelIndicator.updateSoundLevel(level);
                        }
                    });
            /*
             * By the time the UI gets to be initialized, the callPeer may have
             * been removed from its Call. As far as the UI is concerned, the
             * callPeer will never have a Call again and there will be no audio
             * levels to display anyway so there is no point in throwing a
             * NullPointerException here.
             */
            if (call != null)
            {
                call.addLocalUserSoundLevelListener(
                        new SoundLevelListener()
                        {
                            public void soundLevelChanged(
                                    Object source,
                                    int level)
                            {
                                localLevelIndicator.updateSoundLevel(level);
                            }
                        });
            }
        }
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
        disposed = true;

        if(detailsChangeListener != null)
            detailsChangeListener.setInterested(false);

        callPeerAdapter.dispose();
        uiVideoHandler.deleteObserver(uiVideoHandlerObserver);

        if (callRenderer instanceof Component)
        {
            ((Component) callRenderer).removePropertyChangeListener(
                    CallContainer.PROP_FULL_SCREEN,
                    this);
        }
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
     * Tests a provided boolean property name, returning false if it should be
     * hidden.
     *
     * @param componentHidePropertyName the name of the boolean property to
     *        check.
     * @return false if the component should be hidden, true otherwise.
     *
     */
    private boolean isComponentEnabled(String componentHidePropertyName)
    {
        return !GuiActivator.getConfigurationService().getBoolean(
            componentHidePropertyName,
            false);
    }

    /**
     * Gets the <tt>Icon</tt> to be displayed in {@link #photoLabel}.
     *
     * @return the <tt>Icon</tt> to be displayed in {@link #photoLabel}
     */
    private ImageIcon getPhotoLabelIcon()
    {
        return
            (peerImage == null && isComponentEnabled(HIDE_PLACEHOLDER_PIC_PROP))
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

        if (securityEvent instanceof CallPeerSecurityOnEvent)
            securityOn((CallPeerSecurityOnEvent) securityEvent);
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
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Only show the security details if the security is on.
                SrtpControl ctrl = securityPanel.getSecurityControl();
                if (ctrl instanceof ZrtpControl
                    && ctrl.getSecureCommunicationStatus())
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
            if(isComponentEnabled(HIDE_PLACEHOLDER_PIC_PROP))
            {
                photoLabel.setIcon(
                        new ImageIcon(
                                ImageLoader.getImage(
                                        ImageLoader.DEFAULT_USER_PHOTO)));
            }
            else
            {
                photoLabel.setIcon(null);
            }
            sizePhotoLabel();
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
     * Notifies this instance about a change in the value of a property of a
     * source which of interest to this instance. For example,
     * <tt>OneToOneCallPeerPanel</tt> updates its user interface-related
     * properties upon changes in the value of the
     * {@link CallContainer#PROP_FULL_SCREEN} property of its associated
     * {@link #callRenderer}.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which identifies the source, the
     * name of the property and the old and new values
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        if (CallContainer.PROP_FULL_SCREEN.equals(ev.getPropertyName()))
            updateViewFromModel();
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
     *
     * @param evt the <tt>CallPeerSecurityOffEvent</tt> that notified us
     */
    public void securityOff(final CallPeerSecurityOffEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    securityOff(evt);
                }
            });
            return;
        }

        if (evt.getSessionType() == CallPeerSecurityOffEvent.AUDIO_SESSION)
        {
            securityStatusLabel.setText("");
            securityStatusLabel.setSecurityOff();
            if (securityStatusLabel.getBorder() == null)
                securityStatusLabel.setBorder(
                    BorderFactory.createEmptyBorder(2, 5, 2, 3));
        }

        securityPanel.securityOff(evt);
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(final CallPeerSecurityOnEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    securityOn(evt);
                }
            });
            return;
        }

        // If the securityOn is called without a specific event, we'll just set
        // the security label status to on.
        if (evt == null)
        {
            securityStatusLabel.setSecurityOn();
            return;
        }

        SrtpControl srtpControl = evt.getSecurityController();

        if (!srtpControl.requiresSecureSignalingTransport()
                || callPeer.getProtocolProvider().isSignalingTransportSecure())
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
    public void securityTimeout(final CallPeerSecurityTimeoutEvent evt)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    securityTimeout(evt);
                }
            });
            return;
        }

        if (securityPanel != null)
            securityPanel.securityTimeout(evt);
    }

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    public void setErrorReason(final String reason)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setErrorReason(reason);
                }
            });
            return;
        }

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
    public void setMute(final boolean isMute)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setMute(isMute);
                }
            });
            return;
        }

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

        // Update input volume control button state to reflect the current
        // mute status.
        if (localLevel.isSelected() != isMute)
            localLevel.setSelected(isMute);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(final boolean isOnHold)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setOnHold(isOnHold);
                }
            });
            return;
        }

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
        // If the image is still null we try to obtain it from one of the
        // available contact sources.
        if (image == null || image.length <= 0)
        {
            // will do nothing, as querying for display name will also
            // set and image if it exist
        }
        else
        {
            peerImage = ImageUtils.getScaledRoundedIcon(image, 100, 100);
            if (peerImage == null)
                peerImage = getPhotoLabelIcon();

            if(!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        photoLabel.setIcon(peerImage);
                        sizePhotoLabel();
                        photoLabel.repaint();
                    }
                });
            }
            else
            {
                photoLabel.setIcon(peerImage);
                sizePhotoLabel();
                photoLabel.repaint();
            }
        }
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(final String name)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setPeerName(name);
                }
            });
            return;
        }

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
    public void setPeerState(final CallPeerState oldState,
                             final CallPeerState newState,
                             final String stateString)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setPeerState(oldState, newState, stateString);
                }
            });
            return;
        }

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
    public void setSecurityPanelVisible(final boolean isVisible)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSecurityPanelVisible(isVisible);
                }
            });
            return;
        }

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
                @Override
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
        /*
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event
         * because it may, for example, steal a visual Components depicting
         * video (which cannot belong to more than one parent at a time) from
         * another non-disposed OneToOneCallPeerPanel.
         */
        if (!disposed)
        {
            if (SwingUtilities.isEventDispatchThread())
                updateViewFromModelInEventDispatchThread();
            else
            {
                SwingUtilities.invokeLater(
                        updateViewFromModelInEventDispatchThread);
            }
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
         * We receive events/notifications from various threads and we respond
         * to them in the AWT event dispatching thread. It is possible to first
         * schedule an event to be brought to the AWT event dispatching thread,
         * then to have #dispose() invoked on this instance and, finally, to
         * receive the scheduled event in the AWT event dispatching thread. In
         * such a case, this disposed instance should not respond to the event
         * because it may, for example, steal a visual Components depicting
         * video (which cannot belong to more than one parent at a time) from
         * another non-disposed OneToOneCallPeerPanel.
         */
        if (disposed)
            return;

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
                    logger.warn(
                            "Failed to retrieve local video to be displayed.",
                            ofe);
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

            // If the remote video has changed, maybe the CallPanel can display
            // the LO/SD/HD button.
            if(remoteVideoChanged)
            {
                // Updates video component which may listen the mouse and key
                // events.
                if (desktopSharingMouseAndKeyboardListener != null)
                {
                    desktopSharingMouseAndKeyboardListener.setVideoComponent(
                            remoteVideo);
                }

                CallPanel callPanel = callRenderer.getCallContainer();
                // The remote video has been added, then tries to display the
                // LO/SD/HD button.
                if(remoteVideo != null)
                {
                    callPanel.addRemoteVideoSpecificComponents(callPeer);
                }
                // The remote video has been removed, then hide the LO/SD/HD
                // button if it is currently displayed.
                else
                {
                    callPanel.removeRemoteVideoSpecificComponents();
                }
            }

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
                    this.localVideo = null;
                    this.remoteVideo = null;

                    /*
                     * AWT does not make a guarantee about the Z order even
                     * within an operating system i.e. the order of adding the
                     * Components to their Container does not mean that they
                     * will be determinedly painted in that or reverse order.
                     * Anyway, there appears to be an expectation among the
                     * developers less acquainted with AWT that AWT paints the
                     * Components of a Container in an order that is the reverse
                     * of the order of their adding. In order to satisfy that
                     * expectation and thus give at least some idea to the
                     * developers reading the code bellow, do add the Components
                     * according to that expectation.
                     */

                    if (localVideo != null)
                    {
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

                        center.add(localVideo, VideoLayout.LOCAL, -1);
                        this.localVideo = localVideo;
                    }

                    if (remoteVideo != null)
                    {
                        center.add(remoteVideo, VideoLayout.CENTER_REMOTE, -1);
                        this.remoteVideo = remoteVideo;
                    }
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
                g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Listens for display name update and image update, some searches for
     * display name are slow, so we add a listener to update them when
     * result comes in.
     */
    private class DisplayNameAndImageChangeListener
        implements CallManager.DetailsResolveListener
    {
        /**
         * By default we are interested in events.
         */
        private boolean interested = true;

        @Override
        public void displayNameUpdated(String displayName)
        {
            setPeerName(displayName);
        }

        @Override
        public void imageUpdated(byte[] image)
        {
            setPeerImage(image);
        }

        /**
         * Are we interested.
         * @return
         */
        @Override
        public boolean isInterested()
        {
            return interested;
        }

        /**
         * Changes the interested value.
         * @param value
         */
        public void setInterested(boolean value)
        {
            this.interested = value;
        }
    }
}
