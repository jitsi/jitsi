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

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.*;
import org.jitsi.util.xml.*;
import org.w3c.dom.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.message.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * Provides operations necessary to monitor line activity and pickup calls
 * if needed. BLF stands for Busy Lamp Field.
 * Implementation using rfc4235 and rfc5359#section-2.16.
 *
 * @author Damian Minkov
 */
public class OperationSetTelephonyBLFSipImpl
    implements OperationSetTelephonyBLF,
               RegistrationStateChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetTelephonyBLFSipImpl.class);

    /**
     * Account property to enable/disable OperationSetTelephonyBLF.
     */
    public static final String BLF_ENABLED_ACC_PROP = "BLF_ENABLED";

    /**
     * Account property prefix to set/provision monitored lines.
     */
    public static final String BLF_LINE_ACC_PROP_PREFIX = "BLF_LINE";

    /**
     * Account property suffix to set/provision monitored line address.
     */
    public static final String BLF_LINE_ADDR_ACC_PROP_SUFFIX = "Address";
    /**
     * Account property suffix to set/provision monitored line display name.
     */
    public static final String BLF_LINE_NAME_ACC_PROP_PREFIX = "Name";
    /**
     * Account property suffix to set/provision monitored line group.
     */
    public static final String BLF_LINE_GROUP_ACC_PROP_PREFIX = "Group";

    /**
     * The name of the event package supported by
     * <tt>BLFServiceImpl</tt> in SUBSCRIBE
     * and NOTIFY requests.
     */
    static final String EVENT_PACKAGE = "dialog";

    /**
     * The content sub-type of the content supported in NOTIFY requests handled
     * by <tt>OperationSetTelephonyBLFSipImpl</tt>.
     */
    private static final String CONTENT_SUB_TYPE = "dialog-info+xml";

    /**
     * The time in seconds after which a <tt>Subscription</tt> should be expired
     * by the <tt>OperationSetTelephonyBLFSipImpl</tt> instance
     * which manages it.
     */
    private static final int SUBSCRIPTION_DURATION = 3600;

    /**
     * How many seconds before a timeout should we refresh our state
     */
    private static final int REFRESH_MARGIN = 60;

    /**
     * A list of listeners.
     */
    private final List<BLFStatusListener> listeners
        = new ArrayList<BLFStatusListener>();

    /**
     * List of monitored lines.
     */
    private List<Line> lines = new ArrayList<Line>();

    /**
     * The parent provider.
     */
    private ProtocolProviderServiceSipImpl provider;

    /**
     * The <code>EventPackageSubscriber</code> which provides the ability of
     * this instance to act as a subscriber for the dialog+info event package.
     */
    private final EventPackageSubscriber subscriber;

    /**
     * The timer which will handle all the scheduled tasks
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * Namespace wildcard.
     */
    private static final String ANY_NS = "*";

    /**
     * The property to access details for the dataObject Line.
     */
    private static final String DATA_PROP
        = LineDetails.class.getCanonicalName();

    /**
     * Constructs the operations set and initializes the monitored lines.
     * @param provider
     */
    public OperationSetTelephonyBLFSipImpl(
        ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(this);

        initLines();

        this.subscriber
            = new EventPackageSubscriber(
            this.provider,
            EVENT_PACKAGE,
            SUBSCRIPTION_DURATION,
            CONTENT_SUB_TYPE,
            this.timer,
            REFRESH_MARGIN);
        this.provider.registerEvent(EVENT_PACKAGE);
    }

    /**
     * Initializes the lines from the account properties.
     */
    private void initLines()
    {
        Map<String, String> props
            = provider.getAccountID().getAccountProperties();

        Map<String,String[]> lines
            = new HashMap<String, String[]>();

        // parse the properties into the map where the index is the key
        for(Map.Entry<String, String> entry : props.entrySet())
        {
            String pName = entry.getKey();
            String entryValue = entry.getValue();
            String ix;

            if(!pName.startsWith(BLF_LINE_ACC_PROP_PREFIX) || entryValue == null)
                continue;

            entryValue = entryValue.trim();

            if(!pName.contains("."))
                continue;

            pName = pName.replaceAll(BLF_LINE_ACC_PROP_PREFIX + ".", "");

            if(!pName.contains("."))
                continue;

            ix = pName.substring(0, pName.lastIndexOf('.')).trim();

            String[] lineValues = lines.get(ix);
            if(lineValues == null)
            {
                lineValues = new String[3];
                lines.put(ix, lineValues);
            }

            if(pName.contains(BLF_LINE_ADDR_ACC_PROP_SUFFIX))
            {
                lineValues[0] = entryValue;
            }
            else if(pName.contains(BLF_LINE_NAME_ACC_PROP_PREFIX))
            {
                lineValues[1] = entryValue;
            }
            else if(pName.contains(BLF_LINE_GROUP_ACC_PROP_PREFIX))
            {
                lineValues[2] = entryValue;
            }
        }

        for(Map.Entry<String, String[]> en : lines.entrySet())
        {
            String[] vals = en.getValue();

            this.lines.add(new Line(vals[0], vals[1], vals[2], this.provider));
        }
    }

    /**
     * Adds BLFStatus listener
     * @param listener the listener to add.
     */
    @Override
    public void addStatusListener(BLFStatusListener listener)
    {
        synchronized (this.listeners)
        {
            if (!this.listeners.contains(listener))
            {
                this.listeners.add(listener);
            }
        }
    }

    /**
     * Removes BLFStatus listener.
     * @param listener the listener to remove.
     */
    @Override
    public void removeStatusListener(BLFStatusListener listener)
    {
        synchronized (this.listeners)
        {
            this.listeners.remove(listener);
        }
    }

    /**
     * To pickup the call for the monitored line if possible.
     *
     * @param line to try to pick up.
     */
    @Override
    public void pickup(Line line)
        throws OperationFailedException
    {
        LineDetails details = (LineDetails)line.getData(DATA_PROP);
        if(details == null)
            return;

        if(StringUtils.isNullOrEmpty(details.callID)
            || StringUtils.isNullOrEmpty(details.localTag)
            || StringUtils.isNullOrEmpty(details.remoteTag))
            return;

        // replaces
        Address targetAddress = null;
        try
        {
            targetAddress = provider.parseAddressString(line.getAddress());
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to parse address string " + line.getAddress(),
                OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }

        Replaces replacesHeader = null;
        SipURI sipURI = (SipURI) targetAddress.getURI();

        try
        {
            replacesHeader = (Replaces)
                ((HeaderFactoryImpl) provider.getHeaderFactory())
                    .createReplacesHeader(
                        details.callID,
                        details.remoteTag,
                        details.localTag);
        }
        catch (ParseException ex)
        {
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to create Replaces header for target call-id "
                    + details.callID,
                OperationFailedException.ILLEGAL_ARGUMENT, ex, logger);
        }
        try
        {
            sipURI.setHeader(ReplacesHeader.NAME,
                URLEncoder.encode(replacesHeader.encodeBody(
                    new StringBuilder()).toString(), "UTF-8"));
        }
        catch (Exception ex)
        {
            //ParseException or UnsupportedEncodingException
            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                "Failed to set Replaces header " + replacesHeader
                    + " to SipURI " + sipURI,
                OperationFailedException.INTERNAL_ERROR, ex, logger);
        }

        OperationSetBasicTelephonySipImpl telOpSet
            = (OperationSetBasicTelephonySipImpl)provider
                .getOperationSet(OperationSetBasicTelephony.class);

        CallSipImpl call
            = telOpSet.createOutgoingCall(targetAddress, null, null);
    }

    /**
     * List of currently monitored lines.
     * @return list of currently monitored lines.
     */
    @Override
    public List<Line> getCurrentlyMonitoredLines()
    {
        return new ArrayList<Line>(this.lines);
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if(evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            for(Line line : this.lines)
            {
                // Attempt to subscribe.
                try
                {
                    subscriber.poll(new DialogInfoSubscriberSubscription(
                        provider.parseAddressString(line.getAddress()), line));
                }
                catch (OperationFailedException ex)
                {
                    logger.error(
                        "Failed to create and send the subscription", ex);
                }
                catch (ParseException ex)
                {
                    logger.error(
                        "Failed to create and send the subscription", ex);
                }
            }
        }
        else if(evt.getNewState().equals(RegistrationState.UNREGISTERING))
        {
            timer.cancel();
        }
        else if(evt.getNewState().equals(
                        RegistrationState.CONNECTION_FAILED)
                || evt.getNewState().equals(
                        RegistrationState.AUTHENTICATION_FAILED)
                || evt.getNewState().equals(
                        RegistrationState.UNREGISTERED))
        {
            if (subscriber != null)
            {
                for(Line line : this.lines)
                {
                    try
                    {
                        subscriber.removeSubscription(
                            provider.parseAddressString(line.getAddress()));
                    }
                    catch(ParseException ex)
                    {
                        if(logger.isDebugEnabled())
                        {
                            logger.debug("Failed to remove subscription for "
                                + line.getAddress());
                        }
                    }

                    fireEvent(line, BLFStatusEvent.STATUS_OFFLINE);
                }
            }
            timer.cancel();
        }
    }

    /**
     * Fires event.
     * @param line
     * @param eventType
     */
    private void fireEvent(Line line, int eventType)
    {
        LineDetails details = (LineDetails)line.getData(DATA_PROP);
        if(details == null)
        {
            details = new LineDetails();
            line.setData(DATA_PROP, details);
        }
        details.lastStatusEvent = eventType;

        BLFStatusEvent evt
            = new BLFStatusEvent(line, eventType);

        Collection<BLFStatusListener> listeners;
        synchronized (this.listeners)
        {
            listeners
                = new ArrayList<BLFStatusListener>(this.listeners);
        }

        if (logger.isInfoEnabled())
            logger.info(
                "Dispatching BLF change. Listeners="
                    + listeners.size()
                    + " evt=" + evt
                    + " line=" + line.getAddress());

        for (BLFStatusListener listener : listeners)
            listener.blfStatusChanged(evt);
    }

    /**
     * Convert a xml document
     *
     * @param document the document as a String
     *
     * @return a <tt>Document</tt> representing the document or null if an
     * error occur
     */
    Document convertDocument(byte[] document)
    {
        try
        {
            return XMLUtils.createDocument(new String(document, "UTF-8"));
        }
        catch (Exception e)
        {
            logger.error("Can't convert the string into a xml document", e);
            return null;
        }
    }

    /**
     * Secured call to XMLUtils.getText (no null returned but an empty string)
     *
     * @param node the node with which call <tt>XMLUtils.getText()</tt>
     *
     * @return the string contained in the node or an empty string if there is
     * no text information in the node.
     */
    private String getTextContent(Element node)
    {
        String res = XMLUtils.getText(node);

        if (res == null)
        {
            logger.warn("no text for element '" + node.getNodeName() + "'");
            return "";
        }

        return res;
    }

    /**
     * Represents a subscription to the dialog+info event package of a specific
     * <code>Address</code>.
     *
     * @author Damian Minkov
     */
    private class DialogInfoSubscriberSubscription
        extends EventPackageSubscriber.Subscription
    {
        /**
         * The line.
         */
        private final Line line;

        /**
         * Initializes a new <code>PresenceSubscriberSubscription</code>
         * instance which is to represent a subscription to the dialog event
         * package of a specific <code>Address</code>.
         *
         * @param address the <code>Address</code> which is the notifier
         *                the new subscription is to subscribed to
         * @throws OperationFailedException if we fail extracting
         *                                  <tt>address</tt>'s address.
         */
        public DialogInfoSubscriberSubscription(Address address, Line line)
            throws
            OperationFailedException
        {
            super(address);

            this.line = line;
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processActiveRequest(RequestEvent
         * , byte[]).
         */
        @Override
        protected void processActiveRequest(
            RequestEvent requestEvent,
            byte[] rawContent)
        {
            if(rawContent == null)
                return;

            Document doc = convertDocument(rawContent);

            if (doc == null)
                return;

            if (logger.isTraceEnabled())
                logger.trace("parsing:\n" + new String(rawContent));

            LineDetails details = (LineDetails)line.getData(DATA_PROP);
            if(details == null)
            {
                details = new LineDetails();
                line.setData(DATA_PROP, details);
            }

            // <dialog>
            NodeList dialogList = doc.getElementsByTagNameNS(ANY_NS,
                "dialog");

            if(dialogList.getLength() == 0)
            {
                // no dialogs - it is free
                updateLineState(details, "Terminated");
                return;
            }

            for (int i = 0; i < dialogList.getLength(); i++)
            {
                Node dialogNode = dialogList.item(i);
                Element dialogElem = (Element)dialogNode;

                details.callID = dialogElem.getAttribute("call-id");
                details.localTag = dialogElem.getAttribute("local-tag");
                details.remoteTag = dialogElem.getAttribute("remote-tag");

                NodeList states = ((Element)dialogNode)
                    .getElementsByTagNameNS(ANY_NS, "state");

                if(states.getLength() == 0)
                    continue;

                updateLineState(
                    details,
                    getTextContent((Element)states.item(0)));
            }
        }

        /**
         * Dispatch the state from the xml and set the corresponding state
         * of the line.
         *
         * @param state is one of: Trying, Proceeding, Early,
         * Confirmed, Terminated
         */
        private void updateLineState(LineDetails details, String state)
        {
            int newEvent = BLFStatusEvent.STATUS_OFFLINE;

            switch(details.lastStatusEvent)
            {
                case BLFStatusEvent.STATUS_OFFLINE:
                    if(state.equalsIgnoreCase("Trying")
                        || state.equalsIgnoreCase("Proceeding")
                        || state.equalsIgnoreCase("Early"))
                    {
                        newEvent = BLFStatusEvent.STATUS_RINGING;
                    }
                    else if(state.equalsIgnoreCase("Confirmed"))
                    {
                        newEvent = BLFStatusEvent.STATUS_BUSY;
                    }
                    else if(state.equalsIgnoreCase("Terminated"))
                    {
                        newEvent = BLFStatusEvent.STATUS_FREE;
                    }
                    break;
                case BLFStatusEvent.STATUS_FREE:
                    if(state.equalsIgnoreCase("Trying")
                        || state.equalsIgnoreCase("Proceeding")
                        || state.equalsIgnoreCase("Early"))
                    {
                        newEvent = BLFStatusEvent.STATUS_RINGING;
                    }
                    else if(state.equalsIgnoreCase("Confirmed"))
                    {
                        newEvent = BLFStatusEvent.STATUS_BUSY;
                    }
                    else if(state.equalsIgnoreCase("Terminated"))
                    {
                        // status is free so return
                        return;
                    }
                    break;
                case BLFStatusEvent.STATUS_BUSY:
                    if(state.equalsIgnoreCase("Terminated"))
                    {
                        newEvent = BLFStatusEvent.STATUS_FREE;
                    }
                    else
                    {
                        // status is busy so return
                        return;
                    }
                    break;
                case BLFStatusEvent.STATUS_RINGING:
                    if(state.equalsIgnoreCase("Confirmed"))
                    {
                        newEvent = BLFStatusEvent.STATUS_BUSY;
                    }
                    else if(state.equalsIgnoreCase("Terminated"))
                    {
                        newEvent = BLFStatusEvent.STATUS_FREE;
                    }
                    else
                    {
                        // status is ringing so return
                        return;
                    }
                    break;
                default:
                    return;
            }

            fireEvent(line, newEvent);
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processFailureResponse(
         * ResponseEvent, int).
         */
        @Override
        protected void processFailureResponse(
            ResponseEvent responseEvent,
            int statusCode)
        {
            fireEvent(line, BLFStatusEvent.STATUS_OFFLINE);
        }

        /*
         * Implements
         * EventPackageSubscriber.Subscription#processSuccessResponse(
         * ResponseEvent, int).
         */
        @Override
        protected void processSuccessResponse(
            ResponseEvent responseEvent,
            int statusCode)
        {
            switch(statusCode)
            {
                case Response.OK:
                case Response.ACCEPTED:
                    fireEvent(line, BLFStatusEvent.STATUS_FREE);
                    break;
            }
        }

        /**
         * Implements the corresponding <tt>SipListener</tt> method by
         * terminating the corresponding subscription and polling the related
         * address.
         *
         * @param requestEvent the event containing the request that was
         *                     terminated.
         * @param reasonCode   a String indicating the reason of the termination.
         */
        @Override
        protected void processTerminatedRequest(
            RequestEvent requestEvent, String reasonCode)
        {
            fireEvent(line, BLFStatusEvent.STATUS_OFFLINE);
        }
    }

    /**
     * Details for a line.
     */
    private class LineDetails
    {
        /**
         * The current status of the line, the last event fired for it.
         */
        int lastStatusEvent = BLFStatusEvent.STATUS_OFFLINE;

        /**
         * call-id of the dialog if any, used for remote pickup.
         */
        String callID = null;

        /**
         * local-tag of the dialog if any, used for remote pickup.
         */
        String localTag = null;

        /**
         * remote-tag of the dialog if any, used for remote pickup.
         */
        String remoteTag = null;
    }
}
