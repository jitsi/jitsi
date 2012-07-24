/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.call.CallPeerAdapter;
import net.java.sip.communicator.impl.gui.main.presence.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
// disambiguation

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
    private final ConferenceCallPanel callRenderer;

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
    private SecurityPanel<?> securityPanel;

    /**
     * The call peer adapter.
     */
    private CallPeerAdapter callPeerAdapter;

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
     * @param callContainer the call panel containing this peer panel
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for the
     * call
     * @param isVideo indicates if the video interface is enabled
     */
    public ConferencePeerPanel( ConferenceCallPanel callRenderer,
                                CallPanel callContainer,
                                ProtocolProviderService protocolProvider,
                                boolean isVideo)
    {
        super(callRenderer, true, isVideo);

        this.callRenderer = callRenderer;
        this.callPanel = callContainer;
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

        if (isVideo)
            this.setTitleBackground(Color.DARK_GRAY);
        else
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
     * @param videoHandler the video handler
     * @param isVideo indicates if the video interface is enabled
     */
    public ConferencePeerPanel( ConferenceCallPanel callRenderer,
                                CallPanel callContainer,
                                CallPeer callPeer,
                                boolean isVideo)
    {
        super(callRenderer, false, isVideo);

        this.callRenderer = callRenderer;
        this.callPanel = callContainer;
        this.callPeer = callPeer;

        this.securityPanel = SecurityPanel.create(this, callPeer, null);

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

        if (isVideo)
            this.setTitleBackground(Color.DARK_GRAY);
        else
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

        securityStatusLabel.setBorder(
            BorderFactory.createEmptyBorder(2, 5, 2, 5));

        securityStatusLabel.setSecurityOff();

        securityStatusLabel.addMouseListener(new MouseAdapter()
        {
            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e)
            {
                setSecurityPanelVisible(!callRenderer.getCallContainer()
                    .getCallWindow().getFrame().getGlassPane().isVisible());
            }
        });
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
            securityPanel.securityOff(evt);
        }
    }

    /**
     * Indicates that the security status is pending confirmation.
     */
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
     * Returns null to indicate that there's no stream sound level listener
     * registered with this focus panel.
     */
    public ConferenceMembersSoundLevelListener
        getConferenceMembersSoundLevelListener()
    {
        return null;
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
     * Shows/hides the security panel.
     *
     * @param isVisible <tt>true</tt> to show the security panel, <tt>false</tt>
     * to hide it
     */
    public void setSecurityPanelVisible(boolean isVisible)
    {
        final JFrame callFrame = callRenderer.getCallContainer()
            .getCallWindow().getFrame();

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
            glassPane.addMouseListener(new MouseListener()
            {
                public void mouseReleased(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mousePressed(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseExited(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseEntered(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }

                public void mouseClicked(MouseEvent e)
                {
                    redispatchMouseEvent(glassPane, e);
                }
            });

            Point securityLabelPoint = securityStatusLabel.getLocation();

            Point newPoint
                = SwingUtilities.convertPoint(securityStatusLabel.getParent(),
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

            glassPane.addComponentListener(new ComponentAdapter()
            {
                /**
                 * Invoked when the component's size changes.
                 */
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
     * Re-dispatches glass pane mouse events only in case they occur on the
     * security panel.
     *
     * @param glassPane the glass pane
     * @param e the mouse event in question
     */
    private void redispatchMouseEvent(Component glassPane, MouseEvent e)
    {
        Point glassPanePoint = e.getPoint();

        Point securityPanelPoint = SwingUtilities.convertPoint(
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
                = callRenderer.getCallContainer().getCallWindow()
                                    .getFrame().getContentPane();

            Point containerPoint = SwingUtilities.convertPoint(
                                                    glassPane,
                                                    glassPanePoint,
                                                    contentPane);

            component = SwingUtilities.getDeepestComponentAt(contentPane,
                    containerPoint.x, containerPoint.y);

            componentPoint = SwingUtilities.convertPoint(contentPane,
                glassPanePoint, component);
        }

        if (component != null)
            component.dispatchEvent(new MouseEvent( component,
                                                    e.getID(),
                                                    e.getWhen(),
                                                    e.getModifiers(),
                                                    componentPoint.x,
                                                    componentPoint.y,
                                                    e.getClickCount(),
                                                    e.isPopupTrigger()));

        e.consume();
    }
}
