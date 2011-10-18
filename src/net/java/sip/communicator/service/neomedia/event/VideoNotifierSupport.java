/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.awt.*;
import java.util.*;
import java.util.List; // disambiguation

/**
 * Represents a mechanism to easily add to a specific <tt>Object</tt> by means
 * of composition support for firing <tt>VideoEvent</tt>s to
 * <tt>VideoListener</tt>s.
 *
 * @author Lyubomir Marinov
 */
public class VideoNotifierSupport
{
    private static final long THREAD_TIMEOUT = 5000;

    /**
     * The list of <tt>VideoEvent</tt>s which are to be delivered to the
     * {@link #listeners} registered with this instance when
     * {@link #synchronous} is equal to <tt>false</tt>.
     */
    private final List<VideoEvent> events;

    /**
     * The list of <tt>VideoListener</tt>s interested in changes in the
     * availability of visual <tt>Component</tt>s depicting video.
     */
    private final List<VideoListener> listeners
        = new ArrayList<VideoListener>();

    /**
     * The <tt>Object</tt> which is to be reported as the source of the
     * <tt>VideoEvent</tt>s fired by this instance.
     */
    private final Object source;

    /**
     * The indicator which determines whether this instance delivers the
     * <tt>VideoEvent</tt>s to the {@link #listeners} synchronously.
     */
    private final boolean synchronous;

    /**
     * The <tt>Thread</tt> in which {@link #events} are delivered to the
     * {@link #listeners} when {@link #synchronous} is equal to <tt>false</tt>.
     */
    private Thread thread;

    /**
     * Initializes a new <tt>VideoNotifierSupport</tt> instance which is to
     * facilitate the management of <tt>VideoListener</tt>s and firing
     * <tt>VideoEvent</tt>s to them for a specific <tt>Object</tt>.
     *
     * @param source the <tt>Object</tt> which is to be reported as the source
     * of the <tt>VideoEvent</tt>s fired by the new instance
     */
    public VideoNotifierSupport(Object source)
    {
        this(source, true);
    }

    /**
     * Initializes a new <tt>VideoNotifierSupport</tt> instance which is to
     * facilitate the management of <tt>VideoListener</tt>s and firing
     * <tt>VideoEvent</tt>s to them for a specific <tt>Object</tt>.
     *
     * @param source the <tt>Object</tt> which is to be reported as the source
     * of the <tt>VideoEvent</tt>s fired by the new instance
     * @param synchronous <tt>true</tt> if the new instance is to deliver the
     * <tt>VideoEvent</tt>s synchronously; otherwise, <tt>false</tt>
     */
    public VideoNotifierSupport(Object source, boolean synchronous)
    {
        this.source = source;
        this.synchronous = synchronous;

        events = this.synchronous ? null : new LinkedList<VideoEvent>();
    }

    /**
     * Adds a specific <tt>VideoListener</tt> to this
     * <tt>VideoNotifierSupport</tt> in order to receive notifications when
     * visual/video <tt>Component</tt>s are being added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times.
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoNotifierSupport</tt>
     */
    public void addVideoListener(VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (listeners)
        {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    protected void doFireVideoEvent(VideoEvent event)
    {
        VideoListener[] listeners;

        synchronized (this.listeners)
        {
            listeners
                = this.listeners.toArray(
                        new VideoListener[this.listeners.size()]);
        }

        for (VideoListener listener : listeners)
            switch (event.getType())
            {
                case VideoEvent.VIDEO_ADDED:
                    listener.videoAdded(event);
                    break;
                case VideoEvent.VIDEO_REMOVED:
                    listener.videoRemoved(event);
                    break;
                default:
                    listener.videoUpdate(event);
                    break;
            }
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>VideoMediaStream</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @param wait <tt>true</tt> if the call is to wait till the specified
     * <tt>VideoEvent</tt> has been delivered to the <tt>VideoListener</tt>s;
     * otherwise, <tt>false</tt>
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    public boolean fireVideoEvent(
            int type, Component visualComponent, int origin,
            boolean wait)
    {
        VideoEvent event
            = new VideoEvent(source, type, visualComponent, origin);

        fireVideoEvent(event, wait);
        return event.isConsumed();
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to be fired to the
     * <tt>VideoListener</tt>s registered with this instance
     * @param wait <tt>true</tt> if the call is to wait till the specified
     * <tt>VideoEvent</tt> has been delivered to the <tt>VideoListener</tt>s;
     * otherwise, <tt>false</tt>
     */
    public void fireVideoEvent(VideoEvent event, boolean wait)
    {
        if (synchronous)
            doFireVideoEvent(event);
        else
        {
            synchronized (events)
            {
                events.add(event);

                if (thread == null)
                    startThread();
                else
                    events.notify();

                if (wait)
                {
                    boolean interrupted = false;

                    while (events.contains(event) && (thread != null))
                    {
                        try
                        {
                            events.wait();
                        }
                        catch (InterruptedException ie)
                        {
                            interrupted = true;
                        }
                    }
                    if (interrupted)
                        Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Removes a specific <tt>VideoListener</tt> from this
     * <tt>VideoNotifierSupport</tt> in order to have to no longer receive
     * notifications when visual/video <tt>Component</tt>s are being added and
     * removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed
     */
    public void removeVideoListener(VideoListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    private void runInThread()
    {
        while (true)
        {
            VideoEvent event = null;

            synchronized (events)
            {
                long emptyTime = -1;
                boolean interrupted = false;

                while (events.isEmpty())
                {
                    if (emptyTime == -1)
                        emptyTime = System.currentTimeMillis();
                    else
                    {
                        long newEmptyTime = System.currentTimeMillis();

                        if ((newEmptyTime - emptyTime) >= THREAD_TIMEOUT)
                        {
                            events.notify();
                            return;
                        }
                    }

                    try
                    {
                        events.wait(THREAD_TIMEOUT);
                    }
                    catch (InterruptedException ie)
                    {
                        interrupted = true;
                    }
                }
                if (interrupted)
                    Thread.currentThread().interrupt();

                event = events.remove(0);
            }

            if (event != null)
            {
                try
                {
                    doFireVideoEvent(event);
                }
                catch (Throwable t)
                {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }

                synchronized (events)
                {
                    events.notify();
                }
            }
        }
    }

    private void startThread()
    {
        thread
            = new Thread("VideoNotifierSupportThread")
            {
                @Override
                public void run()
                {
                    try
                    {
                        runInThread();
                    }
                    finally
                    {
                        synchronized (events)
                        {
                            if (Thread.currentThread().equals(thread))
                            {
                                thread = null;
                                if (events.isEmpty())
                                    events.notify();
                                else
                                    startThread();
                            }
                        }
                    }
                }
            };
        thread.setDaemon(true);
        thread.start();
    }
}
