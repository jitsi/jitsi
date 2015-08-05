/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.desktoputil.presence;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import java.beans.*;

/**
 * The <tt>StatusMessageMenu<tt> is added to every status selector box in order
 * to enable the user to choose a status message.
 *
 * @author Yana Stamcheva
 */
public class StatusMessageMenu
    extends AbstractStatusMessageMenu
    implements ProviderPresenceStatusListener
{
    /**
     * Our logger.
     */
    private final static Logger logger
        = Logger.getLogger(StatusMessageMenu.class);

    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>StatusMessageMenu</tt>, by specifying the
     * <tt>ProtocolProviderService</tt> to which this menu belongs.
     *
     * @param protocolProvider the protocol provider service to which this
     * menu belongs
     * @param swing should we use swing or awt
     */
    public StatusMessageMenu(ProtocolProviderService protocolProvider,
                             boolean swing)
    {
        super(swing);

        this.protocolProvider = protocolProvider;

        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);
        if(presenceOpSet != null)
        {
            presenceOpSet.addProviderPresenceStatusListener(this);
        }
    }

    /**
     * Returns the descriptor common for this status message menu instance.
     * @return the descriptor common for this status message menu instance.
     */
    public Object getDescriptor()
    {
        return protocolProvider;
    }

    /**
     * Returns the currently set status message.
     * @return the currently set status message.
     */
    public String getCurrentStatusMessage()
    {
        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        return presenceOpSet.getCurrentStatusMessage();
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
    public void publishStatusMessage(String message,
                              Object menuItem,
                              boolean saveIfNewMessage)
    {
        new PublishStatusMessageThread(message, menuItem, saveIfNewMessage)
                .start();
    }

    /**
     * Not used.
     * @param evt ProviderStatusChangeEvent the event describing the status
     */
    @Override
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt)
    {}

    /**
     * Detects a provider status changed.
     * @param evt a PropertyChangeEvent with a STATUS_MESSAGE property name,
     */
    @Override
    public void providerStatusMessageChanged(PropertyChangeEvent evt)
    {
        fireStatusMessageUpdated(
            (String)evt.getOldValue(),
            (String)evt.getNewValue());
    }

    /**
     * Clears resources.
     */
    @Override
    public void dispose()
    {
        super.dispose();

        OperationSetPresence presenceOpSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);
        if(presenceOpSet != null)
        {
            presenceOpSet.removeProviderPresenceStatusListener(this);
        }
    }

    /**
     *  This class allow to use a thread to change the presence status message.
     */
    private class PublishStatusMessageThread extends Thread
    {
        private String message;

        private PresenceStatus currentStatus;

        private OperationSetPresence presenceOpSet;

        private Object menuItem;

        private boolean saveIfNewMessage;

        public PublishStatusMessageThread(
                    String message,
                    Object menuItem,
                    boolean saveIfNewMessage)
        {
            this.message = message;

            presenceOpSet
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            this.currentStatus = presenceOpSet.getPresenceStatus();

            this.menuItem = menuItem;

            this.saveIfNewMessage = saveIfNewMessage;
        }

        @Override
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
