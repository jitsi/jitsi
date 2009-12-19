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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>ConferencePeerPanel</tt> renders a single <tt>ConferencePeer</tt>,
 * which is not a conference focus.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ConferencePeerPanel
    extends BasicConferenceParticipantPanel
    implements  CallPeerRenderer,
                CallPeerConferenceListener
{
    /**
     * The parent dialog containing this panel.
     */
    private final CallDialog callDialog;

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
     * The component showing the security details.
     */
    private SecurityPanel securityPanel;

    private CallPeerAdapter callPeerAdapter;

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
     * @param callDialog the call dialog containing this panel
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for the
     * call
     */
    public ConferencePeerPanel( CallDialog callDialog,
                                ProtocolProviderService protocolProvider)
    {
        this.callDialog = callDialog;
        this.callPeer = null;

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
     * @param callDialog the dialog, in which this panel is shown
     * @param callPeer The peer who own this UI
     */
    public ConferencePeerPanel(CallDialog callDialog, CallPeer callPeer)
    {
        this.callDialog = callDialog;
        this.callPeer = callPeer;

        this.setPeerName(callPeer.getDisplayName());

        // If we have any members we switch to the focus UI.
        if (callPeer.getConferenceMemberCount() > 0)
            setFocusUI(true);

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
    }

    /**
     * Indicates that the security has gone off.
     */
    public void securityOff()
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF)));
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

        if ((securityPanel == null) && (callPeer != null))
        {
            securityPanel = new SecurityPanel(callPeer);

            GridBagConstraints constraints = new GridBagConstraints();

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            this.add(securityPanel, constraints);
        }
        if (securityPanel != null)
            securityPanel.refreshStates(securityString, isSecurityVerified);

        this.revalidate();
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
    public void setPeerImage(ImageIcon icon)
    {
        if (!callPeer.isConferenceFocus())
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
     * Returns the parent <tt>CallDialog</tt> containing this renderer.
     * @return the parent <tt>CallDialog</tt> containing this renderer
     */
    public CallDialog getCallDialog()
    {
        return callDialog;
    }

    /**
     * Enters in full screen view mode.
     */
    public void enterFullScreen()
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Exits from the full screen view mode by specifying the full screen window
     * previously created.
     *
     * @param fullScreenWindow the window previously shown in full screen mode
     */
    public void exitFullScreen(Window fullScreenWindow)
    {
        // TODO: Implement full screen mode for this renderer.
    }

    /**
     * Initializes the status bar component for the given <tt>callPeer</tt>.
     *
     * @param callPeer the underlying peer, which status would be displayed
     */
    private void initStatusBar(CallPeer callPeer)
    {
        this.setParticipantState(callPeer.getState().getStateString());

        this.addToStatusBar(securityStatusLabel);
        this.addToStatusBar(holdStatusLabel);
        this.addToStatusBar(muteStatusLabel);
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
        // It's already there.
        if (conferenceMembersPanels.containsKey(member))
            return;

        ConferenceMemberPanel memberPanel
            = new ConferenceMemberPanel(member);

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

        // Refresh this panel.
        this.revalidate();
        this.repaint();

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
            this.remove(memberPanel);
            conferenceMembersPanels.remove(member);
            member.removePropertyChangeListener(memberPanel);

            this.revalidate();
            this.repaint();
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
        ConferenceMember member = conferenceEvent.getConferenceMember();
        String memberAddress = member.getAddress();
        String localUserAddress
            = callPeer.getProtocolProvider().getAccountID().getAccountAddress();

        // If we're not in a focus UI, when a new member is added we switch to it.
        if (!isFocusUI())
            setFocusUI(true);

        /*
         * The local user isn't depicted by this ConferencePeerPanel and its
         * ConferenceMemberPanels.
         */
        if (!addressesAreEqual(memberAddress, localUserAddress))
            addConferenceMemberPanel(member);
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

        this.removeConferenceMemberPanel(member);
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
}
