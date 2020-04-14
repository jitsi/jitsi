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
import java.net.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.message.Message;

import gov.nist.javax.sip.*;
import gov.nist.javax.sip.header.*;

import net.java.sip.communicator.impl.protocol.sip.net.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.OSUtils;

/**
 * Handles OPTIONS requests by replying with an OK response containing
 * methods that we support.
 *
 * @author Emil Ivov
 * @author Pawel Domas
 */
public class ClientCapabilities
    extends MethodProcessorAdapter
{

    /**
     * The <tt>Logger</tt> used by the <tt>ClientCapabilities</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ClientCapabilities.class);

    /**
     * The protocol provider that created us.
     */
    private final ProtocolProviderServiceSipImpl provider;

    /**
     * Registration listener instance.
     */
    private final RegistrationListener registrationListener;

    /**
     * The timer that runs the keep-alive task
     */
    private Timer keepAliveTimer = null;

    /**
     * The next long to use as a cseq header value.
     */
    private long nextCSeqValue = 1;

    /**
     * Creates a new instance of this class using the specified parent
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider a reference to the
     * <tt>ProtocolProviderServiceSipImpl</tt> instance that created us.
     */
    public ClientCapabilities(ProtocolProviderServiceSipImpl protocolProvider)
    {
        this.provider = protocolProvider;

        provider.registerMethodProcessor(Request.OPTIONS, this);

        registrationListener = new RegistrationListener();
        provider.addRegistrationStateChangeListener(registrationListener);
    }

    /**
     * Receives options requests and replies with an OK response containing
     * methods that we support.
     *
     * @param requestEvent the incoming options request.
     * @return <tt>true</tt> if request has been successfully processed,
     * <tt>false</tt> otherwise
     */
    @Override
    public boolean processRequest(RequestEvent requestEvent)
    {
        Response optionsOK = null;
        try
        {
            optionsOK = provider.getMessageFactory().createResponse(
                Response.OK, requestEvent.getRequest());

            //add to the allows header all methods that we support
            for (String method : provider.getSupportedMethods())
            {
                //don't support REGISTERs
                if(!method.equals(Request.REGISTER))
                    optionsOK.addHeader(
                            provider.getHeaderFactory().createAllowHeader(
                                    method));
            }

            addAllowEventsHeader(optionsOK);
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
            if (logger.isInfoEnabled())
                logger.info("Failed to respond to an incoming "
                        +"transactionless OPTIONS request");
            if (logger.isTraceEnabled())
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
     * Creates a list of known events and add them to the value of
     * Allow-Events header
     * @param message the message to set the newly created header
     * @throws ParseException error on creating header
     */
    private void addAllowEventsHeader(Message message)
        throws ParseException
    {
        Iterable<String> knownEventsList = provider.getKnownEventsList();

        AllowEventsList eventsList = new AllowEventsList();

        synchronized (knownEventsList)
        {
            for (String event : knownEventsList)
            {
                eventsList.add(
                    (AllowEvents)provider.getHeaderFactory()
                        .createAllowEventsHeader(event));
            }
        }

        message.setHeader(eventsList);
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
     * Frees allocated resources.
     */
    void shutdown()
    {
        provider.removeRegistrationStateChangeListener(registrationListener);
    }

    /**
     * The task would continuously send OPTIONs request that we use as a keep
     * alive method.
     */
    private class OptionsKeepAliveTask
        extends TimerTask
    {
        @Override
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
                        SipMessageFactory.generateLocalTag());
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

                addAllowEventsHeader(request);

                //Transaction
                ClientTransaction optionsTrans = null;
                try
                {
                    optionsTrans = provider.getDefaultJainSipProvider()
                        .getNewClientTransaction(request);
                }
                catch (TransactionUnavailableException ex)
                {
                    logger.error("Could not create options transaction!\n",
                              ex);
                    return;
                }
                try
                {
                    optionsTrans.sendRequest();
                    if (logger.isDebugEnabled())
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

    /**
     * Class implements CRLF keep alive method.
     */
    private class CRLfKeepAliveTask
        extends TimerTask
    {

        @Override
        public void run()
        {
            ProxyConnection connection = provider.getConnection();
            if(connection == null)
            {
                logger.error("No connection found to send CRLF keep alive" +
                                 " with " + provider);
                return;
            }

            ListeningPoint lp
                = provider.getListeningPoint(connection.getTransport());

            if( !(lp instanceof ListeningPointExt) )
            {
                logger.error("ListeningPoint is not ListeningPointExt" +
                                 "(or is null)");
                return;
            }

            InetSocketAddress address = connection.getAddress();
            try
            {
                ((ListeningPointExt)lp)
                    .sendHeartbeat( address.getAddress().getHostAddress(),
                                    address.getPort() );
            }
            catch (IOException e)
            {
                logger.error("Error while sending a heartbeat", e);
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
                evt.getNewState() == RegistrationState.UNREGISTERED ||
                evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED ||
                evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                // stop any task associated with the timer
                if (keepAliveTimer != null)
                {
                    keepAliveTimer.cancel();
                    keepAliveTimer = null;
                }
            }
            else if (evt.getNewState().equals(RegistrationState.REGISTERED))
            {
                String keepAliveMethod =
                    provider.getAccountID().getAccountPropertyString(
                        ProtocolProviderFactory.KEEP_ALIVE_METHOD);

                if (logger.isTraceEnabled())
                    logger.trace("Keep alive method " + keepAliveMethod);
                // options is default keep-alive, if property is missing
                // then options is used
                if(keepAliveMethod != null &&
                    !(keepAliveMethod.equalsIgnoreCase("options")
                      || keepAliveMethod.equalsIgnoreCase("crlf")))
                    return;

                int keepAliveInterval =
                    provider.getAccountID().getAccountPropertyInt(
                        ProtocolProviderFactory.KEEP_ALIVE_INTERVAL, -1);

                if (logger.isTraceEnabled())
                    logger.trace("Keep alive interval is " + keepAliveInterval);
                if (keepAliveInterval > 0
                    && !provider.getRegistrarConnection().isRegistrarless())
                {
                    if (keepAliveTimer == null)
                        keepAliveTimer = new Timer();

                    TimerTask keepAliveTask;
                    // CRLF is used by default on Android
                    if( (OSUtils.IS_ANDROID && keepAliveMethod == null)
                        || "crlf".equalsIgnoreCase(keepAliveMethod) )
                    {
                        keepAliveTask = new CRLfKeepAliveTask();
                    }
                    else
                    {
                        // OPTIONS
                        keepAliveTask = new OptionsKeepAliveTask();
                    }

                    if (logger.isDebugEnabled())
                        logger.debug("Scheduling keep alives: "+keepAliveTask);

                    keepAliveTimer.schedule(keepAliveTask, 0,
                                            keepAliveInterval * 1000);
                }
            }
        }
    }
}
