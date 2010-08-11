/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;


/**
 * The call recording interface.
 * Provides the capability to start and stop call recording. 
 *  
 * @author Dmitri Melnikov
 */
public interface Recorder
{
    /**
     * Configuration property for the full path to the directory with saved 
     * calls. 
     */
    public static final String SAVED_CALLS_PATH =
        "net.java.sip.communicator.impl.neomedia.SAVED_CALLS_PATH";
    /**
     * Configuration property format of the saved call.
     */
    public static final String CALL_FORMAT =
        "net.java.sip.communicator.impl.neomedia.CALL_FORMAT";

    /**
     * Starts the call recording.
     * 
     * @param callFilename call filename
     */
    public void startRecording(String callFilename);

    /**
     * Stops the call recording.
     */
    public void stopRecording();
}
