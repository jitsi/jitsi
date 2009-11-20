/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * Enables reading from a <tt>PushBufferStream</tt> a certain maximum number
 * of data units (e.g. bytes, shorts, ints) even if the
 * <tt>PushBufferStream</tt> itself pushes a larger number of data units.
 * <p>
 * An example use of this functionality is pacing a
 * <tt>PushBufferStream</tt> which pushes more data units in a single step
 * than a <tt>CaptureDevice</tt>. When these two undergo audio mixing, the
 * different numbers of per-push data units will cause the
 * <tt>PushBufferStream</tt> "play" itself faster than the
 * <tt>CaptureDevice</tt>.
 * </p>
 *
 * @author Lubomir Marinov
 */
public class CachingPushBufferStream
    implements PushBufferStream
{

    /**
     * The <tt>Buffer</tt> in which this instance stores the data it reads
     * from the wrapped <tt>PushBufferStream</tt> and from which it reads in
     * chunks later on when its <tt>#read(Buffer)</tt> method is called.
     */
    private Buffer cache;

    /**
     * The last <tt>IOException</tt> this stream has received from the
     * <tt>#read(Buffer)</tt> method of the wrapped stream and to be thrown
     * by this stream on the earliest call of its <tt>#read(Buffer)</tt>
     * method.
     */
    private IOException readException;

    /**
     * The <tt>PushBufferStream</tt> being paced by this instance with
     * respect to the maximum number of data units it provides in a single push. 
     */
    private final PushBufferStream stream;

    /**
     * Initializes a new <tt>CachingPushBufferStream</tt> instance which is
     * to pace the number of per-push data units a specific
     * <tt>PushBufferStream</tt> provides.
     * 
     * @param stream the <tt>PushBufferStream</tt> to be paced with respect
     *            to the number of per-push data units it provides
     */
    public CachingPushBufferStream(PushBufferStream stream)
    {
        this.stream = stream;
    }

    /**
     * Implements {@link SourceStream#endOfStream()}. Delegates to the wrapped
     * <tt>PushBufferStream</tt> when the cache of this instance is fully read;
     * otherwise, returns <tt>false</tt>.
     *
     * @return <tt> if this <tt>PushBufferStream</tt> has reached the end of the
     * content it makes available; otherwise, <tt>false</tt>
     */
    public boolean endOfStream()
    {
        /*
         * TODO If the cache is still not exhausted, don't report the end of
         * this stream even if the wrapped stream has reached its end.
         */
        return stream.endOfStream();
    }

    /**
     * Implements {@link SourceStream#getContentDescriptor()}. Delegates to the
     * wrapped <tt>PushBufferStream</tt>.
     *
     * @return a <tt>ContentDescriptor</tt> which describes the type of the
     * content made available by the wrapped <tt>PushBufferStream</tt>
     */
    public ContentDescriptor getContentDescriptor()
    {
        return stream.getContentDescriptor();
    }

    /**
     * Implements {@link SourceStream#getContentLength()}. Delegates to the
     * wrapped <tt>PushBufferStream</tt>.
     *
     * @return the length of the content made available by the wrapped
     * <tt>PushBufferStream</tt>
     */
    public long getContentLength()
    {
        return stream.getContentLength();
    }

    /**
     * Implements {@link Controls#getControl(String)}. Delegates to the wrapped
     * <tt>PushBufferStream</tt>.
     *
     * @param controlType a <tt>String</tt> value which names the type of the
     * control of the wrapped <tt>PushBufferStream</tt> to be retrieved
     * @return an <tt>Object</tt> which represents the control of the wrapped
     * <tt>PushBufferStream</tt> with the specified type if such a control is
     * available; otherwise, <tt>null</tt>
     */
    public Object getControl(String controlType)
    {
        return stream.getControl(controlType);
    }

    /**
     * Implements {@link Controls#getControls()}. Delegates to the wrapped
     * <tt>PushBufferStream</tt>.
     *
     * @return an array of <tt>Object</tt>s which represent the control
     * available for the wrapped <tt>PushBufferStream</tt>
     */
    public Object[] getControls()
    {
        return stream.getControls();
    }

    /**
     * Implements {@link PushBufferStream#getFormat()}. Delegates to the wrapped
     * <tt>PushBufferStream</tt>.
     *
     * @return the <tt>Format</tt> of the media data available for reading in
     * this <tt>PushBufferStream</tt>
     */
    public Format getFormat()
    {
        return stream.getFormat();
    }

    /**
     * Gets the <tt>PushBufferStream</tt> wrapped by this instance.
     *
     * @return the <tt>PushBufferStream</tt> wrapped by this instance
     */
    public PushBufferStream getStream()
    {
        return stream;
    }

    /**
     * Gets the object this instance uses for synchronization of the operations
     * (such as reading from the wrapped stream into the cache of this instance
     * and reading out of the cache into the <tt>Buffer</tt> provided to the
     * <tt>#read(Buffer)</tt> method of this instance) it performs in
     * various threads.
     *  
     * @return the object this instance uses for synchronization of the
     *         operations it performs in various threads
     */
    private Object getSyncRoot()
    {
        return this;
    }

    /**
     * Implements {@link PushBufferStream#read(Buffer)}. If an
     * <tt>IOException</tt> has been thrown by the wrapped stream when data was
     * last read from it, re-throws it. If there has been no such exception,
     * reads from the cache of this instance.
     *
     * @param buffer the <tt>Buffer</tt> to receive the read media data
     * @throws IOException if the wrapped stream has thrown such an exception
     * when data was last read from it
     */
    public void read(Buffer buffer)
        throws IOException
    {
        Object syncRoot = getSyncRoot();

        synchronized (syncRoot)
        {
            if (readException != null)
            {
                IOException ex = new IOException();

                ex.initCause(readException);
                readException = null;
                throw ex;
            }

            if (cache != null)
            {
                try
                {
                    read(cache, buffer);
                }
                catch (UnsupportedFormatException ufex)
                {
                    IOException ioex = new IOException();

                    ioex.initCause(ufex);
                    throw ioex;
                }

                int cacheLength = cache.getLength();

                if ((cacheLength <= 0)
                        || (cacheLength <= cache.getOffset())
                        || (cache.getData() == null))
                {
                    cache = null;
                    syncRoot.notifyAll();
                }
            }
        }
    }

    /**
     * Reads data from a specific input <tt>Buffer</tt> (if such data is
     * available) and writes the read data into a specific output
     * <tt>Buffer</tt>. The input <tt>Buffer</tt> will be modified to
     * reflect the number of read data units. If the output <tt>Buffer</tt>
     * has allocated an array for storing the read data and the type of this
     * array matches that of the input <tt>Buffer</tt>, it will be used and
     * thus the output <tt>Buffer</tt> may control the maximum number of
     * data units to be read into it. 
     * 
     * @param input the <tt>Buffer</tt> to read data from
     * @param output the <tt>Buffer</tt> into which to write the data read
     *            from the specified <tt>input</tt>
     * @throws IOException
     * @throws UnsupportedFormatException
     */
    private void read(Buffer input, Buffer output)
        throws IOException,
               UnsupportedFormatException
    {
        Object outputData = output.getData();

        if (outputData != null)
        {
            Object inputData = input.getData();

            if (inputData == null)
            {
                output.setFormat(input.getFormat());
                output.setLength(0);
                return;
            }

            Class<?> dataType = outputData.getClass();

            if (inputData.getClass().equals(dataType)
                    && dataType.equals(byte[].class))
            {
                byte[] outputBytes = (byte[]) outputData;
                int outputLength
                    = Math.min(input.getLength(), outputBytes.length);

                System.arraycopy(
                    (byte[]) inputData,
                    input.getOffset(),
                    outputBytes,
                    output.getOffset(),
                    outputLength);

                output.setData(outputBytes);
                output.setFormat(input.getFormat());
                output.setLength(outputLength);

                input.setLength(input.getLength() - outputLength);
                input.setOffset(input.getOffset() + outputLength);
                return;
            }
        }

        output.copy(input);

        int outputLength = output.getLength();

        input.setLength(input.getLength() - outputLength);
        input.setOffset(input.getOffset() + outputLength);
    }

    /**
     * Implements
     * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}.
     * Delegates to the wrapped <tt>PushBufferStream<tt> but wraps the specified
     * BufferTransferHandler in order to intercept the calls to
     * {@link BufferTransferHandler#transferData(PushBufferStream)} and read
     * data from the wrapped <tt>PushBufferStream</tt> into the cache during the
     * calls in question.
     *
     * @param transferHandler the <tt>BufferTransferHandler</tt> to be notified
     * by this <tt>PushBufferStream</tt> when media data is available for
     * reading
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        stream.setTransferHandler(
            (transferHandler == null)
                ? null
                : new StreamSubstituteBufferTransferHandler(
                            transferHandler,
                            stream,
                            this)
                        {
                            @Override
                            public void transferData(PushBufferStream stream)
                            {
                                if (CachingPushBufferStream.this.stream
                                        == stream)
                                    CachingPushBufferStream.this.transferData();

                                super.transferData(stream);
                            }
                        });
    }

    /**
     * Reads data from the wrapped/input PushBufferStream into the cache of this
     * stream if the cache is empty. If the cache is not empty, blocks the
     * calling thread until the cache is emptied and data is read from the
     * wrapped PushBufferStream into the cache.
     */
    protected void transferData()
    {
        Object syncRoot = getSyncRoot();

        synchronized (syncRoot)
        {
            boolean interrupted = false;

            try
            {
                while (cache != null)
                    try
                    {
                        syncRoot.wait();
                    }
                    catch (InterruptedException ex)
                    {
                        interrupted = true;
                    }
            }
            finally
            {
                if (interrupted)
                    Thread.currentThread().interrupt();
            }

            cache = new Buffer();

            try
            {
                stream.read(cache);
                readException = null;
            }
            catch (IOException ex)
            {
                readException = ex;
            }
        }
    }
}
