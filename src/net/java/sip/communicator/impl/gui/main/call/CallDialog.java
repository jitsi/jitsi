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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The dialog created for a given call.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class CallDialog
    extends SIPCommFrame
    implements CallContainer,
               CallTitleListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Enabling force minimized mode will always open call dialog minimized.
     * The call dialog still can be shown, but by default it will be minimized.
     */
    private static final String FORCE_MINIMIZED_MODE
        = "net.java.sip.communicator.impl.gui.main.call.FORCE_MINIMIZED_MODE";

    /**
     * Finds a <tt>Container</tt> which is an ancestor of a specific
     * <tt>Component</tt>, has a set <tt>preferredSize</tt> and is closest to
     * the specified <tt>Component</tt> up the ancestor hierarchy.
     *
     * @param component the <tt>Component</tt> whose ancestor hierarchy is to be
     * searched upwards
     * @return a <tt>Container</tt>, if any, which is an ancestor of the
     * specified <tt>component</tt>, has a set <tt>preferredSize</tt> and is
     * closest to the specified <tt>component</tt> up the ancestor hierarchy
     */
    private static Container findClosestAncestorWithSetPreferredSize(
            Component component)
    {
        if ((component instanceof Container) && component.isPreferredSizeSet())
            return (Container) component;
        else
        {
            Container parent;

            while ((parent = component.getParent()) != null)
            {
                if (parent.isPreferredSizeSet())
                    return parent;
                else
                    component = parent;
            }
            return null;
        }
    }

    /**
     * The panel, where all call components are added.
     */
    private CallPanel callPanel;

    private final WindowStateListener windowStateListener
        = new WindowStateListener()
        {
            public void windowStateChanged(WindowEvent ev)
            {
                switch (ev.getID())
                {
                case WindowEvent.WINDOW_DEACTIVATED:
                case WindowEvent.WINDOW_ICONIFIED:
                case WindowEvent.WINDOW_LOST_FOCUS:
                    setFullScreen(false);
                    break;
                }
            }
        };

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     */
    public CallDialog()
    {
        super(true, false);

        setMinimumSize(new Dimension(360, 300));
    }

    /**
     * Adds a call panel.
     *
     * @param callPanel the call panel to add to this dialog
     */
    public void addCallPanel(CallPanel callPanel)
    {
        this.callPanel = callPanel;

        getContentPane().add(callPanel);

        callPanel.addCallTitleListener(this);
        setTitle(callPanel.getCallTitle());

        if (!isVisible())
        {
            pack();

            // checks whether we need to open the call dialog in minimized mode
            if(GuiActivator.getConfigurationService()
                    .getBoolean(FORCE_MINIMIZED_MODE, false))
            {
                setState(ICONIFIED);
            }
            setVisible(true);
        }
    }

    /**
     * Called when the title of the given <tt>CallPanel</tt> changes.
     *
     * @param callPanel the <tt>CallPanel</tt>, which title has changed
     */
    public void callTitleChanged(CallPanel callPanel)
    {
        if (this.callPanel.equals(callPanel))
            setTitle(callPanel.getCallTitle());
    }

    /**
     * {@inheritDoc}
     *
     * Hang ups the call/telephony conference depicted by this
     * <tt>CallDialog</tt> on close.
     */
    @Override
    protected void close(boolean escape)
    {
        if (escape)
        {
            /*
             * In full-screen mode, ESC does not close this CallDialog but exits
             * from full-screen to windowed mode.
             */
            if (isFullScreen())
            {
                setFullScreen(false);
                return;
            }
        }
        else
        {
            /*
             * If the window has been closed by clicking the X button or
             * pressing the key combination corresponding to the same button we
             * close the window first and then perform all hang up operations.
             */

            callPanel.disposeCallInfoFrame();
            // We hide the window here. It will be disposed when the call has
            // been ended.
            setVisible(false);
        }

        // Then perform hang up operations.
        callPanel.actionPerformedOnHangupButton(escape);
    }

    /**
     * {@inheritDoc}
     *
     * The delay implemented by <tt>CallDialog</tt> is 5 seconds.
     */
    public void close(final CallPanel callPanel, boolean delay)
    {
        if (this.callPanel.equals(callPanel))
        {
            if (delay)
            {
                Timer timer
                    = new Timer(
                            5000,
                            new ActionListener()
                            {
                                public void actionPerformed(ActionEvent ev)
                                {
                                    dispose();
                                }
                            });

                timer.setRepeats(false);
                timer.start();
            }
            else
            {
                dispose();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <tt>CallDialog</tt> prepares the <tt>CallPanel</tt> it contains for
     * garbage collection.
     */
    @Override
    public void dispose()
    {
        super.dispose();

        /*
         * Technically, CallManager adds/removes the callPanel to/from this
         * instance. It may want to just move it to another CallContainer so it
         * does not sound right that we are disposing of it. But we do not have
         * such a case at this time so try to reduce the risk of memory leaks.
         */
        if (this.callPanel != null)
        {
            callPanel.disposeCallInfoFrame();
            callPanel.dispose();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Attempts to adjust the size of this <tt>Frame</tt> as requested in the
     * AWT event dispatching thread.
     * <p>
     * The method may be executed on the AWT event dispatching thread only
     * because whoever is making the decision to request an adjustment of the
     * Frame size in relation to a AWT Component should be analyzing that same
     * Component in the AWT event dispatching thread only.
     * </p>
     *
     * @throws RuntimeException if the method is not called on the AWT event
     * dispatching thread
     */
    public void ensureSize(Component component, int width, int height)
    {
        CallManager.assertIsEventDispatchingThread();

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
        else if (frame.equals(
                frame.getGraphicsConfiguration().getDevice()
                        .getFullScreenWindow()))
        {
            /*
             * Forcing the size of a Component which is displayed in a
             * full-screen window does not sound like anything we want to do.
             */
            return;
        }
        else if (!frame.equals(this))
        {
            /* This Frame will try to adjust only its own size. */
            return;
        }
        else if ((component.getHeight() >= height)
                && (component.getWidth() >= width))
        {
            /*
             * We will only enlarge the frame size. If the component has already
             * been given at least what it is requesting, do not enlarge the
             * frame size because the whole calculation is prone to inaccuracy.
             */
            return;
        }
        else
        {
            /*
             * If there is no callPanel, it is unlikely that this CallDialog
             * will be asked to ensureSize. Anyway, support the scenario just in
             * case. In light of the absence of a callPanel to guide this
             * CallDialog about the preferred size, we do not have much of a
             * choice but to trust the method arguments.
             */
            if (callPanel != null)
            {
                /*
                 * If there is a callPanel, we are likely to get a much better
                 * estimation about the preferred size by asking the callPanel
                 * rather than by trusting the method arguments. For example,
                 * the visual Component displaying the video streaming from the
                 * local user/peer to the remote peer(s) will think that its
                 * preferred size is the one to base this Frame's size on but
                 * that may be misleading because the local video may not be
                 * displayed with its preferred size even if this Frame's size
                 * will accommodate it.
                 */
                /*
                 * Just asking the callPanel about its preferredSize would've
                 * been terrificly great. Unfortunately, that is presently
                 * futile because the callPanel may have a preferredSize while
                 * we are still required to display visual Components displaying
                 * video in their non-scaled size. The same goes for any
                 * Container which is an ancestor of the specified component.
                 */
                Container ancestor
                    = findClosestAncestorWithSetPreferredSize(component);

                if (ancestor == null)
                    ancestor = callPanel;
                /*
                 * If the ancestor has a forced preferredSize, its LayoutManager
                 * may be able to give a good enough estimation.
                 */
                if (ancestor.isPreferredSizeSet())
                {
                    LayoutManager ancestorLayout = ancestor.getLayout();

                    if (ancestorLayout != null)
                    {
                        Dimension preferredLayoutSize
                            = ancestorLayout.preferredLayoutSize(ancestor);

                        if (preferredLayoutSize != null)
                        {
                            component = ancestor;
                            width = preferredLayoutSize.width;
                            height = preferredLayoutSize.height;
                        }
                    }
                }
                else
                {
                    /*
                     * If the ancestor doesn't have a preferredSize forced, then
                     * we may think that it will calculate an appropriate
                     * preferredSize itself.
                     */
                    Dimension prefSize = ancestor.getPreferredSize();

                    if (prefSize != null)
                    {
                        component = ancestor;
                        width = prefSize.width;
                        height = prefSize.height;
                    }
                }
            }

            /*
             * If the component (which may be an ancestor of the Component
             * specified as an argument to the ensureSize method at this point)
             * has not been given a size, we will make a mistake if we try to
             * use it for the purposes of determining how much this Frame is to
             * be enlarged.
             */
            Dimension componentSize = component.getSize();

            if ((componentSize.width < 1) || (componentSize.height < 1))
                return;

            Dimension frameSize = frame.getSize();
            int newFrameWidth = frameSize.width + width - componentSize.width;
            int newFrameHeight
                = frameSize.height + height - componentSize.height;

            // Respect the minimum size.
            Dimension minSize = frame.getMinimumSize();

            if (newFrameWidth < minSize.width)
                newFrameWidth = minSize.width;
            if (newFrameHeight < minSize.height)
                newFrameHeight = minSize.height;

            // Don't get bigger than the screen.
            Rectangle screenBounds
                = frame.getGraphicsConfiguration().getBounds();

            if (newFrameWidth > screenBounds.width)
                newFrameWidth = screenBounds.width;
            if (newFrameHeight > screenBounds.height)
                newFrameHeight = screenBounds.height;

            /*
             * If we're going to make too small a change, don't even bother.
             * Besides, we don't want some weird recursive resizing.
             * Additionally, do not reduce the Frame size.
             */
            boolean changeWidth = ((newFrameWidth - frameSize.width) > 1);
            boolean changeHeight = ((newFrameHeight - frameSize.height) > 1);

            if (changeWidth || changeHeight)
            {
                if (!changeWidth)
                    newFrameWidth = frameSize.width;
                else if (!changeHeight)
                    newFrameHeight = frameSize.height;

                /*
                 * The latest requirement with respect to the behavior upon
                 * resizing is to center the Frame.
                 */
                int newFrameX
                    = screenBounds.x
                        + (screenBounds.width - newFrameWidth) / 2;
                int newFrameY
                    = screenBounds.y
                        + (screenBounds.height - newFrameHeight) / 2;

                // Do not let the top left go out of the screen.
                if (newFrameX < screenBounds.x)
                    newFrameX = screenBounds.x;
                if (newFrameY < screenBounds.y)
                    newFrameY = screenBounds.y;

                frame.setBounds(
                        newFrameX, newFrameY,
                        newFrameWidth, newFrameHeight);

                /*
                 * Make sure that the component which originally requested the
                 * update to the size of the frame realizes the change as soon
                 * as possible; otherwise, it may request yet another update.
                 */
                if (frame.isDisplayable())
                {
                    if (frame.isValid())
                        frame.doLayout();
                    else
                        frame.validate();
                    frame.repaint();
                }
                else
                    frame.doLayout();
            }
        }
    }

    /**
     * Returns the frame of the call window.
     *
     * @return the frame of the call window
     */
    public JFrame getFrame()
    {
        return this;
    }

    /**
     * Overrides getMinimumSize and checks the minimum size that
     * is needed to display buttons and use it for minimum size if
     * needed.
     * @return minimum size.
     */
    @Override
    public Dimension getMinimumSize()
    {
        Dimension minSize = super.getMinimumSize();

        if(callPanel != null)
        {
            int minButtonWidth = callPanel.getMinimumButtonWidth();

            if(minButtonWidth > minSize.getWidth())
                minSize = new Dimension(minButtonWidth, 300);
        }

        return minSize;
    }

    /**
     * Indicates if the given <tt>callPanel</tt> is currently visible.
     *
     * @param callPanel the <tt>CallPanel</tt>, for which we verify
     * @return <tt>true</tt> if the given call container is visible in this
     * call window, otherwise - <tt>false</tt>
     */
    public boolean isCallVisible(CallPanel callPanel)
    {
        return this.callPanel.equals(callPanel) ? isVisible() : false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullScreen()
    {
        return isFullScreen(getFrame());
    }

    /**
     * Determines whether a specific <tt>Window</tt> is displayed in full-screen
     * mode.
     *
     * @param window the <tt>Window</tt> to be checked whether it is displayed
     * in full-screen mode
     * @return <tt>true</tt> if the specified <tt>window</tt> is displayed in
     * full-screen mode; otherwise, <tt>false</tt>
     */
    public static boolean isFullScreen(Window window)
    {
        GraphicsConfiguration graphicsConfiguration
            = window.getGraphicsConfiguration();

        if (graphicsConfiguration != null)
        {
            GraphicsDevice device = graphicsConfiguration.getDevice();

            if (device != null)
                return window.equals(device.getFullScreenWindow());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setFullScreen(boolean fullScreen)
    {
        GraphicsConfiguration graphicsConfiguration
            = getGraphicsConfiguration();

        if (graphicsConfiguration != null)
        {
            GraphicsDevice device = graphicsConfiguration.getDevice();

            if (device != null)
            {
                boolean thisIsFullScreen = equals(device.getFullScreenWindow());
                boolean firePropertyChange = false;
                boolean setVisible = isVisible();

                try
                {
                    if (fullScreen)
                    {
                        if (!thisIsFullScreen)
                        {
                            /*
                             * XXX The setUndecorated method will only work if
                             * this Window is not displayable.
                             */
                            windowDispose();
                            setUndecorated(true);

                            device.setFullScreenWindow(this);
                            firePropertyChange = true;
                        }
                    }
                    else if (thisIsFullScreen)
                    {
                        /*
                         * XXX The setUndecorated method will only work if this
                         * Window is not displayable.
                         */
                        windowDispose();
                        setUndecorated(false);

                        device.setFullScreenWindow(null);
                        firePropertyChange = true;
                    }

                    if (firePropertyChange)
                    {
                        if (fullScreen)
                        {
                            addWindowStateListener(windowStateListener);

                            /*
                             * If full-screen mode, a black background is the
                             * most common.
                             */
                            getContentPane().setBackground(Color.BLACK);
                        }
                        else
                        {
                            removeWindowStateListener(windowStateListener);

                            /*
                             * In windowed mode, a system-defined background is
                             * the most common.
                             */
                            getContentPane().setBackground(null);
                        }

                        firePropertyChange(
                            PROP_FULL_SCREEN,
                            thisIsFullScreen,
                            fullScreen);
                    }
                }
                finally
                {
                    /*
                     * Regardless of whether this Window successfully entered or
                     * exited full-screen mode, make sure that remains visible.
                     */
                    if (setVisible)
                        setVisible(true);
                }
            }
        }
    }
}
