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

import gov.nist.javax.sip.stack.*;

import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>Router</tt> interface wrapping around JAIN-SIP
 * RI <tt>DefaultRouter</tt> in order to be able to change the outbound proxy
 * depending on the account which sent the request.
 *
 * @author Sebastien Mazy
 */
public class ProxyRouter
    implements Router
{
    /**
     * Logger for this class.
     */
    private static final Logger logger = Logger.getLogger(ProxyRouter.class);

    /**
     * The running JAIN-SIP stack.
     */
    private final SipStack stack;

    /**
     * Used to cache the <tt>DefaultRouter</tt>s. One <tt>DefaultRouter</tt> per
     * outbound proxy.
     */
    private final Map<String, Router> routerCache
        = new HashMap<String, Router>();

    /**
     * The jain-sip router to use for accounts that do not have a proxy or as a
     * default. Do not use this attribute directly but getDefaultRouter() (lazy
     * initialization)
     */
    private Router defaultRouter = null;

    /**
     * Simple constructor. Ignores the <tt>defaultRoute</tt> parameter.
     *
     * @param stack the currently running stack.
     * @param defaultRoute ignored parameter.
     */
    public ProxyRouter(SipStack stack, String defaultRoute)
    {
        if (stack == null)
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
    public Hop getNextHop(Request request) throws SipException
    {
        return this.getRouterFor(request).getNextHop(request);
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
        return this.getRouterFor(request).getNextHops(request);
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
            + "setup and send the following stack trace to"
            + "dev@jitsi.org", new Exception());
        return null;
    }

    /**
     * Retrieves a DefaultRouter whose default route is the outbound proxy of
     * the account which sent the <tt>request</tt>, or a default one.
     *
     * @param request the <tt>Request</tt> which to retrieve a <tt>Router</tt>
     *            for.
     * @return a <tt>Router</tt> with the outbound proxy set for this
     *         <tt>request</tt> if needed, or a default router
     */
    private Router getRouterFor(Request request)
    {
        // any out-of-dialog or dialog creating request should be marked with
        // the service which created it
        Object service  = SipApplicationData.getApplicationData(request,
                SipApplicationData.KEY_SERVICE);
        if (service instanceof ProtocolProviderServiceSipImpl)
        {
            ProtocolProviderServiceSipImpl sipProvider
                = ((ProtocolProviderServiceSipImpl) service);

            String proxy = sipProvider.getConnection().getOutboundProxyString();

            boolean forceLooseRouting
                = sipProvider.getAccountID()
                    .getAccountPropertyBoolean(
                        ProtocolProviderFactory.FORCE_PROXY_BYPASS, false);

            // P2P case
            if (proxy == null || forceLooseRouting )
                return this.getDefaultRouter();

            // outbound proxy case
            Router router = routerCache.get(proxy);
            if (router == null)
            {
                router = new DefaultRouter(stack, proxy);
                routerCache.put(proxy, router);
            }
            return router;
        }

        // check the request is in-dialog
        ToHeader to = (ToHeader) request.getHeader(ToHeader.NAME);
        if (to.getTag() == null)
            logger.error("unable to identify the service which created this "
                    + "out-of-dialog request");

        return this.getDefaultRouter();
    }

    /**
     * Returns and create if needed a default router (no outbound proxy)
     *
     * @return a router with no outbound proxy set
     */
    private Router getDefaultRouter()
    {
        if (this.defaultRouter == null)
            this.defaultRouter = new DefaultRouter(stack, null);
        return this.defaultRouter;
    }
}
