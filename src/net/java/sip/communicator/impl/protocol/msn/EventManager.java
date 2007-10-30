/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.sf.jml.*;
import net.sf.jml.impl.*;
import net.sf.jml.net.*;
import net.sf.jml.protocol.*;
import net.sf.jml.protocol.incoming.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.sf.jml.net.Message;


/**
 * Manager which listens for changing of the contact list
 * and fires some events
 *
 * @author Damian Minkov
 */
public class EventManager
    extends SessionAdapter
{
    private static final Logger logger = Logger.getLogger(EventManager.class);

    private BasicMessenger msnMessenger = null;
    private Vector listeners = new Vector();

    /**
     * The provider that is on top of us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;
    
    /**
     * Creates the manager
     * @param msnMessenger BasicMessenger the messanger
     */
    public EventManager(ProtocolProviderServiceMsnImpl msnProvider, 
        BasicMessenger msnMessenger)
    {
        this.msnProvider = msnProvider;
        this.msnMessenger = msnMessenger;

        msnMessenger.addSessionListener(this);
    }

    /**
     * Adds listener of the modification fired events
     * @param listener the modifification listener we're adding
     */
    public void addModificationListener(MsnContactListEventListener listener)
    {
        synchronized(listeners)
        {
            listeners.add(listener);
        }
    }

    /**
     * Removes listener of the modification fired events
     * @param listener EventListener
     */
    public void removeModificationListener(MsnContactListEventListener listener)
    {
        synchronized(listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Called from the underling lib when message is sent to the server
     * @param session Session
     * @param message Message
     * @throws Exception
     */
    public void messageSent(Session session, Message message) throws Exception
    {
        logger.trace(msnMessenger.getOwner().getEmail().getEmailAddress() +
                     " outgoing " + message);
    }

    /**
     * Called from the underling lib when message is received from the server
     * @param session Session
     * @param message Message
     * @throws Exception
     */
    public void messageReceived(Session session, Message message)
        throws Exception
    {
        MsnIncomingMessage incoming = (MsnIncomingMessage)((WrapperMessage)message)
            .getMessage();

        logger.trace(msnMessenger.getOwner().getEmail().getEmailAddress() +
                     " incoming : " + incoming);

        // These are the incoming messages that are NOT handled
        //IncomingADC,IncomingANS,IncomingBLP,IncomingBPR,IncomingBYE
        //IncomingCAL,IncomingCHL,IncomingCHG,IncomingCVR,IncomingFLN
        //IncomingGTC,IncomingILN,IncomingIRO,IncomingJOI,IncomingLSG
        //IncomingLST,IncomingMSG,IncomingNLN
        //IncomingOUT - The notice message that logout. Maybe because of MSN
        //              server maintenance or someone login in other place.
        //IncomingPRP,IncomingQNG,IncomingQRY,IncomingREA,IncomingRNG
        //IncomingSYN,IncomingUBX,IncomingURL,IncomingUSR,IncomingUUX
        //IncomingUnknown,IncomingVER,IncomingXFR

        if(incoming instanceof IncomingACK)
        {
            //indicate the message has successed send to remote user.
            fireMessageDelivered(((IncomingACK)incoming).getTransactionId());
        }
        else if(incoming instanceof IncomingADC)
        {
            // add user to contact list
            IncomingADC incomingADC = (IncomingADC) incoming;
            if (incomingADC.getId() != null &&
                incomingADC.getList().equals(MsnList.FL))
            {
                MsnContact contact = msnMessenger.getContactList().
                    getContactById(incomingADC.getId());

                if (incomingADC.getGroupId() != null)
                {
                    MsnGroup group = msnMessenger.getContactList().
                        getGroup(incomingADC.getGroupId());

                    fireContactAddedInGroup(contact, group);
                }
                else
                    fireContactAdded(contact);
            }

        }
        else if(incoming instanceof IncomingADG)
        {
            //indicate add a group success
            IncomingADG incomingADG  = (IncomingADG)incoming;

            MsnGroupImpl group =
                (MsnGroupImpl)msnMessenger.getContactList().getGroup(incomingADG.getGroupId());

            fireGroupAdded(group);
        }
        else if(incoming instanceof IncomingNAK)
        {
            //indicate the message has not successed send to remote user.
            fireMessageDeliveredFailed(((IncomingNAK)incoming).getTransactionId());
        }
        else if(incoming instanceof IncomingREG)
        {
            //indicate the group name has changed successfully.
            IncomingREG incomingREG  = (IncomingREG)incoming;

            MsnGroupImpl group = (MsnGroupImpl)msnMessenger.getContactList().
                getGroup(incomingREG.getGroupId());
            fireGroupRenamed(group);
        }
        else if(incoming instanceof IncomingRMG)
        {
            // indicate delete the group successfully.
            IncomingRMG incomingRMG  = (IncomingRMG)incoming;
            fireGroupRemoved(incomingRMG.getGroupId());
        }
        else if(incoming instanceof IncomingREM)
        {
            // indicate delete the contact successfully.
            IncomingREM incomingREM  = (IncomingREM)incoming;

            if(incomingREM.getList().equals(MsnList.FL))
            {
                if(incomingREM.getGroupId() == null)
                {
                    // just contact removed
                    MsnContactImpl contact = (MsnContactImpl)
                       msnMessenger.getContactList().getContactById(incomingREM.getId());

                   if(contact != null)
                   {
                       fireContactRemoved(contact);
                   }
                }
                else
                {
                    // contact removed from group
                    MsnContact contact =
                        msnMessenger.getContactList().
                            getContactById(incomingREM.getId());

                    MsnGroup group =
                        msnMessenger.getContactList().
                            getGroup(incomingREM.getGroupId());

                   fireContactRemovedFromGroup(contact, group);
                }
            }
        }
        else if(incoming instanceof IncomingOUT)
        {
            IncomingOUT incomingOUT  = (IncomingOUT)incoming;
            if(incomingOUT.isLoggingFromOtherLocation())
                fireLoggingFromOtherLocation();
        }
        else if(incoming instanceof IncomingQNG)
        {
            IncomingQNG incomingQNG  = (IncomingQNG)incoming;
            
            connected = true;
        }
    }

    private boolean connected = false;
    private Timer connectionTimer = new Timer();
            
    public void sessionTimeout(Session socketSession) throws Exception
    {
        connectionTimer.schedule(new TimerTask()
        {
            public void run()
            {
                if(!connected && msnProvider.isRegistered())
                {
                    msnProvider.unregister(false);
                    msnProvider.reconnect();
                }
            }
        }, 20000);
        connected = false;
    }

    /**
     * Fired when a message is delivered successfully
     * @param transactionID int
     */
    private void fireMessageDelivered(int transactionID)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).
                    messageDelivered(transactionID);
            }
        }
    }

    /**
     * Fired when a message is not delivered successfully
     * @param transactionID int
     */
    private void fireMessageDeliveredFailed(int transactionID)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).
                    messageDeliveredFailed(transactionID);
            }
        }
    }

    /**
     * Fired when a contact is added successfully
     * @param contact MsnContact
     */
    private void fireContactAdded(MsnContact contact)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).contactAdded(contact);
            }
        }
    }

    /**
     * Fired when a contact is added in a group successfully
     * @param contact MsnContact
     * @param group MsnGroup
     */
    private void fireContactAddedInGroup(MsnContact contact, MsnGroup group)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).
                    contactAddedInGroup(contact, group);
            }
        }
    }

    /**
     * Fired when a contact is removed successfully
     * @param contact MsnContact
     */
    private void fireContactRemoved(MsnContact contact)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).contactRemoved(contact);
            }
        }
    }

    /**
     * Fired when a contact is removed from group successfully
     * @param contact MsnContact
     * @param group MsnGroup
     */
    private void fireContactRemovedFromGroup(MsnContact contact, MsnGroup group)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).
                    contactRemovedFromGroup(contact, group);
            }
        }
    }

    /**
     * Fired when a group is added successfully
     * @param group MsnGroup
     */
    private void fireGroupAdded(MsnGroup group)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).groupAdded(group);
            }
        }
    }

    /**
     * Fired when a group is renamed successfully
     * @param group MsnGroup
     */
    private void fireGroupRenamed(MsnGroup group)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).groupRenamed(group);
            }
        }
    }

    /**
     * Fired when a group is removed successfully
     * @param id String
     */
    private void fireGroupRemoved(String id)
    {
        synchronized(listeners){
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ((MsnContactListEventListener)iter.next()).groupRemoved(id);
            }
        }
    }

    /**
     * Fired when we recived event for logging in from other location
     */
    private void fireLoggingFromOtherLocation()
    {
        synchronized (listeners)
        {
            Iterator iter = listeners.iterator();
            while (iter.hasNext())
            {
                ( (MsnContactListEventListener) iter.next())
                    .loggingFromOtherLocation();
            }
        }
    }
}
