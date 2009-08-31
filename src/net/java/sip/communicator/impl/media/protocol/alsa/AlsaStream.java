/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.protocol.alsa;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import java.io.IOException;

/**
 * low-latency ALSA access through JNI wrapper
 *
 * @author Jean Lorchat
 */
public class AlsaStream
    implements PushBufferStream, Runnable
{
    protected ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);
    protected int maxDataLength;
    protected AudioFormat audioFormat;
    protected boolean started;
    protected Thread thread;
    protected BufferTransferHandler transferHandler;
    protected Control [] controls = new Control[0];

    private native void jni_alsa_init();
    private native void jni_alsa_read(byte [] arr);
    private native void jni_alsa_delete();

    int seqNo = 0;

    /**
     * Dynamically loads JNI object. Will fail if non-Linux
     * or when libjmf_alsa.so is outside of the LD_LIBRARY_PATH
     */
    static {
    System.loadLibrary("jmf_alsa");
    }

    /**
     * We only provide 8 kbps access to the device for now
     * In addition, datablock size is fixed to 160 samples (320 bytes)
     * This turns out to be 20ms worth of samples at 8kbps
     * Very convenient for ilbc 20ms mode...
     */
    public AlsaStream() {

    audioFormat = new AudioFormat(AudioFormat.LINEAR,
                      8000.0,
                      16,
                      1,
                      AudioFormat.LITTLE_ENDIAN,
                      AudioFormat.SIGNED,
                      16,
                      Format.NOT_SPECIFIED,
                      Format.byteArray);

    maxDataLength = 160;

    jni_alsa_init();
    thread = new Thread(this);
    }

    /**
     * Methods required for SourceStream interface
     *
     */

    /**
     * We are providing access to raw data
     *
     */
    public ContentDescriptor getContentDescriptor() {
    return cd;
    }

    /**
     * Come on, we are streaming. Can't guess when the user wants to stop...
     *
     */
    public long getContentLength() {
    return LENGTH_UNKNOWN;
    }

    /**
     * The stream of happiness never ends, somehow.
     *
     */
    public boolean endOfStream() {
    return false;
    }

    /**
     * Tell whoever has interest how we are going to send the data
     *
     */
    public Format getFormat() {
        return audioFormat;
    }

    /**
     * Reads sample data using native call
     *
     * @param buffer A Buffer with byte array as data, large enough to be
     *   filled with sample data. If this is not the case, we are going
     *   to allocate it ourselves anyway and replace the current data.
     *
     * @throws IOException Native method might throw an exception.
     *   Well actually that would surprise me because it is not
     *   implemented, but we love to be future-proof, isn't it ?
     */
    public void read(Buffer buffer) throws IOException {
    synchronized (this) {
        Object outdata = buffer.getData();

        if (outdata == null ||
        !(outdata.getClass() == Format.byteArray) ||
        ((byte[])outdata).length < maxDataLength) {
          outdata = new byte[maxDataLength];
          buffer.setData(outdata);
        }

        buffer.setFormat(audioFormat);
        buffer.setTimeStamp(1000000000 / 8);
        jni_alsa_read((byte[])outdata);
        buffer.setSequenceNumber(seqNo);
        buffer.setLength(maxDataLength);
        buffer.setFlags(0);
        buffer.setHeader(null);
        seqNo++;
    }
    }

    /**
     * Dunno about that piece yet...
     *
     */
    public void setTransferHandler(BufferTransferHandler transferHandler) {
    synchronized (this) {
        this.transferHandler = transferHandler;
        notifyAll();
    }
    }

    /**
     * Starts a new thread for the stream operation
     *
     */
    void start(boolean started) {
    synchronized (this) {
        this.started = started;
        if (started && !thread.isAlive()) {
        thread = new Thread(this);
        thread.start();
        }
        notifyAll();
    }
    }

    /**
     * Transfer Buffer information through current thread then sleep
     * until next data slice is ready.
     *
     */
    public void run() {
    while (started) {

        synchronized (this) {
        while (transferHandler == null && started) {
            try {
            wait(1000);
            } catch (InterruptedException ie) {
            }
        }
        }

        if (started && transferHandler != null) {
        transferHandler.transferData(this);
        try {
            Thread.sleep(1);
        } catch (InterruptedException ise) {
        }
        }

    }
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
}
