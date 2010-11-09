/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import javax.sip.*;
import javax.sip.address.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Message Waiting Indication Event rfc3842.
 *
 * @author Damian Minkov
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
     * @param provider
     */
    OperationSetMessageWaitingSipImpl(
            ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(this);
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
            List l = this.messageWaitingNotificationListeners.get(type);
            if(l != null)
                this.messageWaitingNotificationListeners.remove(listener);
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
                        {
                            if(s instanceof MessageSummarySubscriber)
                            {
                                return (MessageSummarySubscriber)s;
                            }
                        }

                        return null;
                    }
                };

            try
            {
                final Address subscribeAddress;
                String vmAddressURI = (String)provider.getAccountID()
                    .getAccountProperty(
                            ProtocolProviderFactory.VOICEMAIL_URI);

                if(StringUtils.isNullOrEmpty(vmAddressURI))
                    subscribeAddress = provider.getRegistrarConnection()
                            .getAddressOfRecord();
                else
                    subscribeAddress = provider.parseAddressString(
                            vmAddressURI); 

                messageWaitingSubscriber.subscribe(
                    new MessageSummarySubscriber(subscribeAddress));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fires new event on message waiting = yes.
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
        {
            listener.messageWaitingNotify(event);
        }
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
         * group 3 - new urgent messages count.
         * group 4 - old urgent messages count.
         */
        private Pattern messageWaitingCountPattern = Pattern.compile(
                "(\\d+)/(\\d+) \\((\\d+)/(\\d+)\\)");

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
                String messageAccount = null;

                BufferedReader input = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(rawContent)));
                String line;
                while((line = input.readLine()) != null)
                {
                    String lcaseLine = line.toLowerCase();
                    if(lcaseLine.startsWith("messages-waiting"))
                    {
                        // we fire event for every message notification
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
                            fireVoicemailNotificationEvent(
                                    msgType,
                                    messageAccount,
                                    Integer.valueOf(matcher.group(1)),
                                    Integer.valueOf(matcher.group(2)),
                                    Integer.valueOf(matcher.group(3)),
                                    Integer.valueOf(matcher.group(4)));
                        }
                    }
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
