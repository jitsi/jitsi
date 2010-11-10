/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallPanel</tt> is the panel containing call information. It's created
 * and added to the main tabbed pane when user makes or receives calls. It shows
 * information about call peers, call duration, etc.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
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
     * The panel representing the underlying <tt>CallPeer</tt>.
     */
    private OneToOneCallPeerPanel peerPanel;

    /**
     * The parent call dialog.
     */
    private final CallDialog callDialog;

    /**
     * The underlying <tt>Call</tt>, this panel is representing.
     */
    private final Call call;

    /**
     * The underlying <tt>CallPeer</tt>.
     */
    private final CallPeer callPeer;

    /**
     * The current <code>Window</code> being displayed in full-screen. Because
     * the AWT API with respect to the full-screen support doesn't seem
     * sophisticated enough, the field is used sparingly i.e. when there are no
     * other means (such as a local variable) of acquiring the instance.
     */
    private Window fullScreenWindow;

    /**
     * The check box allowing to turn on remote control when in a desktop
     * sharing session.
     */
    private JCheckBox enableDesktopRemoteControl;

    /**
     * The panel added on the south of this container.
     */
    private JPanel southPanel;

    /**
     * Creates a call panel for the corresponding call, by specifying the
     * call type (incoming or outgoing) and the parent dialog.
     *
     * @param callDialog    the dialog containing this panel
     * @param call          the call corresponding to this panel
     * @param callPeer      the remote participant in the call
     */
    public OneToOneCallPanel(   CallDialog callDialog,
                                Call call,
                                CallPeer callPeer)
    {
        super(new BorderLayout());

        this.callDialog = callDialog;
        this.call = call;
        this.callPeer = callPeer;

        this.setBorder(BorderFactory
            .createEmptyBorder(5, 0, 5, 0));

        this.setTransferHandler(new CallTransferHandler(call));

        this.addCallPeerPanel(callPeer);

        this.setPreferredSize(new Dimension(400, 400));
    }

    /**
     * Creates and adds a panel for a call peer.
     *
     * @param peer the call peer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        if (peerPanel == null)
        {
            peerPanel = new OneToOneCallPeerPanel(this, peer);

            this.add(peerPanel);

            // Create an adapter which would manage all common call peer
            // listeners.
            CallPeerAdapter callPeerAdapter
                = new CallPeerAdapter(peer, peerPanel);

            peerPanel.setCallPeerAdapter(callPeerAdapter);

            peer.addCallPeerListener(callPeerAdapter);
            peer.addPropertyChangeListener(callPeerAdapter);
            peer.addCallPeerSecurityListener(callPeerAdapter);

            // Refresh the call panel if it's already visible.
            if (isVisible())
            {
                this.revalidate();
                this.repaint();
            }
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
        frame.setTitle(peerPanel.getPeerName());
        frame.setUndecorated(true);

        Component center = peerPanel.createCenter();
        final Component buttonBar = createFullScreenButtonBar();

        // Lay out the main Components of the UI.
        final Container contentPane = frame.getContentPane();
        contentPane.setLayout(new FullScreenLayout(false));
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
                    exitFullScreen();
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

        getGraphicsConfiguration().getDevice().setFullScreenWindow(frame);
        this.fullScreenWindow = frame;

        GuiUtils.addWindow(fullScreenWindow);
    }

    /**
     * Exits the full screen mode.
     */
    public void exitFullScreen()
    {
        GraphicsConfiguration graphicsConfig = getGraphicsConfiguration();
        if (graphicsConfig != null)
            graphicsConfig.getDevice().setFullScreenWindow(null);

        if (fullScreenWindow != null)
        {
            GuiUtils.removeWindow(fullScreenWindow);

            if (fullScreenWindow.isVisible())
                fullScreenWindow.setVisible(false);
            fullScreenWindow.dispose();
            fullScreenWindow = null;
        }
    }

    /**
     * Create the buttons bar for the fullscreen mode.
     *
     * @return the buttons bar <tt>Component</tt>
     */
    private Component createFullScreenButtonBar()
    {
        Component[] buttons
            = new Component[]
            {
                new HoldButton(call,
                               true,
                               CallPeerState.isOnHold(callPeer.getState())),
                new MuteButton(call,
                               true,
                               callPeer.isMute()),
                CallPeerRendererUtils.createExitFullScreenButton(this)
            };

        return CallPeerRendererUtils.createButtonBar(true, buttons);
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
     */
    public void ensureSize(Component component, int width, int height)
    {
        Frame frame = CallPeerRendererUtils.getFrame(component);

        if (frame == null)
            return;
        else if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH)
                == Frame.MAXIMIZED_BOTH)
        {
            /*
             * Forcing the size of a Component which is displayed in a maximized
             * window does not sound like anything we want to do.
             */
            return;
        }
        else if (frame.equals(frame.getGraphicsConfiguration()
                                .getDevice().getFullScreenWindow()))
        {
            /*
             * Forcing the size of a Component which is displayed in a
             * full-screen window does not sound like anything we want to do.
             */
            return;
        }
        else
        {
            Insets frameInsets = frame.getInsets();

            /*
             * XXX This is a very wild guess and it is very easy to break
             * because it wants to have the component with the specified width
             * yet it forces the Frame to have nearly the same width without
             * knowing anything about the layouts of the containers between the
             * Frame and the component.
             */
            int newFrameWidth = width + frameInsets.left + frameInsets.right;

            Dimension frameSize = frame.getSize();
            Dimension componentSize = component.getSize();

            int newFrameHeight
                = frameSize.height + height - componentSize.height;

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

            /*
             * XXX Unreliable because VideoRenderer Components such as the
             * Component of the AWTRenderer on Linux overrides its
             * #getPreferredSize().
             */
            component.setPreferredSize(new Dimension(width, height));

            /*
             * If we're going to make too small a change, don't even bother.
             * Besides, we don't want some weird recursive resizing.
             */
            int frameWidthDelta = newFrameWidth - frameSize.width;
            int frameHeightDelta = newFrameHeight - frameSize.height;

            if ((frameWidthDelta < -1)
                    || (frameWidthDelta > 1)
                    || (frameHeightDelta < -1)
                    || (frameHeightDelta > 1))
            {
                frame.setBounds(
                        newFrameX, newFrameY,
                        newFrameWidth, newFrameHeight);
            }
        }
    }

    /**
     * Adds all desktop sharing related components to this container.
     */
    public void addDesktopSharingComponents()
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

            enableDesktopRemoteControl.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    CallManager.enableDesktopRemoteControl(
                        callPeer, e.getStateChange() == ItemEvent.SELECTED);
                }
            });
        }

        add(southPanel, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    /**
     * Removes all desktop sharing related components from this container.
     */
    public void removeDesktopSharingComponents()
    {
        if (southPanel != null)
            remove(southPanel);

        revalidate();
        repaint();
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
     * Returns the call represented by this call renderer.
     * @return the call represented by this call renderer
     */
    public Call getCall()
    {
        return call;
    }

    /**
     * Returns the parent call dialog, where this renderer is contained.
     * @return the parent call dialog, where this renderer is contained
     */
    public CallDialog getCallDialog()
    {
        return callDialog;
    }
}
