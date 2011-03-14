/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.directshow;

/**
 * DirectShow capture device.
 *
 * @author Sebastien Vincent
 */
public class DSCaptureDevice
{
    /**
     * Empty array with <tt>DSFormat</tt> element type. Explicitly defined
     * in order to avoid unnecessary allocations.
     */
    private static final DSFormat EMPTY_FORMATS[] = new DSFormat[0];

    /**
     * Native pointer of <tt>DSCaptureDevice</tt>.
     *
     * This pointer is hold and will be released by <tt>DSManager</tt>
     * singleton.
     */
    private long ptr = 0;

    /**
     * Constructor.
     *
     * @param ptr native pointer
     */
    public DSCaptureDevice(long ptr)
    {
        /* do not allow 0 pointer value */
        if(ptr == 0)
        {
            throw new IllegalArgumentException("invalid ptr value (0)");
        }

        this.ptr = ptr;
    }

    /**
     * Open and initialize the capture device.
     */
    public void open()
    {
        open(ptr);
    }

    /**
     * Stop and close the capture device.
     */
    public void close()
    {
        close(ptr);
    }

    /**
     * Get name of the capture device.
     *
     * @return name of the capture device
     */
    public String getName()
    {
        return getName(ptr).trim();
    }

    /**
     * Set format to use with this capture device.
     *
     * @param format format to set
     */
    public void setFormat(DSFormat format)
    {
        setFormat(ptr, format);
    }

    /**
     * Get current format.
     *
     * @return current format used
     */
    public DSFormat getFormat()
    {
        return getFormat(ptr);
    }

    /**
     * Get the supported video format this capture device supports.
     *
     * @return array of <tt>DSFormat</tt>
     */
    public DSFormat[] getSupportedFormats()
    {
        DSFormat formats[] = getSupportedFormats(ptr);

        if(formats == null)
        {
            formats = EMPTY_FORMATS;
        }

        return formats;
    }

    /**
     * Set a delegate to use when a frame is received.
     * @param delegate delegate
     */
    public void setDelegate(GrabberDelegate delegate)
    {
        setDelegate(ptr, delegate);
    }

    /**
     * Native method to open capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     */
    private native void open(long ptr);

    /**
     * Native method to close capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     */
    private native void close(long ptr);

    /**
     * Native method to get name of the capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     * @return name of the capture device
     */
    private native String getName(long ptr);

    /**
     * Native method to set format on the capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     * @param format format to set
     */
    private native void setFormat(long ptr, DSFormat format);

    /**
     * Native method to get format on the capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     * @return format current format
     */
    private native DSFormat getFormat(long ptr);

    /**
     * Native method to get supported formats from capture device.
     *
     * @param ptr native pointer of <tt>DSCaptureDevice</tt>
     * @return array of native pointer corresponding to formats
     */
    private native DSFormat[] getSupportedFormats(long ptr);

    /**
     * Native method to set a delegate to use when a frame is received.
     * @param ptr native pointer
     * @param delegate delegate
     */
    public native void setDelegate(long ptr, GrabberDelegate delegate);

    /**
     * Get bytes from <tt>buf</tt> native pointer and copy them
     * to <tt>ptr</tt> byte native pointer.
     *
     * @param ptr pointer to native data
     * @param buf byte native pointer (see ByteBufferPool)
     * @param length length of buf pointed by <tt>ptr</tt>
     * @return length written to <tt>buf</tt>
     */
    public static native int getBytes(long ptr, long buf, int length);

    /**
     * Delegate class to handle grabbing frames.
     *
     * @author Sebastien Vincent
     */
    public static abstract class GrabberDelegate
    {
        /**
         * Callback method when receiving frames.
         *
         * @param ptr native pointer to data
         * @param length length of data
         */
        public abstract void frameReceived(long ptr, int length);
    }
}

