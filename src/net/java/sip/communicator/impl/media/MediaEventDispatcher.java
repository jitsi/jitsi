/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.util.*;

import net.java.sip.communicator.service.media.event.*;

/**
 * This is a utility class that can be used by objects that support constrained
 * properties. You can use an instance of this class as a member field of your
 * bean and delegate various work to it.
 * 
 * @author Martin Andre
 * @author Emil Ivov
 */
public class MediaEventDispatcher
{

    /**
     * All media listeners registered so far.
     */
    private List<MediaListener> mediaListeners;

    public MediaEventDispatcher()
    {
    }

    /**
     * Add a mediaListener to the listener list.
     * 
     * @param listener The MediaListener to be added
     */
    protected synchronized void addMediaListener(MediaListener listener)
    {
        if (mediaListeners == null)
        {
            mediaListeners = new Vector<MediaListener>();
        }

        mediaListeners.add(listener);
    }

    /**
     * Remove a MediaListener from the listener list.
     * 
     * @param listener The MediaListener to be removed
     */
    protected synchronized void removeMediaListener(MediaListener listener)
    {
        if (mediaListeners != null)
        {
            mediaListeners.remove(listener);
        }
    }

    /**
     * Alert all media listeners that we're receiving a media stream.
     * 
     * @param mediaEvent the source of the event
     */
    protected void fireReceivedMediaStream(MediaEvent mediaEvent)
    {
        MediaListener[] targets = null;

        synchronized (this)
        {
            if (mediaListeners != null)
            {
                targets =
                    mediaListeners.toArray(new MediaListener[mediaListeners
                        .size()]);
            }
        }

        if (targets != null)
        {
            for (int i = 0; i < targets.length; i++)
            {
                targets[i].receivedMediaStream(mediaEvent);
            }
        }
    }

    /**
     * Alert all media listeners that status has changed.
     */
    protected void fireMediaServiceStatusChanged()
    {
        MediaListener[] targets = null;

        synchronized (this)
        {
            if (mediaListeners != null)
            {
                targets =
                    mediaListeners.toArray(new MediaListener[mediaListeners
                        .size()]);
            }
        }

        if (targets != null)
        {
            for (int i = 0; i < targets.length; i++)
            {
                targets[i].mediaServiceStatusChanged();
            }
        }
    }
}
