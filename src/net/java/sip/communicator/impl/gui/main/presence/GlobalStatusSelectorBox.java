/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>GlobalStatusSelectorBox</tt> is a global status selector box, which
 * appears in the status panel, when the user has more than one account. It
 * allows to the user to change globally its status with only one click, instead
 * of going through all accounts and selecting the desired status for every one
 * of them.
 * <p>
 * By default the <tt>GlobalStatusSelectorBox</tt> will show the most connected
 * status of all registered accounts.
 * 
 * @author Yana Stamcheva
 */
public class GlobalStatusSelectorBox
    extends StatusSelectorBox
    implements ActionListener
{
    private Logger logger = Logger.getLogger(
        PresenceStatusSelectorBox.class.getName());

    private MainFrame mainFrame;

    private ImageIcon onlineIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON));

    private ImageIcon offlineIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON));

    private ImageIcon awayIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_AWAY_ICON));

//    private ImageIcon dndIcon = new ImageIcon(
//        ImageLoader.getImage(ImageLoader.USER_DND_ICON));

    private ImageIcon ffcIcon = new ImageIcon(
        ImageLoader.getImage(ImageLoader.USER_FFC_ICON));

    private JMenuItem onlineItem = new JMenuItem(
        Messages.getI18NString("online").getText(),
        onlineIcon);

    private JMenuItem offlineItem = new JMenuItem(
        Messages.getI18NString("offline").getText(),
        offlineIcon);

    private JMenuItem awayItem = new JMenuItem(
        Messages.getI18NString("awayStatus").getText(),
        awayIcon);

//    private JMenuItem dndItem = new JMenuItem(
//        Messages.getI18NString("dndStatus").getText(),
//        dndIcon);

    private JMenuItem ffcItem = new JMenuItem(
        Messages.getI18NString("ffcStatus").getText(),
        ffcIcon);

    private JLabel titleLabel;

    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     *
     * @param mainFrame The main application window.
     */
    public GlobalStatusSelectorBox(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setPreferredSize(new Dimension(28, 24));

        String tooltip = "<html><b>"
            + "Set global status"
            + "</b></html>";

        this.setToolTipText(tooltip);

        onlineItem.setName(Constants.ONLINE_STATUS);
        offlineItem.setName(Constants.OFFLINE_STATUS);
        awayItem.setName(Constants.AWAY_STATUS);
        ffcItem.setName(Constants.FREE_FOR_CHAT_STATUS);
//      dndItem.setName(Constants.DO_NOT_DISTURB_STATUS);

        onlineItem.addActionListener(this);
        offlineItem.addActionListener(this);
        awayItem.addActionListener(this);
        ffcItem.addActionListener(this);
//      dndItem.addActionListener(this);

        titleLabel = new JLabel("Set global status");

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        this.add(onlineItem);
        this.addSeparator();
        this.add(ffcItem);
        this.add(awayItem);
        this.addSeparator();
        this.add(offlineItem);
//        this.add(dndItem);

        setSelected(offlineItem, offlineIcon);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        Iterator pProviders = mainFrame.getProtocolProviders();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) pProviders.next();

            if(itemName.equals(Constants.ONLINE_STATUS))
            {
                if(!protocolProvider.isRegistered())
                {
                    saveStatusInformation(  protocolProvider,
                        onlineItem.getName());

                    this.mainFrame.getLoginManager().login(protocolProvider);
                }
                else
                {
                    OperationSetPresence presence
                        = (OperationSetPresence) protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                onlineItem.getName());

                        continue;
                    }

                    Iterator statusSet = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status
                            = (PresenceStatus) statusSet.next();

                        if( status.getStatus()
                                < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD
                            && status.getStatus()
                                >= PresenceStatus.AVAILABLE_THRESHOLD)
                        {
                            new PublishPresenceStatusThread(presence, status)
                                .start();

                            this.saveStatusInformation( protocolProvider,
                                                        status.getStatusName());

                            break;
                        }
                    }
                }
            }
            else if (itemName.equals(Constants.OFFLINE_STATUS))
            {
                if(    !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERED)
                    && !protocolProvider.getRegistrationState()
                                .equals(RegistrationState.UNREGISTERING))
                {
                    OperationSetPresence presence
                        = (OperationSetPresence) protocolProvider
                            .getOperationSet(OperationSetPresence.class);

                    if (presence == null)
                    {
                        saveStatusInformation(  protocolProvider,
                                                offlineItem.getName());

                        mainFrame.getLoginManager().logoff(protocolProvider);

                        continue;
                    }

                    Iterator statusSet = presence.getSupportedStatusSet();

                    while (statusSet.hasNext())
                    {
                        PresenceStatus status
                            = (PresenceStatus) statusSet.next();

                        if(status.getStatus()
                            < PresenceStatus.ONLINE_THRESHOLD)
                        {
                            this.saveStatusInformation( protocolProvider,
                                status.getStatusName());

                            break;
                        }
                    }

                    try 
                    {
                        mainFrame.getLoginManager()
                            .setManuallyDisconnected(true);

                        protocolProvider.unregister();
                    }
                    catch (OperationFailedException e1)
                    {
                        logger.error(
                            "Unable to unregister the protocol provider: "
                            + protocolProvider
                            + " due to the following exception: " + e1);
                    }
                }
            }
            else if (itemName.equals(Constants.FREE_FOR_CHAT_STATUS))
            {
                if (!protocolProvider.isRegistered())
                    continue;

                OperationSetPresence presence
                    = (OperationSetPresence) protocolProvider
                        .getOperationSet(OperationSetPresence.class);

                if (presence == null)
                    continue;

                Iterator statusSet = presence.getSupportedStatusSet();

                PresenceStatus status = null;

                while (statusSet.hasNext())
                {
                    PresenceStatus currentStatus
                        = (PresenceStatus) statusSet.next();

                    if (status == null)
                        status = currentStatus; 

                    if(status.getStatus() < currentStatus.getStatus())
                    {
                        status = currentStatus;
                    }
                }

                if (status != null)
                {
                    new PublishPresenceStatusThread(presence, status)
                        .start();

                    this.saveStatusInformation( protocolProvider,
                        status.getStatusName());
                }
            }
            else if (itemName.equals(Constants.AWAY_STATUS))
            {
                if (!protocolProvider.isRegistered())
                    continue;

                OperationSetPresence presence
                    = (OperationSetPresence) protocolProvider
                        .getOperationSet(OperationSetPresence.class);

                if (presence == null)
                    continue;

                Iterator statusSet = presence.getSupportedStatusSet();

                PresenceStatus status = null;

                while (statusSet.hasNext())
                {
                    PresenceStatus currentStatus
                        = (PresenceStatus) statusSet.next();

                    if (status == null
                        && currentStatus.getStatus()
                            < PresenceStatus.AVAILABLE_THRESHOLD
                        && currentStatus.getStatus()
                            >= PresenceStatus.ONLINE_THRESHOLD)
                    {
                        status = currentStatus;
                    }

                    if (currentStatus.getStatus()
                            < PresenceStatus.AVAILABLE_THRESHOLD
                        && currentStatus.getStatus()
                            >= PresenceStatus.ONLINE_THRESHOLD
                        && currentStatus.getStatus() > status.getStatus())
                    {
                        status = currentStatus;
                    }
                }

                if (status != null)
                {
                    new PublishPresenceStatusThread(presence, status)
                        .start();

                    this.saveStatusInformation( protocolProvider,
                        status.getStatusName());

                }
            }
        }
    }

    /**
     * Updates the global status by picking the most connected protocol provider
     * status.
     */
    public void updateStatus()
    {
        int status = 0;

        Iterator pProviders = mainFrame.getProtocolProviders();

        while (pProviders.hasNext())
        {
            ProtocolProviderService protocolProvider
                = (ProtocolProviderService) pProviders.next();

            OperationSetPresence presence
                = (OperationSetPresence) protocolProvider
                    .getOperationSet(OperationSetPresence.class);

            if (presence == null)
            {
                if (protocolProvider.isRegistered()
                    && status < PresenceStatus.AVAILABLE_THRESHOLD)
                {
                    status = PresenceStatus.AVAILABLE_THRESHOLD;
                }
            }
            else
            {
                if (protocolProvider.isRegistered()
                    && status < presence.getPresenceStatus().getStatus())
                {
                    status = presence.getPresenceStatus().getStatus();
                }
            }
        }

        JMenuItem item = getItemFromStatus(status);

        setSelected(item, (ImageIcon)item.getIcon());
    }

    /**
     * Publishes the given status to the given presence operation set.
     */
    private class PublishPresenceStatusThread
        extends Thread
    {
        private PresenceStatus status;

        private OperationSetPresence presence;

        /**
         * 
         * @param presence
         * @param status
         */
        public PublishPresenceStatusThread( OperationSetPresence presence,
                                            PresenceStatus status)
        {
            this.presence = presence;
            this.status = status;
        }

        public void run()
        {
            try
            {
                presence.publishPresenceStatus(status, "");
            }
            catch (IllegalArgumentException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    String msgText =
                        Messages.getI18NString("statusChangeGeneralError")
                            .getText();

                    new ErrorDialog(null, Messages.getI18NString(
                        "generalError").getText(), msgText, e1)
                    .showDialog();
                }
                else if (e1.getErrorCode()
                    == OperationFailedException.NETWORK_FAILURE)
                {
                    String msgText =
                        Messages.getI18NString("statusChangeNetworkFailure")
                            .getText();

                    new ErrorDialog(null, msgText,
                        Messages.getI18NString("networkFailure").getText(), e1)
                    .showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        Messages.getI18NString("statusChangeNetworkFailure")
                            .getText();

                    new ErrorDialog(null, Messages.getI18NString(
                        "networkFailure").getText(), msgText, e1)
                    .showDialog();
                }
                logger.error("Error - changing status", e1);
            }
        }
    }

    /**
     * Returns the <tt>JMenuItem</tt> corresponding to the given status. For
     * status constants we use here the values defined in the
     * <tt>PresenceStatus</tt>, but this is only for convenience.
     * 
     * @param status the status to which the item should correspond
     * @return the <tt>JMenuItem</tt> corresponding to the given status
     */
    private JMenuItem getItemFromStatus(int status)
    {
        if(status < PresenceStatus.ONLINE_THRESHOLD)
        {
            return offlineItem;
        }
        else if(status < PresenceStatus.AVAILABLE_THRESHOLD)
        {
            return awayItem;
        }
        else if(status < PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD)
        {
            return onlineItem;
        }
        else if(status < PresenceStatus.MAX_STATUS_VALUE)
        {
            return ffcItem;
        }
        else
        {
            return offlineItem;
        }
    }
}
