/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class TransferActiveCallsMenu
    extends JPopupMenu
    implements Skinnable
{
    /**
     * The invoker component.
     */
    private final JComponent invoker;

    /**
     * The initial peer we're aiming to transfer.
     */
    private final CallPeer initialPeer;

    /**
     * Creates an instance of the <tt>TransferActiveCallsMenu</tt>.
     * @param invoker the invoker component
     * @param peer the initial peer we're aiming to transfer
     * @param callPeers a list of all possible call peers to transfer to
     */
    public TransferActiveCallsMenu(
        JComponent invoker,
        CallPeer peer,
        Collection<CallPeer> callPeers)
    {
        this.invoker = invoker;
        this.initialPeer = peer;

        this.init();

        for (CallPeer callPeer : callPeers)
        {
            this.addCallPeerItem(callPeer);
        }

        this.addSeparator();

        // At the end add the possibility to transfer to anyone
        // (Unattended transfer).
        JMenuItem transferToMenuItem = new JMenuItem(
            GuiActivator.getResources()
                .getI18NString("service.gui.TRANSFER_TO"));

        transferToMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                CallManager.openCallTransferDialog(initialPeer);
            }
        });

        this.add(transferToMenuItem);
    }

    /**
     * Initializes and add some common components.
     */
    private void init()
    {
        setInvoker(invoker);

        this.add(createInfoLabel());

        this.addSeparator();

        this.setFocusable(true);
    }

    /**
     * Adds the given <tt>callPeer</tt> to the list of available
     * call peers for call transfer.
     * @param callPeer the call peer to add item for
     */
    private void addCallPeerItem(final CallPeer callPeer)
    {
        final CallPeerMenuItem peerItem = new CallPeerMenuItem(callPeer);

        peerItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CallManager.transferCall(initialPeer, callPeer);
            }
        });

        this.add(peerItem);
    }

    /**
     * Shows the dialog at the given location.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void showPopupMenu(int x, int y)
    {
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Shows this popup menu regarding to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
            invoker.getY() + invoker.getHeight());

        SwingUtilities
            .convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
    }

    /**
     * Creates the info label.
     * @return the created info label
     */
    private Component createInfoLabel()
    {
        JLabel infoLabel = new JLabel();

        infoLabel.setText("<html><b>" + GuiActivator.getResources()
            .getI18NString("service.gui.TRANSFER_CALL_TO")
            + "</b></html>");

        return infoLabel;
    }

    /**
     * Reloads all menu items.
     */
    public void loadSkin()
    {
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

    /**
     * A custom menu item corresponding to a specific <tt>CallPeer</tt>.
     */
    private class CallPeerMenuItem
        extends JMenuItem
        implements Skinnable
    {
        private final CallPeer callPeer;

        public CallPeerMenuItem(CallPeer peer)
        {
            this.callPeer = peer;
            this.setText(callPeer.getDisplayName());

            loadSkin();
        }

        public CallPeer getCallPeer()
        {
            return callPeer;
        }

        /**
         * Reloads icon.
         */
        public void loadSkin()
        {
            byte[] peerIcon = callPeer.getImage();

            if (peerIcon != null)
                this.setIcon(new ImageIcon(peerIcon));
        }

    }
}
