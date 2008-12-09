/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import org.w3c.dom.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

/**
 * A implementation of the typing notification operation
 * set.
 * 
 * rfc3994
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsSipImpl
    implements  OperationSetTypingNotifications, 
                SipMessageProcessor,
                MessageListener
{
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsSipImpl.class);

    /**
     * A list of listeners registered for message events.
     */
    private Vector typingNotificationsListeners = new Vector();

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPresenceSipImpl opSetPersPresence = null;
    
    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetBasicInstantMessagingSipImpl opSetBasicIm = null;
    
    // XML documents types
    private final String CONTENT_TYPE = "application/im-iscomposing+xml";
    private final String CONTENT_SUBTYPE = "im-iscomposing+xml";
    
    // isComposing elements and attributes
    private static final String NS_VALUE = "urn:ietf:params:xml:ns:im-iscomposing";
    private static final String STATE_ELEMENT= "state";
    private static final String REFRESH_ELEMENT= "refresh";
    
    private static final int REFRESH_DEFAULT_TIME = 120;
    
    private static final String COMPOSING_STATE_ACTIVE = "active";
    private static final String COMPOSING_STATE_IDLE = "idle";
    
    private Timer timer = new Timer();
    private Vector<TypingTask> typingTasks = new Vector<TypingTask>();
    
    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     */
    OperationSetTypingNotificationsSipImpl(
        ProtocolProviderServiceSipImpl provider, 
        OperationSetBasicInstantMessagingSipImpl opSetBasicIm)
    {
        this.sipProvider = provider;

        provider.addRegistrationStateChangeListener(new
            RegistrationStateListener());
        this.opSetBasicIm = opSetBasicIm;
        opSetBasicIm.addMessageProcessor(this);
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws IllegalStateException
    {
        if (this.sipProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                + "service before being able to communicate.");
        if (!this.sipProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                + "being able to communicate.");
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPresenceSipImpl) sipProvider
                        .getOperationSet(OperationSetPersistentPresence.class);
            }
        }
    }

    /**
     * Delivers a <tt>TypingNotificationEvent</tt> to all registered listeners.
     * @param sourceContact the contact who has sent the notification.
     * @param evtCode the code of the event to deliver.
     */
    private void fireTypingNotificationsEvent(Contact sourceContact
                                              ,int evtCode)
    {
        logger.debug("Dispatching a TypingNotif. event to "
            + typingNotificationsListeners.size()+" listeners. Contact "
            + sourceContact.getAddress() + " has now a typing status of "
            + evtCode);

        TypingNotificationEvent evt = new TypingNotificationEvent(
            sourceContact, evtCode);

        Iterator listeners = null;
        synchronized (typingNotificationsListeners)
        {
            listeners = new ArrayList(typingNotificationsListeners).iterator();
        }

        while (listeners.hasNext())
        {
            TypingNotificationsListener listener
                = (TypingNotificationsListener) listeners.next();

            listener.typingNotificationReceived(evt);
        }
    }
    
    /**
     * Process the incoming sip messages
     * @param requestEvent the incoming event holding the message
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processMessage(RequestEvent requestEvent)
    {
        // get the content
        String content = null;

        Request req = requestEvent.getRequest();

        ContentTypeHeader ctheader = 
            (ContentTypeHeader)req.getHeader(ContentTypeHeader.NAME);

        // ignore messages which are not typing 
        // notifications and continue processing
        if (ctheader == null || !ctheader.getContentSubType()
                .equalsIgnoreCase(CONTENT_SUBTYPE))
            return true;

        content = new String(req.getRawContent());

        if(content == null || content.length() == 0)
        {
            // send error
            sendResponse(requestEvent, Response.BAD_REQUEST);
            return false;
        }
        
        // who sent this request ?
        FromHeader fromHeader = (FromHeader)
            requestEvent.getRequest().getHeader(FromHeader.NAME);

        if (fromHeader == null)
        {
            logger.error("received a request without a from header");
            return true;
        }

        Contact from = opSetPersPresence.resolveContactID(
            fromHeader.getAddress().getURI().toString());
        
        // parse content
        Document doc = null;
        try
        {
            // parse content
            doc = opSetPersPresence.convertDocument(content);
        }
        catch(Exception e){}
        
        if (doc == null) 
        {
             // send error
             sendResponse(requestEvent, Response.BAD_REQUEST);
             return false;
        }

        logger.debug("parsing:\n" + content);

        // <state>
        NodeList stateList = doc.getElementsByTagNameNS(NS_VALUE,
             STATE_ELEMENT);

        if (stateList.getLength() == 0) 
        {
            logger.error("no state element in this document");
            // send error
            sendResponse(requestEvent, Response.BAD_REQUEST);
            return false;
        }

        Node stateNode = stateList.item(0);
        if (stateNode.getNodeType() != Node.ELEMENT_NODE) 
        {
            logger.error("the state node is not an element");
            // send error
            sendResponse(requestEvent, Response.BAD_REQUEST);
            return false;
        }

        String state = XMLUtils.getText((Element)stateNode);

        if(state == null || state.length() == 0)
        {
            logger.error("the state element without value");
            // send error
            sendResponse(requestEvent, Response.BAD_REQUEST);
            return false;
        }

        // <refresh>
        NodeList refreshList = doc.getElementsByTagNameNS(NS_VALUE,
                REFRESH_ELEMENT);
        int refresh = REFRESH_DEFAULT_TIME;
        if (refreshList.getLength() != 0)
        {
           Node refreshNode = refreshList.item(0);
           if (refreshNode.getNodeType() == Node.ELEMENT_NODE)
           {
               String refreshStr = XMLUtils.getText((Element)refreshNode);

               try
               {
                   refresh = Integer.parseInt(refreshStr);
               }
               catch (Exception e)
               {
                   logger.error("Wrong content for refresh", e);
               }
           }
        }
        
        // process the typing info we have gathered
        if(state.equals(COMPOSING_STATE_ACTIVE))
        {
            TypingTask task = findTypigTask(from);
            
            if(task == null)
            {
                task = new TypingTask(from);
                typingTasks.add(task);
            }
            else
                task.cancel();
            
            timer.schedule(task, refresh * 1000);
         
            fireTypingNotificationsEvent(from, STATE_TYPING);
        }
        else
            if(state.equals(COMPOSING_STATE_IDLE))
            {
                fireTypingNotificationsEvent(from, STATE_PAUSED);
            }
         
        // send ok
        sendResponse(requestEvent, Response.OK);
        return false;
    }
    
    /**
     * Process the responses of sent messages
     * @param responseEvent the incoming event holding the response
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processResponse(ResponseEvent responseEvent, Map sentMsg)
    {
        // get the content
        String content = null;

        Request req = responseEvent.getClientTransaction().getRequest();

        ContentTypeHeader ctheader = 
            (ContentTypeHeader)req.getHeader(ContentTypeHeader.NAME);

        // ignore messages which are not typing 
        // notifications and continue processing
        if (ctheader == null || !ctheader.getContentSubType()
                .equalsIgnoreCase(CONTENT_SUBTYPE))
            return true;
     
        int status = responseEvent.getResponse().getStatusCode();

        // we retrieve the original message
        String key = ((CallIdHeader)req.getHeader(CallIdHeader.NAME))
            .getCallId();
        
        if (status >= 200 && status < 300)
        {
            logger.debug(
                "Ack received from the network : "
                + responseEvent.getResponse().getReasonPhrase());

            // we don't need this message anymore
            sentMsg.remove(key);
            
            return false;
        }
        else if (status >= 400 && status != 401 && status != 407)
        {
            logger.warn(
                "Error received : "
                + responseEvent.getResponse().getReasonPhrase());
            
            // we don't need this message anymore
            sentMsg.remove(key);
            
            return false;
        }
        
        // process messages as auth required
        return true;
    }
    
    /**
     * Process the timeouts of sent messages
     * @param timeoutEvent the event holding the request
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent, Map sentMessages)
    {
        Request req = timeoutEvent.getClientTransaction().getRequest();
        
        ContentTypeHeader ctheader = 
            (ContentTypeHeader)req.getHeader(ContentTypeHeader.NAME);

        // ignore messages which are not typing 
        // notifications and continue processing
        if (ctheader == null || !ctheader.getContentSubType()
                .equalsIgnoreCase(CONTENT_SUBTYPE))
            return true;
        
        return false;
    }
    
    private TypingTask findTypigTask(Contact contact)
    {
        Iterator<TypingTask> tasksIter = typingTasks.iterator();
        while (tasksIter.hasNext())
        {
            TypingTask typingTask = tasksIter.next();
            if(typingTask.equals(contact))
                return typingTask;
        }
        
        return null;
    }
    
    /**
     * Adds <tt>l</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     *  we'd like to add.
     */
    public void addTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized(typingNotificationsListeners)
        {
            if(!typingNotificationsListeners.contains(listener))
                typingNotificationsListeners.add(listener);
        }
    }

    /**
     * Removes <tt>l</tt> from the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     * we'd like to remove
     */
    public void removeTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized(typingNotificationsListeners)
        {
            typingNotificationsListeners.remove(listener);
        }
    }
    
    public void sendTypingNotification(Contact to, int typingState) 
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        if( !(to instanceof ContactSipImpl) )
           throw new IllegalArgumentException(
               "The specified contact is not a Sip contact."
               + to);
        
        Document doc = opSetPersPresence.createDocument();
        
        Element rootEl = doc.createElementNS(NS_VALUE, "isComposing");
        rootEl.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        doc.appendChild(rootEl);
        
        
        /*
        Element contentType = doc.createElement("contenttype");
        Node contentTypeValue = 
            doc.createTextNode(OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE);
        contentType.appendChild(contentTypeValue);
        rootEl.appendChild(contentType);*/
        
        if(typingState == STATE_TYPING)
        {
            Element state = doc.createElement("state");
            Node stateValue = 
                doc.createTextNode(COMPOSING_STATE_ACTIVE);
            state.appendChild(stateValue);
            rootEl.appendChild(state);

            Element refresh = doc.createElement("refresh");
            Node refreshValue = doc.createTextNode("60");
            refresh.appendChild(refreshValue);
            rootEl.appendChild(refresh);
        }
        else if(typingState == STATE_STOPPED)
        {
            Element state = doc.createElement("state");
            Node stateValue = 
                doc.createTextNode(COMPOSING_STATE_IDLE);
            state.appendChild(stateValue);
            rootEl.appendChild(state);
        }
        else // ignore other events
            return;

        Message message =
            opSetBasicIm.createMessage(opSetPersPresence.convertDocument(doc),
                CONTENT_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
            
        //create the message
        Request mes;
        try
        {
            mes = opSetBasicIm.createMessage(to, message);
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to create the message."
                , ex);
            return;
        }

        try
        {
            opSetBasicIm.sendRequestMessage(mes, to, message);
        }
        catch(TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create messageTransaction.\n"
                + "This is most probably a network connection error."
                , ex);
            return;
        }
        catch(SipException ex)
        {
            logger.error(
                "Failed to send the message."
                , ex);
            return;
        }
    }
    
    private void sendResponse(RequestEvent requestEvent, int response)
    {
        // answer
        try
        {
            Response ok = sipProvider.getMessageFactory()
                .createResponse(response, requestEvent.getRequest());
            SipProvider jainSipProvider = (SipProvider) requestEvent.
                getSource();
            jainSipProvider.getNewServerTransaction(
                requestEvent.getRequest()).sendResponse(ok);
        }
        catch (ParseException exc)
        {
            logger.error("failed to build the response", exc);
        }
        catch (SipException exc)
        {
            logger.error("failed to send the response : "
                         + exc.getMessage(),
                         exc);
        }
        catch (InvalidArgumentException exc)
        {
            logger.debug("Invalid argument for createResponse : "
                         + exc.getMessage(),
                         exc);
        }
    }

    /**
     * When a message is delivered fire that typing has stoped.
     * @param evt the received message event
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        Contact from = evt.getSourceContact();
        TypingTask task = findTypigTask(from);
        
        if(task != null)
        {
            task.cancel();
            
            fireTypingNotificationsEvent(from, STATE_STOPPED); 
        }
    }

    public void messageDelivered(MessageDeliveredEvent evt)
    {}

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {}
    
    /**
     * Task that will fire typing stoppped when refresh time expires.
     */
    private class TypingTask
        extends TimerTask
    {
        Contact typingContact = null;
        
        TypingTask(Contact typingContact)
        {
            this.typingContact = typingContact;
        }
        
        public void run()
        {
            fireTypingNotificationsEvent(typingContact, STATE_STOPPED);
        }
    }
}
