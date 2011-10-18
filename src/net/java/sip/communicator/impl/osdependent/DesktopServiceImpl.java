/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.desktop.*;

/**
 * Implementation of the <tt>DesktopService</tt>.
 * 
 * @author Yana Stamcheva
 */
public class DesktopServiceImpl
    implements DesktopService
{
    private final Desktop defaultDesktop;

    /**
     * Creates a <tt>DesktopServiceImpl</tt> and initializes the default
     * desktop to use for all desktop operations.
     */
    public DesktopServiceImpl()
    {
        defaultDesktop = Desktop.getDefaultDesktop();
    }

    /**
     * Invokes the default desktop browse method.
     * 
     * @see DesktopService#browse(URI)
     */
    public void browse(URI uri)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().browse(uri);
    }

    /**
     * Invokes the default desktop edit method.
     * 
     * @see DesktopService#edit(File)
     */
    public void edit(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().edit(file);
    }

    /**
     * Invokes the default desktop open method.
     * 
     * @see DesktopService#open(File)
     */
    public void open(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().open(file);
    }

    /**
     * Invokes the default desktop print method.
     * 
     * @see DesktopService#print(File)
     */
    public void print(File file)
        throws NullPointerException,
        IllegalArgumentException,
        UnsupportedOperationException,
        IOException,
        SecurityException
    {
        defaultDesktop.getPeer().print(file);
    }
}
