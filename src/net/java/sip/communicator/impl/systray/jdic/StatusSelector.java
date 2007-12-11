/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.impl.systray.jdic;


import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;


/**
 * The <tt>StatusSelector</tt> is a submenu which allows to select a status for
 * a protocol provider which supports the OperationSetPresence.
 * 
 * @author Nicolas Chamouard
 *
 */
public class StatusSelector
    extends JMenu
    implements ActionListener
{
    /**
     * A reference of <tt>Systray</tt>
     */
    private SystrayServiceJdicImpl parentSystray;
    /**
     * The protocol provider
     */
    private ProtocolProviderService provider;
    /**
     * The presence status
     */
    private OperationSetPresence presence;
    
    /**
     * The logger for this class.
     */
    private Logger logger = Logger.getLogger(
            StatusSelector.class.getName());

    private StatusMessageMenu statusMessageMenu;

    /**
     * Creates an instance of StatusSelector
     * 
     * @param jdicSystray a reference of the parent <tt>Systray</tt>
     * @param provider the protocol provider
     * @param presence the presence status
     */
    public StatusSelector(  SystrayServiceJdicImpl jdicSystray,
                            ProtocolProviderService provider,
                            OperationSetPresence presence)
    {
        this.parentSystray = jdicSystray;
        this.provider = provider;
        this.presence = presence;

        this.statusMessageMenu = new StatusMessageMenu(provider);
        /* the parent item */

        this.setText(provider.getAccountID().getUserID());
        this.setIcon(new ImageIcon(
                presence.getPresenceStatus().getStatusIcon()));

        /* the submenu itself */

        Iterator statusIterator = this.presence.getSupportedStatusSet();

        while(statusIterator.hasNext()) 
        {
            PresenceStatus status = (PresenceStatus) statusIterator.next();

            ImageIcon icon = new ImageIcon(status.getStatusIcon());
            JMenuItem item = new JMenuItem(status.getStatusName(),icon);

            item.addActionListener(this);

            this.add(item);
        }

        this.addSeparator();

        this.add(statusMessageMenu);
    }

    /**
     * Change the status of the protocol according to
     * the menu item selected
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
        JMenuItem menuItem = (JMenuItem) evt.getSource();

        Iterator statusSet = presence.getSupportedStatusSet();

        while (statusSet.hasNext()) 
        {
            PresenceStatus status = ((PresenceStatus) statusSet.next());

            if (status.getStatusName().equals(menuItem.getText())) 
            {
                
                if (this.provider.getRegistrationState()
                        == RegistrationState.REGISTERED
                    && !presence.getPresenceStatus().equals(status))
                {
                    if (status.isOnline()) 
                    {
                        new PublishPresenceStatusThread(status).start();
                    }
                    else
                    {
                        new ProviderUnRegistration(this.provider).start();
                    }
                }
                else if (this.provider.getRegistrationState()
                            != RegistrationState.REGISTERED
                        && this.provider.getRegistrationState()
                            != RegistrationState.REGISTERING
                        && this.provider.getRegistrationState()
                            != RegistrationState.AUTHENTICATING
                        && status.isOnline())
                {
                    new ProviderRegistration(provider).start();
                }
                else
                {
                    if(!status.isOnline()
                        && !(this.provider.getRegistrationState()
                        == RegistrationState.UNREGISTERING))
                    {
                        new ProviderUnRegistration(this.provider).start();
                    }
                }    
                
                parentSystray.saveStatusInformation(
                    provider, status.getStatusName());
                
                break;
            }
        }
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus(PresenceStatus presenceStatus)
    {
        logger.trace("Systray update status for provider: "
            + provider.getAccountID().getAccountAddress()
            + ". The new status will be: " + presenceStatus.getStatusName());

        this.setIcon(new ImageIcon(presenceStatus.getStatusIcon()));
    }

    /**
     *  This class allow to use a thread to change the presence status.
     */
    private class PublishPresenceStatusThread extends Thread
    {
        PresenceStatus status;
        
        public PublishPresenceStatusThread(PresenceStatus status)
        {
            this.status = status;
        }

        public void run()
        {
            try {
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
