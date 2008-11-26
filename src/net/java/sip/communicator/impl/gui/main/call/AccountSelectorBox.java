/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The AccountSelectorBox is located in the main application window under the
 * field where the telephone number is written. It contains all accounts that
 * support telephony operation set and is meant to be used by user to select the
 * account, which he/she would like to use when calling. The selected account
 * could be changed at any time.
 * 
 * By default the most connected account is selected.
 * 
 * @author Yana Stamcheva
 */
public class AccountSelectorBox
    extends JMenuBar
    implements ActionListener
{
    private static final Logger logger =
        Logger.getLogger(AccountSelectorBox.class);

    private final Map<ProtocolProviderService, JMenuItem> accountsTable =
        new Hashtable<ProtocolProviderService, JMenuItem>();

    private final SIPCommMenu menu = new SIPCommMenu();

    private ProtocolProviderService selectedProvider;

    private final MainCallPanel parentCallPanel;

    /**
     * Creates an instance of AccountSelectorBox.
     */
    public AccountSelectorBox(MainCallPanel parentPanel)
    {
        this.parentCallPanel = parentPanel;

        this.menu.setPreferredSize(new Dimension(28, 24));

        this.menu.setUI(new SIPCommSelectorMenuUI());

        this.add(menu);
    }

    /**
     * Adds an account to this account selector box. The account is represented
     * by its protocol provider.
     * 
     * @param pps the protocol provider for the added account
     */
    public void addAccount(ProtocolProviderService pps)
    {
        Image img = createAccountStatusImage(pps);

        JMenuItem menuItem =
            new JMenuItem(pps.getAccountID().getUserID(), new ImageIcon(img));

        menuItem.addActionListener(this);
        this.accountsTable.put(pps, menuItem);
        this.menu.add(menuItem);

        if (accountsTable.size() < 2)
        {
            this.setSelected(pps);
        }
        else
        {
            OperationSetPresence presence =
                (OperationSetPresence) pps
                    .getOperationSet(OperationSetPresence.class);

            OperationSetPresence selectedPresence =
                (OperationSetPresence) selectedProvider
                    .getOperationSet(OperationSetPresence.class);

            if (presence != null
                && selectedPresence != null
                && (selectedPresence.getPresenceStatus().getStatus() < presence
                    .getPresenceStatus().getStatus()))
            {
                setSelected(pps);
            }
            else if (pps.isRegistered() && !selectedProvider.isRegistered())
            {
                setSelected(pps);
            }
        }
    }

    /**
     * Listens when an account was selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        for (Map.Entry<ProtocolProviderService, JMenuItem> entry : accountsTable
            .entrySet())
        {
            if (entry.getValue().equals(menuItem))
            {
                this
                    .setSelected(entry.getKey(), (ImageIcon) menuItem.getIcon());
                return;
            }
        }

        logger.debug("Could not find account for menu item "
            + menuItem.getText() + ". accountsTable(" + accountsTable.size()
            + ") is : " + accountsTable);
    }

    /**
     * Obtains the status icon for the given protocol contact and adds to it the
     * account index information.
     * 
     * @param pps the protocol provider for which to create the image
     * @return the indexed status image
     */
    public Image createAccountStatusImage(ProtocolProviderService pps)
    {
        Image statusImage;

        OperationSetPresence presence =
            (OperationSetPresence) pps
                .getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            statusImage =
                ImageLoader.getBytesInImage(presence.getPresenceStatus()
                    .getStatusIcon());
        }
        else
        {
            statusImage =
                ImageLoader.getBytesInImage(pps.getProtocolIcon().getIcon(
                    ProtocolIcon.ICON_SIZE_16x16));
            if (!pps.isRegistered())
            {
                statusImage = LightGrayFilter.createDisabledImage(statusImage);
            }
        }

        int index = parentCallPanel.getMainFrame().getProviderIndex(pps);

        Image img = null;
        if (index > 0)
        {
            BufferedImage buffImage =
                new BufferedImage(22, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = (Graphics2D) buffImage.getGraphics();
            AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawImage(statusImage, 0, 0, null);
            g.setComposite(ac);
            g.drawString(new Integer(index + 1).toString(), 14, 8);

            img = buffImage;
        }
        else
        {
            img = statusImage;
        }
        return img;
    }

    /**
     * Updates the protocol account status.
     * 
     * @param pps the protocol provider service to update
     */
    public void updateAccountStatus(ProtocolProviderService pps)
    {
        JMenuItem menuItem = accountsTable.get(pps);

        Icon icon = new ImageIcon(createAccountStatusImage(pps));

        ProtocolProviderService selectedPPS =
            (ProtocolProviderService) menu.getSelectedObject();

        // When the currently selected provider becomes offline, select another
        // one.
        if (selectedPPS.equals(pps) && !pps.isRegistered())
        {
            ProtocolProviderService newPPS = findFirstRegisteredProvider();

            if (newPPS != null)
                this.setSelected(newPPS);
        }

        // If the currently selected provider is offline and some of other
        // providers
        // become online, select the new one.
        if (!selectedPPS.equals(pps) && !selectedPPS.isRegistered()
            && pps.isRegistered())
        {
            this.setSelected(pps);
        }

        menuItem.setIcon(icon);
        if (menu.getSelectedObject().equals(pps))
        {
            this.menu.setIcon(icon);
        }

        menuItem.repaint();
        this.menu.repaint();
    }

    public void setSelected(ProtocolProviderService pps, ImageIcon icon)
    {
        this.selectedProvider = pps;

        this.menu.setSelected(new SelectedObject(icon, pps));
        this.menu.setToolTipText(pps.getAccountID().getUserID());

        this.parentCallPanel.setCallProvider(pps);
    }

    /**
     * Sets the selected contact to the given proto contact.
     * 
     * @param pps the protocol provider to select
     */
    public void setSelected(ProtocolProviderService pps)
    {
        this.setSelected(pps, new ImageIcon(createAccountStatusImage(pps)));
    }

    /**
     * Returns the protocol menu.
     * 
     * @return the protocol menu
     */
    public SIPCommMenu getMenu()
    {
        return menu;
    }

    /**
     * Returns TRUE if the account corresponding to the given protocol provider
     * is already contained in this selector box, otherwise returns FALSE.
     * 
     * @param pps the protocol provider service for the account
     * @return TRUE if the account corresponding to the given protocol provider
     *         is already contained in this selector box, otherwise returns
     *         FALSE
     */
    public boolean containsAccount(ProtocolProviderService pps)
    {
        return this.accountsTable.containsKey(pps);
    }

    /**
     * Returns the number of accounts contained in this account selector box.
     * 
     * @return the number of accounts contained in this account selector box
     */
    public int getAccountsNumber()
    {
        return this.accountsTable.size();
    }

    /**
     * Removes the given account from this account selector box.
     * 
     * @param pps the protocol provider service corresponding to the account to
     *            remove
     */
    public void removeAccount(ProtocolProviderService pps)
    {
        JMenuItem accountItem = this.accountsTable.get(pps);

        this.menu.remove(accountItem);

        this.accountsTable.remove(pps);

        if (selectedProvider == pps && accountsTable.size() > 0)
            setSelected(accountsTable.keySet().iterator().next());
    }

    /**
     * Returns the first registered protocol provider.
     * 
     * @return the first registered protocol provider.
     */
    private ProtocolProviderService findFirstRegisteredProvider()
    {
        for (ProtocolProviderService pps : accountsTable.keySet())
            if (pps.isRegistered())
                return pps;
        return null;
    }
}
