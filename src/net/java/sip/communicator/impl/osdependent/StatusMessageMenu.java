/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class StatusMessageMenu
    implements ActionListener
{
    private final Logger logger = Logger.getLogger(StatusMessageMenu.class);

    private static final String BRB_MESSAGE
        = Resources.getString("service.gui.BRB_MESSAGE");

    private static final String BUSY_MESSAGE
        = Resources.getString("service.gui.BUSY_MESSAGE");

    private final Object newMessageItem;

    private final Object busyMessageItem;

    private final Object brbMessageItem;

    private final ProtocolProviderService protocolProvider;

    private final Object menu;

    public StatusMessageMenu(ProtocolProviderService protocolProvider,
        boolean swing)
    {
        this.protocolProvider = protocolProvider;

        String text = Resources.getString("service.gui.SET_STATUS_MESSAGE");
        if (swing)
            menu = new JMenu(text);
        else
            menu = new Menu(text);

        createMenuItem(Resources.getString("service.gui.NO_MESSAGE"));
        newMessageItem =
            createMenuItem(Resources.getString("service.gui.NEW_MESSAGE"));

        addSeparator();

        busyMessageItem = createMenuItem(BUSY_MESSAGE);
        brbMessageItem = createMenuItem(BRB_MESSAGE);
    }

    private Object createMenuItem(String text)
    {
        if (menu instanceof Container)
        {
            JMenuItem menuItem = new JMenuItem(text);
            menuItem.addActionListener(this);
            ((Container) menu).add(menuItem);
            return menuItem;
        }
        else
        {
            MenuItem menuItem = new MenuItem(text);
            menuItem.addActionListener(this);
            ((Menu) menu).add(menuItem);
            return menuItem;
        }
    }

    private void addSeparator()
    {
        if (menu instanceof JMenu)
            ((JMenu) menu).addSeparator();
        else
            ((Menu) menu).addSeparator();
    }

    public Object getMenu()
    {
        return menu;
    }

    public void actionPerformed(ActionEvent e)
    {
        Object menuItem = e.getSource();

        String statusMessage = "";

        if (menuItem.equals(newMessageItem))
        {
            NewStatusMessageDialog dialog =
                new NewStatusMessageDialog(protocolProvider);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dialog.setLocation(screenSize.width / 2 - dialog.getWidth() / 2,
                screenSize.height / 2 - dialog.getHeight() / 2);

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (menuItem.equals(busyMessageItem))
        {
            statusMessage = BUSY_MESSAGE;
        }
        else if (menuItem.equals(brbMessageItem))
        {
            statusMessage = BRB_MESSAGE;
        }

        new PublishStatusMessageThread(statusMessage).start();
    }

    /**
     *  This class allow to use a thread to change the presence status.
     */
    private class PublishStatusMessageThread extends Thread
    {
        private String message;

        private PresenceStatus currentStatus;

        private OperationSetPresence presenceOpSet;

        public PublishStatusMessageThread(String message)
        {
            this.message = message;

            presenceOpSet
                = (OperationSetPersistentPresence) protocolProvider
                    .getOperationSet(OperationSetPresence.class);

            this.currentStatus = presenceOpSet.getPresenceStatus();
        }

        public void run()
        {
            try
            {
                presenceOpSet.publishPresenceStatus(currentStatus, message);
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
                    logger.error(
                        "General error occured while "
                        + "publishing presence status.",
                        e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .NETWORK_FAILURE)
                {
                    logger.error(
                        "Network failure occured while "
                        + "publishing presence status.",
                        e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .PROVIDER_NOT_REGISTERED)
                {
                    logger.error(
                        "Protocol provider must be"
                        + "registered in order to change status.",
                        e1);
                }
            }
        }
    }
}
