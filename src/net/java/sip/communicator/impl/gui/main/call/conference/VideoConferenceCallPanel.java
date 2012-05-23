package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI for video conference calls. This panel contains all conference peers
 * and members.
 *
 * @author Yana Stamcheva
 */
public class VideoConferenceCallPanel
    extends ConferenceCallPanel
{
    /**
     * Handles video events and components.
     */
    private final UIVideoHandler videoHandler;

    /**
     * The contained call.
     */
    private final Call call;

    /**
     * Maps a <tt>CallPeer</tt> to its renderer.
     */
    protected final Hashtable<CallPeer, ConferenceCallPeerRenderer>
        callPeerPanels = new Hashtable<CallPeer, ConferenceCallPeerRenderer>();

    /**
     * Creates an instance of <tt>VideoConferenceCallPanel</tt>.
     *
     * @param callPanel the call panel which contains this panel
     * @param call the conference call object
     */
    public VideoConferenceCallPanel(CallPanel callPanel, Call call)
    {
        super(callPanel, call, true);

        this.call = call;

        addVideoContainer();

        videoHandler = new UIVideoHandler(this, videoContainers);

        videoHandler.setLocalVideoToolbar(createLocalVideoToolBar());

        Iterator<? extends CallPeer> iterator;

        iterator = call.getCallPeers();
        while (iterator.hasNext())
        {
            CallPeer peer = iterator.next();

            ConferenceCallPeerRenderer peerRenderer = createVideoToolBar(peer);

            videoHandler.addVideoToolbar(peer, peerRenderer.getComponent());

            // Map the call peer to its renderer.
            callPeerPanels.put(peer, peerRenderer);
        }

        iterator = call.getCrossProtocolCallPeers();
        while (iterator.hasNext())
        {
            CallPeer peer = iterator.next();

            ConferenceCallPeerRenderer peerRenderer = createVideoToolBar(peer);

            videoHandler.addVideoToolbar(peer, peerRenderer.getComponent());

            // Map the call peer to its renderer.
            callPeerPanels.put(peer, peerRenderer);
        }

        iterator = call.getCallPeers();
        while (iterator.hasNext())
        {
            CallPeer callPeer = iterator.next();

            ConferenceCallPeerRenderer peerRenderer
                = callPeerPanels.get(callPeer);

            addCallPeerPanel(callPeer, peerRenderer);
        }

        iterator = call.getCrossProtocolCallPeers();
        while (iterator.hasNext())
        {
            CallPeer callPeer = iterator.next();

            ConferenceCallPeerRenderer peerRenderer
                = callPeerPanels.get(callPeer);

            addCallPeerPanel(callPeer, peerRenderer);
        }
    }

    /**
     * Returns the <tt>CallPeerRenderer</tt> corresponding to the given
     * <tt>callPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt>, which renderer we're looking for
     * @return the renderer for the given <tt>callPeer</tt>.
     */
    public CallPeerRenderer getCallPeerRenderer(CallPeer callPeer)
    {
        return callPeerPanels.get(callPeer);
    }

    /**
     * 
     */
    public void conferenceMemberAdded(CallPeer callPeer,
        ConferenceMember conferenceMember) {}

    /**
     * 
     */
    public void conferenceMemberRemoved(CallPeer callPeer,
        ConferenceMember conferenceMember) {}

    /**
     * Creates and adds a <tt>CallPeerRenderer</tt> for the given <tt>peer</tt>.
     *
     * @param peer the peer for which to create a renderer
     */
    public void addCallPeerPanel(CallPeer peer)
    {
        ConferenceCallPeerRenderer peerRenderer = createVideoToolBar(peer);

        videoHandler.addVideoToolbar(peer, peerRenderer.getComponent());

        // Map the call peer to its renderer.
        callPeerPanels.put(peer, peerRenderer);

        addCallPeerPanel(peer, peerRenderer);
    }

    /**
     * Creates and adds a <tt>CallPeerRenderer</tt> for the given <tt>peer</tt>.
     *
     * @param peer the added peer
     * @param peer the peer for which to create a renderer
     */
    private void addCallPeerPanel(   CallPeer peer,
                                    ConferenceCallPeerRenderer peerRenderer)
    {
        videoHandler.addVideoListener(peer);
        videoHandler.addRemoteControlListener(peer);

        if (peer.getConferenceMemberCount() > 0)
        {
            peer.addConferenceMembersSoundLevelListener(peerRenderer.
                getConferenceMembersSoundLevelListener());
            peer.addStreamSoundLevelListener(peerRenderer.
                getStreamSoundLevelListener());
        }
        else
        {
            //peer.addConferenceMembersSoundLevelListener(
            //    confPeerRenderer.getConferenceMembersSoundLevelListener());
            peer.addStreamSoundLevelListener(
                peerRenderer.getStreamSoundLevelListener());
        }

        // Create an adapter which would manage all common call peer listeners.
        CallPeerAdapter callPeerAdapter
            = new CallPeerAdapter(peer, peerRenderer);

        peerRenderer.setCallPeerAdapter(callPeerAdapter);

        peer.addCallPeerListener(callPeerAdapter);
        peer.addPropertyChangeListener(callPeerAdapter);
        peer.addCallPeerSecurityListener(callPeerAdapter);
        peer.addCallPeerConferenceListener(callPeerAdapter);
    }

    /**
     * Removes the <tt>CallPeerRenderer</tt> and all related listeners
     * corresponding to the given <tt>peer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to remove
     */
    public void removeCallPeerPanel(CallPeer peer)
    {
        ConferenceCallPeerRenderer confPeerRenderer = callPeerPanels.get(peer);

        if (confPeerRenderer == null)
            return;

        confPeerRenderer.getVideoHandler().removeRemoteControlListener(peer);

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

        // Remove all common listeners.
        CallPeerAdapter adapter = confPeerRenderer.getCallPeerAdapter();

        peer.removeCallPeerListener(adapter);
        peer.removePropertyChangeListener(adapter);
        peer.removeCallPeerSecurityListener(adapter);
        peer.removeCallPeerConferenceListener(adapter);
    }

    /**
     * Creates the tool bar for the local video component.
     *
     * @return created component
     */
    private Component createLocalVideoToolBar()
    {
        return new ConferencePeerPanel(
            this, getCallContainer(), call.getProtocolProvider(), true);
    }

    /**
     * Initializes the video tool bar.
     *
     * @param callPeer the <tt>CallPeer</tt> for which we create a video toolbar
     * @return the created component
     */
    private ConferenceCallPeerRenderer createVideoToolBar(CallPeer callPeer)
    {
        return new ConferencePeerPanel(
            this, getCallContainer(), callPeer, videoHandler, true);
    }

    /**
     * Initializes a new <tt>VideoContainer</tt> instance which is to contain
     * the visual/video <tt>Component</tt>s of {@link #call}.
     */
    protected void addVideoContainer()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addVideoContainer();
                }
            });
            return;
        }

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
                        }
                        else
                        {
                            constraints.weightx
                                = (component == videoContainer) ? 0 : 1;
                        }
                        layout.setConstraints(component, constraints);
                    }

                    /*
                     * When the first visual/video Component gets added, this
                     * videoContainer is still not accommodated by the frame
                     * size because it has just become visible. So try to resize
                     * the frame to accommodate this videoContainer.
                     */
                    if (e.getID() == ContainerEvent.COMPONENT_ADDED)
                    {
                        Dimension preferredSize
                            = videoContainer.getLayout()
                                .preferredLayoutSize(videoContainer);

                        if ((preferredSize != null)
                                && (preferredSize.width > 0)
                                && (preferredSize.height > 0))
                        {
                            ensureSize(
                                    videoContainer,
                                    preferredSize.width, preferredSize.height);
                        }
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
}
