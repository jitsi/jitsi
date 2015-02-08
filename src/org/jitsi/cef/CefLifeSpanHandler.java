/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

/**
 * Provides a base implementation of the native/unmanaged
 * <tt>CefLifeSpanHandler</tt> interface which enables handling events related
 * to browser life span.
 *
 * @author Lyubomir Marinov
 */
public class CefLifeSpanHandler
    extends CefBase
{
    /** Initializes a new <tt>CefLifeSpanHandler</tt> instance. */
    public CefLifeSpanHandler()
    {
    }

    /**
     * Allocates a native/unmanaged <tt>CefLifeSpanHandler</tt> implementation
     * to be associated with this instance.
     *
     * @return a <tt>CefBase *</tt> to the native/unmanaged
     * <tt>CefLifeSpanHandler</tt> implementation to be associated with this
     * instance
     */
    protected native long _alloc_();

    /**
     * Called after a new browser is created.
     *
     * @param browser the new browser that is created
     */
    public void OnAfterCreated(long browser)
    {
    }

    /**
     * Called just before a browser is destroyed.
     *
     * @param browser the browser that is destroyed
     */
    public void OnBeforeClose(long browser)
    {
    }
}
