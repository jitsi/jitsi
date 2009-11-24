/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.alsa;

import javax.media.Time;
import javax.media.protocol.*;
import java.io.IOException;

/**
 * low-latency ALSA access through JNI wrapper
 *
 * @author Jean Lorchat
 */
public class DataSource
    extends PushBufferDataSource
{
    protected Object [] controls = new Object[0];
    protected boolean started = false;
    protected String contentType = "raw";
    protected boolean connected = false;
    protected Time duration = DURATION_UNKNOWN;
    protected AlsaStream [] streams = null;
    protected AlsaStream stream = null;

    public DataSource() {
    }

    /**
     * Tell we are a raw datasource
     *
     * @return "raw"
     */
    public String getContentType() {
    if (!connected){
            System.err.println("Error: DataSource not connected");
            return null;
        }
    return contentType;
    }

    /**
     * Connect the datasource
     */
    public void connect() throws IOException {
     if (connected)
            return;
     connected = true;
    }

    /**
     * Disconnect the datasource
     */
    public void disconnect() {
    try {
            if (started)
                stop();
        } catch (IOException e) {}
    connected = false;
    }

    /**
     * Start the datasource and the underlying stream
     */
    public void start() throws IOException {
        if (!connected)
            throw new java.lang.Error("DataSource must be connected");

        if (started)
            return;

    started = true;
    stream.start(true);
    }

    /**
     * Stop the datasource and it's underlying stream
     */
    public void stop() throws IOException {

    if ((!connected) || (!started))
        return;

    started = false;
    stream.start(false);

    }

    /**
     * Gives control information to the caller
     *
     */
    public Object [] getControls() {
    return controls;
    }

    /**
     * Return required control from the Control[] array
     * if exists, that is
     */
    public Object getControl(String controlType) {
       try {
          Class<?>  cls = Class.forName(controlType);
          Object cs[] = getControls();
          for (int i = 0; i < cs.length; i++) {
             if (cls.isInstance(cs[i]))
                return cs[i];
          }
          return null;

       } catch (Exception e) {
         return null;
       }
    }

    /**
     * Gives to the caller the duration information of our stream
     * Which is, obviously unknown. Better turn on that premonition
     * switch again, Mrs Cake.
     *
     * @return DURATION_UNKNOWN
     */
    public Time getDuration() {
    return duration;
    }

    /**
     * Returns an array of PushBufferStream containing all the streams
     * i.e. only one in our case : only sound
     *
     * If no stream actually exists, instanciate one on the fly
     *
     * @return Array of one stream
     */
    public PushBufferStream [] getStreams() {
    if (streams == null) {
        streams = new AlsaStream[1];
        stream = streams[0] = new AlsaStream();
    }
    return streams;
    }

}
