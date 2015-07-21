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
package net.java.sip.communicator.impl.protocol.sip.dtmf;

import gov.nist.javax.sip.header.*;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.protocol.*;

/**
 * Sending DTMFs with SIP INFO.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class DTMFInfo
    extends MethodProcessorAdapter
{
    /**
     * The <tt>Logger</tt> used by the <tt>DTMFInfo</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(DTMFInfo.class);

    /**
     * The sub-type of the content of the <tt>Request</tt>s being sent by
     * <tt>DTMFInfo</tt>.
     */
    private static final String CONTENT_SUB_TYPE = "dtmf-relay";

    /**
     * The type of the content of the <tt>Request</tt>s being sent by
     * <tt>DTMFInfo</tt>.
     */
    private static final String CONTENT_TYPE = "application";

    /**
     * Maps call peers and tone and its start time, so we can compute duration.
     */
    private Hashtable<CallPeer, Object[]>
        currentlyTransmittingTones = new Hashtable<CallPeer, Object[]>();

    /**
     * Involved protocol provider service.
     */
    private final ProtocolProviderServiceSipImpl pps;

    /**
     * A list of listeners registered for dtmf tone events.
     */
    private final List<DTMFListener> dtmfListeners =
        new LinkedList<DTMFListener>();

    /**
     * Constructor
     *
     * @param pps the SIP Protocol provider service
     */
    public DTMFInfo(ProtocolProviderServiceSipImpl pps)
    {
        this.pps = pps;

        this.pps.registerMethodProcessor(Request.INFO, this);
    }

    /**
     * Saves the tone we need to send and its start time. With start time we
     * can compute the duration later when we need to send the DTMF.
     *
     * @param callPeer the call peer.
     * @param tone the tone to transmit.
     * @throws OperationFailedException
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void startSendingDTMF(CallPeerSipImpl callPeer, DTMFTone tone)
        throws OperationFailedException,
                NullPointerException,
                IllegalArgumentException
    {
        if(currentlyTransmittingTones.contains(callPeer))
            throw new IllegalStateException(
                "Error starting dtmf tone, already started");

        currentlyTransmittingTones.put(callPeer,
            new Object[]{tone, System.currentTimeMillis()});
    }

    /**
     * Sending of the currently saved tone.
     * @param callPeer
     */
    public void stopSendingDTMF(CallPeerSipImpl callPeer)
    {
        Object[] toneInfo =
            currentlyTransmittingTones.remove(callPeer);

        if(toneInfo != null)
        {
            try
            {
                long startTime = (Long)toneInfo[1];
                sayInfo(callPeer,
                    (DTMFTone) toneInfo[0],
                     System.currentTimeMillis() - startTime);
            } catch (OperationFailedException ex)
            {
                logger.error("Error stoping dtmf ");
            }
        }
    }

    /**
     * This is just a copy of the bye method from the OpSetBasicTelephony,
     * which was enhanced with a body in order to send the DTMF tone
     *
     * @param callPeer destination of the DTMF tone
     * @param dtmftone DTMF tone to send
     * @param duration the duration of the tone
     * @throws OperationFailedException
     */
    private void sayInfo(CallPeerSipImpl callPeer,
                         DTMFTone dtmftone, long duration)
        throws OperationFailedException
    {
        Request info = pps.getMessageFactory().createRequest(
                        callPeer.getDialog(), Request.INFO);

        //here we add the body
        ContentType ct = new ContentType(CONTENT_TYPE, CONTENT_SUB_TYPE);
        String content
            = "Signal=" + dtmftone.getValue()
                + "\r\nDuration=" + duration + "\r\n";

        ContentLength cl = new ContentLength(content.length());
        info.setContentLength(cl);

        try
        {
            info.setContent(content.getBytes(), ct);
        }
        catch (ParseException ex)
        {
            logger.error("Failed to construct the INFO request", ex);
            throw new OperationFailedException(
                "Failed to construct a client the INFO request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);

        }
        //body ended
        ClientTransaction clientTransaction = null;
        try
        {
            clientTransaction = callPeer.getJainSipProvider()
                .getNewClientTransaction(info);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to construct a client transaction from the INFO request"
                , ex);
            throw new OperationFailedException(
                "Failed to construct a client transaction from the INFO request"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        try
        {
            if (callPeer.getDialog().getState()
                == DialogState.TERMINATED)
            {
                //this is probably because the call has just ended, so don't
                //throw an exception. simply log and get lost.
                logger.warn("Trying to send a dtmf tone inside a "
                            +"TERMINATED dialog.");
                return;
            }

            callPeer.getDialog().sendRequest(clientTransaction);
            if (logger.isDebugEnabled())
                logger.debug("sent request:\n" + info);
        }
        catch (SipException ex)
        {
            throw new OperationFailedException(
                "Failed to send the INFO request"
                , OperationFailedException.NETWORK_FAILURE
                , ex);
        }
    }

    /**
     * Just look if the DTMF signal was well received, and log it
     *
     * @param responseEvent the response event
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    @Override
    public boolean processResponse(ResponseEvent responseEvent)
    {
        boolean processed = false;

        if (responseEvent == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("null responseEvent");
        }
        else
        {
            Response response = responseEvent.getResponse();

            if (response == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("null response");
            }
            else
            {
                // Is it even for us?
                ClientTransaction clientTransaction
                    = responseEvent.getClientTransaction();

                if (clientTransaction == null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("null clientTransaction");
                }
                else
                {
                    Request request = clientTransaction.getRequest();

                    if (request == null)
                    {
                        if (logger.isDebugEnabled())
                            logger.debug("null request");
                    }
                    else
                    {
                        ContentTypeHeader contentTypeHeader
                            = (ContentTypeHeader)
                                request.getHeader(ContentTypeHeader.NAME);

                        if ((contentTypeHeader != null)
                                && CONTENT_TYPE.equalsIgnoreCase(
                                        contentTypeHeader.getContentType())
                                && CONTENT_SUB_TYPE.equalsIgnoreCase(
                                        contentTypeHeader.getContentSubType()))
                        {
                            processed = true;

                            int statusCode = response.getStatusCode();

                            if (statusCode == 200)
                            {
                                if (logger.isDebugEnabled())
                                    logger.debug(
                                            "DTMF send succeeded: "
                                                + statusCode);
                            }
                            else
                                logger.error("DTMF send failed: " + statusCode);
                        }
                    }
                }
            }
        }
        return processed;
    }

    /**
     * Receives dtmf info requests.
     */
    @Override
    public boolean processRequest(RequestEvent requestEvent)
    {
        Request request = requestEvent.getRequest();

        ContentTypeHeader contentTypeHeader
            = (ContentTypeHeader)
                request.getHeader(ContentTypeHeader.NAME);

        if ((contentTypeHeader != null)
                && CONTENT_TYPE.equalsIgnoreCase(
                        contentTypeHeader.getContentType())
                && CONTENT_SUB_TYPE.equalsIgnoreCase(
                        contentTypeHeader.getContentSubType()))
        {
            try
            {
                byte[] value;
                Object valueObj = request.getContent();

                if(valueObj instanceof String)
                    value = ((String)valueObj).getBytes("UTF-8");
                else if(valueObj instanceof byte[])
                    value = (byte[])valueObj;
                else
                {
                    logger.error("Unknown content type");
                    return false;
                }

                Properties prop = new Properties();
                prop.load(new ByteArrayInputStream(value));

                String signal = prop.getProperty("Signal");
                String durationStr = prop.getProperty("Duration");

                DTMFTone tone = DTMFTone.getDTMFTone(signal);

                if(tone == null)
                {
                    logger.warn("Unknown tone received: " + tone);
                    return false;
                }

                long duration = 0;
                try
                {
                    duration = Long.parseLong(durationStr);
                }
                catch(NumberFormatException ex)
                {
                    logger.warn("Error parsing duration:" + durationStr, ex);
                }

                // fire event
                fireToneEvent(tone, duration);
            }
            catch(IOException ioe)
            {}

            Response responseOK;

            try
            {
                responseOK = pps.getMessageFactory().createResponse(
                            Response.OK, requestEvent.getRequest());
            }
            catch (ParseException ex)
            {
                //What else could we do apart from logging?
                logger.warn("Failed to create OK for incoming INFO request", ex);
                return false;
            }

            try
            {
                SipStackSharing.getOrCreateServerTransaction(requestEvent).
                    sendResponse(responseOK);
            }
            catch(TransactionUnavailableException ex)
            {
                if (logger.isInfoEnabled())
                    logger.info("Failed to respond to an incoming "
                            +"transactionless INFO request");
                if (logger.isTraceEnabled())
                    logger.trace("Exception was:", ex);
                return false;
            }
            catch (InvalidArgumentException ex)
            {
                //What else could we do apart from logging?
                logger.warn("Failed to send OK for incoming INFO request", ex);
                return false;
            }
            catch (SipException ex)
            {
                //What else could we do apart from logging?
                logger.warn("Failed to send OK for incoming INFO request", ex);
                return false;
            }

            return true;
        }

        return false;
    }

    /**
     * Fire event to interested listeners.
     * @param tone to go into event.
     * @param duration of the tone.
     */
    private void fireToneEvent(DTMFTone tone, long duration)
    {
        Collection<DTMFListener> listeners;
        synchronized (this.dtmfListeners)
        {
            listeners = new ArrayList<DTMFListener>(this.dtmfListeners);
        }

        DTMFReceivedEvent evt = new DTMFReceivedEvent(pps, tone, duration);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching DTMFTone Listeners=" + listeners.size()
            + " evt=" + evt);

        try
        {
            for (DTMFListener listener : listeners)
            {
                listener.toneReceived(evt);
            }
        }
        catch (Throwable e)
        {
            logger.error("Error delivering dtmf tone", e);
        }
    }

    /**
     * Registers the specified DTMFListener with this provider so that it could
     * be notified when incoming DTMF tone is received.
     * @param listener the listener to register with this provider.
     *
     */
    public void addDTMFListener(DTMFListener listener)
    {
        synchronized (dtmfListeners)
        {
            if (!dtmfListeners.contains(listener))
            {
                dtmfListeners.add(listener);
            }
        }
    }

    /**
     * Removes the specified listener from the list of DTMF listeners.
     * @param listener the listener to unregister.
     */
    public void removeDTMFListener(DTMFListener listener)
    {
        synchronized (dtmfListeners)
        {
            dtmfListeners.remove(listener);
        }
    }
}
