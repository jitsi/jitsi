/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

public class CefMainArgs
{
    static
    {
        CefApp.loadLibrary();
    }

    private static native void _delete_(long thiz);

    private static native long _new_();

    final long ptr;

    /** Initializes a new <tt>CefMainArgs</tt> instance. */
    public CefMainArgs()
    {
        ptr = _new_();
        if (ptr == 0)
            throw new OutOfMemoryError();
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        try
        {
            _delete_(ptr);
        }
        finally
        {
            super.finalize();
        }
    }
}
