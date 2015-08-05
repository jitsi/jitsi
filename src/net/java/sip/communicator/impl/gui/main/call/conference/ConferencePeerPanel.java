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
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import net.java.sip.communicator.util.call.CallPeerAdapter;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.service.resources.*;

/**
 * Depicts a single <tt>CallPeer</tt> who participates in a telephony conference
 * and is not a focus or the local user/peer (identified by a specific
 * <tt>Call</tt> instance).
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ConferencePeerPanel
    extends BasicConferenceParticipantPanel<Object>
    implements ConferenceCallPeerRenderer,
               Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Call</tt> which represents the local peer depicted by this
     * instance. If <tt>null</tt>, then this instance does not depict the local
     * peer and depicts {@link #callPeer}.
     */
    private final Call call;

    /**
     * The <tt>CallPeer</tt> depicted by this instance. If <tt>null</tt>, then
     * this instance depicts the local peer represented by {@link #call}.
     */
    private final CallPeer callPeer;

    /**
     * The <tt>CallPeerAdapter</tt> which implements common
     * <tt>CallPeer</tt>-related listeners on behalf of this instance.
     */
    private final CallPeerAdapter callPeerAdapter;

    /**
     * The tools menu available for each peer.
     */
    private CallPeerMenu callPeerMenu;

    /**
     * The DTMF label.
     */
    private final JLabel dtmfLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel holdStatusLabel = new JLabel();

    /**
     * The label showing whether the voice has been set to mute.
     */
    private final JLabel muteStatusLabel = new JLabel();

    /**
     * The component showing the security details.
     */
    private SecurityPanel<?> securityPanel;

    /**
     * The <tt>SoundLevelListener</tt> which listens to the changes in the
     * audio/sound level of the model of this instance. If {@link #callPeer} is
     * non-<tt>null</tt>, <tt>callPeer</tt> is the model of this instance i.e.
     * <tt>soundLevelListener</tt> will be added to the audio stream of
     * <tt>callPeer</tt> and will listen to local calculations of the audio
     * levels of the remote peer; otherwise, {@link #call} is the model of this
     * instance i.e. <tt>soundLevelListener</tt> will be added to <tt>call</tt>
     * and will listen to local calculations of the audio levels of the local
     * peer.
     */
    private final SoundLevelListenerImpl soundLevelListener
        = new SoundLevelListenerImpl();

    /**
     * Initializes a new <tt>ConferencePeerPanel</tt> which is to depict the
     * local peer represented by a specific <tt>Call</tt> on behalf of a
     * specific <tt>BasicConferenceCallPanel</tt> i.e. <tt>CallRenderer</tt>.
     *
     * @param callRenderer the <tt>BasicConferenceCallPanel</tt> which requests
     * the initialization of the new instance and which will use the new
     * instance to depict the local peer represented by the specified
     * <tt>Call</tt>
     * @param call the <tt>Call</tt> which represents the local peer to be
     * depicted by the new instance
     * @param video <tt>true</tt> if the new instance will be associated with a
     * display of video (e.g. which will be streaming to the <tt>CallPeer</tt>s
     * associated with the specified <tt>call</tt>); otherwise, <tt>false</tt>
     * @throws NullPointerException if <tt>call</tt> is <tt>null</tt>
     */
    public ConferencePeerPanel(
            BasicConferenceCallPanel callRenderer,
            Call call,
            boolean video)
    {
        super(callRenderer, call, video);

        if (call == null)
            throw new NullPointerException("call");

        this.call = call;
        this.callPeer = null;

        callPeerAdapter = null;

        String globalDisplayName = null;
        // Try to set the same image as the one in the main window. This way
        // we improve our chances to have an image, instead of looking only at
        // the protocol provider avatar, which could be null, we look for any
        // image coming from one of our accounts.
        GlobalDisplayDetailsService displayDetailsService
            = GuiActivator.getGlobalDisplayDetailsService();

        if(displayDetailsService != null)
        {
            byte[] globalAccountImage
                = displayDetailsService.getGlobalDisplayAvatar();

            if((globalAccountImage != null) && (globalAccountImage.length > 0))
                setPeerImage(globalAccountImage);

            globalDisplayName = displayDetailsService.getGlobalDisplayName();
        }

        ResourceManagementService resources = GuiActivator.getResources();

        setPeerName(
                (globalDisplayName != null && globalDisplayName.length() > 0)
                ? globalDisplayName
                    + " ("
                    + call.getProtocolProvider().getAccountID().getDisplayName()
                    + ")"
                : call.getProtocolProvider().getAccountID().getDisplayName());

        setTitleBackground(
                video
                    ? Color.DARK_GRAY
                    : new Color(
                            resources.getColor(
                                    "service.gui.CALL_LOCAL_USER_BACKGROUND")));

        if(isSoundLevelIndicatorEnabled())
            call.addLocalUserSoundLevelListener(soundLevelListener);
    }

    /**
     * Initializes a new <tt>ConferencePeerPanel</tt> which is to depict a
     * specific <tt>CallPeer</tt> on behalf of a specific
     * <tt>BasicConferenceCallPanel</tt> i.e. <tt>CallRenderer</tt>.
     *
     * @param callRenderer the <tt>BasicConferenceCallPanel</tt> which requests
     * the initialization of the new instance and which will use the new
     * instance to depict the specified <tt>CallPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> to be depicted by the new instance
     */
    public ConferencePeerPanel(
            BasicConferenceCallPanel callRenderer,
            CallPeer callPeer)
    {
        this(callRenderer, callPeer, false);
    }

    /**
     * Initializes a new <tt>ConferencePeerPanel</tt> which is to depict a
     * specific <tt>CallPeer</tt> on behalf of a specific
     * <tt>BasicConferenceCallPanel</tt> i.e. <tt>CallRenderer</tt>.
     *
     * @param callRenderer the <tt>BasicConferenceCallPanel</tt> which requests
     * the initialization of the new instance and which will use the new
     * instance to depict the specified <tt>CallPeer</tt>
     * @param callPeer the <tt>CallPeer</tt> to be depicted by the new instance
     * @param video <tt>true</tt> if the new instance will be associated with a
     * display of video (e.g. which will be streaming from the specified
     * <tt>callPeer</tt>); otherwise, <tt>false</tt>
     * @throws NullPointerException if <tt>callPeer</tt> is <tt>null</tt>
     */
    public ConferencePeerPanel(
            BasicConferenceCallPanel callRenderer,
            CallPeer callPeer,
            boolean video)
    {
        super(callRenderer, callPeer, video);

        if (callPeer == null)
            throw new NullPointerException("callPeer");

        this.call = null;
        this.callPeer = callPeer;

        securityPanel = SecurityPanel.create(this, callPeer, null);

        setMute(callPeer.isMute());

        setPeerImage(CallManager.getPeerImage(callPeer));
        setPeerName(callPeer.getDisplayName());

        // We initialize the status bar for call peers only.
        initStatusBar(callPeer);

        callPeerMenu = new CallPeerMenu(callPeer, callRenderer);

        SIPCommMenuBar menuBar = new SIPCommMenuBar();

        menuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        menuBar.add(callPeerMenu);
        addToNameBar(menuBar);

        setTitleBackground(
                video
                    ? Color.DARK_GRAY
                    : new Color(
                            GuiActivator.getResources().getColor(
                                    "service.gui.CALL_PEER_NAME_BACKGROUND")));

        initSecuritySettings();

        callPeerAdapter = new CallPeerAdapter(this.callPeer, this);

        if(isSoundLevelIndicatorEnabled())
            this.callPeer.addStreamSoundLevelListener(soundLevelListener);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        if (callPeerAdapter != null)
            callPeerAdapter.dispose();
        if (callPeer != null)
        {
            callPeer.removeConferenceMembersSoundLevelListener(
                    soundLevelListener);
            callPeer.removeStreamSoundLevelListener(soundLevelListener);
        }
        if (call != null)
            call.removeLocalUserSoundLevelListener(soundLevelListener);
    }

    /**
     * Gets the <tt>Call</tt> associated with this instance. If this instance
     * depicts the local peer, it was initialized with a specific <tt>Call</tt>
     * which represents the local peer. If this instance depicts an actual
     * <tt>CallPeer</tt>, its associated <tt>Call</tt> is returned.
     *
     * @return the <tt>Call</tt> associated with this instance
     */
    public Call getCall()
    {
        return (callPeer == null) ? call : callPeer.getCall();
    }

    /**
     * Returns <tt>CallPeer</tt> contact address.
     *
     * @return <tt>CallPeer</tt> contact address
     */
    public String getCallPeerContactAddress()
    {
        return callPeer.getURI();
    }

    /**
     * {@inheritDoc}
     *
     * Returns this instance.
     */
    public Component getComponent()
    {
        return this;
    }

    /**
     * Initializes the security settings for this call peer.
     */
    private void initSecuritySettings()
    {
        securityStatusLabel.setSecurityOff();

        CallPeerSecurityStatusEvent securityEvent
            = callPeer.getCurrentSecuritySettings();

        if (securityEvent != null
            && securityEvent instanceof CallPeerSecurityOnEvent)
        {
            CallPeerSecurityOnEvent securityOnEvt
                = (CallPeerSecurityOnEvent) securityEvent;

            securityOn(securityOnEvt);
        }

        securityStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 5, 2, 5));

        securityStatusLabel.addMouseListener(new MouseAdapter()
        {
            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                setSecurityPanelVisible(
                        !getCallPanel().getCallWindow().getFrame()
                                .getGlassPane().isVisible());
            }
        });
    }

    /**
     * Initializes the status bar component for the given <tt>callPeer</tt>.
     *
     * @param callPeer the underlying peer, which status would be displayed
     */
    private void initStatusBar(CallPeer callPeer)
    {
        initSecurityStatusLabel();
        this.setParticipantState(callPeer.getState().getLocalizedStateString());

        this.addToStatusBar(holdStatusLabel);
        this.addToStatusBar(muteStatusLabel);
        this.addToStatusBar(dtmfLabel);
    }

    /**
     * Indicates if the local video component is currently visible.
     *
     * @return <tt>true</tt> if the local video component is currently visible,
     * <tt>false</tt> - otherwise
     */
    public boolean isLocalVideoVisible()
    {
        return false;
    }

    /**
     * Determines whether the indicator which depicts the sound/audio levels (of
     * the local or remote peer in a call) is to be enabled. For example, the
     * indicator may be disabled for performance-related reasons.
     *
     * @return <tt>true</tt> if the indicator which depicts the sound/audio
     * levels (of the local or remote peer in a call) is to be enabled;
     * otherwise, <tt>false</tt>
     */
    static boolean isSoundLevelIndicatorEnabled()
    {
        return
            !GuiActivator.getConfigurationService().getBoolean(
                    "net.java.sip.communicator.impl.gui.main.call"
                        + ".DISABLE_SOUND_LEVEL_INDICATORS",
                    false);
    }

    /**
     * Reloads style information.
     */
    @Override
    public void loadSkin()
    {
        setTitleBackground(
                new Color(
                        GuiActivator.getResources().getColor(
                                "service.gui.CALL_LOCAL_USER_BACKGROUND")));

        if(muteStatusLabel.getIcon() != null)
        {
            muteStatusLabel.setIcon(
                    new ImageIcon(
                            ImageLoader.getImage(
                                    ImageLoader.MUTE_STATUS_ICON)));
        }
        if(holdStatusLabel.getIcon() != null)
        {
            holdStatusLabel.setIcon(
                    new ImageIcon(
                            ImageLoader.getImage(
                                    ImageLoader.HOLD_STATUS_ICON)));
        }
        if(callPeerMenu != null)
            callPeerMenu.loadSkin();
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
     * Re-dispatches glass pane mouse events only in case they occur on the
     * security panel.
     *
     * @param glassPane the glass pane
     * @param e the mouse event in question
     */
    private void redispatchMouseEvent(Component glassPane, MouseEvent e)
    {
        Point glassPanePoint = e.getPoint();
        Point securityPanelPoint
            = SwingUtilities.convertPoint(
                    glassPane,
                    glassPanePoint,
                    securityPanel);

        Component component;
        Point componentPoint;

        if (securityPanelPoint.y > 0)
        {
            component = securityPanel;
            componentPoint = securityPanelPoint;
        }
        else
        {
            Container contentPane
                = getCallPanel().getCallWindow().getFrame().getContentPane();
            Point containerPoint
                = SwingUtilities.convertPoint(
                        glassPane,
                        glassPanePoint,
                        contentPane);

            component
                = SwingUtilities.getDeepestComponentAt(
                        contentPane,
                        containerPoint.x,
                        containerPoint.y);
            componentPoint
                = SwingUtilities.convertPoint(
                        contentPane,
                        glassPanePoint,
                        component);
        }

        if (component != null)
        {
            component.dispatchEvent(
                    new MouseEvent(
                            component,
                            e.getID(),
                            e.getWhen(),
                            e.getModifiers(),
                            componentPoint.x,
                            componentPoint.y,
                            e.getClickCount(),
                            e.isPopupTrigger()));
        }

        e.consume();
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
    {

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
            securityPanel.securityOff(evt);
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

        // If the securityOn is called without a specific event, we'll just call
        // the super and we'll return.
        if (evt == null)
            return;

        SrtpControl srtpControl = evt.getSecurityController();

        // In case this is the local peer.
        if (securityPanel == null)
            return;

        // if we have some other panel, using other control
        if (securityPanel.getSecurityControl() == null
            || !srtpControl.getClass().isInstance(
                securityPanel.getSecurityControl()))
        {
            setSecurityPanelVisible(false);

            securityPanel
                = SecurityPanel.create(this, callPeer, srtpControl);
        }

        securityPanel.securityOn(evt);

        boolean isSecurityLowPriority = Boolean.parseBoolean(
            GuiActivator.getResources().getSettingsString(
                "impl.gui.I_DONT_CARE_THAT_MUCH_ABOUT_SECURITY"));

        if (srtpControl instanceof ZrtpControl
            && !((ZrtpControl) srtpControl).isSecurityVerified()
            && !isSecurityLowPriority)
        {
            setSecurityPanelVisible(true);
        }
    }

    /**
     * Indicates that the security status is pending confirmation.
     */
    @Override
    public void securityPending()
    {
        super.securityPending();
    }

    /**
     * Indicates that the security is timeouted, is not supported by the
     * other end.
     * @param evt Details about the event that caused this message.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent evt)
    {
        if(Boolean.parseBoolean(GuiActivator.getResources()
                .getSettingsString("impl.gui.PARANOIA_UI")))
        {
            try
            {
                CallPeer peer = (CallPeer) evt.getSource();
                OperationSetBasicTelephony<?> telephony
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetBasicTelephony.class);

                telephony.hangupCallPeer(
                    peer,
                    OperationSetBasicTelephony.HANGUP_REASON_ENCRYPTION_REQUIRED,
                    "Encryption Required!");
            }
            catch(OperationFailedException ex)
            {
                Logger.getLogger(getClass())
                    .error("Failed to hangup peer", ex);
            }
        }
    }

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    @Override
    public void setErrorReason(String reason)
    {
        super.setErrorReason(reason);
    }

    /**
     * Shows/hides the local video component.
     *
     * @param isVisible <tt>true</tt> to show the local video, <tt>false</tt> -
     * otherwise
     */
    public void setLocalVideoVisible(boolean isVisible) {}


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
            setParticipantImage(icon);
    }

    /**
     * Sets the name of the peer.
     * @param name the name of the peer
     */
    public void setPeerName(String name)
    {
        setParticipantName(name);
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
        this.setParticipantState(stateString);
    }

    /**
     * Shows/hides the security panel.
     *
     * @param isVisible <tt>true</tt> to show the security panel, <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean isVisible)
    {
        final JFrame callFrame = getCallPanel().getCallWindow().getFrame();
        final JPanel glassPane = (JPanel) callFrame.getGlassPane();

        if (!isVisible)
        {
            // Need to hide the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(false);
            glassPane.setVisible(false);
            glassPane.removeAll();
        }
        else
        {
            glassPane.setLayout(null);
            glassPane.addMouseListener(
                new MouseListener()
                {
                    public void mouseClicked(MouseEvent e)
                    {
                        redispatchMouseEvent(glassPane, e);
                    }

                    public void mouseEntered(MouseEvent e)
                    {
                        redispatchMouseEvent(glassPane, e);
                    }

                    public void mouseExited(MouseEvent e)
                    {
                        redispatchMouseEvent(glassPane, e);
                    }

                    public void mousePressed(MouseEvent e)
                    {
                        redispatchMouseEvent(glassPane, e);
                    }

                    public void mouseReleased(MouseEvent e)
                    {
                        redispatchMouseEvent(glassPane, e);
                    }
                });

            Point securityLabelPoint = securityStatusLabel.getLocation();
            Point newPoint
                = SwingUtilities.convertPoint(
                        securityStatusLabel.getParent(),
                        securityLabelPoint.x, securityLabelPoint.y,
                        callFrame);

            securityPanel.setBeginPoint(
                new Point((int) newPoint.getX() + 15, 0));
            securityPanel.setBounds(
                0, (int) newPoint.getY() - 5, callFrame.getWidth(), 110);

            glassPane.add(securityPanel);
            // Need to show the security panel explicitly in order to keep the
            // fade effect.
            securityPanel.setVisible(true);
            glassPane.setVisible(true);

            glassPane.addComponentListener(
                new ComponentAdapter()
                {
                    /**
                     * Invoked when the component's size changes.
                     */
                    @Override
                    public void componentResized(ComponentEvent e)
                    {
                        if (glassPane.isVisible())
                        {
                            glassPane.setVisible(false);
                            callFrame.removeComponentListener(this);
                        }
                    }
                });
        }
    }

    /**
     * Implements the various types of listeners which get notified about
     * changes in the sound/audio levels of the model of this
     * <tt>ConferencePeerPanel</tt> and updates its sound level indicator.
     */
    private class SoundLevelListenerImpl
        implements ConferenceMembersSoundLevelListener,
                   SoundLevelListener
    {
        /**
         * {@inheritDoc}
         */
        public void soundLevelChanged(ConferenceMembersSoundLevelEvent ev)
        {
            /*
             * If the callPeer depicted by this ConferencePeerPanel instance is
             * represented as a ConferenceMember, update the sound level
             * indicator of this ConferencePeerPanel instance with the specified
             * sound level (value).
             */
            for (Map.Entry<ConferenceMember,Integer> e
                    : ev.getLevels().entrySet())
            {
                if (CallManager.addressesAreEqual(
                        e.getKey().getAddress(),
                        callPeer.getAddress()))
                {
                    updateSoundBar(e.getValue());
                    break;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void soundLevelChanged(Object source, int level)
        {
            if (source.equals(participant))
            {
                /*
                 * If the remote peer is a conference focus and there is at
                 * least one other member (i.e. different than the remote peer
                 * and the local peer/user), we expect the remote peer to send
                 * us CSRC-based audio levels. Otherwise, the stream-based audio
                 * levels may conflict with the CSRC-based audio levels.
                 */
                if (callPeer != null)
                {
                    int conferenceMemberCount
                        = callPeer.getConferenceMemberCount();

                    if (conferenceMemberCount > 2)
                        return;
                }

                updateSoundBar(level);
            }
        }
    }
}
