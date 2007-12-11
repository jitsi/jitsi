/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class StatusMessageMenu
    extends JMenu
    implements ActionListener
{
    private Logger logger = Logger.getLogger(StatusMessageMenu.class);

    private JMenuItem noMessageItem
        = new JMenuItem(Resources.getString("noMessage"));

    private JMenuItem newMessageItem
        = new JMenuItem(Resources.getString("newMessage"));

    private JMenuItem busyMessageItem
        =  new JMenuItem(Resources.getString("busyMessage"));

    private JMenuItem brbMessageItem
        = new JMenuItem(Resources.getString("brbMessage"));

    private ProtocolProviderService protocolProvider;

    public StatusMessageMenu(  ProtocolProviderService protocolProvider)
    {
        super(Resources.getString("setStatusMessage"));

        this.protocolProvider = protocolProvider;

        this.noMessageItem.addActionListener(this);
        this.newMessageItem.addActionListener(this);
        this.busyMessageItem.addActionListener(this);
        this.brbMessageItem.addActionListener(this);

        this.add(noMessageItem);
        this.add(newMessageItem);

        this.addSeparator();

        this.add(busyMessageItem);
        this.add(brbMessageItem);
    }

    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        String statusMessage = "";

        if (menuItem.equals(newMessageItem))
        {
            NewStatusMessageDialog dialog
                = new NewStatusMessageDialog(protocolProvider);

            dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - dialog.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - dialog.getHeight()/2
                );

            dialog.setVisible(true);

            dialog.requestFocusInFiled();
        }
        else if (menuItem.equals(busyMessageItem))
        {
            statusMessage = Resources.getString("busyMessage");
        }
        else if (menuItem.equals(brbMessageItem))
        {
            statusMessage = Resources.getString("brbMessage");
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
