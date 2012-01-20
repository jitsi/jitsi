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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CallPeerActionMenuBar</tt> is situated in the title area of any
 * <tt>CallPeerRenderer</tt> and gives access to operations like: "Put on hold",
 * "Mute", "Hang up" and so on.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallPeerMenu
    extends SIPCommMenu
    implements  CallPeerListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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

    /**
     * Creates a <tt>CallPeerActionMenuBar</tt> by specifying the associated
     * <tt>callPeer</tt>.
     * @param peer the <tt>CallPeer</tt> associated with the contained menus
     */
    public CallPeerMenu(CallPeer peer)
    {
        this.callPeer = peer;

        this.setOpaque(false);

        // Should explicitly remove any border in order to align correctly the
        // icon.
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setPreferredSize(new Dimension(16, 16));
        this.setHorizontalAlignment(SwingConstants.CENTER);

        loadSkin();

        this.setIconTextGap(0);
        this.addItem(
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

        this.add(holdMenuItem);

        // Add the call peer listener that would notify us for call peer state
        // changes. We'll be using these notifications in order to update the
        // hold menu item state.
        peer.addCallPeerListener(this);
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
     * Reloads default icon and menu items.
     */
    public void loadSkin()
    {
        this.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.CALL_PEER_TOOLS)));

        Component[] components = getComponents();
        for(Component component : components)
        {
            if(component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
