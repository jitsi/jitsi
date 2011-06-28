/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.util.*;

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
    extends JScrollPane
    implements CallRenderer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The conference call.
     */
    private final Call call;

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
    private final Hashtable<CallPeer, ConferencePeerPanel> callPeerPanels
        = new Hashtable<CallPeer, ConferencePeerPanel>();

    /**
     * The CallPanel which contains this panel.
     */
    private final CallPanel callPanel;

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
            JScrollBar verticalScrollBar = getVerticalScrollBar();

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
        this.callPanel = callPanel;
        this.call = c;

        mainPanel = new TransparentPanel();

        mainPanel.setLayout(new GridBagLayout());

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.setViewport(new MyViewport());
        this.setViewportView(mainPanel);

        this.setOpaque(false);
        this.getViewport().setOpaque(false);

        this.addLocalCallPeer();

        Iterator<? extends CallPeer> iterator = this.call.getCallPeers();
        while (iterator.hasNext())
        {
            this.addCallPeerPanel(iterator.next());
        }

        this.setBorder(null);
        this.setViewportBorder(BorderFactory
            .createEmptyBorder(5, 5, 5, 5));

        mainPanel.setTransferHandler(new CallTransferHandler(call));
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
        constraints.insets = new Insets(0, 0, 10, 0);

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

        ConferencePeerPanel confPeerPanel
            = new ConferencePeerPanel(this, callPanel, peer);

        peer.addCallPeerConferenceListener(confPeerPanel);

        peer.addConferenceMembersSoundLevelListener(
            confPeerPanel.getConferenceMembersSoundLevelListener());
        peer.addStreamSoundLevelListener(
            confPeerPanel.getStreamSoundLevelListener());

        // Map the call peer to its renderer.
        callPeerPanels.put(peer, confPeerPanel);

        // Add the renderer component to this container.
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 0);

        mainPanel.add(confPeerPanel, constraints);

        // Create an adapter which would manage all common call peer listeners.
        CallPeerAdapter callPeerAdapter
            = new CallPeerAdapter(peer, confPeerPanel);

        confPeerPanel.setCallPeerAdapter(callPeerAdapter);

        peer.addCallPeerListener(callPeerAdapter);
        peer.addPropertyChangeListener(callPeerAdapter);
        peer.addCallPeerSecurityListener(callPeerAdapter);

        SwingUtilities.invokeLater(scrollToBottomRunnable);
    }

    /**
     * Removes the <tt>CallPeerRenderer</tt> and all related listeners
     * corresponding to the given <tt>peer</tt>.
     * @param peer the <tt>CallPeer</tt> to remove
     */
    public void removeCallPeerPanel(CallPeer peer)
    {
        ConferencePeerPanel confPeerPanel = callPeerPanels.get(peer);

        if (confPeerPanel == null)
            return;

        // first remove the listeners as after removing the panel
        // we may still receive sound level indicators and there are
        // missing ui components leading to exception
        peer.removeCallPeerConferenceListener(confPeerPanel);
        peer.removeConferenceMembersSoundLevelListener(
            confPeerPanel.getConferenceMembersSoundLevelListener());
        peer.removeStreamSoundLevelListener(
            confPeerPanel.getStreamSoundLevelListener());

        // Remove the corresponding renderer.
        callPeerPanels.remove(peer);

        // Remove the renderer component.
        mainPanel.remove(confPeerPanel);

        if (call.getCallPeerCount() < 2)
            setSingleConferenceFocusUI(true);

        // Remove all common listeners.
        CallPeerAdapter adapter = confPeerPanel.getCallPeerAdapter();

        peer.removeCallPeerListener(adapter);
        peer.removePropertyChangeListener(adapter);
        peer.removeCallPeerSecurityListener(adapter);
    }

    /**
     * Sets the single conference focus interface.
     * @param isSingleConferenceFocusUI indicates if the single conference
     * focus interface should be enabled or disabled
     */
    private void setSingleConferenceFocusUI(boolean isSingleConferenceFocusUI)
    {
        Enumeration<CallPeer> callPeers = callPeerPanels.keys();

        while (callPeers.hasMoreElements())
        {
            CallPeer callPeer = callPeers.nextElement();

            if (callPeer.isConferenceFocus())
            {
                callPeerPanels.get(callPeer)
                    .setSingleFocusUI(isSingleConferenceFocusUI);
            }
        }
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
     * @param component the component, which size should be considered
     * @param width the desired width
     * @param height the desired height
     */
    public void ensureSize(Component component, int width, int height) {}

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
}
