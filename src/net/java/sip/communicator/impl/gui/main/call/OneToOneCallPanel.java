/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.swing.*;

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
    implements CallRenderer
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
     * The current <code>Window</code> being displayed in full-screen. Because
     * the AWT API with respect to the full-screen support doesn't seem
     * sophisticated enough, the field is used sparingly i.e. when there are no
     * other means (such as a local variable) of acquiring the instance.
     */
    private Window fullScreenWindow;

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

        setPreferredSize(new Dimension(400, 400));
        setTransferHandler(new CallTransferHandler(call));
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
            nameLabel.setText(getPeerDisplayText(peer, peer.getDisplayName()));

            JComponent topBar = createTopComponent();

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
                southPanel.setBackground(new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
            }

            add(southPanel, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
    }

    /**
     * Create the buttons bar for the fullscreen mode.
     *
     * @return the buttons bar <tt>Component</tt>
     */
    private JComponent createFullScreenButtonBar()
    {
        ShowHideVideoButton showHideButton
            = new ShowHideVideoButton(
                    null /* uiVideoHandler */,
                    true,
                    callContainer.isShowHideVideoButtonSelected());
        boolean isVideoButtonSelected = false;//callContainer.isVideoButtonSelected();

        showHideButton.setEnabled(isVideoButtonSelected);

        Component[] buttons
            = new Component[]
            {
                new OutputVolumeControlButton(true).getComponent(),
                new InputVolumeControlButton(call, true, callPeer.isMute()),
                new HoldButton(
                        call,
                        true,
                        CallPeerState.isOnHold(callPeer.getState())),
                new RecordButton(
                        call,
                        true,
                        callContainer.isRecordingStarted()),
                new FullScreenButton(callContainer, true),
                new LocalVideoButton(
                        call,
                        true,
                        isVideoButtonSelected),
                showHideButton,
                new HangupButton(callContainer)
            };

        return CallPeerRendererUtils.createButtonBar(true, buttons);
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
                topComponent.setBackground(new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));
            }

            // Set the color of the center panel.
            peerPanel.setOpaque(true);
            peerPanel.setBackground(new Color(GuiActivator.getResources()
                .getColor("service.gui.MAC_PANEL_BACKGROUND")));
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
        if (peerPanel != null)
            peerPanel.dispose();
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
        frame.setTitle(peerPanel.getPeerName());
        frame.setUndecorated(true);

        Component center = null;//peerPanel.createCenter(videoContainers);
        final Component buttonBar = createFullScreenButtonBar();

        // Lay out the main Components of the UI.
        final Container contentPane = frame.getContentPane();
        contentPane.setLayout(new FullScreenLayout(false, 10));
        if (buttonBar != null)
            contentPane.add(buttonBar, FullScreenLayout.SOUTH);
        if (center != null)
            contentPane.add(center, FullScreenLayout.CENTER);

        // Full-screen windows usually have black backgrounds.
        Color background = Color.black;
        contentPane.setBackground(background);
        CallPeerRendererUtils.setBackground(center, background);

        class FullScreenListener
            implements ContainerListener,
                       KeyListener,
                       WindowStateListener
        {
            public void componentAdded(ContainerEvent event)
            {
                event.getChild().addKeyListener(this);
            }

            public void componentRemoved(ContainerEvent event)
            {
                event.getChild().removeKeyListener(this);
            }

            public void keyPressed(KeyEvent event)
            {
                if (!event.isConsumed()
                        && (event.getKeyCode() == KeyEvent.VK_ESCAPE))
                {
                    event.consume();
                    exitFullScreen();
                }
            }

            public void keyReleased(KeyEvent event) {}

            public void keyTyped(KeyEvent event) {}

            public void windowStateChanged(WindowEvent event)
            {
                switch (event.getID())
                {
                case WindowEvent.WINDOW_CLOSED:
                case WindowEvent.WINDOW_DEACTIVATED:
                case WindowEvent.WINDOW_ICONIFIED:
                case WindowEvent.WINDOW_LOST_FOCUS:
                    exitFullScreen();
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

        GraphicsConfiguration graphicsConfiguration
            = getGraphicsConfiguration();

        if (graphicsConfiguration != null)
        {
            GraphicsDevice graphicsDevice = graphicsConfiguration.getDevice();

            this.fullScreenWindow = frame;
            graphicsDevice.setFullScreenWindow(frame);
        }

        GuiUtils.addWindow(fullScreenWindow);
    }

    /**
     * Exits the full screen mode.
     */
    public void exitFullScreen()
    {
        if (fullScreenWindow != null)
        {
            try
            {
                /*
                 * XXX Attempt to return to windowed mode only if
                 * fullScreenWindow exists and is the current full-screen window
                 * (known to the GraphicsDevice of the GraphicsConfiguration).
                 * Otherwise, a deadlock may be experienced on Mac OS X.
                 */
                GraphicsConfiguration graphicsConfiguration
                    = getGraphicsConfiguration();

                if (graphicsConfiguration != null)
                {
                    GraphicsDevice graphicsDevice
                        = graphicsConfiguration.getDevice();

                    if (graphicsDevice.getFullScreenWindow()
                            == fullScreenWindow)
                        graphicsDevice.setFullScreenWindow(null);
                }
            }
            finally
            {
                GuiUtils.removeWindow(fullScreenWindow);

                if (fullScreenWindow.isVisible())
                    fullScreenWindow.setVisible(false);
                fullScreenWindow.dispose();
                fullScreenWindow = null;
            }
        }
    }

    /**
     * Returns the call represented by this call renderer.
     * @return the call represented by this call renderer
     */
    public Call getCall()
    {
        return call;
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
    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer)
    {
        if (callPeer.equals(this.callPeer))
            return peerPanel;
        return null;
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
        this.nameLabel.setText(getPeerDisplayText(callPeer, name));
    }
}
