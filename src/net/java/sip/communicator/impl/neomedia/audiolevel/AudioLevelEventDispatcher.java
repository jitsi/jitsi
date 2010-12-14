/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.audiolevel;

import javax.media.*;

import net.java.sip.communicator.service.neomedia.event.*;

/**
 * The class implements an audio level measurement thread. The thread will
 * measure new data every time it is added through the <tt>addData()</tt> method
 * and would then deliver it to a registered listener if any. (No measurement
 * would be performed until we have a <tt>levelListener</tt>). We use a
 * separate thread so that we could compute and deliver audio levels in a way
 * that won't delay the media processing thread.
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
     * The map where we'd need to register our measurements in addition to
     * notifying listeners about them.
     */
    private AudioLevelMap levelMap = null;

    /**
     * The SSRC of the stream we are measuring that we should use as a key for
     * entries of the levelMap level cache.
     */
    private long ssrc = -1;

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
            byte[] dataToProcess;
            int dataToProcessLength;

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
                    catch (InterruptedException iex)
                    {
                    }
                }

                dataToProcess = data;
                dataToProcessLength = dataLength;
                data = null;
                dataLength = 0;
            }

            if(dataToProcess != null)
            {
                int newLevel
                    = AudioLevelCalculator.calculateSoundPressureLevel(
                            dataToProcess, 0, dataToProcessLength,
                            SimpleAudioLevelListener.MIN_LEVEL,
                            SimpleAudioLevelListener.MAX_LEVEL,
                            lastLevel);

                //cache the result for csrc delivery in case a cache has been
                //set
                if((levelMap != null) && (ssrc != -1))
                    levelMap.putLevel(ssrc, newLevel);

                //now notify our listener
                if (listenerToNotify != null)
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
    public synchronized void addData(Buffer buffer)
    {
        dataLength = buffer.getLength();
        if((data == null) || (data.length < dataLength))
            data = new byte[dataLength];

        Object bufferData = buffer.getData();

        if (bufferData != null)
        {
            System.arraycopy(
                    bufferData, buffer.getOffset(),
                    data, 0,
                    dataLength);
        }

        notifyAll();
    }

    /**
     * Sets the new listener that will be gathering all events from this
     * dispatcher.
     *
     * @param l the listener that we will be notifying or <tt>null</tt> if we
     * are to remove it.
     */
    public synchronized void setAudioLevelListener(SimpleAudioLevelListener l)
    {
        this.levelListener = l;
    }

    /**
     * Interrupts this audio level dispatcher so that it would no longer analyze
     */
    public synchronized void stop()
    {
        stopped = true;
        notifyAll();
    }

    /**
     * Returns <tt>true</tt> if this dispatcher is currently running and
     * delivering audio level events when available and <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if this dispatcher is currently running and
     * delivering audio level events when available and <tt>false</tt>
     * otherwise.
     */
    public boolean isRunning()
    {
        return !stopped;
    }

    /**
     * Sets an <tt>AudioLevelMap</tt> that this dispatcher could use to cache
     * levels it's measuring in addition to simply delivering them to a
     * listener.
     *
     * @param cacheMap the <tt>AudioLevelMap</tt> where this dispatcher should
     * cache measured results.
     * @param ssrc the SSRC key where entries should be logged
     */
    public void setMapCache(AudioLevelMap cacheMap, long ssrc)
    {
        this.levelMap = cacheMap;
        this.ssrc = ssrc;
    }
}
