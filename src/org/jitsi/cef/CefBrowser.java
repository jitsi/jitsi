/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

import java.awt.*;
import java.awt.peer.*;
import java.lang.reflect.*;

import net.java.sip.communicator.util.*;

/**
 * Implements a Web browser <tt>Component</tt> using Chromium Embedded Framework
 * (CEF). In the terms of CEF, <tt>CefBrowser</tt> represents a browser window.
 *
 * @author Lyubomir Marinov
 */
@SuppressWarnings("serial")
public class CefBrowser
    extends Canvas
{
    /**
     * The indicator which determines whether {@link #cefInitialize()} has been
     * invoked and what value it has returned. If <tt>null</tt>, then
     * <tt>cefInitialize()</tt> has not been invoked yet.
     */
    private static Boolean cefInitialize;

    private static final int DEFAULT_WIDTH_OR_HEIGHT = 100;

    /**
     * The <tt>Logger</tt> used by the <tt>CefBrowser</tt> class and its
     * instances to print out debugging information.
     */
    private static final Logger logger = Logger.getLogger(CefBrowser.class);

    /**
     * Invokes the function <tt>CefInitialize</tt> to initialize the Chromium
     * Embedded Framework (CEF) library. In the terms of CEF, initializes the
     * CEF browser process.
     *
     * @return the return value of the invocation of the function
     * <tt>CefInitialize</tt> which is <tt>true</tt> if the library has been
     * successfully initialized; <tt>false</tt>, otherwise
     */
    private static boolean cefInitialize()
    {
        CefSettings settings = new CefSettings();

        settings.setMultiThreadedMessageLoop(true);
        settings.setSingleProcess(true);

        return
            CefApp.CefInitialize(
                    new CefMainArgs(),
                    settings,
                    /* application */ null);
    }

    private CefClient client;

    private long ptr;

    /** Initializes a new <tt>CefBrowser</tt> instance. */
    public CefBrowser()
    {
        // Invoke cefInitialize() once.
        synchronized (CefBrowser.class)
        {
            if (cefInitialize == null)
            {
                try
                {
                    cefInitialize = Boolean.valueOf(cefInitialize());
                }
                finally
                {
                    if (cefInitialize == null)
                        cefInitialize = Boolean.FALSE;
                }
            }
            if (cefInitialize.booleanValue())
            {
                if (logger.isTraceEnabled())
                    logger.trace("CefInitialize succeeded.");
            }
            else
                throw new IllegalStateException("CefInitialize failed.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotify()
    {
        super.addNotify();

        if ((ptr == 0) && (this.client == null))
        {
            long hWndParent = getHWndParent();
            int width = getWidth();
            int height = getHeight();

            if (width <= 0)
                width = DEFAULT_WIDTH_OR_HEIGHT;
            if (height <= 0)
                height = DEFAULT_WIDTH_OR_HEIGHT;

            CefWindowInfo windowInfo = new CefWindowInfo();

            windowInfo.SetAsChild(hWndParent, 0, 0, width, height);

            CefBrowserSettings settings = new CefBrowserSettings();

            CefClient client
                = new CefClient()
                {
                    /**
                     * The <tt>CefLifeSpanHandler</tt> of this
                     * <tt>CefClient</tt>.
                     */
                    private final CefLifeSpanHandler lifeSpanHandler
                        = new CefLifeSpanHandler()
                        {
                            @Override
                            public void OnAfterCreated(long browser)
                            {
                                onLifeSpanHandlerAfterCreated(this, browser);
                            }

                            @Override
                            public void OnBeforeClose(long browser)
                            {
                                onLifeSpanHandlerBeforeClose(this, browser);
                            }
                        };

                    @Override
                    public CefLifeSpanHandler GetLifeSpanHandler()
                    {
                        return lifeSpanHandler;
                    }

                    /**
                     * Called after a new browser is created.
                     *
                     * @param lifeSpanHandler the <tt>CefLifeSpanHandler</tt>
                     * that is being notified
                     * @param browser the new browser that is created
                     */
                    private void onLifeSpanHandlerAfterCreated(
                            CefLifeSpanHandler lifeSpanHandler,
                            long browser)
                    {
                        onClientLifeSpanHandlerAfterCreated(
                                this,
                                lifeSpanHandler,
                                browser);
                    }

                    /**
                     * Called just before a browser is destroyed.
                     *
                     * @param lifeSpanHandler the <tt>CefLifeSpanHandler</tt>
                     * that is being notified
                     * @param browser the browser that is destroyed
                     */
                    private void onLifeSpanHandlerBeforeClose(
                            CefLifeSpanHandler lifeSpanHandler,
                            long browser)
                    {
                        onClientLifeSpanHandlerBeforeClose(
                                this,
                                lifeSpanHandler,
                                browser);
                    }

                    /**
                     * {@inheritDoc}
                     *
                     * Overrides the super implementation in order to release
                     * the <tt>CefBase</tt> instances initialized by this
                     * instance when the reference count of this
                     * <tt>CefBase</tt> instance falls to <tt>0</tt>.
                     */
                    @Override
                    public int Release()
                    {
                        int refCt = super.Release();

                        if (refCt == 0)
                            lifeSpanHandler.Release();
                        return refCt;
                    }
                };
            boolean createBrowser = false;

            try
            {
                this.client = client;
                createBrowser
                    = CefBrowserHost.CreateBrowser(
                            windowInfo,
                            client,
                            /* url */ null,
                            settings);
                if (!createBrowser)
                {
                    logger.error(
                            "Failed to initialize a new Chromium Embedded"
                                + " Framework (CEF) Web browser instance.");
                }
            }
            finally
            {
                if (!createBrowser && (this.client == client))
                    this.client = null;
                if (client != this.client)
                    client.Release();
            }
        }
    }

    /**
     * Retrieves the <tt>HWND</tt> of this <tt>Canvas</tt>.
     *
     * @return the <tt>HWND</tt> of this <tt>Canvas</tt>
     */
    private long getHWndParent()
    {
        @SuppressWarnings("deprecation")
        ComponentPeer componentPeer = getPeer();
        long hWnd = 0;

        if (componentPeer != null)
        {
            Class<? extends ComponentPeer> clazz = componentPeer.getClass();
            Method method;

            try
            {
                method = clazz.getMethod("getHWnd");
            }
            catch (NoSuchMethodException nsme)
            {
                logger.error(
                        "Failed to find method 'getHWnd()' of class '" + clazz
                            + "'.",
                        nsme);
                throw new RuntimeException(nsme);
            }
            if (method != null)
            {
                Class<?> returnType = method.getReturnType();

                if (long.class.equals(returnType))
                {
                    Object o = null;
                    Exception exception = null;

                    try
                    {
                        o = method.invoke(componentPeer);
                    }
                    catch (IllegalAccessException iae)
                    {
                        exception = iae;
                    }
                    catch (InvocationTargetException ite)
                    {
                        exception = ite;
                    }
                    if (exception == null)
                    {
                        hWnd = ((Long) o).longValue();
                    }
                    else
                    {
                        logger.error(
                                "Method 'long getHWnd()' of class '" + clazz
                                    + "' failed.",
                                exception);
                        throw new RuntimeException(exception);
                    }
                }
                else
                {
                    String message
                        = "Failed to find method 'long getHWnd()' of class '"
                            + clazz + "'.";

                    logger.error(message);
                    throw new RuntimeException(message);
                }
            }
        }

        return hWnd;
    }

    /**
     * Called after a new browser is created.
     *
     * @param client the <tt>CefClient</tt> that is being notified
     * @param lifeSpanHandler the <tt>CefLifeSpanHandler</tt> that is being
     * notified
     * @param browser the new browser that is created
     */
    private void onClientLifeSpanHandlerAfterCreated(
            CefClient client,
            CefLifeSpanHandler lifeSpanHandler,
            long browser)
    {
        if ((client == this.client) && (ptr != browser))
            ptr = browser;
    }

    /**
     * Called just before a browser is destroyed.
     *
     * @param client the <tt>CefClient</tt> that is being notified
     * @param lifeSpanHandler the <tt>CefLifeSpanHandler</tt> that is being
     * notified
     * @param browser the browser that is destroyed
     */
    private void onClientLifeSpanHandlerBeforeClose(
            CefClient client,
            CefLifeSpanHandler lifeSpanHandler,
            long browser)
    {
        if ((client == this.client) && (ptr == browser))
            ptr = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(Graphics g)
    {
        // TODO Auto-generated method stub
        super.paint(g);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify()
    {
        super.removeNotify();

        ptr = 0;

        CefClient client = this.client;

        if (client != null)
        {
            this.client = null;
            client.Release();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Overrides the super implementation to skip the clearing of this
     * <tt>Canvas</tt> by filling it with the background color because
     * {@link #paint(Graphics)} does it anyway.
     */
    @Override
    public void update(Graphics g)
    {
        paint(g);
    }
}
