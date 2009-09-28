/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.util;

import gov.nist.javax.sip.header.extensions.*;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The class contains a number of utility methods dealing with construction of
 * messages related to telephony.
 *
 * @author Emil Ivov
 */
public class JainSipTelephonyHelper
{
    private static final Logger logger
                        = Logger.getLogger(JainSipTelephonyHelper.class);

    /**
     * Creates an invite request destined for <tt>callee</tt>.
     *
     * @param toAddress the sip address of the callee that the request is meant
     *            for.
     * @param protocolProvider a reference to the protocol provider that is
     * creating the request.
     *
     * @return a newly created sip <tt>Request</tt> destined for <tt>callee</tt>
     *         .
     * @throws OperationFailedException with the corresponding code if creating
     *             the request fails.
     * @throws IllegalArgumentException if <tt>toAddress</tt> does not appear
     * to be a valid destination.
     */
    public static Request createInviteRequest(
                    Address                        toAddress,
                    ProtocolProviderServiceSipImpl protocolProvider)
        throws OperationFailedException, IllegalArgumentException
    {
        // Call ID
        CallIdHeader callIdHeader =
            protocolProvider.getDefaultJainSipProvider().getNewCallId();

        // CSeq
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = headerFactory.createCSeqHeader(1l, Request.INVITE);
        }
        catch (InvalidArgumentException ex)
        {
            // Shouldn't happen
            throwOperationFailedException("An unexpected erro occurred while"
                + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        catch (ParseException exc)
        {
            // shouldn't happen
            throwOperationFailedException("An unexpected erro occurred while"
                + "constructing the CSeqHeadder",
                OperationFailedException.INTERNAL_ERROR, exc);
        }

        // ReplacesHeader
        Header replacesHeader
            = stripReplacesHeader(toAddress, protocolProvider);

        // FromHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            // FromHeader
            fromHeader =
                headerFactory.createFromHeader(
                    protocolProvider.getOurSipAddress(toAddress), localTag);

            // ToHeader
            toHeader = headerFactory.createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            // these two should never happen.
            throwOperationFailedException("An unexpected erro occurred while"
                + "constructing the ToHeader",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        // ViaHeaders
        ArrayList<ViaHeader> viaHeaders =
            protocolProvider.getLocalViaHeaders(toAddress);

        // MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();

        // Contact
        ContactHeader contactHeader = null;
        try
        {
            contactHeader
                = protocolProvider.getContactHeader(toHeader.getAddress());
        }
        catch (IllegalArgumentException exc)
        {
            // encapsulate the illegal argument exception into an OpFailedExc
            // so that the UI would notice it.
            throw new OperationFailedException(
                            exc.getMessage(),
                            OperationFailedException.ILLEGAL_ARGUMENT,
                            exc);
        }

        Request invite = null;
        try
        {
            invite =
                protocolProvider.getMessageFactory().createRequest(
                    toHeader.getAddress().getURI(), Request.INVITE,
                    callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders,
                    maxForwards);

        }
        catch (ParseException ex)
        {
            // shouldn't happen
            throwOperationFailedException("Failed to create invite Request!",
                OperationFailedException.INTERNAL_ERROR, ex);
        }

        // User Agent
        UserAgentHeader userAgentHeader =
            protocolProvider.getSipCommUserAgentHeader();
        if (userAgentHeader != null)
            invite.addHeader(userAgentHeader);

        // add the contact header.
        invite.addHeader(contactHeader);

        // Add the ReplacesHeader if any.
        if (replacesHeader != null)
        {
            invite.setHeader(replacesHeader);
        }

        return invite;
    }

    /**
     * Returns the <tt>ReplacesHeader</tt> contained, if any, in the
     * <tt>URI</tt> of a specific <tt>Address</tt> after removing it
     * from there.
     *
     * @param address the <tt>Address</tt> which is to have its
     *            <tt>URI</tt> examined and modified
     * @param protocolProvider a reference to the protocol provider that is
     * creating the request.
     *
     * @return a <tt>Header</tt> which represents the Replaces header
     *         contained in the <tt>URI</tt> of the specified
     *         <tt>address</tt>; <tt>null</tt> if no such header is
     *         present
     */
    private static Header stripReplacesHeader(
                    Address                        address,
                    ProtocolProviderServiceSipImpl protocolProvider)
        throws OperationFailedException
    {
        javax.sip.address.URI uri = address.getURI();
        Header replacesHeader = null;

        if (uri.isSipURI())
        {
            SipURI sipURI = (SipURI) uri;
            String replacesHeaderValue = sipURI.getHeader(ReplacesHeader.NAME);

            if (replacesHeaderValue != null)
            {
                for (Iterator<?> headerNameIter = sipURI.getHeaderNames();
                        headerNameIter.hasNext();)
                {
                    if (ReplacesHeader.NAME.equals(headerNameIter.next()))
                    {
                        headerNameIter.remove();
                        break;
                    }
                }

                try
                {
                    replacesHeader =
                        protocolProvider.getHeaderFactory().createHeader(
                            ReplacesHeader.NAME, replacesHeaderValue);
                }
                catch (ParseException ex)
                {
                    throw new OperationFailedException(
                        "Failed to create ReplacesHeader from "
                            + replacesHeaderValue,
                        OperationFailedException.INTERNAL_ERROR, ex);
                }
            }
        }
        return replacesHeader;
    }

    /**
     * Logs a specific message and associated <tt>Throwable</tt> cause as an
     * error using the current <tt>Logger</tt> and then throws a new
     * <tt>OperationFailedException</tt> with the message, a specific error code
     * and the cause.
     *
     * @param message the message to be logged and then wrapped in a new
     *            <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     *            <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     *            an error and have a new <tt>OperationFailedException</tt>
     *            thrown
     * @throws OperationFailedException
     */
    private static void throwOperationFailedException(String message,
                                                      int errorCode,
        Throwable cause) throws OperationFailedException
    {
        logger.error(message, cause);
        throw new OperationFailedException(message, errorCode, cause);
    }
}
