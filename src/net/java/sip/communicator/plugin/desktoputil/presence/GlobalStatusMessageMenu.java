/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.presence;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import javax.swing.*;
import java.util.*;

/**
 * @author Damian Minkov
 */
public class GlobalStatusMessageMenu
    extends AbstractStatusMessageMenu
{
    /**
     * Our logger.
     */
    private final static Logger logger
        = Logger.getLogger(GlobalStatusMessageMenu.class);

    /**
     * Creates an instance of <tt>GlobalStatusMessageMenu</tt>.
     *
     * @param swing should we use swing or awt
     */
    public GlobalStatusMessageMenu(boolean swing)
    {
        super(swing);
    }

    /**
     * Returns the currently set status message.
     * @return the currently set status message.
     */
    @Override
    public String getCurrentStatusMessage()
    {
        // we will always return empty string
        // as we have several providers that can have different status messages
        // this is normally used when creating new status messages, to pre fill
        return "";
    }

    /**
     * Publishes the new message in separate thread. If successfully ended
     * the message item is created and added to te list of current status
     * messages and if needed the message is saved.
     * @param message the message to save
     * @param menuItem the item which was clicked to set this status
     * @param saveIfNewMessage whether to save the status on the custom
     *                         statuses list.
     */
    @Override
    public void publishStatusMessage(
        String message,
        Object menuItem,
        boolean saveIfNewMessage)
    {
        new PublishStatusMessageThread(message, menuItem, saveIfNewMessage)
            .start();
    }

    /**
     * Returns the descriptor common for this status message menu instance.
     * @return the descriptor common for this status message menu instance.
     */
    @Override
    public Object getDescriptor()
    {
        // we will receive the calls in order to update all global status menu
        // instances when one is changed
        return GlobalStatusMessageMenu.class;
    }

    @Override
    protected Icon getMenuIcon()
    {
        return DesktopUtilActivator.getResources().getImage(
            "service.gui.statusmessage.GLOBAL_STATUS_MESSAGE_ICON");
    }

    /**
     *  This class allow to use a thread to change the presence status message
     *  to all providers available.
     */
    private class PublishStatusMessageThread extends Thread
    {
        private String message;

        private Object menuItem;

        private boolean saveIfNewMessage;

        public PublishStatusMessageThread(
            String message,
            Object menuItem,
            boolean saveIfNewMessage)
        {
            this.message = message;

            this.menuItem = menuItem;

            this.saveIfNewMessage = saveIfNewMessage;
        }

        @Override
        public void run()
        {
            Iterator<ProtocolProviderService> pProvidersIter
                = AccountUtils.getRegisteredProviders().iterator();
            while(pProvidersIter.hasNext())
            {
                try
                {
                    ProtocolProviderService protocolProvider
                        = pProvidersIter.next();

                    OperationSetPresence presenceOpSet
                        = protocolProvider.getOperationSet(
                                OperationSetPresence.class);

                    if(presenceOpSet == null
                        || !protocolProvider.isRegistered())
                        continue;

                    presenceOpSet.publishPresenceStatus(
                        presenceOpSet.getPresenceStatus(), message);
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
}
