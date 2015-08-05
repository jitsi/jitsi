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
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;

/**
 * A implementation of the typing notification operation
 * set.
 *
 * rfc3994
 *
 * @author Damian Minkov
 */
public class OperationSetTypingNotificationsSipImpl
    extends AbstractOperationSetTypingNotifications<ProtocolProviderServiceSipImpl>
    implements SipMessageProcessor,
               MessageListener
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetTypingNotificationsSipImpl.class);

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

    /**
     * Registration listener instance.
     */
    private final RegistrationStateListener registrationListener;

    // XML documents types
    /**
     * The content type of the sent message.
     */
    private static final String CONTENT_TYPE = "application/im-iscomposing+xml";

    /**
     * The subtype of the message.
     */
    private static final String CONTENT_SUBTYPE = "im-iscomposing+xml";

    // isComposing elements and attributes
    /**
     * IsComposing body name space.
     */
    private static final String NS_VALUE = "urn:ietf:params:xml:ns:im-iscomposing";

    /**
     * The state element.
     */
    private static final String STATE_ELEMENT= "state";

    /**
     * The refresh element.
     */
    private static final String REFRESH_ELEMENT= "refresh";

    /**
     * Default refresh time for incoming events.
     */
    private static final int REFRESH_DEFAULT_TIME = 120;

    /**
     * The minimum refresh time used to be sent.
     */
    private static final int REFRESH_TIME = 60;

    /**
     * The state active when composing.
     */
    private static final String COMPOSING_STATE_ACTIVE = "active";

    /**
     * The state idle when composing is finished.
     */
    private static final String COMPOSING_STATE_IDLE = "idle";

    /**
     * The global timer managing the tasks.
     */
    private Timer timer = new Timer();

    /**
     * The timer tasks for received events, it timer time is reached this
     * means the user has gone idle.
     */
    private final List<TypingTask> typingTasks = new Vector<TypingTask>();

    /**
     * Creates an instance of this operation set.
     * @param provider a ref to the <tt>ProtocolProviderServiceImpl</tt>
     * that created us and that we'll use for retrieving the underlying aim
     * connection.
     * @param opSetBasicIm the parent instant messaging operation set.
     */
    OperationSetTypingNotificationsSipImpl(
        ProtocolProviderServiceSipImpl provider,
        OperationSetBasicInstantMessagingSipImpl opSetBasicIm)
    {
        super(provider);

        this.registrationListener = new RegistrationStateListener();
        provider.addRegistrationStateChangeListener(registrationListener);
        this.opSetBasicIm = opSetBasicIm;
        opSetBasicIm.addMessageProcessor(this);
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
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                        + evt.getOldState()
                        + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPresenceSipImpl) parentProvider
                        .getOperationSet(OperationSetPersistentPresence.class);
            }
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

        // create fn not in contact list
        if (from == null)
        {
            //create the volatile contact
            if (fromHeader.getAddress().getDisplayName() != null)
            {
                from = opSetPersPresence.createVolatileContact(
                    fromHeader.getAddress().getURI().toString(),
                    fromHeader.getAddress().getDisplayName().toString());
            }
            else
            {
                from = opSetPersPresence.createVolatileContact(
                    fromHeader.getAddress().getURI().toString());
            }
        }

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

        if (logger.isDebugEnabled())
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
            TypingTask task = findTypingTask(from);

            if(task != null)
            {
                typingTasks.remove(task);
                task.cancel();
            }

            // when a task is canceled it cannot be
            // resheduled, we will create new task each time we shedule
            task = new TypingTask(from, true);
            typingTasks.add(task);

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
     * @param sentMsg map containing sent messages
     * @return whether this message needs further processing(true) or no(false)
     */
    public boolean processResponse(ResponseEvent responseEvent,
                                   Map<String, Message> sentMsg)
    {
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
            if (logger.isDebugEnabled())
                logger.debug("Ack received from the network : "
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
    public boolean processTimeout(TimeoutEvent timeoutEvent,
                                  Map<String, Message> sentMessages)
    {
        Request req = timeoutEvent.getClientTransaction().getRequest();
        ContentTypeHeader ctheader =
            (ContentTypeHeader)req.getHeader(ContentTypeHeader.NAME);

        // ignore messages which are not typing
        // notifications and continue processing
        return
            (ctheader == null)
                || !CONTENT_SUBTYPE
                        .equalsIgnoreCase(ctheader.getContentSubType());
    }

    /**
     * Finds typing task for a contact.
     * @param contact the contact.
     * @return the typing task.
     */
    private TypingTask findTypingTask(Contact contact)
    {
        for (TypingTask typingTask : typingTasks)
        {
            if (typingTask.getContact().equals(contact))
                return typingTask;
        }
        return null;
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
            Node refreshValue = doc.createTextNode(String.valueOf(REFRESH_TIME));
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
        Request messageRequest;
        try
        {
            messageRequest = opSetBasicIm.createMessageRequest(to, message);
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
            opSetBasicIm.sendMessageRequest(messageRequest, to, message);
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

    /**
     * Sending responses.
     * @param requestEvent the request
     * @param response the response code.
     */
    private void sendResponse(RequestEvent requestEvent, int response)
    {
        // answer
        try
        {
            Response ok = parentProvider.getMessageFactory()
                .createResponse(response, requestEvent.getRequest());
            SipStackSharing.getOrCreateServerTransaction(requestEvent).
                sendResponse(ok);
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
            if (logger.isDebugEnabled())
                logger.debug("Invalid argument for createResponse : "
                        + exc.getMessage(),
                        exc);
        }
    }

    /**
     * When a message is delivered fire that typing has stopped.
     * @param evt the received message event
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        Contact from = evt.getSourceContact();
        TypingTask task = findTypingTask(from);

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
     * Frees allocated resources.
     */
    void shutdown()
    {
        parentProvider.removeRegistrationStateChangeListener(
            registrationListener);
    }

    /**
     * Task that will fire typing stopped when refresh time expires.
     */
    private class TypingTask
        extends TimerTask
    {
        /**
         * The contact that is typing in case of receiving the event and
         * the contact to which we are sending notifications in case of
         * sending events.
         */
        private final Contact contact;

        /**
         * Create typing task.
         * @param contact the contact.
         * @param receiving the direction.
         */
        TypingTask(Contact contact, boolean receiving)
        {
            this.contact = contact;
        }

        @Override
        public void run()
        {
            typingTasks.remove(this);

            fireTypingNotificationsEvent(contact, STATE_STOPPED);
        }

        /**
         * @return the contact
         */
        public Contact getContact()
        {
            return contact;
        }
    }
}
