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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter; // disambiguation
import net.java.sip.communicator.impl.gui.main.presence.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ConferencePeerPanel</tt> renders a single <tt>ConferencePeer</tt>,
 * which is not a conference focus.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ConferencePeerPanel
    extends BasicConferenceParticipantPanel
    implements  CallPeerRenderer,
                CallPeerConferenceListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The parent dialog containing this panel.
     */
    private final CallPanel callPanel;

    /**
     * The parent call renderer.
     */
    private final CallRenderer callRenderer;

    /**
     * The call peer shown in this panel.
     */
    private final CallPeer callPeer;

    /**
     * The tools menu available for each peer.
     */
    private CallPeerMenu callPeerMenu;

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel = new SecurityStatusLabel();

    /**
     * The DTMF label.
     */
    private final JLabel dtmfLabel = new JLabel();

    /**
     * The component showing the security details.
     */
    private SecurityPanel securityPanel;

    /**
     * The call peer adapter.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * Security imageID.
     */
    private ImageID securityImageID = ImageLoader.SECURE_BUTTON_OFF;

    /**
     * Maps a <tt>ConferenceMember</tt> to its renderer panel.
     */
    private final Hashtable<ConferenceMember, ConferenceMemberPanel>
        conferenceMembersPanels
            = new Hashtable<ConferenceMember, ConferenceMemberPanel>();

    /**
     * Listens for sound level events on the conference members.
     */
    private ConferenceMembersSoundLevelListener
        conferenceMembersSoundLevelListener = null;

    /**
     * Listens for sound level changes on the stream of the
     * conference members.
     */
    private StreamSoundLevelListener streamSoundLevelListener = null;

    /**
     * Creates a <tt>ConferencePeerPanel</tt> by specifying the parent
     * <tt>callDialog</tt>, containing it and the corresponding
     * <tt>protocolProvider</tt>.
     *
     * @param callRenderer the renderer of the corresponding call
     * @param callPanel the call panel containing this peer panel
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for the
     * call
     */
    public ConferencePeerPanel( CallRenderer callRenderer,
                                CallPanel callPanel,
                                ProtocolProviderService protocolProvider)
    {
        super(callRenderer);

        this.callRenderer = callRenderer;
        this.callPanel = callPanel;
        this.callPeer = null;

        // Try to set the same image as the one in the main window. This way
        // we improve our chances to have an image, instead of looking only at
        // the protocol provider avatar, which could be null, we look for any
        // image coming from one of our accounts.
        byte[] globalAccountImage = AccountStatusPanel.getGlobalAccountImage();
        if (globalAccountImage != null && globalAccountImage.length > 0)
            this.setPeerImage(globalAccountImage);

        this.setPeerName(protocolProvider.getAccountID().getUserID()
            + " (" + GuiActivator.getResources()
                .getI18NString("service.gui.ACCOUNT_ME").toLowerCase() + ")");

        this.setTitleBackground(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_LOCAL_USER_BACKGROUND")));
    }

    /**
     * Creates a <tt>ConferencePeerPanel</tt>, that would be contained in
     * the given <tt>callDialog</tt> and would correspond to the given
     * <tt>callPeer</tt>.
     *
     * @param callRenderer the renderer of the corresponding call
     * @param callContainer the container, in which this panel is shown
     * @param callPeer The peer who own this UI
     */
    public ConferencePeerPanel( CallRenderer callRenderer,
                                CallPanel callContainer,
                                CallPeer callPeer)
    {
        super(callRenderer);

        this.callRenderer = callRenderer;
        this.callPanel = callContainer;
        this.callPeer = callPeer;

        this.setMute(callPeer.isMute());

        this.setPeerImage(CallManager.getPeerImage(callPeer));

        this.setPeerName(callPeer.getDisplayName());

        // We initialize the status bar for call peers only.
        this.initStatusBar(callPeer);

        callPeerMenu = new CallPeerMenu(callPeer);

        SIPCommMenuBar menuBar = new SIPCommMenuBar();
        menuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        menuBar.add(callPeerMenu);
        this.addToNameBar(menuBar);

        this.setTitleBackground(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_PEER_NAME_BACKGROUND")));

        // Add the UI for all contained conference members.
        for (ConferenceMember member : callPeer.getConferenceMembers())
            this.addConferenceMemberPanel(member);

        initSecuritySettings();
    }

    /**
     * Initializes the security settings for this call peer.
     */
    private void initSecuritySettings()
    {
        OperationSetSecureTelephony secure
            = callPeer.getProtocolProvider().getOperationSet(
                        OperationSetSecureTelephony.class);

        if (secure != null)
        {
            CallPeerSecurityStatusEvent securityEvent
                = callPeer.getCurrentSecuritySettings();

            if (securityEvent != null
                && securityEvent instanceof CallPeerSecurityOnEvent)
            {
                CallPeerSecurityOnEvent securityOnEvt
                    = (CallPeerSecurityOnEvent) securityEvent;

                securityOn( securityOnEvt.getSecurityString(),
                            securityOnEvt.isSecurityVerified());

                setEncryptionCipher(securityOnEvt.getCipher());

                switch (securityOnEvt.getSessionType())
                {
                case CallPeerSecurityOnEvent.AUDIO_SESSION:
                    setAudioSecurityOn(true);
                    break;
                case CallPeerSecurityOnEvent.VIDEO_SESSION:
                    setVideoSecurityOn(true);
                    break;
                }
            }
        }
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff()
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF)));

        securityImageID = ImageLoader.SECURE_BUTTON_OFF;
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified by the underlying <tt>CallPeer</tt>
     */
    public void securityOn( String securityString,
                            boolean isSecurityVerified)
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_ON)));

        securityImageID = ImageLoader.SECURE_BUTTON_ON;

        if ((securityPanel == null) && (callPeer != null))
        {
            securityPanel = new SecurityPanel(callPeer);

            securityPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

            this.addToCenter(securityPanel);
        }
        if (securityPanel != null)
            securityPanel.refreshStates(securityString, isSecurityVerified);

        callPanel.refreshContainer();
    }

    /**
     * Sets the mute status icon to the status panel.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute)
    {
        if(isMute)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));
        else
            muteStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the "on hold" property value.
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold)
    {
        if(isOnHold)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));
        else
            holdStatusLabel.setIcon(null);

        this.revalidate();
        this.repaint();
    }

    /**
     * Sets the <tt>icon</tt> of the peer.
     * @param icon the icon to set
     */
    public void setPeerImage(byte[] icon)
    {
        // If this is the local peer (i.e. us) or any peer, but the focus peer.
        if (callPeer == null || !callPeer.isConferenceFocus())
            this.setParticipantImage(icon);
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        this.setParticipantName(name);
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state)
    {
        this.setParticipantState(state);
    }

    /**
     * Updates all related components to fit the new value.
     * @param isAudioSecurityOn indicates if the audio security is turned on
     * or off.
     */
    public void setAudioSecurityOn(boolean isAudioSecurityOn)
    {
        securityStatusLabel.setAudioSecurityOn(isAudioSecurityOn);
    }

    /**
     * Updates all related components to fit the new value.
     * @param encryptionCipher the encryption cipher to show
     */
    public void setEncryptionCipher(String encryptionCipher)
    {
        securityStatusLabel.setEncryptionCipher(encryptionCipher);
    }

    /**
     * Updates all related components to fit the new value.
     * @param isVideoSecurityOn indicates if the video security is turned on
     * or off.
     */
    public void setVideoSecurityOn(boolean isVideoSecurityOn)
    {
        securityStatusLabel.setVideoSecurityOn(isVideoSecurityOn);
    }

    /**
     * Sets the call peer adapter that manages all related listeners.
     * @param adapter the call peer adapter
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        this.callPeerAdapter = adapter;
    }

    /**
     * Returns the call peer adapter that manages all related listeners.
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter()
    {
        return callPeerAdapter;
    }

    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel()
    {
        return callPanel;
    }

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar)
    {
        dtmfLabel.setText(dtmfLabel.getText() + dtmfChar);
    }

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    public void setErrorReason(String reason)
    {
        super.setErrorReason(reason);
    }

    /**
     * Initializes the status bar component for the given <tt>callPeer</tt>.
     *
     * @param callPeer the underlying peer, which status would be displayed
     */
    private void initStatusBar(CallPeer callPeer)
    {
        this.setParticipantState(callPeer.getState().getLocalizedStateString());

        this.addToStatusBar(securityStatusLabel);
        this.addToStatusBar(holdStatusLabel);
        this.addToStatusBar(muteStatusLabel);
        this.addToStatusBar(dtmfLabel);
    }

    /**
     * Adds a <tt>ConferenceMemberPanel</tt> for a given
     * <tt>ConferenceMember</tt>.
     *
     * @param member the <tt>ConferenceMember</tt> that will correspond to the
     * panel to add.
     */
    private void addConferenceMemberPanel(ConferenceMember member)
    {
        String localUserAddress
            = callPeer.getProtocolProvider().getAccountID().getAccountAddress();

        boolean isLocalMember
            = addressesAreEqual(member.getAddress(), localUserAddress);

        // We don't want to add the local member to the list of members.
        if (isLocalMember)
            return;

        // If we're not in a focus UI, when a new member is added we
        // switch to it.
        if (!isFocusUI())
            setFocusUI(true);

        // If this is the only call peer we switch to the single focus user
        // interface.
        if (callPeer.getCall().getCallPeerCount() > 1)
            setSingleFocusUI(false);
        else
            setSingleFocusUI(true);

        // It's already there.
        if (conferenceMembersPanels.containsKey(member))
            return;

        ConferenceMemberPanel memberPanel
            = new ConferenceMemberPanel(callRenderer, member);

        // Map the conference member to the created member panel.
        conferenceMembersPanels.put(member, memberPanel);

        GridBagConstraints constraints = new GridBagConstraints();

        // Add the member panel to this container
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = getComponentCount();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(10, 0, 0, 0);

        this.add(memberPanel, constraints);

        member.addPropertyChangeListener(memberPanel);
    }

    /**
     * Removes the <tt>ConferenceMemberPanel</tt> corresponding to the given
     * <tt>member</tt>.
     *
     * @param member the <tt>ConferenceMember</tt>, which panel to remove
     */
    private void removeConferenceMemberPanel(ConferenceMember member)
    {
        ConferenceMemberPanel memberPanel = conferenceMembersPanels.get(member);

        if (memberPanel != null)
        {
            int i = 0;
            this.remove(memberPanel);
            conferenceMembersPanels.remove(member);
            member.removePropertyChangeListener(memberPanel);

            for(Map.Entry<ConferenceMember, ConferenceMemberPanel> m :
                conferenceMembersPanels.entrySet())
            {
                GridBagConstraints constraints = new GridBagConstraints();
                ConferenceMemberPanel mV = m.getValue();

                this.remove(mV);

                // Add again the member panel to this container
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 0;
                constraints.gridy = i;
                constraints.weightx = 1;
                constraints.weighty = 0;
                constraints.insets = new Insets(10, 0, 0, 0);

                this.add(mV, constraints);
                i++;
            }
        }
    }

    /**
     * Updates the sound bar level of the local user participating in the
     * conference.
     * @param level the new sound level
     */
    public void fireLocalUserSoundLevelChanged(int level)
    {
        this.updateSoundBar(level);
    }

    /**
     * Adds a <tt>ConferenceMemberPanel</tt> to this container when a
     * <tt>ConferenceMember</tt> has been added to the corresponding conference.
     * @param conferenceEvent the <tt>CallPeerConferenceEvent</tt> that has been
     * triggered
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        addConferenceMemberPanel(conferenceEvent.getConferenceMember());

        callPanel.refreshContainer();
    }

    /**
     * Determines whether two specific addresses refer to one and the same
     * peer/resource/contact.
     * <p>
     * <b>Warning</b>: Use the functionality sparingly because it assumes that
     * an unspecified service is equal to any service.
     * </p>
     *
     * @param a one of the addresses to be compared
     * @param b the other address to be compared to <tt>a</tt>
     * @return <tt>true</tt> if <tt>a</tt> and <tt>b</tt> name one and the same
     * peer/resource/contact; <tt>false</tt>, otherwise
     */
    private static boolean addressesAreEqual(String a, String b)
    {
        if (a.equals(b))
            return true;

        int aServiceBegin = a.indexOf('@');
        String aUserID;
        String aService;

        if (aServiceBegin > -1)
        {
            aUserID = a.substring(0, aServiceBegin);
            aService = a.substring(aServiceBegin + 1);
        }
        else
        {
            aUserID = a;
            aService = null;
        }

        int bServiceBegin = b.indexOf('@');
        String bUserID;
        String bService;

        if (bServiceBegin > -1)
        {
            bUserID = b.substring(0, bServiceBegin);
            bService = b.substring(bServiceBegin + 1);
        }
        else
        {
            bUserID = b;
            bService = null;
        }

        boolean userIDsAreEqual;

        if ((aUserID == null) || (aUserID.length() < 1))
            userIDsAreEqual = ((bUserID == null) || (bUserID.length() < 1));
        else
            userIDsAreEqual = aUserID.equals(bUserID);
        if (!userIDsAreEqual)
            return false;

        boolean servicesAreEqual;

        /*
         * It's probably a veeery long shot but it's assumed here that an
         * unspecified service is equal to any service. Such a case is, for
         * example, RegistrarLess SIP.
         */
        if (((aService == null) || (aService.length() < 1))
                || ((bService == null) || (bService.length() < 1)))
            servicesAreEqual = true;
        else
            servicesAreEqual = aService.equals(bService);
        return servicesAreEqual;
    }

    /**
     * Removes the corresponding <tt>ConferenceMemberPanel</tt> from this
     * container when a <tt>ConferenceMember</tt> has been removed from the
     * corresponding conference.
     * @param conferenceEvent the <tt>CallPeerConferenceEvent</tt> that has been
     * triggered
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {
        ConferenceMember member = conferenceEvent.getConferenceMember();

        if (callPeer.getConferenceMemberCount() == 0 && isFocusUI())
            setFocusUI(false);

        // in situation we were in two conf calls but the other one
        // is now with 2 peers (us and the his) we must show it
        // like a normal conf call peer, we are back to single focus ui
        if(isFocusUI() && !isSingleFocusUI() &&
            callPeer.getConferenceMemberCount() == 2)
        {
            setSingleFocusUI(true);
        }

        this.removeConferenceMemberPanel(member);

        callPanel.refreshContainer();
    }

    /**
     * Enables or disables the conference focus UI depending on the change.
     *
     * When a peer changes its status from focus to not focus or the reverse.
     * we must change its listeners.
     * If the peer is focus we use conference member lister, cause we will
     * receive its status and the statuses of its conference members.
     * And if it is not a focus we must listen with stream
     * sound level listener
     *
     * @param conferenceEvent the conference event
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {
        // We disable the focus UI when a callPeer looses focus. However in the
        // other direction we'll only enable it when we receive the first
        // conference member.
        if (conferenceEvent.getSourceCallPeer().equals(callPeer)
            && !callPeer.isConferenceFocus() && isFocusUI())
            setFocusUI(false);
    }

    /**
     * Paints a special background for conference focus peers.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (isFocusUI() && !isSingleFocusUI())
        {
            Graphics2D g2 = (Graphics2D) g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g2);

                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRoundRect(0, 0,
                    this.getWidth(), this.getHeight(), 20, 20);
            }
            finally
            {
                g2.dispose();
            }
        }
    }

    /**
     * Returns the listener instance and created if needed.
     * @return the conferenceMembersSoundLevelListener
     */
    ConferenceMembersSoundLevelListener getConferenceMembersSoundLevelListener()
    {
        if(conferenceMembersSoundLevelListener == null)
            conferenceMembersSoundLevelListener =
                new ConfMembersSoundLevelListener();

        return conferenceMembersSoundLevelListener;
    }

    /**
     * Returns the listener instance and created if needed.
     * @return the streamSoundLevelListener
     */
    StreamSoundLevelListener getStreamSoundLevelListener()
    {
        if(streamSoundLevelListener == null)
            streamSoundLevelListener = new StreamSoundLevelListener();
        return streamSoundLevelListener;
    }

    /**
     * Updates according sound level indicators to reflect the new member sound
     * level.
     */
    private class ConfMembersSoundLevelListener
        implements ConferenceMembersSoundLevelListener
    {
        /**
         * Delivers <tt>SoundLevelChangeEvent</tt>s on conference member
         * sound level change.
         *
         * @param event the notification event containing the list of changes.
         */
        public void soundLevelChanged(
            ConferenceMembersSoundLevelEvent event)
        {
            Map<ConferenceMember, Integer> levels = event.getLevels();

            for (Map.Entry<ConferenceMember, ConferenceMemberPanel> entry
                    : conferenceMembersPanels.entrySet())
            {
                ConferenceMember member = entry.getKey();
                int memberSoundLevel
                    = levels.containsKey(member) ? levels.get(member) : 0;

                entry.getValue().updateSoundBar(memberSoundLevel);
            }
        }
    }

    /**
     * Updates the sound bar level to fit the new stream sound level.
     */
    private class StreamSoundLevelListener
        implements SoundLevelListener
    {
        /**
         * Delivers <tt>SoundLevelChangeEvent</tt>s on stream sound level change.
         *
         * @param evt the notification event containing the list of changes.
         */
        public void soundLevelChanged(SoundLevelChangeEvent evt)
        {
            Object evtSource = evt.getSource();

            if (evtSource.equals(callPeer))
            {
                updateSoundBar(evt.getLevel());
            }
        }
    }

    /**
     * Reloads style information.
     */
    public void loadSkin()
    {
        this.setTitleBackground(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_LOCAL_USER_BACKGROUND")));

        securityStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(securityImageID)));

        if(muteStatusLabel.getIcon() != null)
            muteStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.MUTE_STATUS_ICON)));

        if(holdStatusLabel.getIcon() != null)
            holdStatusLabel.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HOLD_STATUS_ICON)));

        if(callPeerMenu != null)
            callPeerMenu.loadSkin();
    }


    /**
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible) {}

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible() { return false; }

}
