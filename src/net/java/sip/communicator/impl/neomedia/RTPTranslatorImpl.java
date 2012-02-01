/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

import com.sun.media.rtp.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>RTPTranslator</tt> which represents an RTP translator which
 * forwards RTP and RTCP traffic between multiple <tt>MediaStream</tt>s.
 *
 * @author Lyubomir Marinov
 */
public class RTPTranslatorImpl
    implements ReceiveStreamListener,
               RTPTranslator
{
    /**
     * The <tt>Logger</tt> used by the <tt>RTPTranslatorImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(RTPTranslatorImpl.class);

    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    /**
     * The <tt>RTPConnector</tt> which is used by {@link #manager} and which
     * delegates to the <tt>RTPConnector</tt>s of the <tt>StreamRTPManager</tt>s
     * attached to this instance.
     */
    private RTPConnectorImpl connector;

    /**
     * The <tt>RTPManager</tt> which implements the actual RTP management of
     * this instance.
     */
    private final RTPManager manager = RTPManager.newInstance();

    private final List<SendStreamDesc> sendStreams
        = new LinkedList<SendStreamDesc>();

    /**
     * The list of <tt>StreamRTPManager</tt>s i.e. <tt>MediaStream</tt>s which
     * this instance forwards RTP and RTCP traffic between.
     */
    private final List<StreamRTPManagerDesc> streamRTPManagers
        = new ArrayList<StreamRTPManagerDesc>();

    public RTPTranslatorImpl()
    {
        manager.addReceiveStreamListener(this);
    }

    public synchronized void addFormat(
            StreamRTPManager streamRTPManager,
            Format format, int payloadType)
    {
        manager.addFormat(format, payloadType);

        StreamRTPManagerDesc streamRTPManagerDesc
            = getStreamRTPManagerDesc(streamRTPManager, true);

        streamRTPManagerDesc.addFormat(format, payloadType);
    }

    public synchronized void addReceiveStreamListener(
            StreamRTPManager streamRTPManager,
            ReceiveStreamListener listener)
    {
        StreamRTPManagerDesc streamRTPManagerDesc
            = getStreamRTPManagerDesc(streamRTPManager, true);

        streamRTPManagerDesc.addReceiveStreamListener(listener);
    }

    public void addRemoteListener(
            StreamRTPManager streamRTPManager,
            RemoteListener listener)
    {
        manager.addRemoteListener(listener);
    }

    public void addSendStreamListener(
            StreamRTPManager streamRTPManager,
            SendStreamListener listener)
    {
        // TODO Auto-generated method stub
    }

    public void addSessionListener(
            StreamRTPManager streamRTPManager,
            SessionListener listener)
    {
        // TODO Auto-generated method stub
    }

    private synchronized void closeSendStream(SendStreamDesc sendStreamDesc)
    {
        if (sendStreams.contains(sendStreamDesc)
                && (sendStreamDesc.getSendStreamCount() < 1))
        {
            sendStreamDesc.sendStream.close();
            sendStreams.remove(sendStreamDesc);
        }
    }

    public synchronized SendStream createSendStream(
            StreamRTPManager streamRTPManager,
            DataSource dataSource, int streamIndex)
        throws IOException,
               UnsupportedFormatException
    {
        SendStreamDesc sendStreamDesc = null;

        for (SendStreamDesc s : sendStreams)
            if ((s.dataSource == dataSource) && (s.streamIndex == streamIndex))
            {
                sendStreamDesc = s;
                break;
            }
        if (sendStreamDesc == null)
        {
            SendStream sendStream
                = manager.createSendStream(dataSource, streamIndex);

            if (sendStream != null)
            {
                sendStreamDesc
                    = new SendStreamDesc(dataSource, streamIndex, sendStream);
                sendStreams.add(sendStreamDesc);
            }
        }
        return
            (sendStreamDesc == null)
                ? null
                : sendStreamDesc.getSendStream(streamRTPManager, true);
    }

    /**
     * Releases the resources allocated by this instance in the course of its
     * execution and prepares it to be garbage collected.
     */
    public synchronized void dispose()
    {
        manager.removeReceiveStreamListener(this);
        manager.dispose();
    }

    public synchronized void dispose(StreamRTPManager streamRTPManager)
    {
        Iterator<StreamRTPManagerDesc> streamRTPManagerIter
            = streamRTPManagers.iterator();

        while (streamRTPManagerIter.hasNext())
        {
            StreamRTPManagerDesc streamRTPManagerDesc
                = streamRTPManagerIter.next();

            if (streamRTPManagerDesc.streamRTPManager == streamRTPManager)
            {
                RTPConnectorDesc connectorDesc
                    = streamRTPManagerDesc.connectorDesc;

                if (connectorDesc != null)
                {
                    if (this.connector != null)
                    {
                        this.connector.removeConnector(connectorDesc);
                        connectorDesc.connector.close();
                    }
                    streamRTPManagerDesc.connectorDesc = null;
                }

                streamRTPManagerIter.remove();
                break;
            }
        }
    }

    private synchronized StreamRTPManagerDesc
        findStreamRTPManagerDescByReceiveSSRC(
            long receiveSSRC,
            StreamRTPManagerDesc exclusion)
    {
        for (int i = 0, count = streamRTPManagers.size(); i < count; i++)
        {
            StreamRTPManagerDesc s = streamRTPManagers.get(i);

            if ((s != exclusion) && s.containsReceiveSSRC(receiveSSRC))
                return s;
        }
        return null;
    }

    public Object getControl(
            StreamRTPManager streamRTPManager,
            String controlType)
    {
        return manager.getControl(controlType);
    }

    public GlobalReceptionStats getGlobalReceptionStats(
            StreamRTPManager streamRTPManager)
    {
        return manager.getGlobalReceptionStats();
    }

    public GlobalTransmissionStats getGlobalTransmissionStats(
            StreamRTPManager streamRTPManager)
    {
        return manager.getGlobalTransmissionStats();
    }

    public long getLocalSSRC(StreamRTPManager streamRTPManager)
    {
        return ((RTPSessionMgr) manager).getLocalSSRC();
    }

    public synchronized Vector<ReceiveStream> getReceiveStreams(
            StreamRTPManager streamRTPManager)
    {
        StreamRTPManagerDesc streamRTPManagerDesc
            = getStreamRTPManagerDesc(streamRTPManager, false);
        Vector<ReceiveStream> receiveStreams = null;

        if (streamRTPManagerDesc != null)
        {
            Vector<?> managerReceiveStreams = manager.getReceiveStreams();

            if (managerReceiveStreams != null)
            {
                receiveStreams
                    = new Vector<ReceiveStream>(managerReceiveStreams.size());
                for (Object s : managerReceiveStreams)
                {
                    ReceiveStream receiveStream = (ReceiveStream) s;

                    if (streamRTPManagerDesc.containsReceiveSSRC(
                            receiveStream.getSSRC()))
                        receiveStreams.add(receiveStream);
                }
            }
        }
        return receiveStreams;
    }

    public synchronized Vector<SendStream> getSendStreams(
            StreamRTPManager streamRTPManager)
    {
        Vector<?> managerSendStreams = manager.getSendStreams();
        Vector<SendStream> sendStreams = null;

        if (managerSendStreams != null)
        {
            sendStreams = new Vector<SendStream>(managerSendStreams.size());
            for (SendStreamDesc sendStreamDesc : this.sendStreams)
                if (managerSendStreams.contains(sendStreamDesc.sendStream))
                {
                    SendStream sendStream
                        = sendStreamDesc.getSendStream(streamRTPManager, false);

                    if (sendStream != null)
                        sendStreams.add(sendStream);
                }
        }
        return sendStreams;
    }

    private synchronized StreamRTPManagerDesc getStreamRTPManagerDesc(
            StreamRTPManager streamRTPManager,
            boolean create)
    {
        for (StreamRTPManagerDesc s : streamRTPManagers)
            if (s.streamRTPManager == streamRTPManager)
                return s;

        StreamRTPManagerDesc s;

        if (create)
        {
            s = new StreamRTPManagerDesc(streamRTPManager);
            streamRTPManagers.add(s);
        }
        else
            s = null;
        return s;
    }

    public synchronized void initialize(
            StreamRTPManager streamRTPManager,
            RTPConnector connector)
    {
        if (this.connector == null)
        {
            this.connector = new RTPConnectorImpl();
            manager.initialize(this.connector);
        }

        StreamRTPManagerDesc streamRTPManagerDesc
            = getStreamRTPManagerDesc(streamRTPManager, true);
        RTPConnectorDesc connectorDesc = streamRTPManagerDesc.connectorDesc;

        if ((connectorDesc == null) || (connectorDesc.connector != connector))
        {
            if (connectorDesc != null)
                this.connector.removeConnector(connectorDesc);
            streamRTPManagerDesc.connectorDesc
                = connectorDesc
                    = (connector == null)
                        ? null
                        : new RTPConnectorDesc(streamRTPManagerDesc, connector);
            if (connectorDesc != null)
                this.connector.addConnector(connectorDesc);
        }
    }

    private int read(
            PushSourceStreamDesc streamDesc,
            byte[] buffer, int offset, int length,
            int read)
        throws IOException
    {
        boolean data = streamDesc.data;
        StreamRTPManagerDesc streamRTPManagerDesc
            = streamDesc.connectorDesc.streamRTPManagerDesc;
        Format format = null;

        if (data)
        {
            if (length >= 12)
            {
                long ssrc = readInt(buffer, offset + 8);

                if (!streamRTPManagerDesc.containsReceiveSSRC(ssrc))
                {
                    if (findStreamRTPManagerDescByReceiveSSRC(
                                ssrc,
                                streamRTPManagerDesc)
                            == null)
                        streamRTPManagerDesc.addReceiveSSRC(ssrc);
                    else
                        return 0;
                }

                int payloadType = buffer[offset + 1] & 0x7f;

                format = streamRTPManagerDesc.getFormat(payloadType);
            }
        }

        OutputDataStreamImpl outputStream
            = data
                ? connector.getDataOutputStream()
                : connector.getControlOutputStream();

        if (outputStream != null)
        {
            outputStream.write(
                    buffer, offset, read,
                    format,
                    streamRTPManagerDesc);
        }

        return read;
    }

    private static int readInt(byte[] buffer, int offset)
    {
        return
            ((buffer[offset++] & 0xff) << 24)
                | ((buffer[offset++] & 0xff) << 16)
                | ((buffer[offset++] & 0xff) << 8)
                | (buffer[offset] & 0xff);
    }

    public synchronized void removeReceiveStreamListener(
            StreamRTPManager streamRTPManager,
            ReceiveStreamListener listener)
    {
        StreamRTPManagerDesc streamRTPManagerDesc
            = getStreamRTPManagerDesc(streamRTPManager, false);

        if (streamRTPManagerDesc != null)
            streamRTPManagerDesc.removeReceiveStreamListener(listener);
    }

    public void removeRemoteListener(
            StreamRTPManager streamRTPManager,
            RemoteListener listener)
    {
        manager.removeRemoteListener(listener);
    }

    public void removeSendStreamListener(
            StreamRTPManager streamRTPManager,
            SendStreamListener listener)
    {
        // TODO Auto-generated method stub
    }

    public void removeSessionListener(
            StreamRTPManager streamRTPManager,
            SessionListener listener)
    {
        // TODO Auto-generated method stub
    }

    public void update(ReceiveStreamEvent event)
    {
        StreamRTPManagerDesc streamRTPManagerDesc
            = findStreamRTPManagerDescByReceiveSSRC(
                    event.getReceiveStream().getSSRC(),
                    null);

        if (streamRTPManagerDesc != null)
            for (ReceiveStreamListener listener
                    : streamRTPManagerDesc.getReceiveStreamListeners())
                listener.update(event);
    }

    private static class OutputDataStreamDesc
    {
        public RTPConnectorDesc connectorDesc;

        public OutputDataStream stream;

        public OutputDataStreamDesc(
                RTPConnectorDesc connectorDesc,
                OutputDataStream stream)
        {
            this.connectorDesc = connectorDesc;
            this.stream = stream;
        }
    }

    private static class OutputDataStreamImpl
        implements OutputDataStream,
                   Runnable
    {
        private static final int WRITE_QUEUE_CAPACITY
            = RTPConnectorOutputStream
                .MAX_PACKETS_PER_MILLIS_POLICY_PACKET_QUEUE_CAPACITY;

        private boolean closed;

        private final boolean data;

        private final List<OutputDataStreamDesc> streams
            = new ArrayList<OutputDataStreamDesc>();

        private final RTPTranslatorBuffer[] writeQueue
            = new RTPTranslatorBuffer[WRITE_QUEUE_CAPACITY];

        private int writeQueueHead;

        private int writeQueueLength;

        private Thread writeThread;

        public OutputDataStreamImpl(boolean data)
        {
            this.data = data;
        }

        public synchronized void addStream(
                RTPConnectorDesc connectorDesc,
                OutputDataStream stream)
        {
            for (OutputDataStreamDesc streamDesc : streams)
                if ((streamDesc.connectorDesc == connectorDesc)
                        && (streamDesc.stream == stream))
                    return;
            streams.add(new OutputDataStreamDesc(connectorDesc, stream));
        }

        public synchronized void close()
        {
            closed = true;
            writeThread = null;
            notify();
        }

        private synchronized void createWriteThread()
        {
            writeThread = new Thread(this, getClass().getName());
            writeThread.setDaemon(true);
            writeThread.start();
        }

        private synchronized int doWrite(
                byte[] buffer, int offset, int length,
                Format format,
                StreamRTPManagerDesc exclusion)
        {
            int write = 0;

            for (int streamIndex = 0, streamCount = streams.size();
                    streamIndex < streamCount;
                    streamIndex++)
            {
                OutputDataStreamDesc streamDesc = streams.get(streamIndex);
                StreamRTPManagerDesc streamRTPManagerDesc
                    = streamDesc.connectorDesc.streamRTPManagerDesc;

                if (streamRTPManagerDesc != exclusion)
                {
                    if (data && (format != null) && (length > 0))
                    {
                        Integer payloadType
                            = streamRTPManagerDesc.getPayloadType(format);

                        if ((payloadType == null) && (exclusion != null))
                            payloadType = exclusion.getPayloadType(format);
                        if (payloadType != null)
                        {
                            int payloadTypeByteIndex = offset + 1;

                            buffer[payloadTypeByteIndex]
                                = (byte)
                                    ((buffer[payloadTypeByteIndex] & 0x80)
                                        | (payloadType & 0x7f));
                        }
                    }

                    int streamWrite
                        = streamDesc.stream.write(buffer, offset, length);

                    if (write < streamWrite)
                        write = streamWrite;
                }
            }
            return write;
        }

        public synchronized void removeStreams(RTPConnectorDesc connectorDesc)
        {
            Iterator<OutputDataStreamDesc> streamIter = streams.iterator();

            while (streamIter.hasNext())
            {
                OutputDataStreamDesc streamDesc = streamIter.next();

                if (streamDesc.connectorDesc == connectorDesc)
                    streamIter.remove();
            }
        }

        public void run()
        {
            try
            {
                while (true)
                {
                    int writeIndex;
                    byte[] buffer;
                    StreamRTPManagerDesc exclusion;
                    Format format;
                    int length;

                    synchronized (this)
                    {
                        if (closed
                                || !Thread.currentThread().equals(writeThread))
                            break;
                        if (writeQueueLength < 1)
                        {
                            boolean interrupted = false;

                            try
                            {
                                wait();
                            }
                            catch (InterruptedException ie)
                            {
                                interrupted = true;
                            }
                            if (interrupted)
                                Thread.currentThread().interrupt();
                            continue;
                        }

                        writeIndex = writeQueueHead;

                        RTPTranslatorBuffer write = writeQueue[writeIndex];

                        buffer = write.data;
                        write.data = null;
                        exclusion = write.exclusion;
                        write.exclusion = null;
                        format = write.format;
                        write.format = null;
                        length = write.length;
                        write.length = 0;

                        writeQueueHead++;
                        if (writeQueueHead >= writeQueue.length)
                            writeQueueHead = 0;
                        writeQueueLength--;
                    }

                    try
                    {
                        doWrite(buffer, 0, length, format, exclusion);
                    }
                    finally
                    {
                        synchronized (this)
                        {
                            RTPTranslatorBuffer write = writeQueue[writeIndex];

                            if ((write != null) && (write.data == null))
                                write.data = buffer;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                logger.error("Failed to translate RTP packet", t);
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
            finally
            {
                synchronized (this)
                {
                    if (Thread.currentThread().equals(writeThread))
                        writeThread = null;
                    if (!closed
                            && (writeThread == null)
                            && (writeQueueLength > 0))
                        createWriteThread();
                }
            }
        }

        public int write(byte[] buffer, int offset, int length)
        {
            return doWrite(buffer, offset, length, null, null);
        }

        public synchronized void write(
                byte[] buffer, int offset, int length,
                Format format,
                StreamRTPManagerDesc exclusion)
        {
            if (closed)
                return;

            int writeIndex;

            if (writeQueueLength < writeQueue.length)
            {
                writeIndex
                    = (writeQueueHead + writeQueueLength) % writeQueue.length;
            }
            else
            {
                writeIndex = writeQueueHead;
                writeQueueHead++;
                if (writeQueueHead >= writeQueue.length)
                    writeQueueHead = 0;
                writeQueueLength--;
                logger.warn("Will not translate RTP packet.");
            }

            RTPTranslatorBuffer write
                = writeQueue[writeIndex];

            if (write == null)
                writeQueue[writeIndex] = write = new RTPTranslatorBuffer();

            byte[] data = write.data;

            if ((data == null) || (data.length < length))
                write.data = data = new byte[length];
            System.arraycopy(buffer, offset, data, 0, length);

            write.exclusion = exclusion;
            write.format = format;
            write.length = length;

            writeQueueLength++;

            if (writeThread == null)
                createWriteThread();
            else
                notify();
        }
    }

    private static class PushSourceStreamDesc
    {
        public final RTPConnectorDesc connectorDesc;

        public final boolean data;

        public final PushSourceStream stream;

        public PushSourceStreamDesc(
                RTPConnectorDesc connectorDesc,
                PushSourceStream stream,
                boolean data)
        {
            this.connectorDesc = connectorDesc;
            this.stream = stream;
            this.data = data;
        }
    }

    private class PushSourceStreamImpl
        implements PushSourceStream,
                   SourceTransferHandler
    {
        private final boolean data;

        private final List<PushSourceStreamDesc> streams
            = new LinkedList<PushSourceStreamDesc>();

        private PushSourceStreamDesc streamToReadFrom;

        private SourceTransferHandler transferHandler;

        public PushSourceStreamImpl(boolean data)
        {
            this.data = data;
        }

        public synchronized void addStream(
                RTPConnectorDesc connectorDesc,
                PushSourceStream stream)
        {
            for (PushSourceStreamDesc streamDesc : streams)
                if ((streamDesc.connectorDesc == connectorDesc)
                        && (streamDesc.stream == stream))
                    return;
            streams.add(
                    new PushSourceStreamDesc(connectorDesc, stream, this.data));
            stream.setTransferHandler(this);
        }

        public void close()
        {
            // TODO Auto-generated method stub
        }

        public boolean endOfStream()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public ContentDescriptor getContentDescriptor()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public long getContentLength()
        {
            return LENGTH_UNKNOWN;
        }

        public Object getControl(String controlType)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Object[] getControls()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public synchronized int getMinimumTransferSize()
        {
            int minimumTransferSize = 0;

            for (PushSourceStreamDesc streamDesc : streams)
            {
                int streamMinimumTransferSize
                    = streamDesc.stream.getMinimumTransferSize();

                if (minimumTransferSize < streamMinimumTransferSize)
                    minimumTransferSize = streamMinimumTransferSize;
            }
            return minimumTransferSize;
        }

        public int read(byte[] buffer, int offset, int length)
            throws IOException
        {
            PushSourceStreamDesc streamDesc;
            int read;

            synchronized (this)
            {
                streamDesc = streamToReadFrom;
                read
                    = (streamDesc == null)
                        ? 0
                        : streamDesc.stream.read(buffer, offset, length);
            }
            if (read > 0)
            {
                read
                    = RTPTranslatorImpl.this.read(
                            streamDesc,
                            buffer, offset, length,
                            read);
            }
            return read;
        }

        public synchronized void removeStreams(RTPConnectorDesc connectorDesc)
        {
            Iterator<PushSourceStreamDesc> streamIter = streams.iterator();

            while (streamIter.hasNext())
            {
                PushSourceStreamDesc streamDesc = streamIter.next();

                if (streamDesc.connectorDesc == connectorDesc)
                {
                    streamDesc.stream.setTransferHandler(null);
                    streamIter.remove();
                    if (streamToReadFrom == streamDesc)
                        streamToReadFrom = null;
                }
            }
        }

        public synchronized void setTransferHandler(
                SourceTransferHandler transferHandler)
        {
            if (this.transferHandler != transferHandler)
            {
                this.transferHandler = transferHandler;
                for (PushSourceStreamDesc streamDesc : streams)
                    streamDesc.stream.setTransferHandler(this);
            }
        }

        public synchronized void transferData(PushSourceStream stream)
        {
            SourceTransferHandler transferHandler = null;

            for (PushSourceStreamDesc streamDesc : streams)
                if (streamDesc.stream == stream)
                {
                    streamToReadFrom = streamDesc;
                    transferHandler = this.transferHandler;
                    break;
                }

            if (transferHandler != null)
                transferHandler.transferData(this);
        }
    }

    private static class RTPConnectorDesc
    {
        public final RTPConnector connector;

        public final StreamRTPManagerDesc streamRTPManagerDesc;

        public RTPConnectorDesc(
                StreamRTPManagerDesc streamRTPManagerDesc,
                RTPConnector connector)
        {
            this.streamRTPManagerDesc = streamRTPManagerDesc;
            this.connector = connector;
        }
    }

    private class RTPConnectorImpl
        implements RTPConnector
    {
        /**
         * The <tt>RTPConnector</tt>s this instance delegates to.
         */
        private final List<RTPConnectorDesc> connectors
            = new LinkedList<RTPConnectorDesc>();

        private PushSourceStreamImpl controlInputStream;

        private OutputDataStreamImpl controlOutputStream;

        private PushSourceStreamImpl dataInputStream;

        private OutputDataStreamImpl dataOutputStream;

        public synchronized void addConnector(RTPConnectorDesc connector)
        {
            if (!connectors.contains(connector))
            {
                connectors.add(connector);
                if (this.controlInputStream != null)
                {
                    PushSourceStream controlInputStream = null;

                    try
                    {
                        controlInputStream
                            = connector.connector.getControlInputStream();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
                    if (controlInputStream != null)
                    {
                        this.controlInputStream.addStream(
                                connector,
                                controlInputStream);
                    }
                }
                if (this.controlOutputStream != null)
                {
                    OutputDataStream controlOutputStream = null;

                    try
                    {
                        controlOutputStream
                            = connector.connector.getControlOutputStream();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
                    if (controlOutputStream != null)
                    {
                        this.controlOutputStream.addStream(
                                connector,
                                controlOutputStream);
                    }
                }
                if (this.dataInputStream != null)
                {
                    PushSourceStream dataInputStream = null;

                    try
                    {
                        dataInputStream
                            = connector.connector.getDataInputStream();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
                    if (dataInputStream != null)
                    {
                        this.dataInputStream.addStream(
                                connector,
                                dataInputStream);
                    }
                }
                if (this.dataOutputStream != null)
                {
                    OutputDataStream dataOutputStream = null;

                    try
                    {
                        dataOutputStream
                            = connector.connector.getDataOutputStream();
                    }
                    catch (IOException ioe)
                    {
                        throw new UndeclaredThrowableException(ioe);
                    }
                    if (dataOutputStream != null)
                    {
                        this.dataOutputStream.addStream(
                                connector,
                                dataOutputStream);
                    }
                }
            }
        }

        public synchronized void close()
        {
            if (controlInputStream != null)
            {
                controlInputStream.close();
                controlInputStream = null;
            }
            if (controlOutputStream != null)
            {
                controlOutputStream.close();
                controlOutputStream = null;
            }
            if (dataInputStream != null)
            {
                dataInputStream.close();
                dataInputStream = null;
            }
            if (dataOutputStream != null)
            {
                dataOutputStream.close();
                dataOutputStream = null;
            }

            for (RTPConnectorDesc connectorDesc : connectors)
                connectorDesc.connector.close();
        }

        public synchronized PushSourceStream getControlInputStream()
            throws IOException
        {
            if (this.controlInputStream == null)
            {
                this.controlInputStream = new PushSourceStreamImpl(false);
                for (RTPConnectorDesc connectorDesc : connectors)
                {
                    PushSourceStream controlInputStream
                        = connectorDesc.connector.getControlInputStream();

                    if (controlInputStream != null)
                    {
                        this.controlInputStream.addStream(
                                connectorDesc,
                                controlInputStream);
                    }
                }
            }
            return this.controlInputStream;
        }

        public synchronized OutputDataStreamImpl getControlOutputStream()
            throws IOException
        {
            if (this.controlOutputStream == null)
            {
                this.controlOutputStream = new OutputDataStreamImpl(false);
                for (RTPConnectorDesc connectorDesc : connectors)
                {
                    OutputDataStream controlOutputStream
                        = connectorDesc.connector.getControlOutputStream();

                    if (controlOutputStream != null)
                    {
                        this.controlOutputStream.addStream(
                                connectorDesc,
                                controlOutputStream);
                    }
                }
            }
            return this.controlOutputStream;
        }

        public synchronized PushSourceStream getDataInputStream()
            throws IOException
        {
            if (this.dataInputStream == null)
            {
                this.dataInputStream = new PushSourceStreamImpl(true);
                for (RTPConnectorDesc connectorDesc : connectors)
                {
                    PushSourceStream dataInputStream
                        = connectorDesc.connector.getDataInputStream();

                    if (dataInputStream != null)
                    {
                        this.dataInputStream.addStream(
                                connectorDesc,
                                dataInputStream);
                    }
                }
            }
            return this.dataInputStream;
        }

        public synchronized OutputDataStreamImpl getDataOutputStream()
            throws IOException
        {
            if (this.dataOutputStream == null)
            {
                this.dataOutputStream = new OutputDataStreamImpl(true);
                for (RTPConnectorDesc connectorDesc : connectors)
                {
                    OutputDataStream dataOutputStream
                        = connectorDesc.connector.getDataOutputStream();

                    if (dataOutputStream != null)
                    {
                        this.dataOutputStream.addStream(
                                connectorDesc,
                                dataOutputStream);
                    }
                }
            }
            return this.dataOutputStream;
        }

        public int getReceiveBufferSize()
        {
            return -1;
        }

        public double getRTCPBandwidthFraction()
        {
            return -1;
        }

        public double getRTCPSenderBandwidthFraction()
        {
            return -1;
        }

        public int getSendBufferSize()
        {
            return -1;
        }

        public synchronized void removeConnector(RTPConnectorDesc connector)
        {
            if (connectors.contains(connector))
            {
                if (controlInputStream != null)
                    controlInputStream.removeStreams(connector);
                if (controlOutputStream != null)
                    controlOutputStream.removeStreams(connector);
                if (dataInputStream != null)
                    dataInputStream.removeStreams(connector);
                if (dataOutputStream != null)
                    dataOutputStream.removeStreams(connector);
                connectors.remove(connector);
            }
        }

        public void setReceiveBufferSize(int receiveBufferSize)
            throws IOException
        {
            // TODO Auto-generated method stub
        }

        public void setSendBufferSize(int sendBufferSize)
            throws IOException
        {
            // TODO Auto-generated method stub
        }
    }

    private static class RTPTranslatorBuffer
    {
        public byte[] data;

        public StreamRTPManagerDesc exclusion;

        public Format format;

        public int length;
    }

    private class SendStreamDesc
    {
        public final DataSource dataSource;

        public final SendStream sendStream;

        private final List<SendStreamImpl> sendStreams
            = new LinkedList<SendStreamImpl>();

        private int started;

        public final int streamIndex;

        public SendStreamDesc(
                DataSource dataSource, int streamIndex,
                SendStream sendStream)
        {
            this.dataSource = dataSource;
            this.sendStream = sendStream;
            this.streamIndex = streamIndex;
        }

        void close(SendStreamImpl sendStream)
        {
            boolean close = false;

            synchronized (this)
            {
                if (sendStreams.contains(sendStream))
                {
                    sendStreams.remove(sendStream);
                    close = sendStreams.isEmpty();
                }
            }
            if (close)
                RTPTranslatorImpl.this.closeSendStream(this);
        }

        public synchronized SendStreamImpl getSendStream(
                StreamRTPManager streamRTPManager,
                boolean create)
        {
            for (SendStreamImpl sendStream : sendStreams)
                if (sendStream.streamRTPManager == streamRTPManager)
                    return sendStream;
            if (create)
            {
                SendStreamImpl sendStream
                    = new SendStreamImpl(streamRTPManager, this);

                sendStreams.add(sendStream);
                return sendStream;
            }
            else
                return null;
        }

        public synchronized int getSendStreamCount()
        {
            return sendStreams.size();
        }

        synchronized void start(SendStreamImpl sendStream)
            throws IOException
        {
            if (sendStreams.contains(sendStream))
            {
                if (started < 1)
                {
                    this.sendStream.start();
                    started = 1;
                }
                else
                    started++;
            }
        }

        synchronized void stop(SendStreamImpl sendStream)
            throws IOException
        {
            if (sendStreams.contains(sendStream))
            {
                if (started == 1)
                {
                    this.sendStream.stop();
                    started = 0;
                }
                else if (started > 1)
                    started--;
            }
        }
    }

    private static class SendStreamImpl
        implements SendStream
    {
        private boolean closed;

        public final SendStreamDesc sendStreamDesc;

        private boolean started;

        public final StreamRTPManager streamRTPManager;

        public SendStreamImpl(
                StreamRTPManager streamRTPManager,
                SendStreamDesc sendStreamDesc)
        {
            this.sendStreamDesc = sendStreamDesc;
            this.streamRTPManager = streamRTPManager;
        }

        public void close()
        {
            if (!closed)
            {
                try
                {
                    if (started)
                        stop();
                }
                catch (IOException ioe)
                {
                    throw new UndeclaredThrowableException(ioe);
                }
                finally
                {
                    sendStreamDesc.close(this);
                    closed = true;
                }
            }
        }

        public DataSource getDataSource()
        {
            return sendStreamDesc.sendStream.getDataSource();
        }

        public Participant getParticipant()
        {
            return sendStreamDesc.sendStream.getParticipant();
        }

        public SenderReport getSenderReport()
        {
            return sendStreamDesc.sendStream.getSenderReport();
        }

        public TransmissionStats getSourceTransmissionStats()
        {
            return sendStreamDesc.sendStream.getSourceTransmissionStats();
        }

        public long getSSRC()
        {
            return sendStreamDesc.sendStream.getSSRC();
        }

        public int setBitRate(int bitRate)
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public void setSourceDescription(SourceDescription[] sourceDescription)
        {
            // TODO Auto-generated method stub
        }

        public void start()
            throws IOException
        {
            if (closed)
            {
                throw
                    new IOException(
                            "Cannot start SendStream"
                                + " after it has been closed.");
            }
            if (!started)
            {
                sendStreamDesc.start(this);
                started = true;
            }
        }

        public void stop()
            throws IOException
        {
            if (!closed && started)
            {
                sendStreamDesc.stop(this);
                started = false;
            }
        }
    }

    private static class StreamRTPManagerDesc
    {
        public RTPConnectorDesc connectorDesc;

        private final Map<Integer, Format> formats
            = new HashMap<Integer, Format>();

        private long[] receiveSSRCs = EMPTY_LONG_ARRAY;

        private final List<ReceiveStreamListener> receiveStreamListeners
            = new LinkedList<ReceiveStreamListener>();

        public final StreamRTPManager streamRTPManager;

        public StreamRTPManagerDesc(StreamRTPManager streamRTPManager)
        {
            this.streamRTPManager = streamRTPManager;
        }

        public void addFormat(Format format, int payloadType)
        {
            synchronized (formats)
            {
                formats.put(payloadType, format);
            }
        }

        public synchronized void addReceiveSSRC(long receiveSSRC)
        {
            if (!containsReceiveSSRC(receiveSSRC))
            {
                int receiveSSRCCount = receiveSSRCs.length;
                long[] newReceiveSSRCs = new long[receiveSSRCCount + 1];

                System.arraycopy(
                    receiveSSRCs, 0,
                    newReceiveSSRCs, 0,
                    receiveSSRCCount);
                newReceiveSSRCs[receiveSSRCCount] = receiveSSRC;
                receiveSSRCs = newReceiveSSRCs;
            }
        }

        public void addReceiveStreamListener(ReceiveStreamListener listener)
        {
            synchronized (receiveStreamListeners)
            {
                if (!receiveStreamListeners.contains(listener))
                    receiveStreamListeners.add(listener);
            }
        }

        public Format getFormat(int payloadType)
        {
            synchronized (formats)
            {
                return formats.get(payloadType);
            }
        }

        public Integer getPayloadType(Format format)
        {
            synchronized (formats)
            {
                for (Map.Entry<Integer, Format> entry : formats.entrySet())
                {
                    Format entryFormat = entry.getValue();

                    if (entryFormat.matches(format)
                            || format.matches(entryFormat))
                        return entry.getKey();
                }
            }
            return null;
        }

        public ReceiveStreamListener[] getReceiveStreamListeners()
        {
            synchronized (receiveStreamListeners)
            {
                return
                    receiveStreamListeners.toArray(
                            new ReceiveStreamListener[
                                    receiveStreamListeners.size()]);
            }
        }

        public synchronized boolean containsReceiveSSRC(long receiveSSRC)
        {
            for (int i = 0; i < receiveSSRCs.length; i++)
                if (receiveSSRCs[i] == receiveSSRC)
                    return true;
            return false;
        }

        public void removeReceiveStreamListener(ReceiveStreamListener listener)
        {
            synchronized (receiveStreamListeners)
            {
                receiveStreamListeners.remove(listener);
            }
        }
    }
}
