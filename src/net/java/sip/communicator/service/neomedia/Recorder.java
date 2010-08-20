/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
    public static final String CALL_FORMAT
        = "net.java.sip.communicator.impl.neomedia.CALL_FORMAT";

    /**
     * Gets a list of the formats in which this <tt>Recorder</tt> supports
     * recording media.
     *
     * @return a <tt>List</tt> of the formats in which this <tt>Recorder</tt>
     * supports recording media
     */
    public List<String> getSupportedFormats();

    /**
     * Starts the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) into a file
     * with a specific name.
     *
     * @param filename the name of the file into which the media associated with
     * this <tt>Recorder</tt> is to be recorded
     * @throws IOException if anything goes wrong with the input and/or output
     * performed by this <tt>Recorder</tt>
     * @throws MediaException if anything else goes wrong while starting the
     * recording of media performed by this <tt>Recorder</tt>
     */
    public void start(String filename)
        throws IOException,
               MediaException;

    /**
     * Stops the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) if it has
     * been started and prepares this <tt>Recorder</tt> for garbage collection.
     */
    public void stop();
}
