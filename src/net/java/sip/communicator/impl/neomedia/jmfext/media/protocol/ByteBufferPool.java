/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a pool of <tt>ByteBuffer</tt>s which reduces the allocations and
 * deallocations of <tt>ByteBuffer</tt>s in the Java heap and of native memory
 * in the native heap.
 *
 * @author Lubomir Marinov
 */
public class ByteBufferPool
{

    /**
     * The <tt>Logger</tt> used by the <tt>ByteBufferPool</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ByteBufferPool.class);

    /**
     * The <tt>ByteBuffer</tt>s which are managed by this
     * <tt>ByteBufferPool</tt>.
     */
    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();

    /**
     * The indicator which determines whether this <tt>ByteBufferPool</tt> has
     * been closed. Introduced to determine when <tt>ByteBuffer</tt>s are to be
     * disposed of and no longer be pooled.
     */
    private boolean closed = false;

    /**
     * Closes this <tt>ByteBufferPool</tt> i.e. releases the resource allocated
     * by this <tt>ByteBufferPool</tt> during its existence and prepares it to
     * be garbage collected.
     */
    public void close()
    {
        synchronized (buffers)
        {
            closed = true;

            Iterator<ByteBuffer> bufferIter = buffers.iterator();
            boolean loggerIsTraceEnabled = logger.isTraceEnabled();
            int leakedCount = 0;

            while (bufferIter.hasNext())
            {
                ByteBuffer buffer = bufferIter.next();

                if (buffer.isFree())
                {
                    bufferIter.remove();
                    FFmpeg.av_free(buffer.ptr);
                }
                else if (loggerIsTraceEnabled)
                    leakedCount++;
            }
            if (loggerIsTraceEnabled)
            {
                if (logger.isTraceEnabled())
                    logger.trace(
                        "Leaking " + leakedCount + " ByteBuffer instances.");
            }
        }
    }

    /**
     * Gets a <tt>ByteBuffer</tt> out of the pool of free <tt>ByteBuffer</tt>s
     * (i.e. <tt>ByteBuffer</tt>s ready for writing captured media data into
     * them) which is capable to receiving at least <tt>capacity</tt> number of
     * bytes.
     *
     * @param capacity the minimal number of bytes that the returned
     * <tt>ByteBuffer</tt> is to be capable of receiving
     * @return a <tt>ByteBuffer</tt> which is ready for writing captured media
     * data into and which is capable of receiving at least <tt>capacity</tt>
     * number of bytes
     */
    public ByteBuffer getFreeBuffer(int capacity)
    {
        synchronized (buffers)
        {
            if (closed)
                return null;

            int bufferCount = buffers.size();
            ByteBuffer freeBuffer = null;

            /*
             * XXX Pad with FF_INPUT_BUFFER_PADDING_SIZE or hell will break
             * loose.
             */
            capacity += FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

            for (int bufferIndex = 0; bufferIndex < bufferCount; bufferIndex++)
            {
                ByteBuffer buffer = buffers.get(bufferIndex);

                if (buffer.isFree() && (buffer.capacity >= capacity))
                {
                    freeBuffer = buffer;
                    break;
                }
            }
            if (freeBuffer == null)
            {
                freeBuffer = new ByteBuffer(capacity);
                buffers.add(freeBuffer);
            }
            freeBuffer.setFree(false);
            return freeBuffer;
        }
    }

    /**
     * Returns a specific <tt>ByteBuffer</tt> into the pool of free
     * <tt>ByteBuffer</tt>s (i.e. <tt>ByteBuffer</tt>s ready for writing
     * captured media data into them).
     *
     * @param buffer the <tt>ByteBuffer</tt> to be returned into the pool of
     * free <tt>ByteBuffer</tt>s
     */
    public void returnFreeBuffer(ByteBuffer buffer)
    {
        synchronized (buffers)
        {
            buffer.setFree(true);
            if (closed && buffers.remove(buffer))
                FFmpeg.av_free(buffer.ptr);
        }
    }

    /**
     * Returns a specific <tt>ByteBuffer</tt> given by the pointer to the native
     * memory that it represents into the pool of free <tt>ByteBuffer</tt>s
     * (i.e. <tt>ByteBuffer</tt>s ready for writing captured media data into
     * them).
     *
     * @param bufferPtr the pointer to the native memory represented by the
     * <tt>ByteBuffer</tt> to be returned into the pool of free
     * <tt>ByteBuffer</tt>s
     */
    public void returnFreeBuffer(long bufferPtr)
    {
        synchronized (buffers)
        {
            for (ByteBuffer buffer : buffers)
                if (buffer.ptr == bufferPtr)
                {
                    returnFreeBuffer(buffer);
                    break;
                }
        }
    }
}
