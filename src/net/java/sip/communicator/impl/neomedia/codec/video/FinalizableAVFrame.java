/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;

/**
 * Represents an <tt>AVFrame</tt> used to provide captured media data in native
 * format without representing the very frame data in the Java heap. Since the
 * user may not know when the <tt>AVFrame</tt> instances are really safe for
 * deallocation, <tt>FinalizableAVFrame</tt> relies on the Java finalization
 * mechanism to reclaim the represented native memory.
 *
 * @author Lubomir Marinov
 */
public class FinalizableAVFrame
    extends AVFrame
{

    /**
     * The indicator which determines whether the native memory represented by
     * this instance has already been freed/deallocated.
     */
    private boolean freed = false;

    /**
     * Initializes a new <tt>FinalizableAVFrame</tt> instance which is to
     * allocate a new native FFmpeg <tt>AVFrame</tt> and represent it.
     */
    public FinalizableAVFrame()
    {
        super(FFmpeg.avcodec_alloc_frame());
    }

    /**
     * Deallocates the native memory represented by this instance.
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        try
        {
            if (!freed)
            {
                long ptr = getPtr();
                long bufferPtr = FFmpeg.avpicture_get_data0(ptr);

                if (bufferPtr != 0)
                    freeData0(bufferPtr);
                FFmpeg.av_free(ptr);
                freed = true;
            }
        }
        finally
        {
            super.finalize();
        }
    }

    /**
     * Frees the memory pointed to by the <tt>data0</tt> member of the native
     * <tt>AVFrame</tt>.
     * @param data0 pointer to free
     */
    protected void freeData0(long data0)
    {
        FFmpeg.av_free(data0);
    }

    public static void read(
            Buffer buffer,
            Format format,
            ByteBuffer data,
            final ByteBufferPool byteBufferPool)
    {
        Object bufferData = buffer.getData();
        AVFrame frame;
        long framePtr;
        long bufferPtrToReturnFree;

        if (bufferData instanceof AVFrame)
        {
            frame = (AVFrame) bufferData;
            framePtr = frame.getPtr();
            bufferPtrToReturnFree = FFmpeg.avpicture_get_data0(framePtr);
        }
        else
        {
            frame
                = new FinalizableAVFrame()
                        {
                            @Override
                            protected void freeData0(long data0)
                            {
                                byteBufferPool.returnFreeBuffer(data0);
                            }
                        };
            buffer.setData(frame);
            framePtr = frame.getPtr();
            bufferPtrToReturnFree = 0;
        }

        AVFrameFormat frameFormat = (AVFrameFormat) format;
        Dimension frameSize = frameFormat.getSize();

        FFmpeg.avpicture_fill(
                framePtr,
                data.ptr,
                frameFormat.getPixFmt(),
                frameSize.width, frameSize.height);

        if (bufferPtrToReturnFree != 0)
            byteBufferPool.returnFreeBuffer(bufferPtrToReturnFree);
    }
}
