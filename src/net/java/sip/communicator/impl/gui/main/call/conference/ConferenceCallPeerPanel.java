/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The UI showing the information of a peer in the conference
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Dilshan Amadoru
 */
public class ConferenceCallPeerPanel
        extends ParentCallPeerPanel
        implements CallPeerConferenceListener
{
    /**
     * The name of the peer
     */
    private String peerName;

    /**
     * The unique id of the peer
     */
    private String peerId;

    /**
     * The title panel.
     */
    private final CallTitlePanel callTitlePanel;

    /**
     * The sound level indicator component.
     */
    private SoundLevelIndicator soundLevelsIndicator = null;

    /**
     * The bar showing call information of a peer
     */
    Component statusBar = null;

    /**
     * The constraints to create the GridBagLayout
     */
    private final GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Creates a <tt>ConferenceCallPeerPanel</tt> by specifying the parent
     * <tt>callDialog</tt>, containing it and the corresponding
     * <tt>protocolProvider</tt>.
     *
     * @param callDialog
     * @param protocolProvider
     */
    public ConferenceCallPeerPanel( CallDialog callDialog,
                                    ProtocolProviderService protocolProvider)
    {
        super(callDialog, null);

        this.peerName
            = protocolProvider.getAccountID().getDisplayName();
        this.peerId
            = protocolProvider.getAccountID().getUserID();

        soundLevelsIndicator = createSoundLevelIndicator(protocolProvider);

        callTitlePanel = new CallTitlePanel(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_LOCAL_USER_BACKGROUND")));

        this.init();
    }

    /**
     * Creates a <tt>ConferenceCallPeerPanel</tt>, that would be contained in
     * the given <tt>callDialog</tt> and would correspond to the given
     * <tt>callPeer</tt>.
     *
     * @param callDialog the dialog, in which this panel is shown
     * @param callPeer The peer who own this UI
     */
    public ConferenceCallPeerPanel(CallDialog callDialog, CallPeer callPeer)
    {
        super(callDialog, callPeer);

        this.peerName = callPeer.getDisplayName();
        this.peerId = callPeer.getPeerID();

        soundLevelsIndicator = createSoundLevelIndicator(callPeer);

        statusBar = createStatusBar();
        callTitlePanel = new CallTitlePanel(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_PEER_NAME_BACKGROUND")));

        this.init();
    }

    private void init()
    {
        Component nameBar = createNameBar();

        callTitlePanel.setLayout(new GridBagLayout());

        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        TransparentPanel detailPanel = new TransparentPanel();
        detailPanel.setLayout(new GridBagLayout());
        detailPanel.setBackground(new Color(255, 255, 255));

        this.setLayout(new GridBagLayout());

        // Add the name bar
        if (nameBar != null)
        {
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(2, 0, 2, 0);

            callTitlePanel.add(nameBar, constraints);
        }
        // Add the image
        if (peerImage != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.LINE_START;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 22, 5, 0);

            detailPanel.add(new JLabel(peerImage), constraints);
        }
        if (soundLevelsIndicator != null)
        {
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(2, 20, 2, 20);

            detailPanel.add(soundLevelsIndicator, constraints);
        }

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.add(callTitlePanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 5, 0);

        this.add(detailPanel, constraints);
    }

    /**
     * Create the sound level indicator of the peer
     *
     * @return The sound level indicator component
     */
    private SoundLevelIndicator createSoundLevelIndicator(CallPeer callPeer)
    {
        SoundLevelIndicator indicator
            = new SoundLevelIndicator();

        callPeer.addCallPeerSoundLevelListener(indicator);

        return indicator;
    }

    /**
     * Create the sound level indicator of the given <tt>protocolProvider</tt>.
     *
     * @return The sound level indicator component
     */
    private SoundLevelIndicator createSoundLevelIndicator(
        ProtocolProviderService protocolProvider)
    {
        SoundLevelIndicator indicator
            = new SoundLevelIndicator();

        // TODO: Add a listener for the local user sound level indicator.

        return indicator;
    }

    /**
     * Creates a buttons bar from the given list of button components.
     *
     * @param buttons the list of buttons to add in the created button bar
     * @return the created button bar
     */
    private Component createButtonBar(Component[] buttons)
    {
        Container buttonBar = new TransparentPanel();

        buttonBar.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        for (Component button : buttons)
        {
            if (button != null)
                buttonBar.add(button);
        }
        return buttonBar;
    }

    /**
     * Create the name bar of the peer
     *
     * @return The name bar component
     */
    private Component createNameBar()
    {
        TransparentPanel namePanel = new TransparentPanel(new BorderLayout());
        namePanel.setBorder(BorderFactory.createEmptyBorder(0,22,0,22));

        namePanel.add(nameLabel, BorderLayout.WEST);

        nameLabel.setText(peerName);

        if (statusBar != null)
            namePanel.add(statusBar, BorderLayout.EAST);

        return namePanel;
    }

    /**
     * Create the status bar of the peer
     *
     * @return The status bar component
     */
    private Component createStatusBar()
    {
        if (callPeer != null)
            callStatusLabel.setText(callPeer.getState().getStateString());

        TransparentPanel statusPanel
            = new TransparentPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0));

        statusPanel.add(securityStatusLabel);
        statusPanel.add(muteStatusLabel);
        statusPanel.add(callStatusLabel);

        Component[] buttons = new Component[]
            {
                createTransferCallButton()
            };

        Component buttonBar = createButtonBar(buttons);

        statusPanel.add(buttonBar);

        return statusPanel;
    }

    /**
     * Get id of the peer
     *
     * @return peer id
     */
    public String getPeerId()
    {
        return peerId;
    }

    /**
     * Paints the background of this panel.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
        }
        finally
        {
            g.dispose();
        }
    }

    public void conferenceFocusChanged(CallPeerConferenceEvent conferenceEvent)
    {
        // We're adding the up-coming members on conferenceMemberAdded, so for
        // now we have nothing to do here.
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
    public void addConferenceMemberPanel(ConferenceMember member)
    {
        ConferenceMemberPanel memberPanel
            = new ConferenceMemberPanel(member);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = getComponentCount();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 10, 5, 10);

        this.add(memberPanel, constraints);

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
    public void removeConferenceMemberPanel(ConferenceMember member)
    {
        for (int i=0; i < getComponentCount(); i++)
        {
            Component c = getComponent(i);

            if (c instanceof ConferenceMemberPanel
                && ((ConferenceMemberPanel) c)
                .getConferenceMember().equals(member))
            {
                this.remove(c);
                member.removePropertyChangeListener((ConferenceMemberPanel) c);

                break;
            }
        }

        this.revalidate();
        this.repaint();
    }
}
