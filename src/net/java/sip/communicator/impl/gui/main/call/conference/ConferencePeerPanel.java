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
    implements  ConferenceCallPeerRenderer,
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
        super(callRenderer, true);

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
        super(callRenderer, false);

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

        initSecuritySettings();
    }

    /**
     * Initializes the security settings for this call peer.
     */
    private void initSecuritySettings()
    {
        CallPeerSecurityStatusEvent securityEvent
            = callPeer.getCurrentSecuritySettings();

        if (securityEvent != null
            && securityEvent instanceof CallPeerSecurityOnEvent)
        {
            CallPeerSecurityOnEvent securityOnEvt
                = (CallPeerSecurityOnEvent) securityEvent;

            securityOn(securityOnEvt);
        }
    }

    /**
     * Indicates that the security is turned on.
     * <p>
     * Sets the secured status icon to the status panel and initializes/updates
     * the corresponding security details.
     * 
     * @param evt Details about the event that caused this message.
     */
    @Override
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        super.securityOn(evt);

        if (securityPanel == null)
        {
            securityPanel = SecurityPanel.create(evt.getSecurityController());
            securityPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            this.addToCenter(securityPanel);
        }
        securityPanel.refreshStates();

        callPanel.refreshContainer();
    }

    /**
     * Indicates that the security has gone off.
     * 
     * @param evt Details about the event that caused this message.
     */
    @Override
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        super.securityOff(evt);
        if(securityPanel != null)
        {
            securityPanel.getParent().remove(securityPanel);
        }
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

        this.addToStatusBar(holdStatusLabel);
        this.addToStatusBar(muteStatusLabel);
        this.addToStatusBar(dtmfLabel);
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
     * Returns the listener instance and created if needed.
     * @return the conferenceMembersSoundLevelListener
     */
    public ConferenceMembersSoundLevelListener
        getConferenceMembersSoundLevelListener()
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
    public StreamSoundLevelListener getStreamSoundLevelListener()
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
         * Updates the sound level bar upon stream sound level changes.
         *
         * {@inheritDoc}
         */
        public void soundLevelChanged(Object source, int level)
        {
            if (source.equals(callPeer))
                updateSoundBar(level);
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

    /**
     * Returns the parent call renderer.
     * 
     * @return the parent call renderer
     */
    public CallRenderer getCallRenderer()
    {
        return callRenderer;
    }

    /**
     * Returns the component associated with this renderer.
     *
     * @return the component associated with this renderer
     */
    public Component getComponent()
    {
        return this;
    }
}
