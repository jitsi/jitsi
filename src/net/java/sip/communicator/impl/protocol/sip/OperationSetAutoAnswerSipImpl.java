/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.util.*;

/**
 * An Operation Set defining options to auto answer/forward incoming calls.
 * Forward calls to specified number using same provider.
 * Auto answering calls unconditional, on existence of certain header name, or
 * on existence of specified header name and value.
 *
 * @author Damian Minkov
 */
public class OperationSetAutoAnswerSipImpl
    extends CallPeerAdapter
    implements OperationSetAutoAnswer
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * The parent operation set.
     */
    private OperationSetBasicTelephonySipImpl telephonySip;

    /**
     * Should we unconditionally answer.
     */
    private boolean answerUnconditional = false;

    /**
     * Should we answer on existence of some header and/or name.
     */
    private boolean answerConditional = false;

    /**
     * The header name to look for.
     */
    private String headerName = null;

    /**
     * The header value to look for, if specified.
     */
    private String headerValue = null;

    /**
     * The call number to use for forwarding calls.
     */
    private String callFwdTo = null;

    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param telephonySip the parent opset.
     */
    OperationSetAutoAnswerSipImpl(
        OperationSetBasicTelephonySipImpl telephonySip)
    {
        this.telephonySip = telephonySip;

        // init values from account props
        load();
    }

    /**
     * Load values from account properties.
     */
    private void load()
    {
        AccountID acc = telephonySip.getProtocolProvider().getAccountID();

        answerUnconditional =
            acc.getAccountPropertyBoolean(AUTO_ANSWER_UNCOND_PROP, false);

        headerName =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_NAME_PROP);
        headerValue =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_VALUE_PROP);
        if(!StringUtils.isNullOrEmpty(headerName))
            answerConditional = true;

        callFwdTo =
            acc.getAccountPropertyString(AUTO_ANSWER_FWD_NUM_PROP);
    }

    /**
     * Saves values to account properties.
     */
    private void save()
    {
        AccountID acc = telephonySip.getProtocolProvider().getAccountID();
        Map<String, String> accProps = acc.getAccountProperties();

        // lets clear anything before saving :)
        accProps.put(AUTO_ANSWER_UNCOND_PROP, null);
        accProps.put(AUTO_ANSWER_COND_NAME_PROP, null);
        accProps.put(AUTO_ANSWER_COND_VALUE_PROP, null);
        accProps.put(AUTO_ANSWER_FWD_NUM_PROP, null);

        if(answerUnconditional)
        {
            accProps.put(AUTO_ANSWER_UNCOND_PROP, Boolean.TRUE.toString());
        }
        else if(answerConditional)
        {
            accProps.put(AUTO_ANSWER_COND_NAME_PROP, headerName);

            if(!StringUtils.isNullOrEmpty(headerValue))
                accProps.put(AUTO_ANSWER_COND_VALUE_PROP, headerValue);
        }
        else if(!StringUtils.isNullOrEmpty(callFwdTo))
        {
            accProps.put(AUTO_ANSWER_FWD_NUM_PROP, callFwdTo);
        }

        acc.setAccountProperties(accProps);
        SipActivator.getProtocolProviderFactory().storeAccount(acc);
    }

    /**
     * Sets the auto answer option to unconditionally answer all incoming calls.
     */
    public void setAutoAnswerUnconditional()
    {
        clearLocal();

        this.answerUnconditional = true;

        save();
    }

    /**
     * Is the auto answer option set to unconditionally
     * answer all incoming calls.
     * @return is auto answer set to unconditional.
     */
    public boolean isAutoAnswerUnconditionalSet()
    {
        return answerUnconditional;
    }

    /**
     * Sets a specified header and its value if they exist in the incoming
     * call packet this will activate auto answer.
     * If value is empty or null it will be considered as any (will search
     * only for a header with that name and ignore the value)
     * @param headerName the name of the header to search
     * @param value the value for the header, can be null.
     */
    public void setAutoAnswerCondition(String headerName, String value)
    {
        clearLocal();

        this.answerConditional = true;
        this.headerName = headerName;
        this.headerValue = value;

        save();
    }

    /**
     * Is the auto answer option set to conditionally
     * answer all incoming calls.
     * @return is auto answer set to conditional.
     */
    public boolean isAutoAnswerConditionSet()
    {
        return answerConditional;
    }

    /**
     * Set to automatically forward all calls to the specified
     * number using the same provider.
     * @param numberTo number to use for forwarding
     */
    public void setCallForward(String numberTo)
    {
        clearLocal();

        this.callFwdTo = numberTo;

        save();
    }

    /**
     * Get the value for automatically forward all calls to the specified
     * number using the same provider..
     * @return numberTo number to use for forwarding
     */
    public String getCallForward()
    {
        return this.callFwdTo;
    }

    /**
     * Clear local settings.
     */
    private void clearLocal()
    {
        this.answerUnconditional = false;
        this.answerConditional = false;
        this.headerName = null;
        this.headerValue = null;
        this.callFwdTo = null;
    }

    /**
     * Clear any previous settings.
     */
    public void clear()
    {
        clearLocal();

        save();
    }

    /**
     * Returns the name of the header if conditional auto answer is set.
     * @return the name of the header if conditional auto answer is set.
     */
    public String getAutoAnswerHeaderName()
    {
        return headerName;
    }

    /**
     * Returns the value of the header for the conditional auto answer.
     * @return the value of the header for the conditional auto answer.
     */
    public String getAutoAnswerHeaderValue()
    {
        return headerValue;
    }

    /**
     * Makes a check before locally creating call, should we just forward it.
     * @param invite the current invite to check.
     * @param serverTransaction the transaction.
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    boolean preCallCheck(Request invite,
                         ServerTransaction serverTransaction)
    {
        if(StringUtils.isNullOrEmpty(callFwdTo))
            return false;

        Response response;
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("will send moved temporally response: ");

            response = telephonySip.getProtocolProvider().getMessageFactory()
                .createResponse(Response.MOVED_TEMPORARILY, invite);

            ContactHeader contactHeader =
                    (ContactHeader)response.getHeader(ContactHeader.NAME);
            AddressFactory addressFactory =
                telephonySip.getProtocolProvider().getAddressFactory();

            String destination = getCallForward();
            if(!destination.startsWith("sip"))
                destination = "sip:" + destination;

            contactHeader.setAddress(addressFactory.createAddress(
                addressFactory.createURI(destination)));

            serverTransaction.sendResponse(response);
            if (logger.isDebugEnabled())
                logger.debug("sent a moved temporally response: "
                    + response);
        }
        catch (Throwable ex)
        {
            logger.error("Error while trying to send a request", ex);
        }

        return true;
    }

    /**
     * Makes a check after creating call locally, should we answer it.
     * @param invite the current invite to check.
     * @param call the created call to answer if needed.
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    boolean followCallCheck(Request invite,
                            CallSipImpl call)
    {
        if(!(answerConditional || answerUnconditional))
            return false;

        // lets check for headers
        if(answerConditional)
        {
            SIPHeader callAnswerHeader =
                (SIPHeader)invite.getHeader(headerName);

            if(callAnswerHeader == null)
                return false;

            if(!StringUtils.isNullOrEmpty(headerValue))
            {
                String value = callAnswerHeader.getHeaderValue();

                if(value == null || !headerValue.equals(value))
                    return false;
            }
        }

        // we are here cause we satisfy the conditional,
        // or unconditional is true
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        while (peers.hasNext())
        {
            final CallPeer peer = peers.next();

            answerPeer(peer);
        }

        return true;
    }

    /**
     * Answers call if peer in correct state or wait for it.
     * @param peer the peer to check and answer.
     */
    private void answerPeer(final CallPeer peer)
    {
        CallPeerState state = peer.getState();

        if (state == CallPeerState.INCOMING_CALL)
        {
            // answer in separate thread, don't block current
            // processing
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        telephonySip.answerCallPeer(peer);
                    }
                    catch (OperationFailedException e)
                    {
                        logger.error("Could not answer to : " + peer
                            + " caused by the following exception: " + e);
                    }
                }
            }, getClass().getName()).start();
        }
        else
        {
            peer.addCallPeerListener(this);
        }
    }

    /**
     * If we peer was not in proper state wait for it and then answer.
     * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {

        CallPeerState newState = (CallPeerState) evt.getNewValue();

        if (newState == CallPeerState.INCOMING_CALL)
        {
            CallPeer peer = evt.getSourceCallPeer();

            peer.removeCallPeerListener(this);

            answerPeer(peer);
        }
        else if (newState == CallPeerState.DISCONNECTED
                || newState == CallPeerState.FAILED)
        {
            evt.getSourceCallPeer().removeCallPeerListener(this);
        }
    }
}
