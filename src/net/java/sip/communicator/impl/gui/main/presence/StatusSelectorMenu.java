/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.Dimension;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A parent class for all status selector boxes.
 * 
 * @see SimpleStatusMenu
 * @see GlobalStatusSelectorBox
 * 
 * @author Yana Stamcheva
 */
public abstract class StatusSelectorMenu
    extends SIPCommMenu
{
    public StatusSelectorMenu()
    {
        super();
    }

    public StatusSelectorMenu(String text, Icon defaultIcon)
    {
        super(text, defaultIcon);
    }

    /**
     * Starts the connecting animation.
     * 
     * @param images the animated image to play
     */
    public void startConnecting(BufferedImage[] images){}

    /**
     * Updates the current status.
     */
    public void updateStatus(){}

    /**
     * Returns the account index (In case of more than one account for one and
     * the same protocol).
     * 
     * @return the account index
     */
    public int getAccountIndex(){return -1;}

    /**
     * Sets the account index.
     * 
     * @param index
     */
    public void setAccountIndex(int index){}

    /**
     * Saves the last status for all accounts. This information is used
     * on loging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     */
    protected void saveStatusInformation(
            ProtocolProviderService protocolProvider,
            String statusName)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {

                configService.setProperty(
                        accountRootPropName + ".lastAccountStatus",
                        statusName);

                savedAccount = true;
            }
        }

        if(!savedAccount) {
            String accNodeName
                = "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage
                = "net.java.sip.communicator.impl.gui.accounts."
                        + accNodeName;

            configService.setProperty(accountPackage,
                    protocolProvider.getAccountID().getAccountUniqueID());

            configService.setProperty(
                    accountPackage+".lastAccountStatus",
                    statusName);
        }

        this.setPreferredSize(new Dimension(28, 24));
    }
}
