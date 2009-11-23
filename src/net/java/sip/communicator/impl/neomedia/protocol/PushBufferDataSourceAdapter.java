/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.util.*;

/**
 * Implements <tt>PushBufferDataSource</tt> for a specific
 * <tt>PullBufferDataSource</tt>.
 *
 * @author Lubomir Marinov
 */
public class PushBufferDataSourceAdapter
    extends PushBufferDataSourceDelegate<PullBufferDataSource>
{

    /**
     * The <tt>Logger</tt> used by the <tt>PushBufferDataSourceAdapter</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PushBufferDataSourceAdapter.class);

    /**
     * The indicator which determines whether {@ link #start()} has been called
     * on this <tt>DataSource</tt> without a subsequent call to {@link #stop()}.
     */
    private boolean started = false;

    /**
     * The <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data.
     */
    private final List<PushBufferStreamAdapter> streams
        = new ArrayList<PushBufferStreamAdapter>();

    /**
     * Initializes a new <tt>PushBufferDataSourceAdapter</tt> which is to
     * implement <tt>PushBufferDataSource</tt> capabilities for a specific
     * <tt>PullBufferDataSource</tt>.
     *
     * @param dataSource the <tt>PullBufferDataSource</tt> the new instance is
     * to implement <tt>PushBufferDataSource</tt> capabilities for
     */
    public PushBufferDataSourceAdapter(PullBufferDataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public void disconnect()
    {
        synchronized (streams)
        {
            Iterator<PushBufferStreamAdapter> streamIter = streams.iterator();

            while (streamIter.hasNext())
            {
                PushBufferStreamAdapter stream = streamIter.next();

                streamIter.remove();
                stream.close();
            }
        }

        super.disconnect();
    }

    @Override
    public void start()
        throws IOException
    {
        super.start();

        synchronized (streams)
        {
            started = true;

            for (PushBufferStreamAdapter stream : streams)
                stream.start();
        }
    }

    @Override
    public void stop()
        throws IOException
    {
        synchronized (streams)
        {
            for (PushBufferStreamAdapter stream : streams)
                stream.stop();

            started = false;
        }

        super.stop();
    }

    /**
     * Implements {@link PushBufferDataSource#getStreams()}. Gets the
     * <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data.
     *
     * @return an array of <tt>PushBufferStream</tt>s through which this
     * <tt>PushBufferDataSource</tt> gives access to its media data
     */
    public PushBufferStream[] getStreams()
    {
        synchronized (streams)
        {
            PullBufferStream[] dataSourceStreams = dataSource.getStreams();
            int dataSourceStreamCount;

            /*
             * I don't know whether dataSource returns a copy of its internal
             * storage so I'm not sure if it's safe to modify dataSourceStreams.
             */
            if (dataSourceStreams != null)
            {
                dataSourceStreams = dataSourceStreams.clone();
                dataSourceStreamCount = dataSourceStreams.length;
            }
            else
                dataSourceStreamCount = 0;

            /*
             * Dispose of the PushBufferStreamAdapters which adapt
             * PullBufferStreams which are no longer returned by dataSource.
             */
            Iterator<PushBufferStreamAdapter> streamIter = streams.iterator();

            while (streamIter.hasNext())
            {
                PushBufferStreamAdapter streamAdapter = streamIter.next();
                PullBufferStream stream = streamAdapter.stream;
                boolean removeStream = true;

                for (int dataSourceStreamIndex = 0;
                        dataSourceStreamIndex < dataSourceStreamCount;
                        dataSourceStreamIndex++)
                    if (stream == dataSourceStreams[dataSourceStreamIndex])
                    {
                        removeStream = false;
                        dataSourceStreams[dataSourceStreamIndex] = null;
                        break;
                    }
                if (removeStream)
                {
                    streamIter.remove();
                    streamAdapter.close();
                }
            }

            /*
             * Create PushBufferStreamAdapters for the PullBufferStreams
             * returned by dataSource which are not adapted yet.
             */
            for (int dataSourceStreamIndex = 0;
                    dataSourceStreamIndex < dataSourceStreamCount;
                    dataSourceStreamIndex++)
            {
                PullBufferStream dataSourceStream
                    = dataSourceStreams[dataSourceStreamIndex];

                if (dataSourceStream != null)
                {
                    PushBufferStreamAdapter stream
                        = new PushBufferStreamAdapter(dataSourceStream);

                    streams.add(stream);
                    if (started)
                        stream.start();
                }
            }

            return streams.toArray(EMPTY_STREAMS);
        }
    }

    /**
     * Implements <tt>PushBufferStream</tt> for a specific
     * <tt>PullBufferStream</tt>.
     */
    private class PushBufferStreamAdapter
        implements PushBufferStream
    {

        /**
         * The <tt>Buffer</tt> which contains the media data read by this
         * instance from {@link #stream} and to be returned by this
         * implementation of {@link PushBufferStream#read(Buffer)} by copying.
         */
        private final Buffer buffer = new Buffer();

        /**
         * The indicator which determines whether {@link #buffer} contains media
         * data read by this instance from {@link #stream} and not returned by
         * this implementation of {@link PushBufferStream#read(Buffer)} yet.
         */
        private boolean bufferIsWritten = false;

        /**
         * The indicator which determined whether {@link  #start()} has been
         * called without a subsequent call to {@link #stop()}.
         */
        private boolean started = false;

        /**
         * The <tt>PullBufferStream</tt> to which this instance provides
         * <tt>PushBufferStream</tt> capabilities.
         */
        public final PullBufferStream stream;

        /**
         * The <tt>IOException</tt>, if any, which has been thrown by the last
         * call to {@link PullBufferStream#read(Buffer)} on {@link #stream} and
         * which still hasn't been rethrown by this implementation of
         * {@link PushBufferStream#read(Buffer)}.
         */
        private IOException streamReadException;

        /**
         * The <tt>Thread</tt> which currently reads media data from
         * {@link #stream} into {@link #buffer}.
         */
        private Thread streamReadThread;

        /**
         * The <tt>Object</tt> which synchronizes the access to
         * {@link #streamReadThread}-related members.
         */
        private final Object streamReadThreadSyncRoot = new Object();

        /**
         * The <tt>BufferTransferHandler</tt> through which this
         * <tt>PushBufferStream</tt> notifies its user that media data is
         * available for reading.
         */
        private BufferTransferHandler transferHandler;

        /**
         * Initializes a new <tt>PushBufferStreamAdapter</tt> instance which is
         * to implement <tt>PushBufferStream</tt> for a specific
         * <tt>PullBufferStream</tt>.
         *
         * @param stream the <tt>PullBufferStream</tt> the new instance is to
         * implement <tt>PushBufferStream</tt> for
         */
        public PushBufferStreamAdapter(PullBufferStream stream)
        {
            if (stream == null)
                throw new NullPointerException("stream");

            this.stream = stream;
        }

        /**
         * Disposes of this <tt>PushBufferStreamAdapter</tt>. Afterwards, this
         * instance is not guaranteed to be operation and considered to be
         * available for garbage collection.
         */
        void close()
        {
            stop();
        }

        /**
         * Implements {@link SourceStream#endOfStream()}. Delegates to the
         * wrapped <tt>PullBufferStream</tt>.
         *
         * @return <tt>true</tt> if the wrapped <tt>PullBufferStream</tt> has
         * reached the end of the media data; otherwise, <tt>false</tt>
         */
        public boolean endOfStream()
        {
            return stream.endOfStream();
        }

        /**
         * Implements {@link SourceStream#getContentDescriptor()}. Delegates to
         * the wrapped <tt>PullBufferStream</tt>.
         *
         * @return the <tt>ContentDescriptor</tt> of the wrapped
         * <tt>PullBufferStream</tt> which describes the type of the media data
         * it gives access to
         */
        public ContentDescriptor getContentDescriptor()
        {
            return stream.getContentDescriptor();
        }

        /**
         * Implements {@link SourceStream#getContentLength()}. Delegates to the
         * wrapped <tt>PullBufferStream</tt>.
         *
         * @return the length of the content the wrapped
         * <tt>PullBufferStream</tt> gives access to
         */
        public long getContentLength()
        {
            return stream.getContentLength();
        }

        /**
         * Implements {@link Controls#getControl(String)}. Delegates to the
         * wrapped <tt>PullBufferStream</tt>.
         *
         * @param controlType a <tt>String</tt> value which specifies the type
         * of the control of the wrapped <tt>PullBufferStream</tt> to be
         * retrieved
         * @return an <tt>Object</tt> which represents the control of the
         * wrapped <tt>PushBufferStream</tt> of the requested type if the
         * wrapped <tt>PushBufferStream</tt> has such a control; <tt>null</tt>
         * if the wrapped <tt>PushBufferStream</tt> does not have a control of
         * the specified type
         */
        public Object getControl(String controlType)
        {
            return stream.getControl(controlType);
        }

        /**
         * Implements {@link Controls#getControls()}. Delegates to the wrapped
         * <tt>PushBufferStream</tt>.
         *
         * @return an array of <tt>Object</tt>s which represent the controls
         * available for the wrapped <tt>PushBufferStream</tt>
         */
        public Object[] getControls()
        {
            return stream.getControls();
        }

        /**
         * Implements {@link PushBufferStream#getFormat()}. Delegates to the
         * wrapped <tt>PullBufferStream</tt>.
         *
         * @return the <tt>Format</tt> of the wrapped <tt>PullBufferStream</tt>
         */
        public Format getFormat()
        {
            return stream.getFormat();
        }

        /**
         * Implements {@link PushBufferStream#read(Buffer)}.
         *
         * @param buffer a <tt>Buffer</tt> in which media data is to be written
         * by this <tt>PushBufferDataSource</tt>
         * @throws IOException if anything wrong happens while reading media
         * data from this <tt>PushBufferDataSource</tt> into the specified
         * <tt>buffer</tt>
         */
        public void read(Buffer buffer)
            throws IOException
        {
            synchronized (this.buffer)
            {
                /*
                 * If stream has throw an exception during its last read,
                 * rethrow it as an exception of this stream.
                 */
                if (streamReadException != null)
                {
                    IOException ie = new IOException();

                    ie.initCause(streamReadException);
                    streamReadException = null;
                    throw ie;
                }
                else if (bufferIsWritten)
                {
                    buffer.copy(this.buffer);
                    bufferIsWritten = false;
                }
            }
        }

        /**
         * Executes an iteration of {@link #streamReadThread} i.e. reads media
         * data from {@link #stream} into {@link #buffer} and invokes
         * {@link BufferTransferHandler#transferData(PushBufferStream)} on
         * {@link #transferHandler} if any.
         */
        private void runInStreamReadThread()
        {
            boolean bufferIsWritten;

            synchronized (buffer)
            {
                try
                {
                    stream.read(buffer);
                    this.bufferIsWritten = !buffer.isDiscard();
                    streamReadException = null;
                }
                catch (IOException ie)
                {
                    this.bufferIsWritten = false;
                    streamReadException = ie;
                }
                bufferIsWritten = this.bufferIsWritten;
            }

            if (bufferIsWritten)
            {
                BufferTransferHandler transferHandler = this.transferHandler;

                if (transferHandler != null)
                    transferHandler.transferData(this);
            }
        }

        /**
         * Implements
         * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}.
         * Sets the means through which this <tt>PushBufferStream</tt> is to
         * notify its user that media data is available for reading.
         *
         * @param transferHandler the <tt>BufferTransferHandler</tt> through
         * which <tt>PushBufferStream</tt> is to notify its user that media data
         * is available for reading
         */
        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            if (this.transferHandler != transferHandler)
                this.transferHandler = transferHandler;
        }

        /**
         * Starts the reading of media data of this
         * <tt>PushBufferStreamAdapter</tt> from the wrapped
         * <tt>PullBufferStream</tt>.
         */
        void start()
        {
            synchronized (streamReadThreadSyncRoot)
            {
                started = true;

                if (streamReadThread == null)
                {
                    streamReadThread = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                while (true)
                                {
                                    synchronized (streamReadThreadSyncRoot)
                                    {
                                        if (!started)
                                            break;
                                        if (streamReadThread
                                                != Thread.currentThread())
                                            break;
                                    }
                                    runInStreamReadThread();
                                }
                            }
                            finally
                            {
                                synchronized (streamReadThreadSyncRoot)
                                {
                                    if (streamReadThread
                                            == Thread.currentThread())
                                    {
                                        streamReadThread = null;
                                        streamReadThreadSyncRoot.notifyAll();
                                    }
                                }
                            }
                        }
                    };
                    streamReadThread.start();
                }
            }
        }

        /**
         * Stops the reading of media data of this
         * <tt>PushBufferStreamAdapter</tt> from the wrapped
         * <tt>PullBufferStream</tt>.
         */
        void stop()
        {
            synchronized (streamReadThreadSyncRoot)
            {
                started = false;

                boolean interrupted = false;

                while (streamReadThread != null)
                    try
                    {
                        streamReadThreadSyncRoot.wait();
                    }
                    catch (InterruptedException ie)
                    {
                        logger
                            .info(
                                getClass().getSimpleName()
                                    + " interrupted while waiting for"
                                    + " PullBufferStream read thread to stop.",
                                ie);
                        interrupted = true;
                    }
                if (interrupted)
                    Thread.currentThread().interrupt();
            }
        }
    }
}
