/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.util.*;
import java.util.Map.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel representing a conference focus with its members.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class ConferenceFocusPanel
        extends TransparentPanel
        implements  CallPeerRenderer,
                    CallPeerConferenceListener,
                    ConferenceMembersSoundLevelListener
{
    /**
     * The parent dialog containing this panel.
     */
    private final CallDialog callDialog;

    /**
     * The call peer corresponding, which is the focus of a conference.
     */
    private final CallPeer callPeer;

    /**
     * The call peer adapter managing all call peer common listeners.
     */
    private CallPeerAdapter callPeerAdapter;

    /**
     * The panel containing the title of the participant.
     */
    private final JPanel titleBar = new CallTitlePanel(new BorderLayout());

    /**
     * The constraints to create the GridBagLayout
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * The label showing the name of the participant.
     */
    private final JLabel nameLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The component showing the status of the underlying call peer.
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The security status of the peer
     */
    private SecurityStatusLabel securityStatusLabel = new SecurityStatusLabel();

    /**
     * The component showing the security details.
     */
    private SecurityPanel securityPanel;

    /**
     * Maps a <tt>ConferenceMember</tt> to its renderer panel.
     */
    private final Hashtable<ConferenceMember, ConferenceMemberPanel>
        conferenceMembersPanels
            = new Hashtable<ConferenceMember, ConferenceMemberPanel>();

    /**
     * Creates an instance of <tt>ConferenceFocusPanel</tt> by specifying the
     * parent <tt>callDialog</tt> and the corresponding focus <tt>callPeer</tt>.
     *
     * @param callDialog the parent dialog containing this component
     * @param callPeer the underlying focus call peer
     */
    public ConferenceFocusPanel(CallDialog callDialog, CallPeer callPeer)
    {
        this.callDialog = callDialog;
        this.callPeer = callPeer;

        // Initialize this panel.
        this.init();

        // Add the UI for all contained conference members.
        for (ConferenceMember member : callPeer.getConferenceMembers())
        {
            this.addConferenceMemberPanel(member);
        }
    }

    /**
     * Initializes this call peer panel.
     */
    private void init()
    {
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        this.setLayout(new GridBagLayout());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.initTitleBar();
        this.add(titleBar, constraints);

        this.setPeerName(callPeer.getDisplayName());
        this.setPeerState(callPeer.getState().getStateString());
    }

    /**
     * Initializes the title bar.
     */
    private void initTitleBar()
    {
        Color bgColor = new Color(GuiActivator.getResources()
                        .getColor("service.gui.CALL_PEER_NAME_BACKGROUND"));
        if (bgColor != null)
            titleBar.setBackground(bgColor);

        titleBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JPanel nameBar
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        nameBar.add(nameLabel);

        CallPeerActionMenuBar actionMenuBar
            = new CallPeerActionMenuBar(callPeer);
        nameBar.add(actionMenuBar);

        titleBar.add(nameBar, BorderLayout.WEST);
        titleBar.add(createStatusBar(), BorderLayout.EAST);
    }

    /**
     * Create a status bar component for the given <tt>callPeer</tt>.
     *
     * @return the status bar component
     */
    private Component createStatusBar()
    {
        callStatusLabel.setText(callPeer.getState().getStateString());

        TransparentPanel statusBar
            = new TransparentPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0));

        statusBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        statusBar.add(callStatusLabel);
        statusBar.add(securityStatusLabel);
        statusBar.add(holdStatusLabel);
        statusBar.add(muteStatusLabel);

        Component[] buttons = new Component[]
        {
            CallPeerRendererUtils.createTransferCallButton(callPeer)
        };

        Component buttonBar
            = CallPeerRendererUtils.createButtonBar(false, buttons);

        statusBar.add(buttonBar);

        return statusBar;
    }

    /**
     * Paints the background of this panel.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g2);

            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRoundRect(
                0, 10, this.getWidth(), this.getHeight() - 10, 20, 20);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * We're adding the up-coming members on conferenceMemberAdded, so for
     * now we have nothing to do here.
     * @param conferenceEvent the conference event
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {}

    /**
     * Adds a <tt>ConferenceMemberPanel</tt> to this container when a
     * <tt>ConferenceMember</tt> has been added to the corresponding conference.
     * @param conferenceEvent the <tt>CallPeerConferenceEvent</tt> that has been
     * triggered
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        ConferenceMember member = conferenceEvent.getConferenceMember();

        this.addConferenceMemberPanel(member);
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

        this.removeConferenceMemberPanel(member);
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
        ConferenceMemberPanel memberPanel
            = new ConferenceMemberPanel(member);

        // Map the conference member to the created member panel.
        conferenceMembersPanels.put(member, memberPanel);

        // Add the member panel to this container
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = getComponentCount();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 10, 5, 10);

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
            member.removePropertyChangeListener(memberPanel);
        }

        this.revalidate();
        this.repaint();
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
     * Changes the security icon to off icon.
     */
    public void securityOff()
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_OFF)));
    }

    /**
     * Indicates that the security is turned on by specifying the
     * <tt>securityString</tt> and whether it has been already verified.
     * @param securityString the security string
     * @param isSecurityVerified indicates if the security string has been
     * already verified by the underlying <tt>CallPeer</tt>
     */
    public void securityOn(String securityString, boolean isSecurityVerified)
    {
        securityStatusLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SECURE_BUTTON_ON)));

        if (securityPanel == null)
        {
            SecurityPanel securityPanel = new SecurityPanel(callPeer);

            GridBagConstraints constraints = new GridBagConstraints();

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 0;
            constraints.gridy = 2;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 0, 0, 0);

            this.add(securityPanel, constraints);
        }

        securityPanel.refreshStates(securityString, isSecurityVerified);

        this.revalidate();
    }

    /**
     * If there's a conference member corresponding to this peer we're setting
     * the image for the member, otherwise no image is set.
     * @param icon the peer image
     */
    public void setPeerImage(ImageIcon icon) {}

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        nameLabel.setText(name + "'s conference");
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state)
    {
        callStatusLabel.setText(state);
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
        // TODO: Implement enterFullScreen mode.
    }

    /**
     * Exits from the full screen view mode by specifying the full screen window
     * previously created.
     *
     * @param fullScreenWindow the window previously shown in full screen mode
     */
    public void exitFullScreen(Window fullScreenWindow)
    {
        // TODO: Implement exitFullScreen mode.
    }

    /**
     * Updates according sound level indicators to reflect the new member sound
     * level.
     * @param event the <tt>ConferenceMembersSoundLevelEvent</tt> that notified
     * us
     */
    public void membersSoundLevelChanged(ConferenceMembersSoundLevelEvent event)
    {
        Map<ConferenceMember, Integer> levels = event.getLevels();

        Iterator<Entry<ConferenceMember, ConferenceMemberPanel>>
            memberPanelsIter = conferenceMembersPanels.entrySet().iterator();

        while (memberPanelsIter.hasNext())
        {
            Map.Entry<ConferenceMember, ConferenceMemberPanel>
                 entry = memberPanelsIter.next();

            ConferenceMember member = entry.getKey();
            ConferenceMemberPanel memberPanel = entry.getValue();

            if (levels.containsKey(member))
                memberPanel.updateSoundBar(levels.get(member));
            else
                memberPanel.updateSoundBar(0);
        }
    }
}
