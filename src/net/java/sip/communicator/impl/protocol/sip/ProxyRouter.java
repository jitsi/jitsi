/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.stack.*;
import java.util.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.util.*;

/**
 * An implementation of the router implementation
 * wrappring around DefaultRouter in order to be able
 * to change the outbound depending on the account
 * which sent the request.
 *
 * @author Sebastien Mazy
 */
public class ProxyRouter
    implements Router
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(ProxyRouter.class);

    /**
     * the running JAIN-SIP stack
     */
    private final SipStack stack;

    /**
     * Used to cache the DefaultRouter
     */
    Map<String, Router> routers = new HashMap<String, Router>();

    /**
     * Simple contructor. Ignores the defaultRoute parameter.
     *
     * @see javax.sip.address.Router
     */
    public ProxyRouter(SipStack stack, String defaultRoute)
    {
        if(stack == null)
            throw new IllegalArgumentException("stack shouldn't be null!");
        this.stack = stack;
        // we don't care about the provided default route
    }

    /**
     * @see javax.sip.address.Router#getNextHop
     */
    public Hop getNextHop(Request request)
        throws SipException
    {
        Router router =
            this.getRouterFor(request);
        if(router != null)
            return router.getNextHop(request);
        return null;
    }

    /**
     * @see javax.sip.address.Router#getNextHops
     * @deprecated
     */
    @Deprecated
    public ListIterator getNextHops(Request request)
    {
        Router router =
            this.getRouterFor(request);
        if(router != null)
            return router.getNextHops(request);
        return null;
    }

    /**
     * @see javax.sip.address.Router#getOutboundProxy
     */
    public Hop getOutboundProxy()
    {
        // we can't tell our outbound proxy without a request

        // Emil: we are not quite certain in which cases this method is needed
        // so we are logging a stack trace here.
        logger.fatal("If you see this then please please describe your SIP "
                        +"setup and send the following stack trace to"
                        +"dev@sip-communicator.dev.java.net",
                        new Exception());
        return null;
    }

    /**
     * Creates a DefaultRouter whose default route
     * is the outbound proxy of the account which
     * sent the request.
     *
     * @param request used to determine a default route
     */
    private Router getRouterFor(Request request)
    {
        Address address =
            ((FromHeader) request.getHeader(FromHeader.NAME)).getAddress();
        Set<ProtocolProviderServiceSipImpl> services =
            ProtocolProviderServiceSipImpl.getAllInstances();

        for(ProtocolProviderServiceSipImpl service : services)
        {
            if(request.getRequestURI().isSipURI() &&
                    service.getOurSipAddress((SipURI) request.getRequestURI()).
                    equals(address))
            {
                String proxy = service.getOutboundProxyString();

                if(proxy == null) // no registrar mode
                    return null;
                else if(this.routers.containsKey(proxy))
                    return this.routers.get(proxy);
                else
                {
                    Router router = new DefaultRouter(
                            this.stack
                            , service.getOutboundProxyString());
                    this.routers.put(proxy, router);
                    return router;
                }
            }
        }
        logger.error("couldn't build a router for this request");
        return null;
    }
}
