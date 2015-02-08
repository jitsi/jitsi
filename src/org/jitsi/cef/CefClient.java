/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

/**
 * Provides a base implementation of the native/unmanaged <tt>CefClient</tt>
 * interface which enables providing handler implementations.
 *
 * @author Lyubomir Marinov
 */
public class CefClient
    extends CefBase
{
    /** Initializes a new <tt>CefClient</tt> instance. */
    public CefClient()
    {
    }

    /**
     * Allocates a native/unmanaged <tt>CefClient</tt> implementation to be
     * associated with this instance.
     *
     * @return a <tt>CefBase *</tt> to the native/unmanaged <tt>CefClient</tt>
     * implementation to be associated with this instance
     */
    protected native long _alloc_();

    /**
     * Returns the handler for browser life span events.
     *
     * @return the handler for browser life span events
     */
    public CefLifeSpanHandler GetLifeSpanHandler()
    {
        return null;
    }
}
