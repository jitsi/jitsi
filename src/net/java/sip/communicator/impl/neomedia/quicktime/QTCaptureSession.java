/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QTKit <tt>QTCaptureSession</tt> object.
 *
 * @author Lubomir Marinov
 */
public class QTCaptureSession
    extends NSObject
{
    private boolean closed = false;

    /**
     * Initializes a new <tt>QTCaptureSession</tt> instance which represents a
     * new QTKit <tt>QTCaptureSession</tt> object.
     */
    public QTCaptureSession()
    {
        this(allocAndInit());
    }

    /**
     * Initializes a new <tt>QTCaptureSession</tt> instance which is to
     * represent a specific QTKit <tt>QTCaptureSession</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureSession</tt> object to
     * be represented by the new instance
     */
    public QTCaptureSession(long ptr)
    {
        super(ptr);
    }

    public boolean addInput(QTCaptureInput input)
        throws NSErrorException
    {
        return addInput(getPtr(), input.getPtr());
    }

    private static native boolean addInput(long ptr, long inputPtr)
        throws NSErrorException;

    public boolean addOutput(QTCaptureOutput output)
        throws NSErrorException
    {
        return addOutput(getPtr(), output.getPtr());
    }

    private static native boolean addOutput(long ptr, long outputPtr)
            throws NSErrorException;

    private static native long allocAndInit();

    /**
     * Releases the resources used by this instance throughout its existence and
     * makes it available for garbage collection. This instance is considered
     * unusable after closing.
     */
    public synchronized void close()
    {
        if (!closed)
        {
            stopRunning();
            release();
            closed = true;
        }
    }

    /**
     * Called by the garbage collector to release system resources and perform
     * other cleanup.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
    {
        close();
    }

    public void startRunning()
    {
        startRunning(getPtr());
    }

    private static native void startRunning(long ptr);

    public void stopRunning()
    {
        stopRunning(getPtr());
    }

    private static native void stopRunning(long ptr);
}
