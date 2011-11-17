/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.io.*;
import java.util.*;

/**
 * The call recording interface. Provides the capability to start and stop call
 * recording. 
 *
 * @author Dmitri Melnikov
 * @author Lubomir Marinov
 */
public interface Recorder
{
    /**
     * The name of the configuration property the value of which specifies the
     * full path to the directory with media recorded by <tt>Recorder</tt> (e.g.
     * the media being sent and received in a <tt>Call</tt>).
     */
    public static final String SAVED_CALLS_PATH
        = "net.java.sip.communicator.impl.neomedia.SAVED_CALLS_PATH";

    /**
     * The name of the configuration property the value of which specifies the
     * format in which media is to be recorded by <tt>Recorder</tt> (e.g. the
     * media being sent and received in a <tt>Call</tt>).
     */
    public static final String FORMAT
        = "net.java.sip.communicator.impl.neomedia.Recorder.FORMAT";

    /**
     * Adds a new <tt>Listener</tt> to the list of listeners interested in
     * notifications from this <tt>Recorder</tt>.
     *
     * @param listener the new <tt>Listener</tt> to be added to the list of
     * listeners interested in notifications from this <tt>Recorder</tt>
     */
    public void addListener(Listener listener);

    /**
     * Gets a list of the formats in which this <tt>Recorder</tt> supports
     * recording media.
     *
     * @return a <tt>List</tt> of the formats in which this <tt>Recorder</tt>
     * supports recording media
     */
    public List<String> getSupportedFormats();

    /**
     * Removes an existing <tt>Listener</tt> from the list of listeners
     * interested in notifications from this <tt>Recorder</tt>.
     *
     * @param listener the existing <tt>Listener</tt> to be removed from the
     * list of listeners interested in notifications from this <tt>Recorder</tt>
     */
    public void removeListener(Listener listener);

    /**
     * Starts the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) into a file
     * with a specific name.
     *
     * @param format the format into which the media associated with this
     * <tt>Recorder</tt> is to be recorded into the specified file
     * @param filename the name of the file into which the media associated with
     * this <tt>Recorder</tt> is to be recorded
     * @throws IOException if anything goes wrong with the input and/or output
     * performed by this <tt>Recorder</tt>
     * @throws MediaException if anything else goes wrong while starting the
     * recording of media performed by this <tt>Recorder</tt>
     */
    public void start(String format, String filename)
        throws IOException,
               MediaException;

    /**
     * Stops the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) if it has
     * been started and prepares this <tt>Recorder</tt> for garbage collection.
     */
    public void stop();

    /**
     * Represents a listener interested in notifications from a <tt>Recorder</tt>.
     *
     * @author Lubomir Marinov
     */
    public interface Listener
    {
        /**
         * Notifies this <tt>Listener</tt> that a specific <tt>Recorder</tt> has
         * stopped recording the media associated with it.
         *
         * @param recorder the <tt>Recorder</tt> which has stopped recording its
         * associated media
         */
        public void recorderStopped(Recorder recorder);
    }

    /**
     * Put the recorder in mute state. It won't record the local input.
     * This is used when the local call is muted and we don't won't to record
     * the local input.
     * @param mute the new value of the mute property
     */
    public void setMute(boolean mute);

    /**
     * Returns the filename we are last started or stopped recording to,
     * null if not started.
     * @return the filename we are last started or stopped recording to,
     * null if not started.
     */
    public String getFilename();
}
