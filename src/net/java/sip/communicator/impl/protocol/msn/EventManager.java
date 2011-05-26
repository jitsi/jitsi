/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.sf.jml.*;
import net.sf.jml.impl.*;
import net.sf.jml.net.*;
import net.sf.jml.net.Message;
import net.sf.jml.protocol.*;
import net.sf.jml.protocol.incoming.*;

/**
 * Manager which listens for changing of the contact list and fires some events.
 *
 * @author Damian Minkov
 */
public class EventManager
    extends SessionAdapter
{
    /**
     * The class logger.
     */
    private static final Logger logger = Logger.getLogger(EventManager.class);

    /**
     * Whether we are connected.
     */
    private boolean connected = false;

    /**
     * The timer for monitoring connection.
     */
    private Timer connectionTimer;

    /**
     * Event listeners.
     */
    private final List<MsnContactListEventListener> listeners
        = new Vector<MsnContactListEventListener>();

    /**
     * The messenger.
     */
    private final BasicMessenger msnMessenger;

    /**
     * The provider that is on top of us.
     */
    private final ProtocolProviderServiceMsnImpl msnProvider;

    /**
     * Initializes a new <tt>EventManager</tt> instance which is to manage the
     * events of a specific <tt>BasicMessenger</tt> as part of its operation for
     * the purposes of a specific <tt>ProtocolProviderServiceMsnImpl</tt>.
     *
     * @param msnProvider the <tt>ProtocolProviderServiceMsnImpl</tt> which is
     * the creator of the new instance
     * @param msnMessenger the <tt>BasicMessenger</tt> which is to have its
     * events managed by the new instance
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
     * @param listener the modification listener we're adding
     */
    public void addModificationListener(MsnContactListEventListener listener)
    {
        synchronized(listeners)
        {
            if (!listeners.contains(listener))
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
        if (logger.isTraceEnabled())
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

        if (logger.isTraceEnabled())
            logger.trace(msnMessenger.getOwner().getEmail().getEmailAddress() +
                     " incoming : " + incoming);

        if(incoming instanceof IncomingACK)
        {
            //indicate the message has successed send to remote user.
            fireMessageDelivered(((IncomingACK)incoming).getTransactionId());
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
        else if(incoming instanceof IncomingOUT)
        {
            IncomingOUT incomingOUT  = (IncomingOUT)incoming;
            if(incomingOUT.isLoggingFromOtherLocation())
                fireLoggingFromOtherLocation();
        }
        else if(incoming instanceof IncomingQNG)
        {
            connected = true;
        }
    }

    /**
     * Called when there was timeout on the connection.
     * @param socketSession
     * @throws Exception
     */
    public void sessionTimeout(Session socketSession) throws Exception
    {
        Timer connectionTimer;

        /*
         * Delays the creation of Timer because it immediately starts a new
         * Thread while it may not be necessary at all.
         */
        synchronized (this)
        {
            if (this.connectionTimer == null)
                this.connectionTimer = new Timer("Msn connection timer", true);
            connectionTimer = this.connectionTimer;
        }

        connectionTimer.schedule(new TimerTask()
        {
            public void run()
            {
                if(!connected && msnProvider.isRegistered())
                {
                    msnProvider.fireRegistrationStateChanged(
                        msnProvider.getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_NOT_SPECIFIED,
                    null);
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
        synchronized(listeners)
        {
            for (MsnContactListEventListener listener : listeners)
                listener.messageDelivered(transactionID);
        }
    }

    /**
     * Fired when a message is not delivered successfully
     * @param transactionID int
     */
    private void fireMessageDeliveredFailed(int transactionID)
    {
        synchronized(listeners)
        {
            for (MsnContactListEventListener listener : listeners)
                listener.messageDeliveredFailed(transactionID);
        }
    }

    /**
     * Fired when a group is renamed successfully
     * @param group MsnGroup
     */
    private void fireGroupRenamed(MsnGroup group)
    {
        synchronized(listeners)
        {
            for (MsnContactListEventListener listener : listeners)
                listener.groupRenamed(group);
        }
    }

    /**
     * Fired when we received event for logging in from other location
     */
    private void fireLoggingFromOtherLocation()
    {
        synchronized (listeners)
        {
            for (MsnContactListEventListener listener : listeners)
                listener.loggingFromOtherLocation();
        }
    }
}
