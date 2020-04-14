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
package net.java.sip.communicator.plugin.desktoputil;

import java.util.concurrent.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * Utility class based on the javax.swing.SwingWorker. <tt>SwingWorker</tt> is
 * an abstract class that you subclass to perform GUI-related work in a
 * dedicated thread. In addition to the original SwingWorker this class takes
 * care of exceptions occurring during the execution of the separate thread. It
 * will call a catchException() method in the Swing thread if an exception
 * occurs.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public abstract class SwingWorker
{
    /** Logging instance for SwingWorker */
    private final static Logger logger = Logger.getLogger(SwingWorker.class);

    /**
     * The <tt>ExecutorService</tt> which is shared by the <tt>SwingWorker</tt>
     * instances for the purposes of controlling the use of <tt>Thread</tt>s.
     */
    private static ExecutorService executorService;

    /**
     * The <tt>Callable</tt> implementation which is (to be) submitted to
     * {@link #executorService} and invokes {@link #construct()} on behalf of
     * this <tt>SwingWorker</tt>.
     */
    private final Callable<Object> callable;

    /**
     * The <tt>Future</tt> instance which represents the state and the return
     * value of the execution of {@link #callable} i.e. {@link #construct()}.
     */
    private Future<?> future;

    /**
     * Start a thread that will call the <code>construct</code> method
     * and then exit.
     */
    public SwingWorker()
    {
        callable
            = new Callable<Object>()
            {
                public Object call()
                {
                    Object value = null;

                    try
                    {
                        value = construct();
                    }
                    catch (final Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else
                        {
                            // catchException
                            SwingUtilities.invokeLater(
                                    new Runnable()
                                    {
                                        public void run()
                                        {
                                            catchException(t);
                                        }
                                    });
                        }
                    }

                    // We only want to perform the finished if the thread hasn't
                    // been interrupted.
                    if (!Thread.currentThread().isInterrupted())
                        // finished
                        SwingUtilities.invokeLater(
                                new Runnable()
                                {
                                    public void run()
                                    {
                                        finished();
                                    }
                                });

                    return value;
                }
            };
    }

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * if an exception has occurred during the <code>construct</code> method.
     *
     * @param exception the exception that has occurred
     */
    protected void catchException(Throwable exception)
    {
        logger.error("unhandled exception caught", exception);
    }

    /**
     * Computes the value to be returned by {@link #get()}.
     */
    protected abstract Object construct()
        throws Exception;

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    protected void finished()
    {
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
        Future<?> future;

        synchronized (this)
        {
            /*
             * SwingWorker assigns a value to the future field only once and we
             * do not want to invoke Future#cancel(true) while holding a lock.
             */
            future = this.future;
        }

        Object value = null;

        if (future != null)
        {
            boolean interrupted = false;

            do
            {
                try
                {
                    value = future.get();
                    break;
                }
                catch (CancellationException ce)
                {
                    break;
                }
                catch (ExecutionException ee)
                {
                    break;
                }
                catch (InterruptedException ie)
                {
                    interrupted = true;
                }
            }
            while (true);
            if (interrupted) // propagate
                Thread.currentThread().interrupt();
        }

        return value;
    }

    /**
     * A new method that interrupts the worker thread.  Call this method
     * to force the worker to stop what it's doing.
     */
    public void interrupt()
    {
        Future<?> future;

        synchronized (this)
        {
            /*
             * SwingWorker assigns a value to the future field only once and we
             * do not want to invoke Future#cancel(true) while holding a lock.
             */
            future = this.future;
        }

        if (future != null)
            future.cancel(true);
    }

    /**
     * Start the worker thread.
     */
    public void start()
    {
        ExecutorService executorService;

        synchronized (SwingWorker.class)
        {
            if (SwingWorker.executorService == null)
                SwingWorker.executorService = Executors.newCachedThreadPool();
            executorService = SwingWorker.executorService;
        }

        synchronized (this)
        {
            if (future == null || future.isDone())
                future = executorService.submit(callable);
            else
                throw new IllegalStateException("future");
        }
    }
}
