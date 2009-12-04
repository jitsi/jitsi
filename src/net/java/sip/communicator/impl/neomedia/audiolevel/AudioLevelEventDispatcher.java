/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.audiolevel;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * The class implements an audio level measurement thread. The thread will
 * measure new data every time it is added through the <tt>addData</tt> method
 * and would then deliver it to all registered  We use a separate thread so that
 * we could compute and deliver audio levels in a way that won't delay the
 * media processing thread.
 * <p>
 * Note that, for performance reasons this class is not 100% thread safe and you
 * should not modify add or remove audio listeners in this dispatcher in the
 * notification thread (i.e. in the thread where you were notified of an audio
 * level change).
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class AudioLevelEventDispatcher
    implements Runnable
{
    /**
     * The listener that's interested in event changes.
     */
    private SimpleAudioLevelListener levelListener = null;

    /**
     * start/stop indicator.
     */
    private boolean stopped = true;

    /**
     * The data to process.
     */
    private byte[] data = null;

    /**
     * The length of the data last recorded in the <tt>data</tt> array.
     */
    private int dataLength = 0;

    /**
     * This is the last level we fired.
     */
    private int lastLevel = 0;

    /**
     * Runs the actual audio level calculations.
     */
    public void run()
    {
        stopped = false;

        SimpleAudioLevelListener listenerToNotify = null;
        while(!stopped)
        {
            byte[] dataToProcess = null;

            synchronized(this)
            {
                if(data == null)
                {
                    try
                    {
                        wait();
                        //no point in measuring level if there's no one
                        //listening
                        if (levelListener == null)
                            continue;

                        //store the ref of the listener in case someone resets
                        //it before we've had a chance to notify it.
                        listenerToNotify = levelListener;
                    }
                    catch (InterruptedException ie) {}
                }

                dataToProcess = data;
                data = null;
            }

            if(dataToProcess != null)
            {
                int newLevel =
                    AudioLevelEffect.calculateCurrentSignalPower(
                        dataToProcess, 0, dataToProcess.length,
                        SimpleAudioLevelListener.MAX_LEVEL,
                        SimpleAudioLevelListener.MIN_LEVEL,
                        lastLevel);

                listenerToNotify.audioLevelChanged(newLevel);

                lastLevel = newLevel;
            }
        }
    }

    /**
     * Adds data to be processed.
     *
     * @param buffer the data that we'd like to queue for processing.
     */
    public void addData(Buffer buffer)
    {
        synchronized(this)
        {
            dataLength = buffer.getLength();
            if(data == null || data.length < dataLength)
            {
                data = new byte[dataLength];
            }

            System.arraycopy( buffer.getData(), buffer.getOffset(),
                            data, 0, dataLength);

            notifyAll();
        }
    }

    /**
     * Sets the new listener that will be gathering all events from this
     * dispatcher.
     *
     * @param l the listener that we will be notifying or <tt>null</tt> if we
     * are to remove it.
     */
    public void setAudioLevelListener(SimpleAudioLevelListener l)
    {
        synchronized(this)
        {
            this.levelListener = l;
        }
    }

    /**
     * Interrupts this audio level dispatcher so that it would no longer analyze
     */
    public void stop()
    {
        synchronized(this)
        {
            stopped = true;
            notifyAll();
        }
    }
}
