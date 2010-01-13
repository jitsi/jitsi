/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QTKit <tt>QTCaptureDeviceInput</tt> object.
 *
 * @author Lubomir Marinov
 */
public class QTCaptureDeviceInput
    extends QTCaptureInput
{

    /**
     * Initializes a new <tt>QTCaptureDeviceInput</tt> which is to represent a
     * specific QTKit <tt>QTCaptureDeviceInput</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureDeviceInput</tt> object
     * to be represented by the new instance
     */
    public QTCaptureDeviceInput(long ptr)
    {
        super(ptr);
    }

    public static QTCaptureDeviceInput deviceInputWithDevice(
            QTCaptureDevice device)
        throws IllegalArgumentException
    {
        return new QTCaptureDeviceInput(deviceInputWithDevice(device.getPtr()));
    }

    private static native long deviceInputWithDevice(long devicePtr)
        throws IllegalArgumentException;

    /**
     * Called by the garbage collector to release system resources and perform
     * other cleanup.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
    {
        release();
    }
}
