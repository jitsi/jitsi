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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.main.presence.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>StatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that contains
 * the list of statuses for a protocol provider. This is where the user could
 * select its status.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class PresenceStatusMenu
    extends StatusSelectorMenu
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(PresenceStatusMenu.class);

    /**
     * The <tt>ProtocolProviderService</tt> which has its presence status
     * depicted and changed by this instance.
     */
    private final ProtocolProviderService protocolProvider;

    private Iterator<PresenceStatus> statusIterator;

    private PresenceStatus offlineStatus;

    private PresenceStatus onlineStatus;

    private PresenceStatus lastSelectedStatus;

    private OperationSetPresence presence;

    private JLabel titleLabel;

    /**
     * Initializes a new <tt>PresenceStatusMenu</tt> instance which is to
     * depict and change the presence status of a specific
     * <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> which is to
     * have its presence status depicted and changed by the new instance
     */
    public PresenceStatusMenu(ProtocolProviderService protocolProvider)
    {
        super(protocolProvider.getAccountID().getDisplayName(),
            ImageLoader.getAccountStatusImage(protocolProvider));

        this.protocolProvider = protocolProvider;

        this.presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

        this.statusIterator = this.presence.getSupportedStatusSet();

        String tooltip =
            "<html><b>" + protocolProvider.getAccountID().getDisplayName()
                + "</b><br>Connecting</html>";

        this.setToolTipText(tooltip);

        titleLabel = new JLabel(protocolProvider.getAccountID().getDisplayName());

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        while (statusIterator.hasNext())
        {
            PresenceStatus status = statusIterator.next();
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

            this.addItem(
                    status.getStatusName(),
                    new ImageIcon(status.getStatusIcon()),
                    this);
        }

        this.addSeparator();

        this.add(new StatusMessageMenu(protocolProvider));

        this.setSelectedStatus(offlineStatus);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items in the
     * list is selected.
     *
     * @param e an <tt>ActionEvent</tt> which carries the data associated with
     * the performed action
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof JMenuItem)
        {
            String menuItemText = ((JMenuItem) e.getSource()).getText();
            LoginManager loginManager
                = GuiActivator.getUIService().getLoginManager();

            Iterator<PresenceStatus> statusSet = presence.getSupportedStatusSet();

            while (statusSet.hasNext())
            {
                PresenceStatus status = statusSet.next();

                if (status.getStatusName().equals(menuItemText))
                {
                    RegistrationState registrationState
                        = protocolProvider.getRegistrationState();

                    if (registrationState == RegistrationState.REGISTERED
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
                    else if (registrationState != RegistrationState.REGISTERED
                        && registrationState != RegistrationState.REGISTERING
                        && registrationState != RegistrationState.AUTHENTICATING
                        && status.isOnline())
                    {
                        lastSelectedStatus = status;
                        loginManager.login(protocolProvider);
                    }
                    else if (!status.isOnline()
                            && !(registrationState
                                    == RegistrationState.UNREGISTERING))
                    {
                            loginManager.setManuallyDisconnected(true);

                            loginManager.logoff(protocolProvider);

                            setSelectedStatus(status);
                    }

                    saveStatusInformation(
                        protocolProvider,
                        status.getStatusName());

                    break;
                }
            }
        }
    }

    /**
     * Selects a specific <tt>PresenceStatus</tt> in this instance and the
     * <tt>ProtocolProviderService</tt> it depicts.
     *
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     * instance and the <tt>ProtocolProviderService</tt> it depicts
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        OperationSetPresence presence
            = MainFrame.getProtocolPresenceOpSet(protocolProvider);

        logger.trace("Update status for provider: "
            + protocolProvider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        this.setSelectedStatus(presenceStatus);

        if (protocolProvider.isRegistered()
                && !presence.getPresenceStatus().equals(presenceStatus))
            new PublishPresenceStatusThread(presenceStatus).start();
    }

    /**
     * Selects the given status in the status menu.
     *
     * @param status the status to select
     */
    public void setSelectedStatus(PresenceStatus status)
    {
        Icon statusImage = ImageLoader.getAccountStatusImage(protocolProvider);

        SelectedObject selectedObject
            = new SelectedObject(statusImage,
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
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_GENERAL_ERROR");

                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.GENERAL_ERROR"), msgText, e1)
                        .showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.NETWORK_FAILURE)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE");

                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                        "service.gui.NETWORK_FAILURE"), msgText, e1)
                        .showDialog();
                }
                else if (e1.getErrorCode()
                        == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE");

                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                        "service.gui.NETWORK_FAILURE"), msgText, e1).showDialog();
                }
                logger.error("Error - changing status", e1);
            }
        }
    }
}
