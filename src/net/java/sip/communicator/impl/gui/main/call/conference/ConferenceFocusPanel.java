/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 *
 *
 * @author Yana Stamcheva
 */
public class ConferenceFocusPanel
    extends TransparentPanel
    implements  ConferenceCallPeerRenderer,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The peer corresponding to the focus.
     */
    private final CallPeer focusPeer;

    /**
     * The renderer corresponding to the parent call call.
     */
    private final ConferenceCallPanel callRenderer;

    /**
     * The call panel.
     */
    private final CallPanel callPanel;

    /**
     * A mapping of a member and its renderer.
     */
    private final Map<ConferenceMember, ConferenceMemberPanel>
        conferenceMembersPanels
            = new Hashtable<ConferenceMember, ConferenceMemberPanel>();

    /**
     * Listens for sound level events on the conference members.
     */
    private ConferenceMembersSoundLevelListener
        conferenceMembersSoundLevelListener = null;

    /**
     * The panel of the focus peer.
     */
    private ConferencePeerPanel focusPeerPanel;

    /**
     * The video handler for this peer.
     */
    private UIVideoHandler videoHandler;

    /**
     * Creates an instance of <tt>ConferenceFocusPanel</tt> by specifying the
     * parent call renderer, the call panel and the peer represented by this
     * conference focus panel.
     *
     * @param callRenderer the parent call renderer
     * @param callPanel the call panel
     * @param callPeer the peer represented by this focus panel
     * @param videoHandler the video handler
     */
    public ConferenceFocusPanel(ConferenceCallPanel callRenderer,
                                CallPanel callPanel,
                                CallPeer callPeer,
                                UIVideoHandler videoHandler)
    {
        this.focusPeer = callPeer;
        this.callRenderer = callRenderer;
        this.callPanel = callPanel;
        this.videoHandler = videoHandler;

        this.setLayout(new GridBagLayout());

        // First add the focus peer.
        addFocusPeerPanel();

        for (ConferenceMember member : callPeer.getConferenceMembers())
        {
            addConferenceMemberPanel(member);
        }
    }

    /**
     * Adds the focus peer panel.
     */
    public void addFocusPeerPanel()
    {
        focusPeerPanel
            = new ConferencePeerPanel(
                callRenderer, callPanel, focusPeer, videoHandler);

        GridBagConstraints constraints = new GridBagConstraints();

        // Add the member panel to this container
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 3, 0);

        this.add(focusPeerPanel, constraints);
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
        String localUserAddress
            = focusPeer.getProtocolProvider().getAccountID().
                getAccountAddress();

        boolean isLocalMember
            = addressesAreEqual(member.getAddress(), localUserAddress);

        // We don't want to add the local member to the list of members.
        if (isLocalMember)
            return;

        if (addressesAreEqual(member.getAddress(), focusPeer.getAddress()))
            return;

        // It's already there.
        if (conferenceMembersPanels.containsKey(member))
            return;

        ConferenceMemberPanel memberPanel
            = new ConferenceMemberPanel(callRenderer, member);

        member.addPropertyChangeListener(memberPanel);

        // Map the conference member to the created member panel.
        conferenceMembersPanels.put(member, memberPanel);

        GridBagConstraints constraints = new GridBagConstraints();

        // Add the member panel to this container
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = getComponentCount();
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 3, 0);

        this.add(memberPanel, constraints);

        initSecuritySettings();
    }

    /**
     * Removes the <tt>ConferenceMemberPanel</tt> corresponding to the given
     * <tt>member</tt>.
     *
     * @param member the <tt>ConferenceMember</tt>, which panel to remove
     */
    public void removeConferenceMemberPanel(ConferenceMember member)
    {
        Component memberPanel = conferenceMembersPanels.get(member);

        if (memberPanel != null)
        {
            int i = 0;
            this.remove(memberPanel);
            conferenceMembersPanels.remove(member);

            if (!addressesAreEqual(member.getAddress(), focusPeer.getAddress()))
                member.removePropertyChangeListener(
                    (ConferenceMemberPanel) memberPanel);

            for(Map.Entry<ConferenceMember, ConferenceMemberPanel> m :
                conferenceMembersPanels.entrySet())
            {
                GridBagConstraints constraints = new GridBagConstraints();
                Component mV = m.getValue();

                this.remove(mV);

                // Add again the member panel to this container
                constraints.fill = GridBagConstraints.BOTH;
                constraints.gridx = 0;
                constraints.gridy = i;
                constraints.weightx = 1;
                constraints.weighty = 0;
                constraints.insets = new Insets(0, 0, 3, 0);

                this.add(mV, constraints);
                i++;
            }
        }
    }

    /**
     * Overrides {@link JComponent#paintComponent(Graphics)} in order to
     * customize the background of this panel.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.setColor(Color.DARK_GRAY);
            g.drawLine(0, 0, getWidth(), 0);
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Reloads default avatar icon.
     */
    public void loadSkin() {}

    /**
     * Sets the name of the peer.
     *
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        focusPeerPanel.setPeerName(name);
    }

    /**
     * Sets the <tt>image</tt> of the peer.
     *
     * @param image the image to set
     */
    public void setPeerImage(byte[] image)
    {
        focusPeerPanel.setPeerImage(image);
    }

    /**
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param state the state of the contained call peer
     */
    public void setPeerState(String state)
    {
        focusPeerPanel.setPeerState(state);
    }

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     *
     * @param reason the reason of the error to set
     */
    public void setErrorReason(String reason)
    {
        focusPeerPanel.setErrorReason(reason);
    }

    /**
     * Sets the mute property value.
     *
     * @param isMute indicates if the call with this peer is
     * muted
     */
    public void setMute(boolean isMute)
    {
        focusPeerPanel.setMute(isMute);
    }

    /**
     * Sets the "on hold" property value.
     *
     * @param isOnHold indicates if the call with this peer is put on hold
     */
    public void setOnHold(boolean isOnHold)
    {
        focusPeerPanel.setOnHold(isOnHold);
    }

    /**
     * Indicates that the security is turned on.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        focusPeerPanel.securityOn(evt);
        for (ConferenceMemberPanel member : conferenceMembersPanels.values())
        {
            member.securityOn(evt);
        }
    }

    /**
     * Indicates that the security is turned off.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        focusPeerPanel.securityOff(evt);
        for (ConferenceMemberPanel member : conferenceMembersPanels.values())
        {
            member.securityOff(evt);
        }
    }

    /**
     * Sets the call peer adapter that manages all related listeners.
     *
     * @param adapter the call peer adapter
     */
    public void setCallPeerAdapter(CallPeerAdapter adapter)
    {
        focusPeerPanel.setCallPeerAdapter(adapter);
    }

    /**
     * Returns the call peer adapter that manages all related listeners.
     *
     * @return the call peer adapter
     */
    public CallPeerAdapter getCallPeerAdapter()
    {
        return focusPeerPanel.getCallPeerAdapter();
    }

    /**
     * Prints the given DTMG character through this <tt>CallPeerRenderer</tt>.
     *
     * @param dtmfChar the DTMF char to print
     */
    public void printDTMFTone(char dtmfChar)
    {
        focusPeerPanel.printDTMFTone(dtmfChar);
    }

    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     *
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel()
    {
        return callPanel;
    }

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
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible)
    {
        focusPeerPanel.setLocalVideoVisible(isVisible);
    }

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible()
    {
        return focusPeerPanel.isLocalVideoVisible();
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
        addConferenceMemberPanel(conferenceMember);

        callPanel.refreshContainer();
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
        removeConferenceMemberPanel(conferenceMember);

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

        int aProtocolIndex = a.indexOf(':');
        if(aProtocolIndex > -1)
            a = a.substring(aProtocolIndex + 1);

        int bProtocolIndex = b.indexOf(':');
        if(bProtocolIndex > -1)
            b = b.substring(bProtocolIndex + 1);

        if (a.equals(b))
            return true;

        int aServiceBegin = a.indexOf('@');
        String aUserID;
        String aService;

        if (aServiceBegin > -1)
        {
            aUserID = a.substring(0, aServiceBegin);

            int slashIndex = a.indexOf("/");
            if (slashIndex > 0)
                aService = a.substring(aServiceBegin + 1, slashIndex);
            else
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
            int slashIndex = b.indexOf("/");

            if (slashIndex > 0)
                bService = b.substring(bServiceBegin + 1, slashIndex);
            else
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
    public SoundLevelListener getStreamSoundLevelListener()
    {
        return focusPeerPanel.getStreamSoundLevelListener();
    }

    /**
     * Initializes security.
     */
    private void initSecuritySettings()
    {
        CallPeerSecurityStatusEvent securityEvent
            = focusPeer.getCurrentSecuritySettings();

        if (securityEvent instanceof CallPeerSecurityOnEvent)
        {
            securityOn((CallPeerSecurityOnEvent) securityEvent);
        }
    }

    /**
     * Returns the video handler associated with this call peer renderer.
     *
     * @return the video handler associated with this call peer renderer
     */
    public UIVideoHandler getVideoHandler()
    {
        return videoHandler;
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

            for(Map.Entry<ConferenceMember, Integer> e : levels.entrySet())
            {
                ConferenceMember key = e.getKey();
                Integer value = e.getValue();
                Contact contact = focusPeerPanel.getCallPeerContact();

                if(key.getAddress().equals(
                    contact.getAddress()))
                {
                    focusPeerPanel.updateSoundBar(value);
                    break;
                }
            }

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
}
