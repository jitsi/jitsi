/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 * 
 * Based on the 3rd version of SwingWorker (also known as SwingWorker 3), an
 * abstract class that you subclass to perform GUI-related work in a dedicated
 * thread. For instructions on using this class, see:
 * 
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html
 *
 * Note that the API changed slightly in the 3rd version:
 * You must now invoke start() on the SwingWorker after
 * creating it.
 */
package net.java.sip.communicator.util.swing;

import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.*;

/**
 * Utility class based on the javax.swing.SwingWorker. <tt>SwingWorker</tt> is
 * an abstract class that you subclass to perform GUI-related work in a
 * dedicated thread. In addition to the original SwingWorker this class takes
 * care of exceptions occured during the execution of the separate thread. It
 * would call a catchException() method in the Swing thread if an exception
 * occurs.
 * 
 * @author Yana Stamcheva
 */
public abstract class SwingWorker
{
    private Object value;  // see getValue(), setValue()

    /** 
     * Class to maintain reference to current worker thread
     * under separate synchronization control.
     */
    private static class ThreadVar
    {
        private Thread thread;
        ThreadVar(Thread t)
        {
            thread = t;
        }
        synchronized Thread get()
        {
            return thread;
        }
        synchronized void clear()
        {
            thread = null;
        }
    }

    private ThreadVar threadVar;

    /** 
     * Get the value produced by the worker thread, or null if it 
     * hasn't been constructed yet.
     */
    protected synchronized Object getValue()
    { 
        return value; 
    }

    /** 
     * Set the value produced by worker thread 
     */
    private synchronized void setValue(Object x)
    { 
        value = x; 
    }

    /** 
     * Compute the value to be returned by the <code>get</code> method. 
     */
    public abstract Object construct()
        throws Exception;

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    public void finished()
    {
    }

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * if an exception has occured during the <code>construct</code> method.
     * 
     * @param exception the exception that has occured
     */
    public void catchException(Throwable exception)
    {
    }

    /**
     * A new method that interrupts the worker thread.  Call this method
     * to force the worker to stop what it's doing.
     */
    public void interrupt()
    {
        Thread t = threadVar.get();
        if (t != null)
        {
            t.interrupt();
        }
        threadVar.clear();
    }

    /**
     * Return the value created by the <code>construct</code> method.  
     * Returns null if either the constructing thread or the current
     * thread was interrupted before a value was produced.
     * 
     * @return the value created by the <code>construct</code> method
     */
    public Object get()
    {
        while (true)
        {
            Thread t = threadVar.get();
            if (t == null)
            {
                return getValue();
            }
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // propagate
                return null;
            }
        }
    }


    /**
     * Start a thread that will call the <code>construct</code> method
     * and then exit.
     */
    public SwingWorker()
    {
        final Runnable doFinished = new Runnable()
        {
           public void run() { finished(); }
        };

        Runnable doConstruct = new Runnable()
        {
            public void run()
            {
                try
                {
                    setValue(construct());
                }
                catch (final Exception exception)
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            catchException(exception);
                        }
                    });
                }
                finally
                {
                    threadVar.clear();
                }

                SwingUtilities.invokeLater(doFinished);
            }
        };

        Thread t = new Thread(doConstruct);

        t.setUncaughtExceptionHandler(new SwingUncaughtExceptionHandler());
        threadVar = new ThreadVar(t);
    }

    /**
     * Start the worker thread.
     */
    public void start()
    {
        Thread t = threadVar.get();
        if (t != null)
        {
            t.start();
        }
    }

    /**
     * An exception handler that calls the catchException() in the Swing thread
     * if an exception occurs while processing the construct method in a
     * separate thread.
     */
    private class SwingUncaughtExceptionHandler
        implements UncaughtExceptionHandler
    {
        public void uncaughtException(Thread t, final Throwable e)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    catchException(e);
                }
            });
        }
    }
}
