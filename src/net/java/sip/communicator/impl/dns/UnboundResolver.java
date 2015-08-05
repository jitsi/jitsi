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
package net.java.sip.communicator.impl.dns;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.util.*;

import org.xbill.DNS.*;

/**
 * Implementation of the {@link Resolver} interface, wrapping the native NLnet
 * Labs Unbound resolver. Only the basic methods for queries are supported.
 *
 * @author Ingo Bauersachs
 */
public class UnboundResolver
    implements CustomResolver
{
    private final static Logger logger =
        Logger.getLogger(UnboundResolver.class);

    /**
     * Helper class to synchronize on asynchronous queries.
     */
    private static class CallbackData
    {
        /**
         * The resolver consumer that wishes to be informed when the request
         * completed.
         */
        ResolverListener listener;

        /**
         * The unbound session context.
         */
        long context;

        /**
         * The ID of the unbound async query.
         */
        int asyncId;

        /**
         * Java synchronization on top of unbound.
         */
        CountDownLatch sync = new CountDownLatch(1);
    }

    /**
     * Timeout for DNS queries.
     */
    private int timeout = 10000;

    /**
     * The recursive DNS servers answering our queries.
     */
    private String[] forwarders;

    /**
     * DNSSEC trust anchors for signed zones (usually for the root zone).
     */
    private List<String> trustAnchors = new LinkedList<String>();

    /**
     * Pool that executes our queries.
     */
    private ExecutorService threadPool;

    /**
     * Creates a new instance of this class.
     */
    public UnboundResolver()
    {
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Sets a list of forwarders to use instead of the system default.
     *
     * @param forwarders list of servers to use for our queries.
     */
    public void setForwarders(String[] forwarders)
    {
        this.forwarders = forwarders;
    }

    /**
     * Clears any existing trust anchors previously added.
     */
    public void clearTrustAnchors()
    {
        trustAnchors.clear();
    }

    /**
     * Adds a DNSSEC trust anchor validation of the DNSKEYs.
     *
     * @param anchor trust anchor in the form of
     *            "'zone' IN DS 'key tag' 'algorithm' 'digest type' 'digest'"
     */
    public void addTrustAnchor(String anchor)
    {
        trustAnchors.add(anchor);
    }

    /**
     * {@inheritDoc}
     */
    public SecureMessage send(final Message query) throws IOException
    {
        Future<SecureMessage> future = threadPool.submit(
            new Callable<SecureMessage>()
        {
            public SecureMessage call() throws Exception
            {
                if(logger.isDebugEnabled())
                    logger.debug(query);

                SecureMessage secureMessage = null;
                final long context = prepareContext();
                try
                {
                    UnboundResult result = UnboundApi.resolve(
                        context,
                        query.getQuestion().getName().toString(),
                        query.getQuestion().getType(),
                        query.getQuestion().getDClass()
                        );
                    secureMessage = new SecureMessage(result);
                    validateMessage(secureMessage);
                }
                finally
                {
                    UnboundApi.deleteContext(context);
                    if(logger.isDebugEnabled() && secureMessage != null)
                        logger.debug(secureMessage);
                }

                return secureMessage;
            }
        });
        try
        {
            return future.get(timeout, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            logger.error(e);
            throw new IOException(e.getMessage());
        }
        catch (ExecutionException e)
        {
            if(e.getCause() instanceof DnssecRuntimeException)
                throw new DnssecRuntimeException(e.getCause().getMessage());
            logger.error(e);
            throw new IOException(e.getMessage());
        }
        catch (TimeoutException e)
        {
            throw new SocketTimeoutException(e.getMessage());
        }
    }

    /**
     * Method to allow overriders to inspect the message. This class'
     * implementation does nothing.
     *
     * @param msg The message to inspect.
     * @throws DnssecRuntimeException if the inspector does not want the code to
     *             continue normal processing of the answer.
     */
    protected void validateMessage(SecureMessage msg)
        throws DnssecRuntimeException
    {
    }

    /**
     * Prepares a unbound session context initialized with forwarders and trust
     * anchors.
     *
     * @return The context id
     */
    private long prepareContext()
    {
        final long context = UnboundApi.createContext();
        if(logger.isTraceEnabled())
            UnboundApi.setDebugLevel(context, 100);
        for(String fwd : forwarders == null
            ? ResolverConfig.getCurrentConfig().servers()
            : forwarders)
        {
            fwd = fwd.trim();
            if(NetworkUtils.isValidIPAddress(fwd))
            {
                if(fwd.startsWith("["))
                    fwd = fwd.substring(1, fwd.length() - 1);
                UnboundApi.setForwarder(context, fwd);
            }
        }
        for(String anchor : trustAnchors)
        {
            UnboundApi.addTrustAnchor(context, anchor);
        }
        return context;
    }

    /**
     * Cleans up an Unbound session context.
     *
     * @param cbData The helper object of the asynchronous call.
     * @param cancelAsync Whether an outstanding asynchronous unbound query
     *            should be canceled.
     */
    private static synchronized void deleteContext(CallbackData cbData,
        boolean cancelAsync)
    {
        if(cbData.context == 0)
            return;

        if(cancelAsync)
        {
            try
            {
                UnboundApi.cancelAsync(cbData.context, cbData.asyncId);
            }
            catch (UnboundException ignore)
            {}
        }
        UnboundApi.deleteContext(cbData.context);
        cbData.context = 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xbill.DNS.Resolver#sendAsync(org.xbill.DNS.Message,
     * org.xbill.DNS.ResolverListener)
     */
    public CallbackData sendAsync(Message query, ResolverListener listener)
    {
        if(listener == null)
            throw new IllegalArgumentException("listener cannot be null");

        final long context = prepareContext();
        final CallbackData cbData = new CallbackData();
        cbData.listener = listener;
        cbData.context = context;
        int asyncId;
        try
        {
            asyncId = UnboundApi.resolveAsync(
                context,
                query.getQuestion().getName().toString(),
                query.getQuestion().getType(),
                query.getQuestion().getDClass(),
                cbData,
                new UnboundApi.UnboundCallback()
                {
                    public void UnboundResolveCallback(Object data, int err,
                        UnboundResult result)
                    {
                        CallbackData cbData = (CallbackData)data;
                        deleteContext(cbData, false);

                        ResolverListener l = cbData.listener;
                        if(err == 0)
                        {
                            try
                            {
                                l.receiveMessage(data,
                                    new SecureMessage(result));
                            }
                            catch (IOException e)
                            {
                                l.handleException(data, e);
                            }
                        }
                        else
                            l.handleException(data,
                                new Exception(
                                    UnboundApi.errorCodeToString(err)));

                        cbData.sync.countDown();
                    }
                }
            );
        }
        catch (UnboundException e)
        {
            listener.handleException(null, e);
            return null;
        }
        cbData.asyncId = asyncId;
        threadPool.execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    UnboundApi.processAsync(context);
                }
                catch(UnboundException ex)
                {
                    cbData.listener.handleException(this, ex);
                    deleteContext(cbData, false);
                    cbData.sync.countDown();
                }
            }
        });
        return cbData;
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    public void setEDNS(int level)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    @SuppressWarnings("rawtypes")
    public void setEDNS(int level, int payloadSize, int flags, List options)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    public void setIgnoreTruncation(boolean flag)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    public void setPort(int port)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    public void setTCP(boolean flag)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported.
     * @throws UnsupportedOperationException
     */
    public void setTSIGKey(TSIG key)
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.xbill.DNS.Resolver#setTimeout(int)
     */
    public void setTimeout(int secs)
    {
        timeout = secs * 1000;
    }

    /* (non-Javadoc)
     * @see org.xbill.DNS.Resolver#setTimeout(int, int)
     */
    public void setTimeout(int secs, int msecs)
    {
        timeout = secs * 1000 + msecs;
    }

    /**
     * Does nothing.
     */
    public void reset()
    {
    }
}
