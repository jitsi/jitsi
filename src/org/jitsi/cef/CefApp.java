/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.cef;

import net.java.sip.communicator.util.*;

/**
 * Provides a base implementation of the native/unmanaged <tt>CefApp</tt>
 * interface which enables providing handler implementations.
 *
 * @author Lyubomir Marinov
 */
public class CefApp
    extends CefBase
{
    /**
     * The name of the Java Native Interface (JNI) library which implements the
     * <tt>native</tt> methods related to the integration of Chromium Embedded
     * Framework (CEF).
     */
    private static final String LIBNAME = "jcef";

    /**
     * The <tt>Logger</tt> used by the <tt>CefApp</tt> class and its instances
     * to print out debugging information.
     */
    private static final Logger logger = Logger.getLogger(CefApp.class);

    static
    {
        CefApp.loadLibrary();
    }

    public static boolean CefInitialize(
            CefMainArgs args,
            CefSettings settings,
            CefApp application)
    {
        return
            CefInitialize(
                    args.ptr,
                    settings.ptr,
                    (application == null) ? 0 : application.getPtr());
    }

    static native boolean CefInitialize(
            long args,
            long settings,
            long application);

    /**
     * Loads the Java Native Interface (JNI) library that integrates Chromium
     * Embedded Framework (CEF).
     */
    static void loadLibrary()
    {
        String libname = LIBNAME;

        try
        {
            System.loadLibrary(libname);
        }
        catch (UnsatisfiedLinkError ule)
        {
            logger.error(
                    "Failed to load the Java Native Interface (JNI) library"
                        + " that integrates Chromium Embedded Framework (CEF): "
                        + libname,
                    ule);
            throw ule;
        }
    }

    /** Initializes a new <tt>CefApp</tt> instance. */
    public CefApp()
    {
    }

    /**
     * Allocates a native/unmanaged <tt>CefApp</tt> implementation to be
     * associated with this instance.
     *
     * @return a <tt>CefBase *</tt> to the native/unmanaged <tt>CefApp</tt>
     * implementation to be associated with this instance
     */
    protected native long _alloc_();
}
