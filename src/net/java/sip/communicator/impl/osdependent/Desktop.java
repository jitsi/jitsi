/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import net.java.sip.communicator.util.Logger;

import org.jdesktop.jdic.desktop.*;
import org.jitsi.util.*;

/**
 * The <tt>Desktop</tt> class handles desktop operations through the default
 * desktop implementation. It choose which implementation to use depending on
 * what is currently available (java 6 or Jdic).
 * 
 * @author Yana Stamcheva
 */
public class Desktop
{
    private static final Logger logger = Logger.getLogger(SystemTray.class);

    private static Desktop defaultDesktop;

    /**
     * Returns the default <tt>Desktop</tt> instance depending on the operating
     * system and java version availability.
     * 
     * @return the default <tt>Desktop</tt> instance
     * @throws UnsupportedOperationException if the operation is not supported
     * @throws HeadlessException
     * @throws SecurityException
     */
    public static Desktop getDefaultDesktop()
        throws  UnsupportedOperationException,
                HeadlessException,
                SecurityException
    {
        if (defaultDesktop != null)
            return defaultDesktop;

        Class<?> awtDesktopClass = null;
        try
        {
            awtDesktopClass = Class.forName("java.awt.Desktop");
        }
        catch (ClassNotFoundException ex)
        {
            // We'll try org.jdesktop.jdic.desktop then.
        }
        DesktopPeer peer = null;
        if (awtDesktopClass != null)
            try
            {
                peer = new AWTDesktopPeer(awtDesktopClass);
            }
            catch (Exception ex)
            {
                logger.error(
                "Failed to initialize the java.awt.SystemTray implementation.",
                ex);

                // We'll try org.jdesktop.jdic.desktop then.
            }
        if (peer == null)
            try
            {
                peer = new JdicDesktopPeer();
            }
            catch (Exception ex)
            {
                logger.error(
                "Failed to initialize the org.jdesktop.jdic.tray implementation.",
                ex);
            }
        return (defaultDesktop = new Desktop(peer));
    }

    private final DesktopPeer peer;

    /**
     * Creates a Desktop instance by specifying the underlying <tt>peer</tt> to
     * use for the implementation.
     * 
     * @param peer the implementation peer
     */
    private Desktop(DesktopPeer peer)
    {
        this.peer = peer;
    }

    /**
     * Returns the currently used peer.
     * 
     * @return the currently used peer
     */
    DesktopPeer getPeer()
    {
        return peer;
    }

    /**
     * The <tt>DesktopPeer</tt> interface provides abstraction for operating
     * system related desktop operations like open(file), print(file), etc.
     */
    static interface DesktopPeer
    {
        public void open(File file) throws  NullPointerException,
                                            IllegalArgumentException,
                                            UnsupportedOperationException,
                                            IOException,
                                            SecurityException;

        public void print(File file) throws NullPointerException,
                                            IllegalArgumentException,
                                            UnsupportedOperationException,
                                            IOException,
                                            SecurityException;

        public void edit(File file) throws  NullPointerException,
                                            IllegalArgumentException,
                                            UnsupportedOperationException,
                                            IOException,
                                            SecurityException;

        public void browse(URI uri) throws  NullPointerException,
                                            IllegalArgumentException,
                                            UnsupportedOperationException,
                                            IOException,
                                            SecurityException;
    }

    /**
     * A <tt>DesktopPeer</tt> implementation based on the java.awt.Desktop class
     * provided in java 1.6+
     */
    private static class AWTDesktopPeer
        implements DesktopPeer
    {
        private final Object impl;

        private final Method open;

        private final Method print;

        private final Method edit;

        private final Method browse;

        public AWTDesktopPeer(Class<?> clazz)
            throws  UnsupportedOperationException,
                    HeadlessException,
                    SecurityException
        {
            Method getDefaultDesktop;
            try
            {
                getDefaultDesktop =
                    clazz.getMethod("getDesktop", (Class<?>[]) null);

                open = clazz.getMethod("open", new Class<?>[]{ File.class });
                print = clazz.getMethod("print", new Class<?>[]{ File.class });
                edit = clazz.getMethod("edit", new Class<?>[]{ File.class });
                browse = clazz.getMethod("browse", new Class<?>[]{ URI.class });
            }
            catch (NoSuchMethodException ex)
            {
                throw new UnsupportedOperationException(ex);
            }

            try
            {
                impl = getDefaultDesktop.invoke(null, (Object[]) null);
            }
            catch (IllegalAccessException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
            catch (InvocationTargetException ex)
            {
                throw new UnsupportedOperationException(ex);
            }
        }

        /**
         * Opens a file.
         */
        public void open(File file) throws IOException
        {
            try
            {
                open.invoke(impl, new Object[]{file});
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Prints a file.
         */
        public void print(File file) throws IOException
        {
            try
            {
                print.invoke(impl, new Object[]{file});
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Edits a file.
         */
        public void edit(File file) throws IOException
        {
            try
            {
                edit.invoke(impl, new Object[]{file});
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Browses a file.
         */
        public void browse(URI uri) throws IOException
        {
            try
            {
                browse.invoke(impl, new Object[]{uri});
            }
            catch (IllegalAccessException ex)
            {
                throw new UndeclaredThrowableException(ex);
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }
    }

    /**
     * An implementation of <tt>DesktopPeer</tt> based on the Jdic library
     * Desktop class.
     */
    private static class JdicDesktopPeer
        implements DesktopPeer
    {
        /**
         * Opens a file.
         */
        public void open(final File file) throws IOException
        {
            try
            {
                // Use browse(URL) instead of open(file) if we're on Mac OS,
                // because of a Java VM crash when open(file) is invoked.
                if (!OSUtils.IS_MAC)
                    org.jdesktop.jdic.desktop.Desktop.open(file);
                else if (!file.isDirectory())
                    org.jdesktop.jdic.desktop.Desktop.browse(
                            file.toURI().toURL());
                else
                    Runtime.getRuntime().exec(
                            "open " + file.getCanonicalPath());
            }
            catch (DesktopException ex)
            {
                ex.printStackTrace();

                Throwable cause = ex.getCause();

                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Prints a file.
         */
        public void print(File file) throws IOException
        {
            try
            {
                org.jdesktop.jdic.desktop.Desktop.print(file);
            }
            catch (org.jdesktop.jdic.desktop.DesktopException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Edits the given file.
         */
        public void edit(File file) throws IOException
        {
            try
            {
                org.jdesktop.jdic.desktop.Desktop.edit(file);
            }
            catch (org.jdesktop.jdic.desktop.DesktopException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
        }

        /**
         * Opens a browser with given uri.
         */
        public void browse(URI uri) throws IOException
        {
            try
            {
                org.jdesktop.jdic.desktop.Desktop.browse(uri.toURL());
            }
            catch (org.jdesktop.jdic.desktop.DesktopException ex)
            {
                Throwable cause = ex.getCause();
                if (cause == null)
                    throw new UndeclaredThrowableException(ex);
                else if (cause instanceof NullPointerException)
                    throw (NullPointerException) cause;
                else if (cause instanceof IllegalArgumentException)
                    throw (IllegalArgumentException) cause;
                else if (cause instanceof UnsupportedOperationException)
                    throw (UnsupportedOperationException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else if (cause instanceof SecurityException)
                    throw (SecurityException) cause;
                else
                    throw new UndeclaredThrowableException(cause);
            }
            catch (MalformedURLException ex)
            {
                throw new IllegalArgumentException(ex);
            }
        }
    }
}
