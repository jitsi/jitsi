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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

import com.explodingpixels.macwidgets.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It shows
 * information about call peers, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class OneToOneCallPanel
    extends TransparentPanel
    implements SwingCallRenderer,
               PropertyChangeListener
{
    /**
     * Logger for the OneToOneCallPanel.
     */
    private static final Logger logger
        = Logger.getLogger(OneToOneCallPanel.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The underlying <tt>Call</tt>, this panel is representing.
     */
    private final Call call;

    /**
     * The parent call container.
     */
    private final CallPanel callContainer;

    /**
     * The underlying <tt>CallPeer</tt>.
     */
    private final CallPeer callPeer;

    /**
     * The check box allowing to turn on remote control when in a desktop
     * sharing session.
     */
    private JCheckBox enableDesktopRemoteControl;

    /**
     * The component showing the name of the underlying call peer.
     */
    private final JLabel nameLabel = new JLabel("", JLabel.CENTER);

    /**
     * The panel representing the underlying <tt>CallPeer</tt>.
     */
    private OneToOneCallPeerPanel peerPanel;

    /**
     * The panel added on the south of this container.
     */
    private JPanel southPanel;

    /**
     * The <tt>Component</tt> which is displayed at the top of this view and
     * contains {@link #nameLabel}. It is visible when this view is displayed in
     * windowed mode, it is not visible in full-screen mode.
     */
    private JComponent topBar;

    /**
     * Initializes a new <tt>OneToOneCallPanel</tt> which is to depict a
     * one-to-one audio and/or video conversation of the local peer/user with a
     * specific remote <tt>CallPeer</tt> and which is to be used by a specific
     * <tt>CallPanel</tt> for that purpose.
     *
     * @param callContainer the <tt>CallPanel</tt> which requested the
     * initialization of the new instance and which is to use it to depict the
     * one-to-one audio and/or video conversation of the local peer/user with
     * the specified <tt>callPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> whose one-to-one audio and/or video
     * conversation with the local peer/user is to be depicted by the new
     * instance
     * @param uiVideoHandler the utility which is to aid the new instance in
     * dealing with the video-related information
     */
    public OneToOneCallPanel(
            CallPanel callContainer,
            CallPeer callPeer,
            UIVideoHandler2 uiVideoHandler)
    {
        super(new BorderLayout());

        this.callContainer = callContainer;
        this.callPeer = callPeer;

        call = this.callPeer.getCall();

        addCallPeerPanel(callPeer, uiVideoHandler);

        int preferredHeight = 400;
        if(GuiActivator.getConfigurationService().getBoolean(
                OneToOneCallPeerPanel.HIDE_PLACEHOLDER_PIC_PROP,
                false))
        {
            preferredHeight = 128;
        }
        setPreferredSize(new Dimension(400, preferredHeight));
        setTransferHandler(new CallTransferHandler(call));

        this.callContainer.addPropertyChangeListener(
                CallContainer.PROP_FULL_SCREEN,
                this);
    }

    /**
     * Initializes and adds a new <tt>OneToOneCallPeerPanel</tt> instance which
     * is to depict a specific <tt>CallPeer</tt> on behalf of this instance.
     *
     * @param peer the <tt>CallPeer</tt> to be depicted by the new
     * <tt>OneToOneCallPeerPanel</tt> instance
     * @param uiVideoHandler the facility to aid the new instance in dealing
     * with the video-related information
     */
    private void addCallPeerPanel(CallPeer peer, UIVideoHandler2 uiVideoHandler)
    {
        if (peerPanel == null)
        {
//            videoHandler.addVideoListener(callPeer);
//            videoHandler.addRemoteControlListener(callPeer);

            peerPanel = new OneToOneCallPeerPanel(this, peer, uiVideoHandler);

            /* Create the main Components of the UI. */
            // use already obtained peer name to avoid double querying and
            // checking may result and
            // network search (ldap, various contact sources)
            nameLabel.setText(
                getPeerDisplayText(peer, peerPanel.getPeerName()));

            topBar = createTopComponent();
            topBar.setVisible(!isFullScreen());
            topBar.add(nameLabel);
            add(topBar, BorderLayout.NORTH);

            add(peerPanel);
        }
    }

    /**
     * Adds all desktop sharing related components to this container.
     */
    public void addDesktopSharingComponents()
    {
        OperationSetDesktopSharingServer opSetDesktopSharingServer
            = callPeer.getProtocolProvider().getOperationSet(
                OperationSetDesktopSharingServer.class);
        if(opSetDesktopSharingServer != null
                && opSetDesktopSharingServer.isRemoteControlAvailable(callPeer))
        {
            if (logger.isTraceEnabled())
                logger.trace("Add desktop sharing related components.");

            if (enableDesktopRemoteControl == null)
            {
                enableDesktopRemoteControl = new JCheckBox(
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ENABLE_DESKTOP_REMOTE_CONTROL"));

                southPanel = new TransparentPanel(
                    new FlowLayout(FlowLayout.CENTER));

                southPanel.add(enableDesktopRemoteControl);

                enableDesktopRemoteControl.setAlignmentX(CENTER_ALIGNMENT);
                enableDesktopRemoteControl.setOpaque(false);

                enableDesktopRemoteControl.addItemListener(new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        CallManager.enableDesktopRemoteControl(
                            callPeer, e.getStateChange() == ItemEvent.SELECTED);
                    }
                });
            }

            if (OSUtils.IS_MAC)
            {
                southPanel.setOpaque(true);
                southPanel.setBackground(
                        new Color(
                                GuiActivator.getResources().getColor(
                                        "service.gui.MAC_PANEL_BACKGROUND")));
            }

            add(southPanel, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JComponent createTopComponent()
    {
        JComponent topComponent = null;

        if (OSUtils.IS_MAC)
        {
            /*
             * The topBar is not visible in full-screen mode so
             * macPanelBackground does not interfere with the background set on
             * the ancestors in full-screen mode.
             */
            Color macPanelBackground
                = new Color(
                        GuiActivator.getResources().getColor(
                                "service.gui.MAC_PANEL_BACKGROUND"));

            if (callContainer.getCallWindow() instanceof Window)
            {
                UnifiedToolBar macToolbarPanel = new UnifiedToolBar();

                MacUtils.makeWindowLeopardStyle(
                        callContainer.getCallWindow().getFrame().getRootPane());

                macToolbarPanel.getComponent().setLayout(new BorderLayout());
                macToolbarPanel.disableBackgroundPainter();
                macToolbarPanel.installWindowDraggerOnWindow(
                        callContainer.getCallWindow().getFrame());

                topComponent = macToolbarPanel.getComponent();
            }
            else
            {
                topComponent = new TransparentPanel(new BorderLayout());
                topComponent.setOpaque(true);
                topComponent.setBackground(macPanelBackground);
            }

            /*
             * Set the background color of the center panel. However, that color
             * depends on whether this view is displayed on full-screen or
             * windowed mode (because it is common for full-screen mode to have
             * a black background).
             */
            peerPanel.setOpaque(!isFullScreen());
            peerPanel.setBackground(macPanelBackground);
        }
        else
        {
            JPanel panel = new TransparentPanel(new BorderLayout());

            panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            topComponent = panel;
        }

        return topComponent;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        callContainer.removePropertyChangeListener(
                CallContainer.PROP_FULL_SCREEN,
                this);

        if (peerPanel != null)
            peerPanel.dispose();
    }

    /**
     * Returns the parent call container, where this renderer is contained.
     * @return the parent call container, where this renderer is contained
     */
    public CallPanel getCallContainer()
    {
        return callContainer;
    }

    /**
     * Gets the <tt>CallPeer</tt> depicted by this instance.
     *
     * @return the <tt>CallPeer</tt> depicted by this instance
     */
    public CallPeer getCallPeer()
    {
        return callPeer;
    }

    /**
     * Returns the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>.
     * @param callPeer the <tt>CallPeer</tt>, for which we're looking for a
     * renderer
     * @return the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>
     */
    public SwingCallPeerRenderer getCallPeerRenderer(CallPeer callPeer)
    {
        return this.callPeer.equals(callPeer) ? peerPanel : null;
    }

    /**
     * A informative text to show for the peer.
     * @param peer the peer.
     * @return the text contain address and display name.
     */
    private String getPeerDisplayText(CallPeer peer, String displayName)
    {
        String peerAddress = peer.getAddress();

        if(StringUtils.isNullOrEmpty(displayName, true))
            return peerAddress;

        if(!displayName.equalsIgnoreCase(peerAddress))
            return displayName + " (" + peerAddress + ")";

        return displayName;
    }

    /**
     * Determines whether this view is displayed in full-screen or windowed
     * mode.
     *
     * @return <tt>true</tt> if this view is displayed in full-screen mode or
     * <tt>false</tt> for windowed mode
     */
    boolean isFullScreen()
    {
        return callContainer.isFullScreen();
    }

    /**
     * Notifies this instance about a change in the value of a property of a
     * source which of interest to this instance. For example,
     * <tt>OneToOneCallPanel</tt> updates its user interface-related properties
     * upon changes in the value of the {@link CallContainer#PROP_FULL_SCREEN}
     * property of its associated {@link #callContainer}.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which identifies the source, the
     * name of the property and the old and new values
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (CallContainer.PROP_FULL_SCREEN.equals(propertyName)
                && callContainer.equals(ev.getSource()))
        {
            try
            {
                /*
                 * Apply UI-related to Components which are explicitly owned by
                 * this view or which this view tempers with.
                 */
                boolean fullScreen = isFullScreen();

                if (topBar != null)
                    topBar.setVisible(!fullScreen);
                if (OSUtils.IS_MAC && (peerPanel != null))
                    peerPanel.setOpaque(!fullScreen);
            }
            finally
            {
                /*
                 * Fire the event as originating from this instance in order to
                 * allow listeners to register with a source which is more
                 * similar to them with respect to life span.
                 */
                firePropertyChange(
                        propertyName,
                        ev.getOldValue(), ev.getNewValue());
            }
        }
    }

    /**
     * Removes all desktop sharing related components from this container.
     */
    public void removeDesktopSharingComponents()
    {
        if (southPanel != null)
        {
            remove(southPanel);
            enableDesktopRemoteControl.setSelected(false);
        }

        revalidate();
        repaint();
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        nameLabel.setText(getPeerDisplayText(callPeer, name));
    }

    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        callContainer.startCallTimer();
    }

    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        callContainer.stopCallTimer();
    }

    /**
     * Returns <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>.
     *
     * @return <code>true</code> if the call timer has been started, otherwise
     * returns <code>false</code>
     */
    public boolean isCallTimerStarted()
    {
        return callContainer.isCallTimerStarted();
    }

    /**
     * Updates the state of the general hold button. The hold button is selected
     * only if all call peers are locally or mutually on hold at the same time.
     * In all other cases the hold button is unselected.
     */
    public void updateHoldButtonState()
    {
        callContainer.updateHoldButtonState();
    }
}
