/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 */
public class ChooseCallAccountPopupMenu
    extends JPopupMenu
{
    private final JComponent invoker;

    /**
     * Creates this dialog.
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders)
    {
        this.invoker = invoker;
        this.init();

        for (ProtocolProviderService provider : telephonyProviders)
        {
            this.addTelephonyProviderItem(provider, contactToCall);
        }
    }

    /**
     * Creates this dialog by specifying a list of telephony contacts to choose
     * from.
     * @param invoker the invoker of this pop up
     * @param telephonyContacts the list of telephony contacts to select through
     */
    public ChooseCallAccountPopupMenu(  JComponent invoker,
                                        List<Contact> telephonyContacts)
    {
        this.invoker = invoker;
        this.init();

        for (Contact contact : telephonyContacts)
        {
            this.addTelephonyProviderItem(  contact.getProtocolProvider(),
                                            contact.getAddress());
        }
    }

    /**
     * Initializes and add some common components.
     */
    private void init()
    {
        setInvoker(invoker);

        this.add(createInfoLabel());

        this.addSeparator();
    }

    /**
     * Adds the given <tt>telephonyProvider</tt> in the list of available
     * telephony providers.
     * @param telephonyProvider the provider to add.
     * @param contactString the contact to call when the provider is selected
     */
    private void addTelephonyProviderItem(
        final ProtocolProviderService telephonyProvider,
        final String contactString)
    {
        final ProviderMenuItem providerItem
            = new ProviderMenuItem(telephonyProvider);

        providerItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CallManager.createCall( providerItem.getProtocolProvider(),
                                        contactString);
                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(providerItem);
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
            .getI18NString("service.gui.CALL_VIA")
            + "</b></html>");

        return infoLabel;
    }

    /**
     * A custom radio button corresponding to a <tt>ProtocolProviderService</tt>.
     */
    private class ProviderMenuItem extends JMenuItem
    {
        private final ProtocolProviderService protocolProvider;

        public ProviderMenuItem(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
            this.setText(protocolProvider.getAccountID().getAccountAddress());

            byte[] protocolIcon
                = protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if (protocolIcon != null)
                this.setIcon(new ImageIcon(protocolIcon));
        }

        public ProtocolProviderService getProtocolProvider()
        {
            return protocolProvider;
        }
    }
}
