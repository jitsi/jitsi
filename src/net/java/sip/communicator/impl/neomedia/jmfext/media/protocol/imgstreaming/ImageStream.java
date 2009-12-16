/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * The stream used by JMF for our image streaming.
 * 
 * This class launch a thread to handle <tt>java.awt.Robot</tt>
 * interactions.
 *
 * @author Sebastien Vincent
 */
public class ImageStream implements PushBufferStream, Runnable
{
    /**
     * Content descriptor of this stream which is always RAW.
     */
    private final static ContentDescriptor contentDescriptor =
        new ContentDescriptor(ContentDescriptor.RAW);

    /**
     * Controls associated with the stream.
     */
    private final Control[] controls = new Control[0];

    /**
     * Current format used.
     */
    private Format currentFormat = null;

    /**
     * Callback handler when data from stream is available.
     */
    private BufferTransferHandler transferHandler = null;

    /**
     * Constructor.
     */
    public ImageStream()
    {
    }

    /**
     * Constructor.
     * @param locator <tt>MediaLocator</tt> to use
     */
    public ImageStream(MediaLocator locator)
    {
    }

    /**
     * Set format to use.
     * @param format new format to use
     */
    public void setFormat(Format format)
    {
        currentFormat = format;
    }

    /**
     * Returns the supported format by this stream.
     * @return supported formats
     */
    public Format getFormat()
    {
        return currentFormat;
    }

    /**
     * Block and read a buffer from the stream.
     *
     * @param buffer the <tt>Buffer</tt> to read captured media into
     * @throws IOException if an error occurs while reading.
     */
    public void read(Buffer buffer) throws IOException
    {
        /* TODO get last screen capture of the desktop,
         * convert and put the result in buffer
         */
    }

    /**
     * Query if the next read will block.
     * @return true if a read will block.
     */
    public boolean willReadBlock()
    {
        return false;
    }

    /**
     * Register an object to service data transfers to this stream.
     * @param transferHandler handler to transfer data to
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        synchronized(this)
        {
            this.transferHandler = transferHandler;
            notifyAll();
        }
    }

    /**
     * Indicates whether or not the end of media stream.
     *
     * In case of image streaming it is always false.
     * @return false
     */
    public boolean endOfStream()
    {
        return false;
    }

    /**
     * Get content length of the stream.
     *
     * In case of image streaming it is unknown
     * @return LENGTH_UNKNOWN
     */
    public long getContentLength()
    {
        return LENGTH_UNKNOWN;
    }

    /**
     * We are providing access to raw data
     * @return RAW content descriptor.
     */
    public ContentDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    /**
     * Gives control information to the caller
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        /*
         * The field controls represents is private so we cannot directly return
         * it. Otherwise, the caller will be able to modify it.
         */
        return controls.clone();
    }

    /**
     * Return required control from the Control[] array
     * if exists.
     * @param controlType the control we are interested in.
     * @return the object that implements the control, or null.
     */
    public Object getControl(String controlType)
    {
        try
        {
            Class<?> cls = Class.forName(controlType);
            Object cs[] = getControls();
            for(int i = 0; i < cs.length; i++)
            {
                if(cls.isInstance(cs[i]))
                {
                    return cs[i];
                }
            }

            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Thread entry point.
     */
    public void run()
    {
    }
}

