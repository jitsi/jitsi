/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QTKit <tt>QTCaptureDecompressedVideoOutput</tt> object.
 *
 * @author Lyubomir Marinov
 */
public class QTCaptureDecompressedVideoOutput
    extends QTCaptureOutput
{

    /**
     * Initializes a new <tt>QTCaptureDecompressedVideoOutput</tt> which
     * represents a new QTKit <tt>QTCaptureDecompressedVideoOutput</tt> object.
     */
    public QTCaptureDecompressedVideoOutput()
    {
        this(allocAndInit());
    }

    /**
     * Initializes a new <tt>QTCaptureDecompressedVideoOutput</tt> which is to
     * represent a new QTKit <tt>QTCaptureDecompressedVideoOutput</tt> object.
     *
     * @param ptr the pointer to the QTKit
     * <tt>QTCaptureDecompressedVideoOutput</tt> object to be represented by the
     * new instance
     */
    public QTCaptureDecompressedVideoOutput(long ptr)
    {
        super(ptr);
    }

    private static native long allocAndInit();

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

    /**
     * Gets the minimum time interval between which this
     * <tt>QTCaptureDecompressedVideoOutput</tt> will output consecutive video
     * frames.
     *
     * @return the minimum time interval between which this
     * <tt>QTCaptureDecompressedVideoOutput</tt> will output consecutive video
     * frames. It is equivalent to the inverse of the maximum frame rate. The
     * value of <tt>0</tt> indicates an unlimited maximum frame rate.
     */
    public double minimumVideoFrameInterval()
    {
        return minimumVideoFrameInterval(getPtr());
    }

    /**
     * Gets the minimum time interval between which a specific
     * <tt>QTCaptureDecompressedVideoOutput</tt> instance will output
     * consecutive video frames.
     *
     * @param ptr a pointer to the <tt>QTCaptureDecompressedVideoOutput</tt>
     * instance to get the minimum time interval between consecutive video frame
     * output of
     * @return the minimum time interval between which a specific
     * <tt>QTCaptureDecompressedVideoOutput</tt> instance will output
     * consecutive video frames. It is equivalent to the inverse of the maximum
     * frame rate. The value of <tt>0</tt> indicates an unlimited maximum frame
     * rate.
     */
    private static native double minimumVideoFrameInterval(long ptr);

    public NSDictionary pixelBufferAttributes()
    {
        long pixelBufferAttributesPtr = pixelBufferAttributes(getPtr());

        return
            (pixelBufferAttributesPtr == 0)
                ? null
                : new NSDictionary(pixelBufferAttributesPtr);
    }

    private static native long pixelBufferAttributes(long ptr);

    public boolean setAutomaticallyDropsLateVideoFrames(
            boolean automaticallyDropsLateVideoFrames)
    {
        return
            setAutomaticallyDropsLateVideoFrames(
                    getPtr(),
                    automaticallyDropsLateVideoFrames);
    }

    private static native boolean setAutomaticallyDropsLateVideoFrames(
            long ptr,
            boolean automaticallyDropsLateVideoFrames);

    public void setDelegate(Delegate delegate)
    {
        setDelegate(getPtr(), delegate);
    }

    private static native void setDelegate(long ptr, Delegate delegate);

    /**
     * Sets the minimum time interval between which this
     * <tt>QTCaptureDecompressedVideoOutput</tt> is to output consecutive video
     * frames.
     *
     * @param minimumVideoFrameInterval the minimum time interval between which
     * this <tt>QTCaptureDecompressedVideoOutput</tt> is to output consecutive
     * video frames. It is equivalent to the inverse of the maximum frame rate.
     * The value of <tt>0</tt> indicates an unlimited frame rate.
     */
    public void setMinimumVideoFrameInterval(double minimumVideoFrameInterval)
    {
        setMinimumVideoFrameInterval(getPtr(), minimumVideoFrameInterval);
    }

    /**
     * Sets the minimum time interval between which a specific
     * <tt>QTCaptureDecompressedVideoOutput</tt> instance is to output
     * consecutive video frames.
     *
     * @param ptr a pointer to the <tt>QTCaptureDecompressedVideoOutput</tt>
     * instance to set the minimum time interval between consecutive video frame
     * output on
     * @param minimumVideoFrameInterval the minimum time interval between which
     * a specific <tt>QTCaptureDecompressedVideoOutput</tt> instance is to
     * output consecutive video frames. It is equivalent to the inverse of the
     * maximum frame rate. The value of <tt>0</tt> indicates an unlimited frame
     * rate.
     */
    private static native void setMinimumVideoFrameInterval(
            long ptr,
            double minimumVideoFrameInterval);

    public void setPixelBufferAttributes(NSDictionary pixelBufferAttributes)
    {
        setPixelBufferAttributes(getPtr(), pixelBufferAttributes.getPtr());
    }

    private static native void setPixelBufferAttributes(
            long ptr,
            long pixelBufferAttributesPtr);

    /**
     * Represents the receiver of <tt>CVImageBuffer</tt> video frames and their
     * associated <tt>QTSampleBuffer</tt>s captured by a
     * <tt>QTCaptureDecompressedVideoOutput</tt>.
     */
    public static abstract class Delegate
    {
        private MutableQTSampleBuffer sampleBuffer;

        private MutableCVPixelBuffer videoFrame;

        /**
         * Notifies this <tt>Delegate</tt> that the <tt>QTCaptureOutput</tt> to
         * which it is set has output a specific <tt>CVImageBuffer</tt>
         * representing a video frame with a specific <tt>QTSampleBuffer</tt>.
         *
         * @param videoFrame the <tt>CVImageBuffer</tt> which represents the
         * output video frame
         * @param sampleBuffer the <tt>QTSampleBuffer</tt> which represents
         * additional details about the output video samples
         */
        public abstract void outputVideoFrameWithSampleBuffer(
                CVImageBuffer videoFrame,
                QTSampleBuffer sampleBuffer);

        void outputVideoFrameWithSampleBuffer(
                long videoFramePtr,
                long sampleBufferPtr)
        {
            if (videoFrame == null)
                videoFrame = new MutableCVPixelBuffer(videoFramePtr);
            else
                videoFrame.setPtr(videoFramePtr);

            if (sampleBuffer == null)
                sampleBuffer = new MutableQTSampleBuffer(sampleBufferPtr);
            else
                sampleBuffer.setPtr(sampleBufferPtr);

            outputVideoFrameWithSampleBuffer(videoFrame, sampleBuffer);
        }
    }

    /**
     * Represents a <tt>CVPixelBuffer</tt> which allows public changing of the
     * CoreVideo <tt>CVPixelBufferRef</tt> it represents.
     */
    private static class MutableCVPixelBuffer
        extends CVPixelBuffer
    {
        /**
         * Initializes a new <tt>MutableCVPixelBuffer</tt> which is to represent
         * a specific CoreVideo <tt>CVPixelBufferRef</tt>.
         *
         * @param ptr the CoreVideo <tt>CVPixelBufferRef</tt> to be represented
         * by the new instance
         */
        private MutableCVPixelBuffer(long ptr)
        {
            super(ptr);
        }

        /**
         * Sets the CoreVideo <tt>CVImageBufferRef</tt> represented by this
         * instance.
         *
         * @param ptr the CoreVideo <tt>CVImageBufferRef</tt> to be represented
         * by this instance
         * @see CVPixelBuffer#setPtr(long)
         */
        @Override
        public void setPtr(long ptr)
        {
            super.setPtr(ptr);
        }
    }

    /**
     * Represents a <tt>QTSampleBuffer</tt> which allows public changing of the
     * QTKit <tt>QTSampleBuffer</tt> object it represents.
     */
    private static class MutableQTSampleBuffer
        extends QTSampleBuffer
    {
        /**
         * Initializes a new <tt>MutableQTSampleBuffer</tt> instance which is to
         * represent a specific QTKit <tt>QTSampleBuffer</tt> object.
         *
         * @param ptr the pointer to the QTKit <tt>QTSampleBuffer</tt> object to
         * be represented by the new instance
         */
        private MutableQTSampleBuffer(long ptr)
        {
            super(ptr);
        }

        /**
         * Sets the pointer to the Objective-C object represented by this
         * instance.
         *
         * @param ptr the pointer to the Objective-C object to be represented by
         * this instance
         * @see QTSampleBuffer#setPtr(long)
         */
        @Override
        public void setPtr(long ptr)
        {
            super.setPtr(ptr);
        }
    }
}
