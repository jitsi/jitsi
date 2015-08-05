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

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * Message Waiting Indication Event rfc3842.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class OperationSetMessageWaitingSipImpl
    implements OperationSetMessageWaiting,
                RegistrationStateChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetMessageWaitingSipImpl.class);

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceSipImpl provider;

    /**
     * The timer which will handle all the scheduled tasks
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * The name of the event package supported by
     * <tt>OperationSetMessageWaitingSipImpl</tt> in SUBSCRIBE
     * and NOTIFY requests.
     */
    static final String EVENT_PACKAGE = "message-summary";

    /**
     * The content sub-type of the content supported in NOTIFY requests handled
     * by <tt>OperationSetMessageWaitingSipImpl</tt>.
     */
    private static final String CONTENT_SUB_TYPE = "simple-message-summary";

    /**
     * The time in seconds after which a <tt>Subscription</tt> should be expired
     * by the <tt>OperationSetMessageWaitingSipImpl</tt> instance
     * which manages it.
     */
    private static final int SUBSCRIPTION_DURATION = 3600;

    /**
     * The <code>EventPackageSubscriber</code> which provides the ability of
     * this instance to act as a subscriber
     * for the message-summary event package.
     */
    private EventPackageSubscriber messageWaitingSubscriber = null;

    /**
     * How many seconds before a timeout should we refresh our state
     */
    private static final int REFRESH_MARGIN = 60;

    /**
     * Listeners that would receive event notifications for
     * new messages by msg type.
     */
    private final Map<MessageType, List<MessageWaitingListener>>
            messageWaitingNotificationListeners
                = new HashMap<MessageType,
                              List<MessageWaitingListener>>();
    /**
     * Number of unread messages, count so we don't duplicate events.
     */
    private int unreadMessages = 0;
    /**
     * Number of old messages, count so we don't duplicate events.
     */
    private int readMessages = 0;
    /**
     * Number of unread urgent messages, count so we don't duplicate events.
     */
    private int unreadUrgentMessages = 0;
    /**
     * Number of old urgent messages, count so we don't duplicate events.
     */
    private int readUrgentMessages = 0;

    /**
     * Creates this operation set.
     *
     * @param provider
     */
    OperationSetMessageWaitingSipImpl(
            ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(this);

        /*
         * Answer with NOT_IMPLEMENTED to message-summary SUBSCRIBEs in order to
         * not have its ServerTransaction remaining in the SIP stack forever .
         */
        this.provider.registerMethodProcessor(
                Request.SUBSCRIBE,
                new MethodProcessorAdapter()
                        {
                            @Override
                            public boolean processRequest(
                                    RequestEvent requestEvent)
                            {
                                return
                                    OperationSetMessageWaitingSipImpl.this
                                            .processRequest(requestEvent);
                            }
                        });
    }

    /**
     * Registers a <tt>MessageWaitingListener</tt> with this
     * operation set so that it gets notifications of new and old
     * messages waiting.
     *
     * @param type register the listener for certain type of messages.
     * @param listener the <tt>MessageWaitingListener</tt>
     * to register.
     */
    public void addMessageWaitingNotificationListener(
            MessageType type,
            MessageWaitingListener listener)
    {
        synchronized (messageWaitingNotificationListeners)
        {
            List<MessageWaitingListener> l =
                    this.messageWaitingNotificationListeners.get(type);
            if(l == null)
            {
                l = new ArrayList<MessageWaitingListener>();
                this.messageWaitingNotificationListeners.put(type, l);
            }

            if(!l.contains(listener))
                l.add(listener);
        }
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new messages waiting notifications delivery.
     *
     * @param type register the listener for certain type of messages.
     * @param listener the <tt>MessageWaitingListener</tt>
     * to unregister.
     */
    public void removeMessageWaitingNotificationListener(
            MessageType type,
            MessageWaitingListener listener)
    {
        synchronized (messageWaitingNotificationListeners)
        {
            List<?> l = this.messageWaitingNotificationListeners.get(type);
            if(l != null)
                l.remove(listener);
        }
    }

    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            Address subscribeAddress = null;
            try
            {
                subscribeAddress = getSubscribeAddress();
            }
            catch (ParseException e)
            {
                logger.error("Failed to parse mailbox subscribe address.", e);
            }

            final MessageSummarySubscriber defaultSubscriber
                = new MessageSummarySubscriber(subscribeAddress);

            messageWaitingSubscriber =
                new EventPackageSubscriber(
                        provider,
                        EVENT_PACKAGE,
                        SUBSCRIPTION_DURATION,
                        CONTENT_SUB_TYPE,
                        timer,
                        REFRESH_MARGIN)
                {
                    /**
                     * We may receive some message-waiting notifications
                     * out of dialog but we still want to process them, as
                     * the server is just not rfc compliant.
                     * This happens with asterisk when using qualify option
                     * for configured user(user is behind nat and we * ping it),
                     * as the sent packet pings delete our subscription dialog.
                     *
                     * @param callId the CallId associated with the
                     * <tt>Subscription</tt> to be retrieved
                     * @return the Subscription.
                     */
                    @Override
                    protected Subscription getSubscription(String callId)
                    {
                        Subscription resultSub = super.getSubscription(callId);

                        if(resultSub != null)
                            return resultSub;

                        // lets find our subscription and return it
                        // as we cannot find it by callid
                        Object[] subs = getSubscriptions();

                        for(Object s : subs)
                            if(s instanceof MessageSummarySubscriber)
                                return (MessageSummarySubscriber)s;

                        // We are returning default subscriber because of early
                        // NOTIFICATION messages and NOTIFICATION messages that
                        // are not from the same dialog. That way we also handle
                        // some NOTIFICATIONS that are send regardless of
                        // subscription failure. We noticed this behavior from
                        // some SIP servers.
                        return defaultSubscriber;
                    }
                };

            if(subscribeAddress != null)
            {
                try
                {
                    messageWaitingSubscriber.subscribe(defaultSubscriber);
                }
                catch(Throwable e)
                {
                    logger.error("Error subscribing for mailbox", e);
                }
            }
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERING))
        {
            if(messageWaitingSubscriber != null)
            {
                try
                {
                    messageWaitingSubscriber.unsubscribe(
                        getSubscribeAddress(), false);
                }
                catch(Throwable t)
                {
                    logger.error("Error unsubscribing mailbox", t);
                }
            }
        }
    }

    /**
     * Returns the subscribe address for current account mailbox, default
     * or configured.
     * @return the subscribe address for current account mailbox.
     * @throws ParseException
     */
    private Address getSubscribeAddress()
        throws ParseException
    {
        String vmAddressURI = provider.getAccountID()
            .getAccountPropertyString(
                    ProtocolProviderFactory.VOICEMAIL_URI);

        if(StringUtils.isNullOrEmpty(vmAddressURI))
            return provider.getRegistrarConnection()
                    .getAddressOfRecord();
        else
            return provider.parseAddressString(
                    vmAddressURI);
    }

    /**
     * Fires new event on message waiting = yes.
     *
     * @param msgTypeStr
     * @param account the account to reach the messages.
     * @param unreadMessages number of unread messages.
     * @param readMessages number of old messages.
     * @param unreadUrgentMessages number of unread urgent messages.
     * @param readUrgentMessages number of old urgent messages.
     */
    private void fireVoicemailNotificationEvent(
        String msgTypeStr,
        String account,
        int unreadMessages,
        int readMessages,
        int unreadUrgentMessages,
        int readUrgentMessages)
    {
        synchronized(this)
        {
            if(this.unreadMessages == unreadMessages
                && this.readMessages == readMessages
                && this.unreadUrgentMessages == unreadUrgentMessages
                && this.readUrgentMessages == readUrgentMessages)
            {
                // no new information skip event;
                return;
            }
            else
            {
                this.unreadMessages = unreadMessages;
                this.readMessages = readMessages;
                this.unreadUrgentMessages = unreadUrgentMessages;
                this.readUrgentMessages = readUrgentMessages;
            }
        }

        MessageType msgType = MessageType.valueOfByType(msgTypeStr);
        MessageWaitingEvent event =
            new MessageWaitingEvent(
                provider,
                msgType,
                account,
                unreadMessages,
                readMessages,
                unreadUrgentMessages,
                readUrgentMessages);

        Iterable<MessageWaitingListener> listeners;
        synchronized (messageWaitingNotificationListeners)
        {
            List<MessageWaitingListener> ls =
                    messageWaitingNotificationListeners.get(msgType);

            if(ls == null)
                return;

            listeners = new ArrayList<MessageWaitingListener>(ls);
        }
        for (MessageWaitingListener listener : listeners)
            listener.messageWaitingNotify(event);
    }

    /**
     * Sends a {@link Response#NOT_IMPLEMENTED} <tt>Response</tt> to a specific
     * {@link Request#SUBSCRIBE} <tt>Request</tt> with <tt>message-summary</tt>
     * event type .
     *
     * @param requestEvent the <tt>Request</tt> to process
     * @return <tt>true</tt> if the specified <tt>Request</tt> has been
     * successfully processed; otherwise, <tt>false</tt>
     */
    private boolean processRequest(RequestEvent requestEvent)
    {
        Request request = requestEvent.getRequest();
        EventHeader eventHeader
            = (EventHeader) request.getHeader(EventHeader.NAME);

        if (eventHeader == null)
        {
            /*
             * We are not concerned by this request, perhaps another listener
             * is. So don't send a 489 / Bad event response here.
             */
            return false;
        }

        String eventType = eventHeader.getEventType();

        if (!EVENT_PACKAGE.equalsIgnoreCase(eventType))
            return false;

        boolean processed = false;

        if (Request.SUBSCRIBE.equals(request.getMethod()))
        {
            processed
                = EventPackageSupport.sendNotImplementedResponse(
                        provider,
                        requestEvent);
        }

        return processed;
    }

    /**
     * Frees allocated resources.
     */
    void shutdown()
    {
        provider.removeRegistrationStateChangeListener(this);
    }

    /**
     * Subscribes and receive result for message-summary event package.
     */
    private class MessageSummarySubscriber
        extends EventPackageSubscriber.Subscription
    {
        /**
         * Matching messages count
         * group 1 - new messages count.
         * group 2 - old messages count.
         * group 3 - urgent messages group (0/0), optional.
         * group 4 - new urgent messages count, optional.
         * group 5 - old urgent messages count, optional.
         */
        private Pattern messageWaitingCountPattern = Pattern.compile(
            "(\\d+)/(\\d+)( \\((\\d+)/(\\d+)\\))*");

        /**
         * Initializes a new <tt>Subscription</tt> instance with a specific
         * subscription <tt>Address</tt>/Request URI and an id tag of the
         * associated Event headers of value <tt>null</tt>.
         *
         * @param toAddress the subscription <tt>Address</tt>/Request URI which is
         *                  to be the target of the SUBSCRIBE requests associated with
         *                  the new instance
         */
        public MessageSummarySubscriber(Address toAddress)
        {
            super(toAddress);
        }

        /**
         * Notifies this <tt>Subscription</tt> that an active NOTIFY
         * <tt>Request</tt> has been received and it may process the
         * specified raw content carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full details of
         *                     the received NOTIFY <tt>Request</tt> including the raw
         *                     content which may be processed by this
         *                     <tt>Subscription</tt>
         * @param rawContent   an array of bytes which represents the raw content carried
         *                     in the body of the received NOTIFY <tt>Request</tt>
         *                     and extracted from the specified <tt>RequestEvent</tt>
         *                     for the convenience of the implementers
         */
        @Override
        protected void processActiveRequest(
                RequestEvent requestEvent, byte[] rawContent)
        {
            // If the message body is missing we have nothing more to do here.
            if (rawContent == null || rawContent.length <= 0)
                return;

            try
            {
                String messageAccount =
                    provider.getAccountID().getAccountPropertyString(
                                ProtocolProviderFactory.VOICEMAIL_CHECK_URI);

                BufferedReader input = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(rawContent)));
                String line;
                boolean messageWaiting = false;
                boolean eventFired = false;
                while((line = input.readLine()) != null)
                {
                    String lcaseLine = line.toLowerCase();
                    if(lcaseLine.startsWith("messages-waiting"))
                    {
                        String messageWaitingStr  =
                            line.substring(line.indexOf(":") + 1).trim();
                        if(messageWaitingStr.equalsIgnoreCase("yes"))
                            messageWaiting = true;
                    }
                    else if(lcaseLine.startsWith("message-account"))
                    {
                        messageAccount =
                            line.substring(line.indexOf(":") + 1).trim();
                    }
                    else if(lcaseLine.startsWith(MessageType.VOICE.toString())
                            || lcaseLine.startsWith(MessageType.FAX.toString())
                            || lcaseLine.startsWith(MessageType.MULTIMEDIA.toString())
                            || lcaseLine.startsWith(MessageType.PAGER.toString())
                            || lcaseLine.startsWith(MessageType.TEXT.toString())
                            || lcaseLine.startsWith(MessageType.NONE.toString()))
                    {
                        String msgType =
                                lcaseLine.substring(0, line.indexOf(":")).trim();
                        String messagesCountValue =
                                line.substring(line.indexOf(":") + 1).trim();

                        Matcher matcher = messageWaitingCountPattern.matcher(
                                messagesCountValue);

                        if(matcher.find())
                        {
                            String newM = matcher.group(1);
                            String oldM = matcher.group(2);
                            String urgentNew = matcher.group(4);
                            String urgentOld = matcher.group(5);

                            fireVoicemailNotificationEvent(
                                    msgType,
                                    messageAccount,
                                    Integer.valueOf(newM),
                                    Integer.valueOf(oldM),
                                    urgentNew == null ?
                                        0 : Integer.valueOf(urgentNew),
                                    urgentOld == null ?
                                        0 : Integer.valueOf(urgentOld));
                            eventFired = true;
                        }
                    }
                }

                // as defined in rfc3842
                //'In some cases, detailed message summaries are not available.'
                // this is a simple workaround that will trigger a notification
                // for one message so we can inform the user that there are
                // messages waiting
                // FIXME: account is null, UI will throw NPE when trying to
                // call account mailbox, account is also not mandatory
                if(messageWaiting && !eventFired)
                {
                    // what is the account to call for retrieving messages?
                    fireVoicemailNotificationEvent(
                        MessageType.VOICE.toString(),
                        messageAccount,
                        1,
                        0,
                        0,
                        0);
                }
            }
            catch(IOException ex)
            {
                logger.error("Error processing message waiting info");
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the failure range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full details
         *                      of the received <tt>Response</tt> including the status
         *                      code which may be processed by this
         *                      <tt>Subscription</tt>
         * @param statusCode    the status code carried in the <tt>Response</tt> and
         *                      extracted from the specified <tt>ResponseEvent</tt>
         *                      for the convenience of the implementers
         */
        @Override
        protected void processFailureResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            if(logger.isDebugEnabled())
                logger.debug("Processing failed: " + statusCode);
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the success range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full details
         *                      of the received <tt>Response</tt> including the status
         *                      code which may be processed by this
         *                      <tt>Subscription</tt>
         * @param statusCode    the status code carried in the <tt>Response</tt> and
         *                      extracted from the specified <tt>ResponseEvent</tt>
         *                      for the convenience of the implementers
         */
        @Override
        protected void processSuccessResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            if(logger.isDebugEnabled())
                logger.debug("Cannot subscripe to presence watcher info!");
        }

        /**
         * Notifies this <tt>Subscription</tt> that a terminating NOTIFY
         * <tt>Request</tt> has been received and it may process the reason
         * code carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full details of
         *                     the received NOTIFY <tt>Request</tt> including the
         *                     reason code which may be processed by this
         *                     <tt>Subscription</tt>
         * @param reasonCode   the code of the reason for the termination carried in the
         *                     NOTIFY <tt>Request</tt> and extracted from the
         *                     specified <tt>RequestEvent</tt> for the convenience of
         *                     the implementers
         */
        @Override
        protected void processTerminatedRequest(
                RequestEvent requestEvent, String reasonCode)
        {
            if(logger.isDebugEnabled())
                logger.debug("Processing terminated: " + reasonCode);
        }
    }
}
