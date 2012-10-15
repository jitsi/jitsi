/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

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
     * The panel, where all call components are added.
     */
    private CallPanel callPanel;

    /**
     * Creates a <tt>CallDialog</tt> by specifying the underlying call panel.
     */
    public CallDialog()
    {
        super(false);

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

        this.setTitle(callPanel.getCallTitle());
        callPanel.addCallTitleListener(this);

        if (!isVisible())
        {
            pack();
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
            this.setTitle(callPanel.getCallTitle());
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
        callPanel.actionPerformedOnHangupButton(escape);
    }

    /**
     * {@inheritDoc}
     *
     * The delay implemented by <tt>CallDialog</tt> is 5 seconds.
     */
    public void close(CallPanel callPanel, boolean delay)
    {
        if (this.callPanel.equals(callPanel))
        {
            if (delay)
            {
                Timer timer = new Timer(5000, new DisposeCallDialogListener());

                timer.setRepeats(false);
                timer.start();
            }
            else
            {
                this.callPanel.disposeCallInfoFrame();
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
            this.callPanel.dispose();
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
     */
    public void ensureSize(
            final Component component,
            final int width, final int height)
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
        else
        {
            Dimension frameSize = frame.getSize();
            Dimension componentSize = component.getSize();
            int newFrameWidth
                = frameSize.width + width - componentSize.width;
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

            // Don't get smaller than the min size.
            Dimension minSize = frame.getMinimumSize();

            if (newFrameWidth < minSize.width)
                newFrameWidth = minSize.width;
            if (newFrameHeight < minSize.height)
                newFrameHeight = minSize.height;

            /*
             * If we're going to make too small a change, don't even bother.
             * Besides, we don't want some weird recursive resizing.
             */
            int frameWidthDelta = newFrameWidth - frameSize.width;
            int frameHeightDelta = newFrameHeight - frameSize.height;

            // Do not reduce the frame size.
            if ((frameWidthDelta > 1) || (frameHeightDelta > 1))
            {
                if (!(frameWidthDelta > 1))
                {
                    newFrameX = frameLocation.x;
                    newFrameWidth = frameSize.width;
                }
                else if (!(frameHeightDelta > 1))
                {
                    newFrameY = frameLocation.y;
                    newFrameHeight = frameSize.height;
                }

                frame.setBounds(
                        newFrameX, newFrameY,
                        newFrameWidth, newFrameHeight);
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
     * Removes the given CallPanel from the main tabbed pane.
     */
    private class DisposeCallDialogListener
        implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            dispose();
        }
    }
}
