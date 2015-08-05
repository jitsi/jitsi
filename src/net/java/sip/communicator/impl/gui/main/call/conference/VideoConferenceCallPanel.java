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
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;

import org.jitsi.util.swing.*;

/**
 * Extends <tt>BasicConferenceCallPanel</tt> to implement a user interface
 * <tt>Component</tt> which depicts a <tt>CallConference</tt> with audio and
 * video and is contained in a <tt>CallPanel</tt>.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class VideoConferenceCallPanel
    extends BasicConferenceCallPanel
{
    /**
     * The <tt>Logger</tt> used by the <tt>VideoConferenceCallPanel</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VideoConferenceCallPanel.class);

    /**
     * The compile-time flag which indicates whether each video displayed by
     * <tt>VideoConferenceCallPanel</tt> is to be depicted with an associated
     * tool bar showing information and controls related to the (local or
     * remote) peer sending the respective video.
     */
    private static final boolean SHOW_TOOLBARS = true;

    /**
     * The facility which aids this instance with the video-related information.
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * The <tt>Observer</tt> which listens to {@link #uiVideoHandler} about
     * changes in the video-related information.
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
     * The <tt>VideoContainer</tt> which occupies this whole <tt>Component</tt>
     * and arranges the visual <tt>Component</tt>s displaying the video
     * streaming between the local peer/user and the remote peer(s).
     */
    private final VideoContainer videoContainer;

    /**
     * The set of visual <tt>Component</tt>s displaying video streaming between
     * the local peer/user and the remote peers which are depicted by this
     * instance.
     */
    private final Set<Component> videos = new HashSet<Component>();

    /**
     * The thumbnail container.
     */
    private final ThumbnailConferenceCallPanel thumbnailContainer;

    private final JPanel thumbnailPanel;

    /**
     * Initializes a new <tt>VideoConferenceCallPanel</tt> instance which is to
     * be used by a specific <tt>CallPanel</tt> to depict a specific
     * <tt>CallConference</tt>. The new instance will depict both the
     * audio-related and the video-related information.
     *
     * @param callPanel the <tt>CallPanel</tt> which will use the new instance
     * to depict the specified <tt>CallConference</tt>.
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     * @param uiVideoHandler the utility which is to aid the new instance in
     * dealing with the video-related information
     */
    public VideoConferenceCallPanel(
            CallPanel callPanel,
            CallConference callConference,
            UIVideoHandler2 uiVideoHandler)
    {
        super(callPanel, callConference);

        this.uiVideoHandler = uiVideoHandler;

        thumbnailPanel = new JPanel(new BorderLayout());
        thumbnailContainer
            = new ThumbnailConferenceCallPanel( callPanel,
                                                callConference,
                                                uiVideoHandler);

        videoContainer = createVideoContainer();

        /*
         * Our user interface hierarchy has been initialized so we are ready to
         * begin receiving events warranting updates of this view from its
         * model.
         */
        uiVideoHandler.addObserver(uiVideoHandlerObserver);

        /*
         * Notify the super that this instance has completed its initialization
         * and the view that it implements is ready to be updated from the
         * model.
         */
        initializeComplete();
    }

    private void addConferenceMemberContainers(
            ConferenceParticipantContainer cpc)
    {
        List<ConferenceParticipantContainer> cmcs
            = cpc.conferenceMemberContainers;

        if ((cmcs != null) && !cmcs.isEmpty())
        {
            for (ConferenceParticipantContainer cmc : cmcs)
            {
                if (!cmc.toBeRemoved)
                {
                    videoContainer.add(
                            cmc.getComponent(),
                            VideoLayout.CENTER_REMOTE);
                }
            }
        }
    }

    private Component createDefaultPhotoPanel(Call call)
    {
        OperationSetServerStoredAccountInfo accountInfo
            = call.getProtocolProvider().getOperationSet(
                    OperationSetServerStoredAccountInfo.class);
        ImageIcon photoLabelIcon = null;

        if (accountInfo != null)
        {
            byte[] accountImage = AccountInfoUtils.getImage(accountInfo);

            // do not set empty images
            if ((accountImage != null) && (accountImage.length > 0))
                photoLabelIcon = new ImageIcon(accountImage);
        }
        if (photoLabelIcon == null)
        {
            photoLabelIcon
                = new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO));
        }

        return createDefaultPhotoPanel(photoLabelIcon);
    }

    private Component createDefaultPhotoPanel(CallPeer callPeer)
    {
        byte[] peerImage = CallManager.getPeerImage(callPeer);
        ImageIcon photoLabelIcon
            = (peerImage == null)
                ? new ImageIcon(
                        ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO))
                : new ImageIcon(peerImage);

        return createDefaultPhotoPanel(photoLabelIcon);
    }

    private Component createDefaultPhotoPanel(ConferenceMember conferenceMember)
    {
        return
            createDefaultPhotoPanel(
                    new ImageIcon(
                            ImageLoader.getImage(
                                    ImageLoader.DEFAULT_USER_PHOTO)));
    }

    /**
     * Creates a new <tt>Component</tt> which is to display a specific
     * <tt>ImageIcon</tt> representing the photo of a participant in a call.
     *
     * @param photoLabelIcon the <tt>ImageIcon</tt> which represents the photo
     * of a participant in a call and which is to be displayed by the new
     * <tt>Component</tt>
     * @return a new <tt>Component</tt> which displays the specified
     * <tt>photoLabelIcon</tt>
     */
    private Component createDefaultPhotoPanel(ImageIcon photoLabelIcon)
    {
        JLabel photoLabel = new JLabel();

        photoLabel.setIcon(photoLabelIcon);

        @SuppressWarnings("serial")
        JPanel photoPanel
            = new TransparentPanel(new GridBagLayout())
            {
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

                        g.setColor(Color.GRAY);
                        g.fillRoundRect(
                                0, 0, this.getWidth(), this.getHeight(),
                                6, 6);
                    }
                    finally
                    {
                        g.dispose();
                    }
                }
            };

        photoPanel.setPreferredSize(new Dimension(320, 240));

        GridBagConstraints photoPanelConstraints = new GridBagConstraints();

        photoPanelConstraints.anchor = GridBagConstraints.CENTER;
        photoPanelConstraints.fill = GridBagConstraints.NONE;
        photoPanel.add(photoLabel, photoPanelConstraints);

        return photoPanel;
    }

    /**
     * Initializes a new <tt>VideoContainer</tt> instance which is to contain
     * the visual/video <tt>Component</tt>s of the telephony conference depicted
     * by this instance.
     */
    private VideoContainer createVideoContainer()
    {
        VideoContainer videoContainer = new VideoContainer(null, true);

        thumbnailPanel.setBackground(Color.DARK_GRAY);
        thumbnailPanel.add(thumbnailContainer, BorderLayout.NORTH);

        add(thumbnailPanel, BorderLayout.EAST);
        add(videoContainer, BorderLayout.CENTER);

        return videoContainer;
    }

    /**
     * Shows/hides the participants thumbnails list.
     *
     * @param show <tt>true</tt> to show the participants list, <tt>false</tt>
     * to hide it
     */
    public void showThumbnailsList(boolean show)
    {
        thumbnailPanel.setVisible(show);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
    {
        try
        {
            uiVideoHandler.deleteObserver(uiVideoHandlerObserver);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Determines whether a specific <tt>ConferenceMember</tt> represents the
     * same conference participant as a specific <tt>CallPeer</tt>. If the
     * specified <tt>conferenceMember</tt> is <tt>null</tt>, returns
     * <tt>true</tt>. Otherwise, determines whether the addresses of the
     * specified <tt>conferenceMember</tt> and the specified <tt>callPeer</tt>
     * identify one and the same entity.
     *
     * @param conferenceMember the <tt>ConferenceMember</tt> to be checked
     * whether is represents the same conference participant as the specified
     * <tt>callPeer</tt>. If it is <tt>null</tt>, <tt>true</tt> is returned.
     * @param callPeer the <tt>CallPeer</tt> to be checked whether it represents
     * the same conference participant as the specified
     * <tt>conferenceMember</tt>
     * @return <tt>true</tt> if the specified <tt>conferenceMember</tt> and the
     * specified <tt>callPeer</tt> represent the same conference participant or
     * the specified <tt>conferenceMember</tt> is <tt>null</tt>; otherwise,
     * <tt>false</tt>
     */
    private boolean isConferenceMemberCallPeer(
            ConferenceMember conferenceMember,
            CallPeer callPeer)
    {
        return
            (conferenceMember == null)
                ? true
                : CallManager.addressesAreEqual(
                        conferenceMember.getAddress(),
                        callPeer.getAddress());
    }

    /**
     * Determines whether a specific <tt>ConferenceMember</tt> represents the
     * local peer/user. Since this instance depicts a whole telephony
     * conference, the local peer/user may be participating with multiple
     * <tt>Call</tt>s in it. The <tt>Call</tt>s may be through different
     * (local) accounts. That's why the implementation determines whether the
     * address of the specified <tt>conferenceMember</tt> identifies the address
     * of a (local) accounts involved in the telephony conference depicted by
     * this instance.
     *
     * @param conferenceMember the <tt>ConferenceMember</tt> to be checked
     * whether it represents the local peer/user
     * @return <tt>true</tt> if the specified <tt>conferenceMember</tt>
     * represents the local peer/user; otherwise, <tt>false</tt>
     */
    private boolean isConferenceMemberLocalUser(
            ConferenceMember conferenceMember)
    {
        String address = conferenceMember.getAddress();

        for (Call call : callConference.getCalls())
        {
            if (CallManager.addressesAreEqual(
                    address,
                    call.getProtocolProvider().getAccountID()
                            .getAccountAddress()))
            {
                return true;
            }
        }
        return false;
    }

    private void removeConferenceMemberContainers(
            ConferenceParticipantContainer cpc,
            boolean all)
    {
        List<ConferenceParticipantContainer> cmcs
            = cpc.conferenceMemberContainers;

        if ((cmcs != null) && !cmcs.isEmpty())
        {
            Iterator<ConferenceParticipantContainer> i = cmcs.iterator();

            while (i.hasNext())
            {
                ConferenceParticipantContainer cmc = i.next();

                if (all || cmc.toBeRemoved)
                {
                    i.remove();

                    videoContainer.remove(cmc.getComponent());
                    cmc.dispose();
                }
            }
        }
    }

    /**
     * Updates the <tt>ConferenceParticipantContainer</tt>s which depict the
     * <tt>ConferenceMember</tt>s of the <tt>CallPeer</tt> depicted by a
     * specific <tt>ConferenceParticipantContainer</tt>.
     *
     * @param cpc the <tt>ConferenceParticipantContainer</tt> which depicts the
     * <tt>CallPeer</tt> whose <tt>ConferenceMember</tt>s are to be depicted
     * @param videos the visual <tt>Component</tt>s displaying video streaming
     * from the remote peer (represented by <tt>cpc</tt>) to the local peer/user
     * @param videoTelephony the <tt>OperationSetVideoTelephony</tt> which
     * retrieved the specified <tt>videos</tt> from the <tt>CallPeer</tt>
     * depicted by <tt>cpc</tt>. While the <tt>CallPeer</tt> could be queried
     * for it, such a query would waste more resources at run time given that
     * the invoker has it already.
     */
    private void updateConferenceMemberContainers(
            ConferenceParticipantContainer cpc,
            List<Component> videos,
            OperationSetVideoTelephony videoTelephony)
    {
        CallPeer callPeer = (CallPeer) cpc.getParticipant();
        List<ConferenceParticipantContainer> cmcs
            = cpc.conferenceMemberContainers;

        /*
         * Invalidate all conferenceMemberContainers. Then validate which of
         * them are to remain and which of them are to really be removed
         * later on.
         */
        if (cmcs != null)
        {
            for (ConferenceParticipantContainer cmc : cmcs)
                cmc.toBeRemoved = true;
        }

        /*
         * Depict the remote videos. They may or may not be associated with
         * ConferenceMembers so the ConferenceMembers which have no
         * associated videos will be depicted afterwards.
         */
        if (videos != null)
        {
            Component video = cpc.getVideo();

            for (Component conferenceMemberVideo : videos)
            {
                /*
                 * One of the remote videos is already used to depict the
                 * callPeer.
                 */
                if (conferenceMemberVideo == video)
                    continue;

                boolean addNewConferenceParticipantContainer = true;
                ConferenceMember conferenceMember
                    = videoTelephony.getConferenceMember(
                            callPeer,
                            conferenceMemberVideo);

                if (cmcs == null)
                {
                    cmcs = new LinkedList<ConferenceParticipantContainer>();
                    cpc.conferenceMemberContainers = cmcs;
                }
                else
                {
                    for (ConferenceParticipantContainer cmc : cmcs)
                    {
                        Object cmcParticipant = cmc.getParticipant();

                        if (conferenceMember == null)
                        {
                            if (cmcParticipant == callPeer)
                            {
                                Component cmcVideo = cmc.getVideo();

                                if (cmcVideo == null)
                                {
                                    cmc.setVideo(conferenceMemberVideo);
                                    cmc.toBeRemoved = false;
                                    addNewConferenceParticipantContainer
                                        = false;
                                    break;
                                }
                                else if (cmcVideo == conferenceMemberVideo)
                                {
                                    cmc.toBeRemoved = false;
                                    addNewConferenceParticipantContainer
                                        = false;
                                    break;
                                }
                            }
                        }
                        else if (cmcParticipant == conferenceMember)
                        {
                            cmc.setVideo(conferenceMemberVideo);
                            cmc.toBeRemoved = false;
                            addNewConferenceParticipantContainer = false;
                            break;
                        }
                    }
                }

                if (addNewConferenceParticipantContainer)
                {
                    ConferenceParticipantContainer cmc
                        = (conferenceMember == null)
                            ? new ConferenceParticipantContainer(
                                    callPeer,
                                    conferenceMemberVideo)
                            : new ConferenceParticipantContainer(
                                    conferenceMember,
                                    conferenceMemberVideo);

                    cmcs.add(cmc);
                }
            }
        }

        /*
         * Depict the ConferenceMembers which have not been depicted yet.
         * They have no associated videos.
         */
        List<ConferenceMember> conferenceMembers
            = callPeer.getConferenceMembers();

        if (!conferenceMembers.isEmpty())
        {
            if (cmcs == null)
            {
                cmcs = new LinkedList<ConferenceParticipantContainer>();
                cpc.conferenceMemberContainers = cmcs;
            }
            for (ConferenceMember conferenceMember : conferenceMembers)
            {
                /*
                 * If the callPeer reports itself as a ConferenceMember, then
                 * we've already depicted it with cpc.
                 */
                if (isConferenceMemberCallPeer(conferenceMember, callPeer))
                    continue;
                /*
                 * If the callPeer reports the local peer/user as a
                 * ConferenceMember, then we've already depicted it.
                 */
                if (isConferenceMemberLocalUser(conferenceMember))
                    continue;

                boolean addNewConferenceParticipantContainer = true;

                for (ConferenceParticipantContainer cmc : cmcs)
                {
                    if (cmc.getParticipant() == conferenceMember)
                    {
                        /*
                         * It is possible to have a ConferenceMember who is
                         * sending video but we just do not have the SSRC of
                         * that video to associate the video with the
                         * ConferenceMember. In such a case, we may be depicting
                         * the ConferenceMember twice: once with video without a
                         * ConferenceMember and once with a ConferenceMember
                         * without video. This will surely be the case at the
                         * time of this writing with non-focus participants in a
                         * telephony conference hosted on a Jitsi Videobridge.
                         * Such a display is undesirable. If the
                         * conferenceMember is known to send video, we will not
                         * display it until we associated it with a video. This
                         * way, if a ConferenceMember is not sending video, we
                         * will depict it and we can be sure that no video
                         * without a ConferenceMember association will be
                         * depicting it a second time.
                         */
                        if (cmc.toBeRemoved
                                && !conferenceMember
                                        .getVideoStatus()
                                            .allowsSending())
                        {
                            cmc.setVideo(null);
                            cmc.toBeRemoved = false;
                        }
                        addNewConferenceParticipantContainer = false;
                        break;
                    }
                }

                if (addNewConferenceParticipantContainer)
                {
                    ConferenceParticipantContainer cmc
                        = new ConferenceParticipantContainer(
                                conferenceMember,
                                null);

                    cmcs.add(cmc);
                }
            }
        }

        if ((cmcs != null) && !cmcs.isEmpty())
        {
            removeConferenceMemberContainers(cpc, false);
            /*
             * If cpc is already added to the user interface hierarchy of this
             * instance, then it was there before the update procedure and it
             * was determined to be appropriate to continue to depict its model.
             * Consequently, its Component will be neither added to (because it
             * was already added) nor removed from the user interface hierarchy
             * of this instance. That's why we have make sure that the
             * Components of its conferenceMemberContainers are also added to
             * the user interface.
             */
            if (UIVideoHandler2.isAncestor(this, cpc.getComponent()))
                addConferenceMemberContainers(cpc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConferenceCallPeerRenderer updateViewFromModel(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        if (callPeer == null)
        {
            /*
             * The local peer/user will be represented by a Call which has a
             * CallPeer who provides local video. However, if the user has
             * selected to hide the local video, the local peer/user will not be
             * represented at all.
             */
            Component video = null;

            if (uiVideoHandler.isLocalVideoVisible())
            {
                for (Call aCall : callConference.getCalls())
                {
                    Iterator<? extends CallPeer> callPeerIter
                        = aCall.getCallPeers();
                    OperationSetVideoTelephony videoTelephony
                        = aCall.getProtocolProvider().getOperationSet(
                                OperationSetVideoTelephony.class);

                    while (callPeerIter.hasNext())
                    {
                        callPeer = callPeerIter.next();

                        if (videoTelephony != null)
                        {
                            try
                            {
                                video
                                    = videoTelephony.getLocalVisualComponent(
                                            callPeer);
                            }
                            catch (OperationFailedException ofe)
                            {
                                logger.error(
                                        "Failed to retrieve the local video"
                                            + " for display",
                                        ofe);
                            }
                            if (video != null)
                                break;
                        }
                    }
                    if (video != null)
                        break;
                }
            }

            if (callPeer == null)
                callPeerPanel = null;
            else
            {
                Call call = callPeer.getCall();

                if (callPeerPanel instanceof ConferenceParticipantContainer)
                {
                    ConferenceParticipantContainer cpc
                        = (ConferenceParticipantContainer) callPeerPanel;

                    if (cpc.getParticipant() == call)
                        cpc.setVideo(video);
                    else
                        callPeerPanel = null;
                }
                else
                    callPeerPanel = null;
                if (callPeerPanel == null)
                {
                    callPeerPanel
                        = new ConferenceParticipantContainer(call, video);
                }
            }
        }
        else
        {
            /*
             * The specified callPeer will be represented by one of its remote
             * videos which is not associated with a ConferenceMember or is
             * associated with a ConferenceMember representing the callPeer
             * itself.
             */
            OperationSetVideoTelephony videoTelephony
                = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);
            List<Component> videos = null;
            Component video = null;

            if (videoTelephony != null)
            {
                videos = videoTelephony.getVisualComponents(callPeer);
                if ((videos != null) && !videos.isEmpty())
                {
                    for (Component aVideo : videos)
                    {
                        ConferenceMember conferenceMember
                            = videoTelephony.getConferenceMember(
                                    callPeer,
                                    aVideo);

                        if (isConferenceMemberCallPeer(
                                conferenceMember,
                                callPeer))
                        {
                            video = aVideo;
                            break;
                        }
                    }
                }
            }

            ConferenceParticipantContainer cpc = null;

            if (callPeerPanel instanceof ConferenceParticipantContainer)
            {
                cpc = (ConferenceParticipantContainer) callPeerPanel;
                if (cpc.getParticipant() == callPeer)
                    cpc.setVideo(video);
                else
                    cpc = null;
            }
            if (cpc == null)
                cpc = new ConferenceParticipantContainer(callPeer, video);
            callPeerPanel = cpc;

            // Update the conferenceMemberContainers of the cpc.
            updateConferenceMemberContainers(cpc, videos, videoTelephony);
        }
        return callPeerPanel;
    }

    /**
     * {@inheritDoc}
     *
     * If {@link #SHOW_TOOLBARS} is <tt>false</tt>, disables the use of
     * <tt>ConferenceParticipantContainer</tt>. A reason for such a value of
     * <tt>SHOW_TOOLBARS</tt> may be that the functionality implemented in the
     * model may not fully support mapping of visual <tt>Component</tt>s
     * displaying video to telephony conference participants (e.g. in telephony
     * conferences utilizing the Jitsi Videobridge server-side technology). In
     * such a case displays the videos only, does not map videos to participants
     * and does not display participants who do not have videos.
     */
    @Override
    protected void updateViewFromModelInEventDispatchThread()
    {
        if (SHOW_TOOLBARS)
        {
            super.updateViewFromModelInEventDispatchThread();
            return;
        }

        /*
         * Determine the set of visual Components displaying video streaming
         * between the local peer/user and the remote peers which are to be
         * depicted by this instance.
         */
        Component localVideo = null;
        Set<Component> videos = new HashSet<Component>();

        for (Call call : callConference.getCalls())
        {
            OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetVideoTelephony.class);

            if (videoTelephony == null)
                continue;

            Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

            while (callPeerIter.hasNext())
            {
                CallPeer callPeer = callPeerIter.next();

                /*
                 * TODO VideoConferenceCallPanel respects
                 * UIVideoHandler2.isLocalVideoVisible() in order to react to
                 * the associated button at the bottom of the CallPanel.
                 * However, it does not add a close button on top of the local
                 * video in contrast to OneToOneCallPeerPanel. Overall, the
                 * result is questionable.
                 */
                if (uiVideoHandler.isLocalVideoVisible()
                        && (localVideo == null))
                {
                    try
                    {
                        localVideo
                            = videoTelephony.getLocalVisualComponent(callPeer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        /*
                         * We'll just try to get the local video through another
                         * CallPeer then.
                         */
                    }
                    if (localVideo != null)
                        videos.add(localVideo);
                }

                List<Component> callPeerRemoteVideos
                    = videoTelephony.getVisualComponents(callPeer);

                videos.addAll(callPeerRemoteVideos);
            }
        }

        /*
         * Remove the Components of this view which are no longer present in the
         * model.
         */
        Iterator<Component> thisVideoIter = this.videos.iterator();

        while (thisVideoIter.hasNext())
        {
            Component thisVideo = thisVideoIter.next();

            if (!videos.contains(thisVideo))
            {
                thisVideoIter.remove();
                videoContainer.remove(thisVideo);
            }

            /*
             * If a video is known to be depicted by this view and is still
             * present in the model, then we could remove it from the set of
             * videos present in the model in order to prevent going through the
             * procedure of adding it to this view. However, we choose to play
             * on the safe side.
             */
        }

        /*
         * Add the Components of the model which are not depicted by this view.
         */
        for (Component video : videos)
        {
            if (!UIVideoHandler2.isAncestor(videoContainer, video))
            {
                this.videos.add(video);
                videoContainer.add(
                        video,
                        (video == localVideo)
                            ? VideoLayout.LOCAL
                            : VideoLayout.CENTER_REMOTE);
            }
        }
    }

    @Override
    protected void viewForModelAdded(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        videoContainer.add(
                callPeerPanel.getComponent(),
                VideoLayout.CENTER_REMOTE);
        if ((callPeer != null)
                && (callPeerPanel instanceof ConferenceParticipantContainer))
        {
            addConferenceMemberContainers(
                    (ConferenceParticipantContainer) callPeerPanel);
        }
    }

    @Override
    protected void viewForModelRemoved(
            ConferenceCallPeerRenderer callPeerPanel,
            CallPeer callPeer)
    {
        videoContainer.remove(callPeerPanel.getComponent());
        if ((callPeer != null)
                && (callPeerPanel instanceof ConferenceParticipantContainer))
        {
            removeConferenceMemberContainers(
                    (ConferenceParticipantContainer) callPeerPanel,
                    true);
        }
    }

    /**
     * Implements an AWT <tt>Component</tt> which contains the user interface
     * elements depicting a specific participant in the telephony conference
     * depicted by a <tt>VideoConferenceCallPanel</tt>.
     */
    private class ConferenceParticipantContainer
        extends TransparentPanel
        implements ConferenceCallPeerRenderer
    {
        /**
         * The list of <tt>ConferenceParticipantContainer</tt>s which represent
         * the <tt>ConferenceMember</tt>s of the participant represented by this
         * instance. Since a <tt>CallPeer</tt> may send the local peer/user
         * multiple videos without providing a way to associate a
         * ConferenceMember with each one of them, the list may contain
         * <tt>ConferenceParticipantContainer</tt>s which do not represent a
         * specific <tt>ConferenceMember</tt> instance but rather a video sent
         * by a <tt>CallPeer</tt> to the local peer/user which looks like (in
         * the terms of <tt>VideoConferenceCallPanel) a member of a conference
         * organized by the <tt>CallPeer</tt> in question.
         * <p>
         * Implements a state which is private to
         * <tt>VideoConferenceCallPanel</tt> and is of no concern to
         * <tt>ConferenceParticipantContainer</tt>.
         * </p>
         */
        List<ConferenceParticipantContainer> conferenceMemberContainers;

        /**
         * The indicator which determines whether this instance is to be removed
         * because it has become out-of-date, obsolete, unnecessary.
         * <p>
         * Implements a state which is private to
         * <tt>VideoConferenceCallPanel</tt> and is of no concern to
         * <tt>ConferenceParticipantContainer</tt>.
         * </p>
         */
        boolean toBeRemoved;

        /**
         * The <tt>BasicConferenceParticipantPanel</tt> which is displayed at
         * the bottom of this instance, bellow the {@link #video} (i.e.
         * {@link #videoContainer}) and is referred to as the tool bar.
         */
        private final BasicConferenceParticipantPanel<?> toolBar;

        /**
         * The visual <tt>Component</tt>, if any, displaying video which is
         * depicted by this instance.
         */
        private Component video;

        /**
         * The <tt>VideoContainer</tt> which lays out the video depicted by this
         * instance i.e. {@link #video}.
         */
        private final VideoContainer videoContainer;

        /**
         * The <tt>CallPeer</tt> associated with this container, if it has been
         * created to represent a <tt>CallPeer</tt>.
         */
        private CallPeer callPeer;

        /**
         * The <tt>conferenceMember</tt> associated with this container, if it
         * has been created to represent a <tt>conferenceMember</tt>.
         */
        private ConferenceMember conferenceMember;

        /**
         * Indicates that this container contains information for the local
         * user.
         */
        private boolean isLocalUser;

        /**
         * Initializes a new <tt>ConferenceParticipantContainer</tt> instance
         * which is to depict the local peer/user.
         *
         * @param call a <tt>Call</tt> which is to provide information about the
         * local peer/user
         * @param video the visual <tt>Component</tt>, if any, displaying the
         * video streaming from the local peer/user to the remote peer(s)
         */
        public ConferenceParticipantContainer(Call call, Component video)
        {
            this(
                    createDefaultPhotoPanel(call),
                    video,
                    new ConferencePeerPanel(
                            VideoConferenceCallPanel.this,
                            call,
                            true),
                    null, null, true);
        }

        public ConferenceParticipantContainer(
                CallPeer callPeer,
                Component video)
        {
            this(   createDefaultPhotoPanel(callPeer),
                    video,
                    new ConferencePeerPanel(
                            VideoConferenceCallPanel.this,
                            callPeer,
                            true),
                    callPeer, null, false);
        }

        private ConferenceParticipantContainer(
                Component noVideo,
                Component video,
                BasicConferenceParticipantPanel<?> toolBar,
                CallPeer callPeer,
                ConferenceMember conferenceMember,
                boolean isLocalUser)
        {
            super(new BorderLayout());

            this.callPeer = callPeer;
            this.conferenceMember = conferenceMember;
            this.isLocalUser = isLocalUser;

            videoContainer = new VideoContainer(noVideo, false);
            add(videoContainer, BorderLayout.CENTER);

            this.toolBar = toolBar;
            if (this.toolBar != null)
                add(this.toolBar, BorderLayout.SOUTH);

            if (video != null)
            {
                setVideo(video);
            }
            else
                setVisible(false);
        }

        public ConferenceParticipantContainer(
                ConferenceMember conferenceMember,
                Component video)
        {
            this(
                    createDefaultPhotoPanel(conferenceMember),
                    video,
                    new ConferenceMemberPanel(
                            VideoConferenceCallPanel.this,
                            conferenceMember,
                            true),
                    null, conferenceMember, false);
        }

        public void dispose()
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.dispose();

            // Dispose of the conferenceMemberContainers if any.
            /*
             * XXX The field conferenceMemberContainers implements a state
             * private to VideoConferenceCallPanel which the latter makes sure
             * to access on the AWT event dispatching thread only. Since we are
             * going out of our way here to help VideoConferenceCallPanel,
             * ensure that the mentioned synchronization rule is not violated.
             */
            CallManager.assertIsEventDispatchingThread();
            if (conferenceMemberContainers != null)
            {
                for (ConferenceParticipantContainer cmc
                        : conferenceMemberContainers)
                {
                    cmc.dispose();
                }
            }
        }

        public CallPanel getCallPanel()
        {
            return getCallRenderer().getCallContainer();
        }

        public SwingCallRenderer getCallRenderer()
        {
            return VideoConferenceCallPanel.this;
        }

        public Component getComponent()
        {
            return this;
        }

        private ConferenceCallPeerRenderer
            getConferenceCallPeerRendererDelegate()
        {
            return
                (toolBar instanceof ConferenceCallPeerRenderer)
                    ? (ConferenceCallPeerRenderer) toolBar
                    : null;
        }

        /**
         * Gets the conference participant depicted by this instance.
         *
         * @return the conference participant depicted by this instance
         */
        public Object getParticipant()
        {
            return (toolBar == null) ? null : toolBar.getParticipant();
        }

        public Component getVideo()
        {
            return video;
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only. Otherwise, returns <tt>false</tt>.
         */
        public boolean isLocalVideoVisible()
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            return (delegate == null) ? false : delegate.isLocalVideoVisible();
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void printDTMFTone(char dtmfChar)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.printDTMFTone(dtmfChar);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void securityNegotiationStarted(
                CallPeerSecurityNegotiationStartedEvent ev)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.securityNegotiationStarted(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void securityOff(CallPeerSecurityOffEvent ev)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.securityOff(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void securityOn(CallPeerSecurityOnEvent ev)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.securityOn(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void securityPending()
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.securityPending();
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void securityTimeout(CallPeerSecurityTimeoutEvent ev)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.securityTimeout(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setErrorReason(String reason)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setErrorReason(reason);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setLocalVideoVisible(boolean visible)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setLocalVideoVisible(visible);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setMute(boolean mute)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setMute(mute);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setOnHold(boolean onHold)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setOnHold(onHold);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setPeerImage(byte[] image)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setPeerImage(image);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setPeerName(String name)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setPeerName(name);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setPeerState(
                CallPeerState oldState,
                CallPeerState newState,
                String stateString)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setPeerState(oldState, newState, stateString);
        }

        /**
         * {@inheritDoc}
         *
         * Delegates to the <tt>toolBar</tt>, if the latter implements
         * <tt>ConferenceCallPeerRenderer</tt>, because this instance is a
         * container only.
         */
        public void setSecurityPanelVisible(boolean visible)
        {
            ConferenceCallPeerRenderer delegate
                = getConferenceCallPeerRendererDelegate();

            if (delegate != null)
                delegate.setSecurityPanelVisible(visible);
        }

        /**
         * Sets the visual <tt>Component</tt> displaying the video associated
         * with the participant depicted by this instance.
         *
         * @param video the visual <tt>Component</tt> displaying video which is
         * to be associated with the participant depicted by this instance
         */
        void setVideo(Component video)
        {
            if (this.video != video)
            {
                if (this.video != null)
                    videoContainer.remove(this.video);

                this.video = video;

                if (this.video != null)
                {
                    setVisible(true);
                    videoContainer.add(this.video, VideoLayout.CENTER_REMOTE);
                }
                else
                    setVisible(false);

                // Update thumbnails container according to video status.
                if (thumbnailContainer != null)
                {
                    if (conferenceMember != null)
                        thumbnailContainer
                            .updateThumbnail(conferenceMember, (video != null));
                    else if (callPeer != null)
                        thumbnailContainer
                            .updateThumbnail(callPeer, (video != null));
                    else if (isLocalUser)
                        thumbnailContainer
                            .updateThumbnail((video != null));
                }
            }
        }
    }
}
