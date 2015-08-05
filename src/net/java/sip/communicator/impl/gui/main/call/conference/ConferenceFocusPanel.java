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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.protocol.event.*;

/**
 * Depicts a specific <tt>CallPeer</tt> who is a focus of a telephony conference
 * and the <tt>ConferenceMember</tt>s whom the specified <tt>CallPeer</tt> is
 * acting as a conference focus of.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Hristo Terezov
 */
public class ConferenceFocusPanel
    extends TransparentPanel
    implements ConferenceCallPeerRenderer,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>BasicConferenceCallPanel</tt> which initialized this instance and
     * which uses it to depict {@link #focusPeer}.
     */
    private final BasicConferenceCallPanel callRenderer;

    /*
     * XXX The conferenceMemberPanels field is modified by various threads
     * without any synchronization whatsoever.
     */
    /**
     * The <tt>ConferenceMemberPanel</tt>s which depict the
     * <tt>ConferenceMember</tt>s of {@link #focusPeer}. Mapped by their
     * respective <tt>ConferenceMember</tt> instances for optimized access.
     */
    private final Map<ConferenceMember, ConferenceMemberPanel>
        conferenceMemberPanels
            = new Hashtable<ConferenceMember, ConferenceMemberPanel>();

    private final GridBagConstraints cnstrnts;

    /**
     * The <tt>CallPeer</tt> depicted by this instance.
     */
    private final CallPeer focusPeer;

    private final FocusPeerListener focusPeerListener = new FocusPeerListener();

    /**
     * The <tt>ConferencePeerPanel</tt> which depicts {@link #focusPeer} without
     * the <tt>ConferenceMember</tt>s which participate in the telephony
     * conference that it is the focus of.
     */
    private ConferencePeerPanel focusPeerPanel;

    /**
     * Initializes a new <tt>ConferenceFocusPanel</tt> which is to depict a
     * specific <tt>CallPeer</tt> on behalf of a specific
     * <tt>BasicConferenceCallPanel</tt> i.e. <tt>CallRenderer</tt>.
     *
     * @param callRenderer the <tt>BasicConferenceCallPanel</tt> which requests
     * the initialization of the new instance and which will use the new
     * instance to depict the specified <tt>CallPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> to be depicted by the new instance
     */
    public ConferenceFocusPanel(
            BasicConferenceCallPanel callRenderer,
            CallPeer callPeer)
    {
        super(new GridBagLayout());

        this.focusPeer = callPeer;
        this.callRenderer = callRenderer;

        cnstrnts = new GridBagConstraints();
        cnstrnts.fill = GridBagConstraints.BOTH;
        cnstrnts.gridx = 0;
        cnstrnts.gridy = 0;
        cnstrnts.insets = new Insets(0, 0, 3, 0);
        cnstrnts.weightx = 1;
        cnstrnts.weighty = 0;
        /*
         * Add the user interface which will depict the focusPeer without the
         * ConferenceMembers.
         */
        addFocusPeerPanel();

        this.focusPeer.addCallPeerConferenceListener(focusPeerListener);
        if (ConferencePeerPanel.isSoundLevelIndicatorEnabled())
        {
            this.focusPeer.addConferenceMembersSoundLevelListener(
                    focusPeerListener);
        }

        for (ConferenceMember conferenceMember
                : this.focusPeer.getConferenceMembers())
        {
            addConferenceMemberPanel(conferenceMember);
        }
    }

    /**
     * Adds a <tt>ConferenceMemberPanel</tt> to depict a specific
     * <tt>ConferenceMember</tt> if there is not one yet.
     *
     * @param conferenceMember the <tt>ConferenceMember</tt> to be depicted
     */
    private void addConferenceMemberPanel(ConferenceMember conferenceMember)
    {
        /*
         * The local user/peer should not be depicted by a ConferenceMemberPanel
         * because it is not the responsibility of ConferenceFocusPanel and
         * ConferenceMemberPanel.
         */
        if (CallManager.isLocalUser(conferenceMember))
            return;
        /*
         * If the focusPeer is reported as a ConferenceMember, it should be
         * depicted by focusPeerPanel only and not a ConferenceMemberPanel.
         */
        if (CallManager.addressesAreEqual(
                conferenceMember.getAddress(),
                focusPeer.getAddress()))
            return;

        /*
         * The specified ConferenceMember is already depicted by this view with
         * a ConferenceMemberPanel.
         */
        if (conferenceMemberPanels.containsKey(conferenceMember))
            return;

        ConferenceMemberPanel conferenceMemberPanel
            = new ConferenceMemberPanel(callRenderer, conferenceMember, false);

        /*
         * Remember the ConferenceMemberPanel which depicts the specified
         * ConferenceMember.
         */
        conferenceMemberPanels.put(conferenceMember, conferenceMemberPanel);

        /*
         * Add the newly-initialized ConferenceMemberPanel to the user interface
         * hierarchy of this view.
         */
        add(conferenceMemberPanel, cnstrnts);
        cnstrnts.gridy++;

        initSecuritySettings();

        packWindow();
    }

    /**
     * Adds the <tt>ConferencePeerPanel</tt> which will depict
     * {@link #focusPeer} without the <tt>ConferenceMember</tt>s which
     * participate in the telephony conference that it is the focus of.
     */
    private void addFocusPeerPanel()
    {
        focusPeerPanel = new ConferencePeerPanel(callRenderer, focusPeer);
        add(focusPeerPanel, cnstrnts);
        cnstrnts.gridy++;

        packWindow();

    }

    /**
     * Resizes the window to fit the layout.
     */
    private void packWindow()
    {
        Window window
            = SwingUtilities.getWindowAncestor(this);

        if (window != null)
            window.pack();
    }

    /**
     * Releases the resources acquired by this instance which require explicit
     * disposal (e.g. any listeners added to the depicted <tt>CallPeer</tt>.
     * Invoked by <tt>BasicConferenceCallPanel</tt> when it determines that this
     * <tt>ConferenceFocusPanel</tt> is no longer necessary.
     */
    public void dispose()
    {
        focusPeer.removeCallPeerConferenceListener(focusPeerListener);
        focusPeer.removeConferenceMembersSoundLevelListener(focusPeerListener);

        if (focusPeerPanel != null)
            focusPeerPanel.dispose();
        for (ConferenceMemberPanel conferenceMemberPanel
                : conferenceMemberPanels.values())
        {
            conferenceMemberPanel.dispose();
        }
    }

    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     *
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel()
    {
        return getCallRenderer().getCallContainer();
    }

    /**
     * Returns the parent call renderer.
     *
     * @return the parent call renderer
     */
    public SwingCallRenderer getCallRenderer()
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

    /**
     * Initializes the security settings of {@link #focusPeerPanel} and
     * {@link #conferenceMemberPanels} with the security settings of
     * {@link #focusPeer}.
     */
    private void initSecuritySettings()
    {
        CallPeerSecurityStatusEvent securityEvent
            = focusPeer.getCurrentSecuritySettings();

        if (securityEvent instanceof CallPeerSecurityOnEvent)
            securityOn((CallPeerSecurityOnEvent) securityEvent);
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
     * {@inheritDoc}
     *
     * The implementation of <tt>ConferenceFocusPanel</tt> does nothing.
     */
    public void loadSkin()
    {
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerConferenceEvent</tt>
     * which was fired by {@link #focusPeer}. The notification is brought in the
     * AWT event dispatching thread.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> which was fired by
     * {@link #focusPeer} and which this instance is notified about
     */
    protected void onCallPeerConferenceEvent(final CallPeerConferenceEvent ev)
    {
        /*
         * ConferenceFocusPanel is interested in the additions and the removals
         * of ConferenceMembers only. Because we are handling the
         * CallPeerConferenceEvents in the AWT event dispatching thread which is
         * not friendly to the garbage collection, filter out the
         * CallPeerConferenceEvents which are of no interest at this time.
         */
        switch(ev.getEventID())
        {
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
            if (SwingUtilities.isEventDispatchThread())
            {
                onCallPeerConferenceEventInEventDispatchThread(ev);
            }
            else
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                onCallPeerConferenceEventInEventDispatchThread(
                                        ev);
                            }
                        });
            }
            break;

        default:
            /*
             * As it is said above, we are not interested in all
             * CallPeerConferenceEvents.
             */
            break;
        }
    }

    /**
     * Notifies this instance about a specific <tt>CallPeerConferenceEvent</tt>
     * which was fired by {@link #focusPeer}.
     *
     * @param ev the <tt>CallPeerConferenceEvent</tt> which was fired by
     * {@link #focusPeer} and which this instance is notified about
     */
    protected void onCallPeerConferenceEventInEventDispatchThread(
            CallPeerConferenceEvent ev)
    {
        ConferenceMember conferenceMember = ev.getConferenceMember();

        switch (ev.getEventID())
        {
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
            addConferenceMemberPanel(conferenceMember);
            break;

        case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
            removeConferenceMemberPanel(conferenceMember);
            break;
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

            int height = getHeight();
            int width = getWidth();

            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.DARK_GRAY);
            g.drawLine(0, 0, width, 0);
            g.drawLine(0, height - 1, width, height - 1);
        }
        finally
        {
            g.dispose();
        }
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
     * Removes the <tt>ConferenceMemberPanel</tt> depicting a specific
     * <tt>ConferenceMember</tt>.
     *
     * @param conferenceMember the <tt>ConferenceMember</tt> whose depicting
     * <tt>ConferenceMemberPanel</tt> is to be removed
     */
    private void removeConferenceMemberPanel(ConferenceMember conferenceMember)
    {
        ConferenceMemberPanel conferenceMemberPanel
            = conferenceMemberPanels.remove(conferenceMember);

        if (conferenceMemberPanel != null)
        {
            remove(conferenceMemberPanel);
            conferenceMemberPanel.dispose();

            packWindow();
        }
    }

    /**
     * The handler for the security event received. The security event
     * for starting establish a secure connection.
     *
     * @param securityNegotiationStartedEvent
     *            the security started event received
     */
    public void securityNegotiationStarted(
        CallPeerSecurityNegotiationStartedEvent securityNegotiationStartedEvent)
    {}

    /**
     * Indicates that the security is turned off.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOff(CallPeerSecurityOffEvent evt)
    {
        focusPeerPanel.securityOff(evt);
        for (ConferenceMemberPanel member : conferenceMemberPanels.values())
            member.securityOff(evt);
    }

    /**
     * Indicates that the security is turned on.
     *
     * @param evt Details about the event that caused this message.
     */
    public void securityOn(CallPeerSecurityOnEvent evt)
    {
        focusPeerPanel.securityOn(evt);
        for (ConferenceMemberPanel member : conferenceMemberPanels.values())
            member.securityOn(evt);
    }

    /**
     * Indicates that the security status is pending confirmation.
     */
    public void securityPending()
    {
        focusPeerPanel.securityPending();
    }

    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
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
     * Sets the <tt>image</tt> of the peer.
     *
     * @param image the image to set
     */
    public void setPeerImage(byte[] image)
    {
        focusPeerPanel.setPeerImage(image);
    }

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
     * Sets the state of the contained call peer by specifying the
     * state name.
     *
     * @param oldState the previous state of the peer
     * @param newState the new state of the peer
     * @param stateString the state of the contained call peer
     */
    public void setPeerState(   CallPeerState oldState,
                                CallPeerState newState,
                                String stateString)
    {
        focusPeerPanel.setPeerState(oldState, newState, stateString);
    }

    /**
     * {@inheritDoc}
     *
     * The implementation of <tt>ConferenceFocusPanel</tt> does nothing.
     */
    public void setSecurityPanelVisible(boolean visible)
    {
    }

    /**
     * Enables or disabled video indicator in this conference participant
     * panel.
     *
     * @param confMember the <tt>ConferenceMember</tt>, which video indicator
     * we'd like to update
     * @param enable <tt>true</tt> to enable video indicator, <tt>false</tt> -
     * otherwise
     */
    public void enableVideoIndicator(   ConferenceMember confMember,
                                        boolean enable)
    {
        if (!conferenceMemberPanels.containsKey(confMember))
            return;

        ConferenceMemberPanel confMemberPanel
            = conferenceMemberPanels.get(confMember);

        confMemberPanel.enableVideoIndicator(enable);
    }

    /**
     * Enables or disabled video indicator of the <tt>focusPeerPanel</tt>.
     *
     * @param enable <tt>true</tt> to enable video indicator, <tt>false</tt> -
     * otherwise
     */
    public void enableVideoIndicator(boolean enable)
    {
        focusPeerPanel.enableVideoIndicator(enable);
    }

    /**
     * Implements the listeners which get notified about events related to the
     * <tt>CallPeer</tt> depicted by this <tt>ConferenceFocusPanel</tt> and
     * which may cause a need to update this view from its model.
     */
    private class FocusPeerListener
        extends CallPeerConferenceAdapter
        implements ConferenceMembersSoundLevelListener
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCallPeerConferenceEvent(CallPeerConferenceEvent ev)
        {
            ConferenceFocusPanel.this.onCallPeerConferenceEvent(ev);
        }

        /**
         * {@inheritDoc}
         *
         * Notifies this listener about changes in the audio/sound level-related
         * information of the <tt>ConferenceMember</tt>s of
         * {@link ConferenceFocusPanel#focusPeer}. Updates the user interface
         * which displays the audio/sound levels i.e.
         * <tt>SoundLevelIndicator</tt>.
         */
        public void soundLevelChanged(ConferenceMembersSoundLevelEvent ev)
        {
            Map<ConferenceMember, Integer> levels = ev.getLevels();

            // focusPeerPanel
            String address = focusPeerPanel.getCallPeerContactAddress();

            for(Map.Entry<ConferenceMember, Integer> e : levels.entrySet())
            {
                ConferenceMember key = e.getKey();
                Integer value = e.getValue();

                if(CallManager.addressesAreEqual(key.getAddress(), address))
                {
                    focusPeerPanel.updateSoundBar(value);
                    break;
                }
            }

            // conferenceMemberPanels
            for (Map.Entry<ConferenceMember, ConferenceMemberPanel> entry
                    : conferenceMemberPanels.entrySet())
            {
                ConferenceMember member = entry.getKey();
                Integer memberSoundLevel = levels.get(member);

                entry.getValue().updateSoundBar(
                        (memberSoundLevel == null)
                            ? 0
                            : memberSoundLevel.intValue());
            }
        }
    }
}
