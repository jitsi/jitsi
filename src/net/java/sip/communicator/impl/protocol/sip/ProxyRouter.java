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
 * An implementation of the <tt>Router</tt> interface wrapping around JAIN-SIP
 * RI <tt>DefaultRouter</tt> in order to be able to change the outbound
 * proxy depending on the account which sent the request.
 *
 * @author Sebastien Mazy
 */
public class ProxyRouter
    implements Router
{
    /**
     * Logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ProxyRouter.class);

    /**
     * The running JAIN-SIP stack.
     */
    private final SipStack stack;

    /**
     * Used to cache the <tt>DefaultRouter</tt>s. One <tt>DefaultRouter</tt> per
     * outbound proxy.
     */
    Map<String, Router> routers = new HashMap<String, Router>();

    /**
     * The jain-sip router to use for accounts that do not have a proxy.
     */
    Router defaultRouter = null;

    /**
     * Simple contructor. Ignores the <tt>defaultRoute</tt> parameter.
     *
     * @param stack the currently running stack.
     * @param defaultRoute ignored parameter.
     */
    public ProxyRouter(SipStack stack, String defaultRoute)
    {
        if(stack == null)
            throw new IllegalArgumentException("stack shouldn't be null!");
        this.stack = stack;
        // we don't care about the provided default route
    }

    /**
     * Returns the next hop for this <tt>Request</tt>.
     *
     * @param request <tt>Request</tt> to find the next hop.
     * @return the next hop for the <tt>request</tt>.
     */
    public Hop getNextHop(Request request)
        throws SipException
    {
        logger.debug("Trying to get a router for req: " + request);

        Router router =
            this.getRouterFor(request);
        if(router != null)
            return router.getNextHop(request);

        logger.warn(
            "Hmm we are returning a null router. This doesn't look good",
            new Exception());
        return null;
    }

    /**
     * Returns the next hops for this <tt>Request</tt>.
     *
     * @param request <tt>Request</tt> to find the next hops.
     * @return the next hops for the <tt>request</tt>.
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
     * Returns the outbound proxy for this <tt>Router</tt>.
     *
     * @return the outbound proxy for this <tt>Router</tt>.
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
     * Creates a DefaultRouter whose default route is the outbound proxy of the
     * account which sent the <tt>request</tt>.
     *
     * @param request the <tt>Request</tt> which to build a <tt>Router</tt> for.
     * @return a <tt>Router</tt> with the oubound proxy set for this
     * <tt>request</tt>.
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
                {
                    logger.debug("Returning a router for outbound proxy: "
                        + service.getOutboundProxyString());

                    //no proxy for this account, we need to return the default
                    //router.
                    if( defaultRouter == null)
                        defaultRouter = new DefaultRouter(this.stack, null);

                    return defaultRouter;
                }
                else if(this.routers.containsKey(proxy))
                    return this.routers.get(proxy);
                else
                {


                    Router router = new DefaultRouter(
                            this.stack
                            , service.getOutboundProxyString());
                    this.routers.put(proxy, router);

                    logger.debug("Returning a router for outbound proxy: "
                                 + service.getOutboundProxyString());

                    return router;
                }
            }
        }
        logger.error("couldn't build a router for this request");
        return null;
    }
}
