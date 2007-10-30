/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.image.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A parent class for all status selector boxes.
 * 
 * @see SimpleStatusSelectorBox
 * @see PresenceStatusSelectorBox
 * @see GlobalStatusSelectorBox
 * 
 * @author Yana Stamcheva
 */
public abstract class StatusSelectorBox
    extends SIPCommMenu
{
    /**
     * 
     * @param images
     */
    public void startConnecting(BufferedImage[] images){}

    /**
     * 
     */
    public void updateStatus(){}

    /**
     * 
     * @return
     */
    public int getAccountIndex(){return -1;}

    /**
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

        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;
        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext()) {
            String accountRootPropName
                = (String) accountsIter.next();

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

    }
}
