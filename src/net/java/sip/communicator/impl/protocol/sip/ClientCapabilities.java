/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.*;
import javax.sip.message.*;
import java.text.*;
import net.java.sip.communicator.util.*;
import java.util.*;

/**
 * Handles OPTIONS requests by replying with an OK response containing
 * methods that we support.
 *
 * @author Emil Ivov
 */
public class ClientCapabilities
    implements SipListener
{
    private static Logger logger = Logger.getLogger(ClientCapabilities.class);

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSipImpl provider = null;

    public ClientCapabilities(ProtocolProviderServiceSipImpl protocolProvider)
    {
        this.provider = protocolProvider;
        provider.registerMethodProcessor(Request.OPTIONS, this);
    }

    /**
     * Receives options requests and replies with an OK response containing
     * methods that we support.
     *
     * @param requestEvent the incoming options request.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        Response optionsOK = null;
        try
        {
            optionsOK = provider.getMessageFactory().createResponse(
                Response.OK,
                requestEvent.getRequest());

            Iterator supportedMethods
                = provider.getSupportedMethods().iterator();

            //add to the allows header all methods that we support
            while(supportedMethods.hasNext())
            {
                String method = (String)supportedMethods.next();

                //don't support REGISTERs
                if(method.equals(Request.REGISTER))
                    continue;

                optionsOK.addHeader(
                    provider.getHeaderFactory().createAllowHeader(method));
            }
            
            Iterator events = provider.getKnownEventsList().iterator();
            
            synchronized (provider.getKnownEventsList()) {
                while (events.hasNext()) {
                    String event = (String) events.next();
                    
                    optionsOK.addHeader(provider.getHeaderFactory()
                            .createAllowEventsHeader(event));
                }
            }
            
            //add a user agent header.
            optionsOK.setHeader(provider.getSipCommUserAgentHeader());
        }
        catch (ParseException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to create an incoming OPTIONS request", ex);
            return;
        }

        try
        {
            ServerTransaction sTran =  requestEvent.getServerTransaction();
            if (sTran == null)
            {
                SipProvider sipProvider = (SipProvider)requestEvent.getSource();
                sTran = sipProvider
                    .getNewServerTransaction(requestEvent.getRequest());
            }

            sTran.sendResponse(optionsOK);
        }
        catch (InvalidArgumentException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to send an incoming OPTIONS request", ex);
            return;
        }
        catch (SipException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to send an incoming OPTIONS request", ex);
            return;
        }
    }

    /**
     * ignore. don't needed.
     * @param dialogTerminatedEvent unused
     */
    public void processDialogTerminated(
                            DialogTerminatedEvent dialogTerminatedEvent)
    {

    }

    /**
     * ignore. don't needed.
     * @param exceptionEvent unused
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
    }

    /**
     * ignore for the time being
     * @param responseEvent unused
     */
    public void processResponse(ResponseEvent responseEvent)
    {
    }

    /**
     * ignore for the time being.
     * @param timeoutEvent unused
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
    }

    /**
     * ignore for the time being.
     * @param transactionTerminatedEvent unused
     */
    public void processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
    }
}
