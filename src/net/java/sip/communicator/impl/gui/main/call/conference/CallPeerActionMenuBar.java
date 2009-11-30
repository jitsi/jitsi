/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallPeerActionMenuBar</tt> is situated in the title area of any
 * <tt>CallPeerRenderer</tt> and gives access to operations like: "Put on hold",
 * "Mute", "Hang up" and so on.
 *
 * @author Yana Stamcheva
 */
public class CallPeerActionMenuBar
    extends SIPCommMenuBar
    implements  CallPeerListener,
                PropertyChangeListener
{
    private final CallPeer callPeer;

    private final String onHoldText = GuiActivator.getResources()
        .getI18NString("service.gui.PUT_ON_HOLD");
    private final String offHoldText = GuiActivator.getResources()
        .getI18NString("service.gui.PUT_OFF_HOLD");

    private final JMenuItem holdMenuItem= new JMenuItem(onHoldText);

    private final String muteText = GuiActivator.getResources()
        .getI18NString("service.gui.MUTE");
    private final String unmuteText = GuiActivator.getResources()
        .getI18NString("service.gui.UNMUTE");

    private final JMenuItem muteMenuItem = new JMenuItem(muteText);

    /**
     * Creates a <tt>CallPeerActionMenuBar</tt> by specifying the associated
     * <tt>callPeer</tt>.
     * @param peer the <tt>CallPeer</tt> associated with the contained menus
     */
    public CallPeerActionMenuBar(CallPeer peer)
    {
        this.callPeer = peer;

        SIPCommMenu menu = new SIPCommMenu();

        menu.setPreferredSize(new Dimension(20, 20));

        this.setOpaque(false);
        menu.setOpaque(false);

        // Should explicitly remove any border in order to align correctly the
        // icon.
        menu.setBorder(BorderFactory.createEmptyBorder());
        menu.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.CALL_PEER_TOOLS)));
        menu.setIconTextGap(0);
        menu.addItem(
            GuiActivator.getResources().getI18NString("service.gui.HANG_UP"),
            null,
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    CallManager.hangupCallPeer(callPeer);
                }
            });

        initHoldMenuItem();
        initMuteMenuItem();

        menu.add(holdMenuItem);
        menu.add(muteMenuItem);

        menu.addItem(
            GuiActivator.getResources().getI18NString(
                "service.gui.TRANSFER_BUTTON_TOOL_TIP"),
            null,
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    CallManager.transferCall(callPeer);
                }
            });

        this.add(menu);

        // Add the call peer listener that would notify us for call peer state
        // changes. We'll be using these notifications in order to update the
        // hold menu item state.
        peer.addCallPeerListener(this);
        peer.addPropertyChangeListener(this);
    }

    /**
     * Initializes the hold menu item.
     */
    private void initHoldMenuItem()
    {
        holdMenuItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (holdMenuItem.getText().equals(onHoldText))
                    {
                        CallManager.putOnHold(callPeer, true);
                        holdMenuItem.setText(offHoldText);
                    }
                    else
                    {
                        CallManager.putOnHold(callPeer, false);
                        holdMenuItem.setText(onHoldText);
                    }
                }
            });
    }

    /**
     * Initializes the mute menu item.
     */
    private void initMuteMenuItem()
    {
        muteMenuItem.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (muteMenuItem.getText().equals(muteText))
                    {
                        CallManager.mute(callPeer, true);
                        muteMenuItem.setText(unmuteText);
                    }
                    else
                    {
                        CallManager.mute(callPeer, false);
                        muteMenuItem.setText(muteText);
                    }
                }
            });
    }

    /**
     * Implements <tt>{@link CallPeerListener#peerStateChanged
     * (CallPeerChangeEvent)}</tt>
     * in order to update the "Put on/off hold" menu item to fit the current
     * hold state of the call peer.
     *
     * @param evt the <tt>CallPeerChangeEvent</tt> that notified us of the state
     * change
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        CallPeerState newState = (CallPeerState) evt.getNewValue();
        CallPeerState oldState = (CallPeerState) evt.getOldValue();

        if (newState == CallPeerState.CONNECTED
            && CallPeerState.isOnHold(oldState))
        {
            holdMenuItem.setText(onHoldText);
        }
        else if (CallPeerState.isOnHold(newState))
        {
            holdMenuItem.setText(offHoldText);
        }
    }

    public void peerAddressChanged(CallPeerChangeEvent evt) {}

    public void peerDisplayNameChanged(CallPeerChangeEvent evt) {}

    public void peerImageChanged(CallPeerChangeEvent evt) {}

    public void peerTransportAddressChanged(CallPeerChangeEvent evt) {}

    /**
     * Implements <tt>{@link PropertyChangeListener#
     * propertyChange(PropertyChangeEvent)}</tt> in order to update the
     * "Mute/Unmute" menu item to fit the current state of the mute property for
     * this call peer.
     *
     * @param evt the <tt>PropertyChangeEvent</tt> that notified us of the
     * property change
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(CallPeer.MUTE_PROPERTY_NAME))
        {
            boolean isMute = (Boolean) evt.getNewValue();

            if (isMute)
                muteMenuItem.setText(unmuteText);
            else
                muteMenuItem.setText(muteText);
        }
    }
}
