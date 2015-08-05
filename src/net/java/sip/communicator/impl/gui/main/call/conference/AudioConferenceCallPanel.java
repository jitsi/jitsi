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
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Extends <tt>BasicConferenceCallPanel</tt> to implement a user interface
 * <tt>Component</tt> which depicts an audio-only <tt>CallConference</tt> and is
 * contained in a <tt>CallPanel</tt>. (Even if the <tt>CallConference</tt>
 * actually has video, <tt>AudioConferenceCallPanel</tt> does not dispay the
 * video.)
 *
 * @author Dilshan Amadoru
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class AudioConferenceCallPanel
    extends BasicConferenceCallPanel
{
    /**
     * The <tt>minimumSize</tt> to be set on the {@link #scrollPane} of
     * <tt>AudioConferenceCallPanel</tt>.
     */
    private static final Dimension SCROLL_PANE_MINIMUM_SIZE
        = new Dimension(
                400 /* It sounds reasonable without being justified. */,
                100 /* It is arbitrary and, hopefully, unnecessary. */);

    /**
     * The <tt>GridBagConstraints</tt> of the <tt>Component</tt>s which depict
     * the <tt>CallPeer</tt>s associated with the <tt>Call</tt>s participating
     * in the telephony conference depicted by this
     * <tt>AudioConferenceCallPanel</tt>.
     */
    private final GridBagConstraints constraints;

    /**
     * The panel which contains ConferencePeerPanels.
     */
    private final TransparentPanel mainPanel;

    /**
     * The scroll pane.
     */
    private final JScrollPane scrollPane;

    /**
     * The implementation of the routine which scrolls {@link #scrollPane} to
     * its bottom.
     */
    private final Runnable scrollToBottomRunnable
        = new Runnable()
        {
            public void run()
            {
                JScrollBar verticalScrollBar
                    = scrollPane.getVerticalScrollBar();

                if (verticalScrollBar != null)
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
            }
        };

    /**
     * Initializes a new <tt>AudioConferenceCallPanel</tt> instance which is to
     * be used by a specific <tt>CallPanel</tt> to depict a specific
     * <tt>CallConference</tt>. The new instance will depict only the
     * audio-related information and will ignore the video-related information
     * should there be any.
     *
     * @param callPanel the <tt>CallPanel</tt> which will use the new instance
     * to depict the specified <tt>CallConference</tt>.
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     */
    public AudioConferenceCallPanel(
            CallPanel callPanel,
            CallConference callConference)
    {
        super(callPanel, callConference);

        mainPanel = new TransparentPanel();
        mainPanel.setLayout(new GridBagLayout());

        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        Temporarily disables the custom viewport, as we believe the issues it
//        was fixing are now fixed by calling pack() on the parent window.
//        scrollPane.setViewport(new MyViewport());
        scrollPane.setViewportView(mainPanel);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.setBorder(null);
        scrollPane.setMinimumSize(SCROLL_PANE_MINIMUM_SIZE);

        mainPanel.setTransferHandler(new CallTransferHandler(callConference));

        /*
         * XXX Call addCallPeerPanel(CallPeer) after calling addVideoContainer()
         * because the video may already be flowing between the CallPeers.
         * Otherwise, the videos of the remote CallPeers will not be shown.
         */
        add(scrollPane, BorderLayout.CENTER);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 3, 0);

        /*
         * Notify the super that this instance has completed its initialization
         * and the view that it implements is ready to be updated from the
         * model.
         */
        initializeComplete();
    }

    /**
     * Returns the vertical <tt>JScrollBar</tt>.
     *
     * @return the vertical <tt>JScrollBar</tt>
     */
    public JScrollBar getVerticalScrollBar()
    {
        return (scrollPane == null) ? null : scrollPane.getVerticalScrollBar();
    }

    /**
     * {@inheritDoc}
     *
     * Makes sure that the <tt>CallPeer</tt>s which are conference focuses are
     * depicted by <tt>ConferenceFocusPanel</tt>s and the <tt>CallPeer</tt>s
     * which are not conference focuses are depicted by
     * <tt>ConferencePeerPanel</tt>s.
     */
    @Override
    protected ConferenceCallPeerRenderer updateViewFromModel(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        if (callPeer == null)
        {
            List<Call> calls = callConference.getCalls();
            Call call = calls.isEmpty() ? null : calls.get(0);

            if (callPeerPanel instanceof ConferencePeerPanel)
            {
                if (!((ConferencePeerPanel) callPeerPanel).getCall().equals(
                        call))
                    callPeerPanel = null;
            }
            else
                callPeerPanel = null;
            if ((callPeerPanel == null) && (call != null))
                callPeerPanel = new ConferencePeerPanel(this, call, false);
        }
        else
        {
            if (callPeer.isConferenceFocus()
                    || (callPeer.getConferenceMemberCount() > 0))
            {
                if (!(callPeerPanel instanceof ConferenceFocusPanel))
                {
                    callPeerPanel
                        = new ConferenceFocusPanel(this, callPeer);
                }
            }
            else if (!(callPeerPanel instanceof ConferencePeerPanel))
            {
                callPeerPanel = new ConferencePeerPanel(this, callPeer);
            }
        }
        return callPeerPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void viewForModelAdded(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        /*
         * Add the Component which is widget equivalent to the specified
         * callPeerPanel to the user interface hierarchy of this instance.
         */
        constraints.gridy = (callPeer == null) ? 0 : (constraints.gridy + 1);
        mainPanel.add(callPeerPanel.getComponent(), constraints);

        SwingUtilities.invokeLater(scrollToBottomRunnable);

        // If the parent window exists already try to adjust the size of the
        // window to the new content. Fixes cut off conference window.
        Window parentWindow = SwingUtilities.getWindowAncestor(mainPanel);
        if (parentWindow != null)
            parentWindow.pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void viewForModelRemoved(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        /*
         * Remove the AWT Component of callPeerPanel from
         * the user interface hierarchy of this instance.
         */
        mainPanel.remove(callPeerPanel.getComponent());

        // If the parent window exists already try to adjust the size of the
        // window to the new content.
        Window parentWindow = SwingUtilities.getWindowAncestor(mainPanel);
        if (parentWindow != null)
            parentWindow.pack();
    }

    /**
     * Implements a <tt>JViewport</tt> which allows us to install a custom
     * <tt>LayoutManager</tt>, namely {@link MyViewportLayout}.
     */
    private static class MyViewport
        extends JViewport
    {
        /**
         * {@inheritDoc}
         *
         * Returns {@link MyViewportLayout#SHARED_INSTANCE} so that
         * <tt>MyViewport</tt> uses <tt>MyViewportLayout</tt>.
         */
        @Override
        protected LayoutManager createLayoutManager()
        {
            return MyViewportLayout.SHARED_INSTANCE;
        }
    }

    /**
     * Implements a custom <tt>ViewportLayout</tt> which fixes the size of the
     * associated <tt>JViewport</tt> while resizing the <tt>Window</tt> which
     * contains the <tt>JScrollPane</tt>.
     */
    private static class MyViewportLayout
        extends ViewportLayout
    {
        /**
         * The <tt>MyViewportLayout</tt> instance (to be) shared/used by all
         * <tt>MyViewport</tt> instances.
         */
        static final MyViewportLayout SHARED_INSTANCE = new MyViewportLayout();

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
                viewSize.width = vpSize.width;
            else if (vpSize.height < viewSize.height)
                viewSize.height = vpSize.height;

            vp.setViewPosition(viewPosition);
            vp.setViewSize(viewSize);
        }
    }
}
