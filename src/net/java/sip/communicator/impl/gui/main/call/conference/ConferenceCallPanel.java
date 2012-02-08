/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI for conference calls. This panel contains all conference peers and
 * members.
 *
 * @author Dilshan Amadoru
 * @author Yana Stamcheva
 */
public class ConferenceCallPanel
    extends TransparentPanel
    implements CallRenderer
{
    /**
     * The <tt>preferredSize</tt> to be set on {@link #scrollPane} if there is
     * video displayed.
     */
    private static final Dimension SCROLL_PANE_PREFERRED_SIZE_IF_VIDEO
        = new Dimension(
                400 /* It sounds reasonable without being justified. */,
                100 /* It is arbitrary and, hopefully, unnecessary. */);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The conference call.
     */
    private final Call call;

    /**
     * The scroll pane.
     */
    private final JScrollPane scrollPane = new JScrollPane();

    /**
     * The panel which contains ConferencePeerPanels.
     */
    private final TransparentPanel mainPanel;

    /**
     * The constraints to create the GridBagLayout.
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Maps a <tt>CallPeer</tt> to its renderer.
     */
    private final Hashtable<CallPeer, ConferenceCallPeerRenderer> callPeerPanels
        = new Hashtable<CallPeer, ConferenceCallPeerRenderer>();

    /**
     * The CallPanel which contains this panel.
     */
    private final CallPanel callPanel;

    /**
     * The list containing all video containers.
     */
    private final List<Container> videoContainers
        = new LinkedList<Container>();

    /**
     * The video handler.
     */
    private UIVideoHandler videoHandler;

    /**
     * The implementation of the routine which scrolls this scroll pane to its
     * bottom.
     */
    private final Runnable scrollToBottomRunnable = new Runnable()
    {
        /**
         * Implements Runnable#run().
         * @see Runnable#run()
         */
        public void run()
        {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

            if (verticalScrollBar != null)
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
    };

    /**
     * Creates an instance of <tt>ConferenceCallPanel</tt>.
     *
     * @param callPanel the call panel which contains this panel
     * @param c the conference call object
     */
    public ConferenceCallPanel(CallPanel callPanel, Call c)
    {
        super(new GridBagLayout());

        this.callPanel = callPanel;
        this.call = c;

        mainPanel = new TransparentPanel();
        mainPanel.setLayout(new GridBagLayout());

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setViewport(new MyViewport());
        scrollPane.setViewportView(mainPanel);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        this.addLocalCallPeer();

        Iterator<? extends CallPeer> iterator = this.call.getCallPeers();

        while (iterator.hasNext())
            this.addCallPeerPanel(iterator.next());

        iterator = this.call.getCrossProtocolCallPeers();

        while (iterator.hasNext())
        {
            this.addCallPeerPanel(iterator.next());
        }

        scrollPane.setBorder(null);
        /*
         * The scrollPane seems to receive only a few pixels of width at times
         * when there is video displayed, not always. Try to work around that
         * misbehavior by specifying a minimum size.
         */
        scrollPane.setMinimumSize(SCROLL_PANE_PREFERRED_SIZE_IF_VIDEO);

        mainPanel.setTransferHandler(new CallTransferHandler(call));

        GridBagConstraints scrollPaneGridBagConstraints
            = new GridBagConstraints();

        scrollPaneGridBagConstraints.fill = GridBagConstraints.BOTH;
        scrollPaneGridBagConstraints.gridx = 1;
        scrollPaneGridBagConstraints.gridy = 0;
        scrollPaneGridBagConstraints.weightx = 1;
        scrollPaneGridBagConstraints.weighty = 1;
        add(scrollPane, scrollPaneGridBagConstraints);

        addVideoContainer();
    }

    /**
     * Initializes a new <tt>VideoContainer</tt> instance which is to contain
     * the visual/video <tt>Component</tt>s of {@link #call}. 
     */
    private void addVideoContainer()
    {
        final VideoContainer videoContainer = new VideoContainer(null);

        videoContainer.setPreferredSize(new Dimension(0, 0));

        GridBagConstraints videoContainerGridBagConstraints
            = new GridBagConstraints();

        videoContainerGridBagConstraints.fill = GridBagConstraints.BOTH;
        videoContainerGridBagConstraints.gridx = 0;
        videoContainerGridBagConstraints.gridy = 0;
        videoContainerGridBagConstraints.weightx = 0;
        videoContainerGridBagConstraints.weighty = 1;
        add(videoContainer, videoContainerGridBagConstraints);
        /*
         * When the videoContainer is empty i.e. it has nothing to show, don't
         * show it.
         */
        videoContainer.addContainerListener(
            new ContainerListener()
            {
                public void componentAdded(ContainerEvent e)
                {
                    GridBagLayout layout = (GridBagLayout) getLayout();
                    boolean videoContainerIsVisible
                        = (videoContainer.getComponentCount() > 0);

                    for (Component component : getComponents())
                    {
                        GridBagConstraints constraints
                            = layout.getConstraints(component);

                        if (videoContainerIsVisible)
                        {
                            constraints.weightx
                                = (component == videoContainer) ? 1 : 0;
                            scrollPane.setPreferredSize(
                                    SCROLL_PANE_PREFERRED_SIZE_IF_VIDEO);
                        }
                        else
                        {
                            constraints.weightx
                                = (component == videoContainer) ? 0 : 1;

                            if (SCROLL_PANE_PREFERRED_SIZE_IF_VIDEO.equals(
                                    scrollPane.getPreferredSize()))
                                scrollPane.setPreferredSize(null);
                        }
                        layout.setConstraints(component, constraints);
                    }
                }

                public void componentRemoved(ContainerEvent e)
                {
                    /*
                     * It's all the same with respect to the purpose of this
                     * ContainerListener.
                     */
                    componentAdded(e);
                }
            });

        videoContainers.add(videoContainer);
    }

    /**
     * Returns the vertical <tt>JScrollBar</tt>.
     *
     * @return the vertical <tt>JScrollBar</tt>
     */
    public JScrollBar getVerticalScrollBar()
    {
        return scrollPane.getVerticalScrollBar();
    }

    /**
     * Adds the local call peer panel to this conference call.
     */
    private void addLocalCallPeer()
    {
        final ConferencePeerPanel localPeerPanel
            = new ConferencePeerPanel(  this,
                                        callPanel,
                                        call.getProtocolProvider());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 3, 0);

        mainPanel.add(localPeerPanel, constraints);

        call.addLocalUserSoundLevelListener(new SoundLevelListener()
        {
            public void soundLevelChanged(Object source, int level)
            {
                localPeerPanel.fireLocalUserSoundLevelChanged(level);
            }
        });

        SwingUtilities.invokeLater(scrollToBottomRunnable);
    }

    /**
     * Creates and adds a <tt>CallPeerRenderer</tt> for the given <tt>peer</tt>.
     *
     * @param peer the peer for which to create a renderer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        if (callPeerPanels.containsKey(peer))
            return;

        ConferenceCallPeerRenderer confPeerRenderer;

        videoHandler
            = new UIVideoHandler(peer, this, videoContainers);

        videoHandler.addVideoListener();
        videoHandler.addRemoteControlListener();

        if (peer.getConferenceMemberCount() > 0)
        {
            confPeerRenderer = new ConferenceFocusPanel(
                this, callPanel, peer, videoHandler);
            peer.addConferenceMembersSoundLevelListener(confPeerRenderer.
                getConferenceMembersSoundLevelListener());
            peer.addStreamSoundLevelListener(confPeerRenderer.
                getStreamSoundLevelListener());
        }
        else
        {
            confPeerRenderer
                = new ConferencePeerPanel(this, callPanel, peer, videoHandler);

            //peer.addConferenceMembersSoundLevelListener(
            //    confPeerRenderer.getConferenceMembersSoundLevelListener());
            peer.addStreamSoundLevelListener(
                confPeerRenderer.getStreamSoundLevelListener());
        }

        // Map the call peer to its renderer.
        callPeerPanels.put(peer, confPeerRenderer);

        // Add the renderer component to this container.
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 3, 0);

        mainPanel.add((Component) confPeerRenderer, constraints);

        // Create an adapter which would manage all common call peer listeners.
        CallPeerAdapter callPeerAdapter
            = new CallPeerAdapter(peer, confPeerRenderer);

        confPeerRenderer.setCallPeerAdapter(callPeerAdapter);

        peer.addCallPeerListener(callPeerAdapter);
        peer.addPropertyChangeListener(callPeerAdapter);
        peer.addCallPeerSecurityListener(callPeerAdapter);
        peer.addCallPeerConferenceListener(callPeerAdapter);

        SwingUtilities.invokeLater(scrollToBottomRunnable);
    }

    /**
     * Removes the <tt>CallPeerRenderer</tt> and all related listeners
     * corresponding to the given <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt> to remove
     */
    public void removeCallPeerPanel(CallPeer peer)
    {
        ConferenceCallPeerRenderer confPeerRenderer = callPeerPanels.get(peer);

        if (confPeerRenderer == null)
            return;

        // first remove the listeners as after removing the panel
        // we may still receive sound level indicators and there are
        // missing ui components leading to exception
        ConferenceMembersSoundLevelListener membersSoundLevelListener
            = confPeerRenderer.getConferenceMembersSoundLevelListener();
        if (membersSoundLevelListener != null)
            peer.removeConferenceMembersSoundLevelListener(
                membersSoundLevelListener);

        SoundLevelListener soundLevelListener
            = confPeerRenderer.getStreamSoundLevelListener();
        if (soundLevelListener != null)
            peer.removeStreamSoundLevelListener(soundLevelListener);

        // Remove the corresponding renderer.
        callPeerPanels.remove(peer);

        // Remove the renderer component.
        mainPanel.remove(confPeerRenderer.getComponent());

        // Remove all common listeners.
        CallPeerAdapter adapter = confPeerRenderer.getCallPeerAdapter();

        peer.removeCallPeerListener(adapter);
        peer.removePropertyChangeListener(adapter);
        peer.removeCallPeerSecurityListener(adapter);
        peer.removeCallPeerConferenceListener(adapter);
    }

    private static class MyViewport
        extends JViewport
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Subclassers can override this to install a different
         * layout manager (or <code>null</code>) in the constructor.  Returns
         * the <code>LayoutManager</code> to install on the
         * <code>JViewport</code>.
         * @return a <code>LayoutManager</code>
         */
        protected LayoutManager createLayoutManager()
        {
            return MyViewportLayout.SHARED_INSTANCE;
        }
    }

    /**
     * Custom ViewportLayout that fixes viewport size while resizing the window
     * containing the scrollpane.
     */
    private static class MyViewportLayout extends ViewportLayout
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        // Single instance used by JViewport.
        static MyViewportLayout SHARED_INSTANCE = new MyViewportLayout();

        @Override
        public void layoutContainer(Container parent)
        {
            JViewport vp = (JViewport)parent;
            Component view = vp.getView();
            Scrollable scrollableView = null;

            if (view == null)
                return;
            else if (view instanceof Scrollable)
                scrollableView = (Scrollable) view;

            /* All of the dimensions below are in view coordinates, except
             * vpSize which we're converting.
             */
            Dimension viewPrefSize = view.getPreferredSize();
            Dimension vpSize = vp.getSize();
            Dimension extentSize = vp.toViewCoordinates(vpSize);
            Dimension viewSize = new Dimension(viewPrefSize);

            if (scrollableView != null)
            {
                if (scrollableView.getScrollableTracksViewportWidth())
                    viewSize.width = vpSize.width;
                if (scrollableView.getScrollableTracksViewportHeight())
                    viewSize.height = vpSize.height;
            }

            Point viewPosition = vp.getViewPosition();

            /* If the new viewport size would leave empty space to the
             * right of the view, right justify the view or left justify
             * the view when the width of the view is smaller than the
             * container.
             */
            if (scrollableView == null ||
                vp.getParent() == null ||
                vp.getParent().getComponentOrientation().isLeftToRight())
            {
                if ((viewPosition.x + extentSize.width) > viewSize.width)
                {
                    viewPosition.x
                        = Math.max(0, viewSize.width - extentSize.width);
                }
            }
            else
            {
                if (extentSize.width > viewSize.width)
                {
                    viewPosition.x = viewSize.width - extentSize.width;
                }
                else
                {
                    viewPosition.x = Math.max(0, Math.min(
                        viewSize.width - extentSize.width, viewPosition.x));
                }
            }

            /* If the new viewport size would leave empty space below the
             * view, bottom justify the view or top justify the view when
             * the height of the view is smaller than the container.
             */
            if ((viewPosition.y + extentSize.height) > viewSize.height)
            {
                viewPosition.y
                    = Math.max(0, viewSize.height - extentSize.height);
            }

            /* If we haven't been advised about how the viewports size
             * should change wrt to the viewport, i.e. if the view isn't
             * an instance of Scrollable, then adjust the views size as follows.
             *
             * If the origin of the view is showing and the viewport is
             * bigger than the views preferred size, then make the view
             * the same size as the viewport.
             */
            if (scrollableView == null)
            {
                if ((viewPosition.x == 0)
                    && (vpSize.width > viewPrefSize.width))
                {
                    viewSize.width = vpSize.width;
                }
                if ((viewPosition.y == 0)
                    && (vpSize.height > viewPrefSize.height))
                {
                    viewSize.height = vpSize.height;
                }
            }

            // Fixes incorrect size of the view.
            if (vpSize.width < viewSize.width)
            {
                viewSize.width = vpSize.width;
            }
            else if (vpSize.height < viewSize.height)
            {
                viewSize.height = vpSize.height;
            }

            vp.setViewPosition(viewPosition);
            vp.setViewSize(viewSize);
        }
    }

    /**
     * Ensures the size of the window.
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
            Dimension frameSize = frame.getSize();

            /*
             * XXX This is a very wild guess and it is very easy to break
             * because it wants to have the component with the specified width
             * yet it forces the Frame to have nearly the same width without
             * knowing anything about the layouts of the containers between the
             * Frame and the component.
             */
            int newFrameWidth
                = width + frameSize.width - component.getSize().width;

            int newFrameHeight
                = (frameSize.height > height) ? frameSize.height : height;

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
             * XXX Unreliable because VideoRenderer Components such as the
             * Component of the AWTRenderer on Linux overrides its
             * #getPreferredSize().
             */
            component.setPreferredSize(new Dimension(width, height));
            component.setSize(new Dimension(width, height));

            Container componentParent = component.getParent();

            componentParent.setPreferredSize(new Dimension(width, height));
            componentParent.setSize(new Dimension(width, height));

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

            /*
             * It seems that the video container does not fill its dedicated
             * area because we've set its size above.
             */
            validate();
        }
    }

    /**
     * Enters in full screen view mode.
     */
    public void enterFullScreen()
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Exits from the full screen view mode.
     */
    public void exitFullScreen()
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Returns the parent call container, where this renderer is contained.
     * @return the parent call container, where this renderer is contained
     */
    public CallPanel getCallContainer()
    {
        return callPanel;
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
        return callPeerPanels.get(callPeer);
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
     * Indicates that the given conference member has been added to the given
     * peer.
     *
     * @param callPeer the parent call peer
     * @param conferenceMember the member that was added
     */
    public void conferenceMemberAdded(  CallPeer callPeer,
                                        ConferenceMember conferenceMember)
    {
        CallPeerRenderer peerRenderer = callPeerPanels.get(callPeer);

        if (peerRenderer instanceof ConferencePeerPanel)
        {
            removeCallPeerPanel(callPeer);
            addCallPeerPanel(callPeer);
        }
        else if (peerRenderer instanceof ConferenceFocusPanel)
        {
            ((ConferenceFocusPanel) peerRenderer)
                .addConferenceMemberPanel(conferenceMember);
        }
    }

    /**
     * Indicates that the given conference member has been removed from the
     * given peer.
     *
     * @param callPeer the parent call peer
     * @param conferenceMember the member that was removed
     */
    public void conferenceMemberRemoved(CallPeer callPeer,
                                        ConferenceMember conferenceMember)
    {
        CallPeerRenderer peerRenderer = callPeerPanels.get(callPeer);

        if (callPeer.getConferenceMemberCount() > 0)
        {
            ((ConferenceFocusPanel) peerRenderer)
                .removeConferenceMemberPanel(conferenceMember);
        }
        else
        {
            removeCallPeerPanel(callPeer);
            addCallPeerPanel(callPeer);
        }
    }
}
