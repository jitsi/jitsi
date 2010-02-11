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
    private JComponent sourceComponent;

    /**
     * Creates this dialog.
     * @param sourceComponent the source component, for which the popup menu
     * will be shown
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     */
    public ChooseCallAccountPopupMenu(
        JComponent sourceComponent,
        final String contactToCall,
        Vector<ProtocolProviderService> telephonyProviders)
    {
        this.sourceComponent = sourceComponent;

        this.add(createInfoLabel());

        this.addSeparator();

        for (ProtocolProviderService provider : telephonyProviders)
        {
            final ProviderMenuItem providerItem
                = new ProviderMenuItem(provider);

            providerItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    CallManager.createCall( providerItem.getProtocolProvider(),
                                            contactToCall);
                    ChooseCallAccountPopupMenu.this.setVisible(false);
                }
            });

            this.add(providerItem);
        }
    }

    /**
     * Shows the dialog.
     */
    public void showPopupMenu()
    {
        Point location = new Point(sourceComponent.getX(),
            sourceComponent.getY() + sourceComponent.getHeight());

        SwingUtilities
            .convertPointToScreen(location, sourceComponent.getParent());
        setLocation(location);
        setInvoker(sourceComponent);
        setVisible(true);
    }

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
