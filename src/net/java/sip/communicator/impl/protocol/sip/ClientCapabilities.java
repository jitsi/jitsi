/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.address.*;
import javax.sip.header.*;
import java.text.*;
import java.util.*;
import java.io.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Handles OPTIONS requests by replying with an OK response containing
 * methods that we support.
 *
 * @author Emil Ivov
 */
public class ClientCapabilities
    extends MethodProcessorAdapter
{
    private static Logger logger = Logger.getLogger(ClientCapabilities.class);

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSipImpl provider = null;

    /**
     * The timer that runs the keep-alive task
     */
    private Timer keepAliveTimer = null;

    /**
     * The next long to use as a cseq header value.
     */
    private long nextCSeqValue = 1;

    public ClientCapabilities(ProtocolProviderServiceSipImpl protocolProvider)
    {
        this.provider = protocolProvider;
        provider.registerMethodProcessor(Request.OPTIONS, this);

        provider.addRegistrationStateChangeListener(new RegistrationListener());
    }

    /**
     * Receives options requests and replies with an OK response containing
     * methods that we support.
     *
     * @param requestEvent the incoming options request.
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        Response optionsOK = null;
        try
        {
            optionsOK = provider.getMessageFactory().createResponse(
                Response.OK, requestEvent.getRequest());

            Iterator<String> supportedMethods
                = provider.getSupportedMethods().iterator();

            //add to the allows header all methods that we support
            while(supportedMethods.hasNext())
            {
                String method = supportedMethods.next();

                //don't support REGISTERs
                if(method.equals(Request.REGISTER))
                    continue;

                optionsOK.addHeader(
                    provider.getHeaderFactory().createAllowHeader(method));
            }

            Iterator<String> events = provider.getKnownEventsList().iterator();

            synchronized (provider.getKnownEventsList())
            {
                while (events.hasNext()) {
                    String event = events.next();

                    optionsOK.addHeader(provider.getHeaderFactory()
                            .createAllowEventsHeader(event));
                }
            }
        }
        catch (ParseException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to create an incoming OPTIONS request", ex);
            return false;
        }

        try
        {
            SipStackSharing.getOrCreateServerTransaction(requestEvent).
                sendResponse(optionsOK);
        }
        catch(TransactionUnavailableException ex)
        {
            //this means that we received an OPTIONS request outside the scope
            //of a transaction which could mean that someone is simply sending
            //us b****hit to keep a NAT connection alive, so let's not get too
            //excited.
            logger.info("Failed to respond to an incoming "
                            +"transactionless OPTIONS request");
            logger.trace("Exception was:", ex);
            return false;
        }
        catch (InvalidArgumentException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to send an incoming OPTIONS request", ex);
            return false;
        }
        catch (SipException ex)
        {
            //What else could we do apart from logging?
            logger.warn("Failed to send an incoming OPTIONS request", ex);
            return false;
        }

        return true;
    }

    /**
     * Returns the next long to use as a cseq header value.
     * @return the next long to use as a cseq header value.
     */
    private long getNextCSeqValue()
    {
        return nextCSeqValue++;
    }

    /**
     * Fire event that connection has failed and we had to unregister
     * the protocol provider.
     */
    private void disconnect()
    {
        //don't alert the user if we're already off
       if(provider.getRegistrationState()
               .equals(RegistrationState.UNREGISTERED))
       {
            return;
       }

      provider.getRegistrarConnection().setRegistrationState(
            RegistrationState.CONNECTION_FAILED
            , RegistrationStateChangeEvent.REASON_NOT_SPECIFIED
            , "A timeout occurred while trying to connect to the server.");
    }

    /**
     * The task would continuously send OPTIONs request that we use as a keep
     * alive method.
     */
    private class KeepAliveTask
        extends TimerTask
    {
        public void run()
        {
            try
            {
                logger.logEntry();

                //From
                FromHeader fromHeader = null;
                try
                {
                    //this keep alive task only makes sense in case we have
                    //a registrar so we deliberately use our AOR and do not
                    //use the getOurSipAddress() method.
                    fromHeader = provider.getHeaderFactory().createFromHeader(
                        provider.getRegistrarConnection().getAddressOfRecord(),
                        ProtocolProviderServiceSipImpl.generateLocalTag());
                }
                catch (ParseException ex)
                {
                    //this should never happen so let's just log and bail.
                    logger.error("Failed to generate a from header for "
                                 + "our register request."
                                 , ex);
                    return;
                }

                //Call ID Header
                CallIdHeader callIdHeader
                    = provider.getDefaultJainSipProvider().getNewCallId();

                //CSeq Header
                CSeqHeader cSeqHeader = null;
                try
                {
                    cSeqHeader = provider.getHeaderFactory().createCSeqHeader(
                        getNextCSeqValue(), Request.OPTIONS);
                }
                catch (ParseException ex)
                {
                    //Should never happen
                    logger.error("Corrupt Sip Stack", ex);
                    return;
                }
                catch (InvalidArgumentException ex)
                {
                    //Should never happen
                    logger.error("The application is corrupt", ex);
                    return;
                }

                //To Header
                ToHeader toHeader = null;
                try
                {
                    //this request isn't really going anywhere so we put our
                    //own address in the To Header.
                    toHeader = provider.getHeaderFactory().createToHeader(
                        fromHeader.getAddress(), null);
                }
                catch (ParseException ex)
                {
                    logger.error("Could not create a To header for address:"
                                  + fromHeader.getAddress(),
                                  ex);
                    return;
                }

                //MaxForwardsHeader
                MaxForwardsHeader maxForwardsHeader = provider.
                    getMaxForwardsHeader();
                //Request
                Request request = null;
                try
                {
                    //create a host-only uri for the request uri header.
                    String domain
                        = ((SipURI) toHeader.getAddress().getURI()).getHost();

                    //request URI
                    SipURI requestURI = provider.getAddressFactory()
                        .createSipURI(null, domain);

                    //Via Headers
                    ArrayList<ViaHeader> viaHeaders = provider
                        .getLocalViaHeaders(requestURI);

                    request = provider.getMessageFactory().createRequest(
                          requestURI
                        , Request.OPTIONS
                        , callIdHeader
                        , cSeqHeader
                        , fromHeader
                        , toHeader
                        , viaHeaders
                        , maxForwardsHeader);

                    if (logger.isDebugEnabled())
                        logger.debug("Created OPTIONS request " + request);
                }
                catch (ParseException ex)
                {
                    logger.error("Could not create an OPTIONS request!", ex);
                    return;
                }

                Iterator<String> supportedMethods
                    = provider.getSupportedMethods().iterator();

                //add to the allows header all methods that we support
                while(supportedMethods.hasNext())
                {
                    String method = supportedMethods.next();

                    //don't support REGISTERs
                    if(method.equals(Request.REGISTER))
                        continue;

                    request.addHeader(
                        provider.getHeaderFactory().createAllowHeader(method));
                }

                Iterator<String> events
                                    = provider.getKnownEventsList().iterator();

                synchronized (provider.getKnownEventsList())
                {
                    while (events.hasNext())
                    {
                        String event = events.next();

                        request.addHeader(provider.getHeaderFactory()
                                .createAllowEventsHeader(event));
                    }
                }

                //Contact Header (should contain IP)
                ContactHeader contactHeader = provider
                    .getContactHeader((SipURI)request.getRequestURI());

                request.addHeader(contactHeader);

                //Transaction
                ClientTransaction optionsTrans = null;
                try
                {
                    optionsTrans = provider.getDefaultJainSipProvider()
                        .getNewClientTransaction(request);
                }
                catch (TransactionUnavailableException ex)
                {
                    logger.error("Could not create a register transaction!\n"
                              + "Check that the Registrar address is correct!",
                              ex);
                    return;
                }
                try
                {
                    optionsTrans.sendRequest();
                    logger.debug("sent request= " + request);
                }
                catch (SipException ex)
                {
                    logger.error("Could not send out the options request!", ex);

                    if(ex.getCause() instanceof IOException)
                    {
                        // IOException problem with network
                        disconnect();
                    }

                    return;
                }
            }catch(Exception ex)
            {
                logger.error("Cannot send OPTIONS keep alive", ex);
            }
        }
   }

    private class RegistrationListener
        implements RegistrationStateChangeListener
    {
        /**
        * The method is called by a ProtocolProvider implementation whenever
        * a change in the registration state of the corresponding provider had
        * occurred. The method is particularly interested in events stating
        * that the SIP provider has unregistered so that it would fire
        * status change events for all contacts in our buddy list.
        *
        * @param evt ProviderStatusChangeEvent the event describing the status
        * change.
        */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if(evt.getNewState() == RegistrationState.UNREGISTERING ||
                evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                // stop any task associated with the timer
                if (keepAliveTimer != null)
                {
                    keepAliveTimer.cancel();
                    keepAliveTimer = null;
                }
            } else if (evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                String keepAliveMethod =
                    provider.getAccountID().getAccountPropertyString(
                        ProtocolProviderServiceSipImpl.KEEP_ALIVE_METHOD);

                logger.trace("Keep alive method " + keepAliveMethod);
                if(keepAliveMethod == null ||
                    !keepAliveMethod.equalsIgnoreCase("options"))
                    return;

                int keepAliveInterval =
                    provider.getAccountID().getAccountPropertyInt(
                        ProtocolProviderServiceSipImpl.KEEP_ALIVE_INTERVAL, -1);

                logger.trace("Keep alive inerval is " + keepAliveInterval);
                if (keepAliveInterval > 0
                    && !provider.getRegistrarConnection().isRegistrarless())
                {
                    if (keepAliveTimer == null)
                        keepAliveTimer = new Timer();

                    logger.debug("Scheduling OPTIONS keep alives");

                    keepAliveTimer.schedule(new KeepAliveTask(), 0,
                        keepAliveInterval * 1000);
                }
            }
        }
    }
}
