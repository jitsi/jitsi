/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.awt.Component;
import java.util.*;

/**
 * Represents a mechanism to easily add to a specific <tt>Object</tt> by means
 * of composition support for firing <tt>VideoEvent</tt>s to
 * <tt>VideoListener</tt>s.
 *
 * @author Lubomir Marinov
 */
public class VideoNotifierSupport
{

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
     * Initializes a new <tt>VideoNotifierSupport</tt> instance which is to
     * facilitate the management of <tt>VideoListener</tt>s and firing
     * <tt>VideoEvent</tt>s to them for a specific <tt>Object</tt>.
     *
     * @param source the <tt>Object</tt> which is to be reported as the source
     * of the <tt>VideoEvent</tt>s fired by the new instance
     */
    public VideoNotifierSupport(Object source)
    {
        this.source = source;
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
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    public boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        VideoListener[] listeners;

        synchronized (this.listeners)
        {
            listeners
                = this.listeners
                    .toArray(new VideoListener[this.listeners.size()]);
        }

        boolean consumed;

        if (listeners.length > 0)
        {
            VideoEvent event
                = new VideoEvent(source, type, visualComponent, origin);

            for (VideoListener listener : listeners)
                switch (type)
                {
                    case VideoEvent.VIDEO_ADDED:
                        listener.videoAdded(event);
                        break;
                    case VideoEvent.VIDEO_REMOVED:
                        listener.videoRemoved(event);
                        break;
                    default:
                        throw new IllegalArgumentException("type");
                }

            consumed = event.isConsumed();
        }
        else
            consumed = false;
        return consumed;
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to be fired to the
     * <tt>VideoListener</tt>s registered with this instance
     */
    public void fireVideoEvent(VideoEvent event)
    {
        VideoListener[] listeners;

        synchronized (this.listeners)
        {
            listeners
                = this.listeners
                    .toArray(new VideoListener[this.listeners.size()]);
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
}
