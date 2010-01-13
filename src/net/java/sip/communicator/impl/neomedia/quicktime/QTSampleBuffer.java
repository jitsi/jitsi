/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QuickTime/QTKit <tt>QTSampleBuffer</tt> object.
 *
 * @author Lubomir Marinov
 */
public class QTSampleBuffer
    extends NSObject
{

    /**
     * Initializes a new <tt>QTSampleBuffer</tt> which is to represent a
     * specific QuickTime/QTKit <tt>QTSampleBuffer</tt> object.
     *
     * @param ptr the pointer to the QuickTime/QTKit <tt>QTSampleBuffer</tt>
     * object to be represented by the new instance
     */
    public QTSampleBuffer(long ptr)
    {
        super(ptr);
    }

    public byte[] bytesForAllSamples()
    {
        return bytesForAllSamples(getPtr());
    }

    private static native byte[] bytesForAllSamples(long ptr);

    public QTFormatDescription formatDescription()
    {
        long formatDescriptionPtr = formatDescription(getPtr());

        return
            (formatDescriptionPtr == 0)
                ? null
                : new QTFormatDescription(formatDescriptionPtr);
    }

    private static native long formatDescription(long ptr);
}
