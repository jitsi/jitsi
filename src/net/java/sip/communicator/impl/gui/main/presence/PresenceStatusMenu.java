/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.main.presence.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>StatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * the list of statuses for a protocol provider. This is where the user could
 * select its status.
 * 
 * @author Yana Stamcheva
 */
public class PresenceStatusMenu
    extends StatusSelectorMenu
{
    private Logger logger =
        Logger.getLogger(PresenceStatusMenu.class.getName());

    private ProtocolProviderService protocolProvider;

    private Iterator statusIterator;

    private PresenceStatus offlineStatus;

    private PresenceStatus onlineStatus;

    private PresenceStatus lastSelectedStatus;

    private OperationSetPresence presence;

    private JLabel titleLabel;

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>StatusSelectorBox</tt> and initializes the
     * selector box with data.
     * 
     * @param mainFrame The main application window.
     * @param protocolProvider The protocol provider.
     * @param accountIndex If we have more than one account for a protocol, each
     *            account has an index.
     */
    public PresenceStatusMenu(  MainFrame mainFrame,
                                ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getUserID(),
            new ImageIcon(protocolProvider
                .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));

        this.protocolProvider = protocolProvider;

        this.mainFrame = mainFrame;

        this.presence
            = (OperationSetPresence) protocolProvider
                .getOperationSet(OperationSetPresence.class);

        this.statusIterator = this.presence.getSupportedStatusSet();

        String tooltip =
            "<html><b>" + protocolProvider.getAccountID().getUserID()
                + "</b><br>Connecting</html>";

        this.setToolTipText(tooltip);

        titleLabel = new JLabel(protocolProvider.getAccountID().getUserID());

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = (PresenceStatus) statusIterator.next();
            int connectivity = status.getStatus();

            if (connectivity < 1)
            {
                this.offlineStatus = status;
            }
            else if ((onlineStatus != null
                    && (onlineStatus.getStatus() < connectivity))
                || (onlineStatus == null
                    && (connectivity > 50 && connectivity < 80)))
            {
                this.onlineStatus = status;
            }

            this.addItem(status.getStatusName(), new ImageIcon(ImageLoader
                .getBytesInImage(status.getStatusIcon())),
                new ItemActionListener());
        }

        this.addSeparator();

        this.add(new StatusMessageMenu(protocolProvider));

        this.setSelectedStatus(offlineStatus);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     */
    private class ItemActionListener
        implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() instanceof JMenuItem)
            {
                JMenuItem menuItem = (JMenuItem) e.getSource();

                LoginManager loginManager = mainFrame.getLoginManager();

                Iterator statusSet = presence.getSupportedStatusSet();

                while (statusSet.hasNext())
                {

                    PresenceStatus status = ((PresenceStatus) statusSet.next());

                    if (status.getStatusName().equals(menuItem.getText()))
                    {

                        if (protocolProvider.getRegistrationState()
                            == RegistrationState.REGISTERED
                            && !presence.getPresenceStatus().equals(status))
                        {
                            if (status.isOnline())
                            {

                                new PublishPresenceStatusThread(status).start();
                            }
                            else
                            {
                                loginManager.setManuallyDisconnected(true);

                                loginManager.logoff(protocolProvider);
                            }

                            setSelectedStatus(status);
                        }
                        else if (protocolProvider.getRegistrationState()
                                != RegistrationState.REGISTERED
                            && protocolProvider.getRegistrationState()
                                != RegistrationState.REGISTERING
                            && protocolProvider.getRegistrationState()
                                != RegistrationState.AUTHENTICATING
                            && status.isOnline())
                        {
                            lastSelectedStatus = status;
                            loginManager.login(protocolProvider);
                        }
                        else
                        {
                            if (!status.isOnline()
                                && !(protocolProvider.getRegistrationState()
                                        == RegistrationState.UNREGISTERING))
                            {
                                loginManager.setManuallyDisconnected(true);

                                loginManager.logoff(protocolProvider);

                                setSelectedStatus(status);
                            }
                        }

                        saveStatusInformation(protocolProvider, status
                            .getStatusName());

                        break;
                    }
                }
            }
        }
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus(Object presenceStatus)
    {
        PresenceStatus status = (PresenceStatus) presenceStatus;

        OperationSetPresence presence =
            mainFrame.getProtocolPresenceOpSet(protocolProvider);

        logger.trace("Update status for provider: "
            + protocolProvider.getAccountID().getAccountAddress()
            + ". The new status will be: " + status.getStatusName());

        this.setSelectedStatus(status);

        if (protocolProvider.isRegistered()
            && !presence.getPresenceStatus().equals(status))
        {
            new PublishPresenceStatusThread(status).start();
        }
    }

    /**
     * Selects the given status in the status menu.
     * 
     * @param status the status to select
     */
    public void setSelectedStatus(PresenceStatus status)
    {
        Image statusImage = ImageLoader.getBytesInImage(status.getStatusIcon());

        SelectedObject selectedObject
            = new SelectedObject(new ImageIcon(statusImage),
                                status.getStatusName());

        this.setSelected(selectedObject);

        String tooltip = this.getToolTipText();

        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));

        this.setToolTipText(tooltip.concat("<br>" + status.getStatusName()));
    }

    /**
     * Returns the Offline status in this selector box.
     * 
     * @return the Offline status in this selector box
     */
    public PresenceStatus getOfflineStatus()
    {
        return offlineStatus;
    }

    /**
     * Returns the Online status in this selector box.
     * 
     * @return the Online status in this selector box
     */
    public PresenceStatus getOnlineStatus()
    {
        return onlineStatus;
    }

    /**
     * Returns the status that is currently selected.
     * 
     * @return the status that is currently selected
     */
    public PresenceStatus getLastSelectedStatus()
    {
        return lastSelectedStatus;
    }

    public void updateStatus()
    {
    }

    private class PublishPresenceStatusThread
        extends Thread
    {
        PresenceStatus status;

        public PublishPresenceStatusThread(PresenceStatus status)
        {
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

                    new ErrorDialog(null, Messages
                        .getI18NString("generalError").getText(), msgText, e1)
                        .showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.NETWORK_FAILURE)
                {
                    String msgText =
                        Messages.getI18NString("statusChangeNetworkFailure")
                            .getText();

                    new ErrorDialog(null, Messages.getI18NString(
                        "networkFailure").getText(), msgText, e1).showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        Messages.getI18NString("statusChangeNetworkFailure")
                            .getText();

                    new ErrorDialog(null, Messages.getI18NString(
                        "networkFailure").getText(), msgText, e1).showDialog();
                }
                logger.error("Error - changing status", e1);
            }
        }
    }
}
