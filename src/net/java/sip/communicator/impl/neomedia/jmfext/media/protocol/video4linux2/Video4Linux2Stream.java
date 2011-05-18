/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.video4linux2;

import java.awt.*;
import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;

/**
 * Implements a <tt>PullBufferStream</tt> using the Video for Linux Two API
 * Specification.
 *
 * @author Lyubomir Marinov
 */
public class Video4Linux2Stream
    extends AbstractVideoPullBufferStream
{
    /**
     * The pool of <tt>ByteBuffer</tt>s this instances is using to transfer the
     * media data captured by the Video for Linux Two API Specification device
     * out of this instance through the <tt>Buffer</tt>s specified in its
     * {@link #read(Buffer)}.
     */
    private final ByteBufferPool byteBufferPool = new ByteBufferPool();

    /**
     * The capabilities of the Video for Linux Two API Specification device
     * represented by {@link #fd}.
     */
    private int capabilities = 0;

    /**
     * The file descriptor of the Video for Linux Two API Specification device
     * read through this <tt>PullBufferStream</tt>.
     */
    private int fd = -1;

    /**
     * The last-known <tt>Format</tt> of the media data made available by this
     * <tt>PullBufferStream</tt>
     */
    private Format format;

    /**
     * The lengths in bytes of the buffers in the application's address space
     * through which the Video for Linux Two API Specification device provides
     * the captured media data to this instance when
     * {@link #requestbuffersMemory} is equal to <tt>V4L2_MEMORY_MAP</tt>.
     */
    private int[] mmapLengths;

    /**
     * The buffers through which the Video for Linux Two API Specification
     * device provides the captured media data to this instance when
     * {@link #requestbuffersMemory} is equal to <tt>V4L2_MEMORY_MAP</tt>. These
     * are mapped in the application's address space.
     */
    private long[] mmaps;

    /**
     * The number of buffers through which the Video for Linux Two API
     * Specification device provides the captured media data to this instance
     * when {@link #requestbuffersMemory} is equal to <tt>V4L2_MEMORY_MMAP</tt>.
     */
    private int requestbuffersCount = 0;

    /**
     * The input method negotiated by this instance with the Video for Linux Two
     * API Specification device.
     */
    private int requestbuffersMemory = 0;

    /**
     * The <tt>v4l2_buffer</tt> instance via which captured media data is
     * fetched from the Video for Linux Two API Specification device to this
     * instance in {@link #read(Buffer)}.
     */
    private long v4l2_buffer;

    /**
     * Native Video for Linux Two pixel format.
     */
    private int nativePixelFormat = 0;

    /**
     * Tell device to start capture in read() method.
     */
    private boolean startInRead = false;

    /**
     * AVCodecContext for MJPEG conversion.
     */
    private long mjpeg_context = 0;

    /**
     * AVFrame that is used in case of JPEG/MJPEG conversion.
     */
    private long avframe = 0;

    /**
     * Initializes a new <tt>Video4Linux2Stream</tt> instance which is to have
     * its <tt>Format</tt>-related information abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param dataSource the <tt>DataSource</tt> which is creating the new
     * instance so that it becomes one of its <tt>streams</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    public Video4Linux2Stream(
            DataSource dataSource,
            FormatControl formatControl)
    {
        super(dataSource, formatControl);

        v4l2_buffer
            = Video4Linux2.v4l2_buffer_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);
        if (0 == v4l2_buffer)
            throw new OutOfMemoryError("v4l2_buffer_alloc");
        Video4Linux2.v4l2_buffer_setMemory(
                v4l2_buffer,
                Video4Linux2.V4L2_MEMORY_MMAP);
    }

    /**
     * Releases the resources used by this instance throughout its existence and
     * makes it available for garbage collection. This instance is considered
     * unusable after closing.
     *
     * @see AbstractPullBufferStream#close()
     */
    @Override
    public void close()
    {
        super.close();

        if (v4l2_buffer != 0)
        {
            Video4Linux2.free(v4l2_buffer);
            v4l2_buffer = 0;
        }
    }

    /**
     * Gets the <tt>Format</tt> of this <tt>PullBufferStream</tt> as directly
     * known by it.
     *
     * @return the <tt>Format</tt> of this <tt>PullBufferStream</tt> as directly
     * known by it or <tt>null</tt> if this <tt>PullBufferStream</tt> does not
     * directly know its <tt>Format</tt> and it relies on the
     * <tt>PullBufferDataSource</tt> which created it to report its
     * <tt>Format</tt>
     * @see AbstractPullBufferStream#doGetFormat()
     */
    @Override
    protected Format doGetFormat()
    {
        Format format;

        if (this.format == null)
        {
            format = getFdFormat();
            if (format == null)
                format = super.doGetFormat();
            else
            {
                VideoFormat videoFormat = (VideoFormat) format;

                if (videoFormat.getSize() != null)
                    this.format = format;
            }
        }
        else
            format = this.format;
        return format;
    }

    /**
     * Reads media data from this <tt>PullBufferStream</tt> into a specific
     * <tt>Buffer</tt> with blocking.
     *
     * @param buffer the <tt>Buffer</tt> in which media data is to be read from
     * this <tt>PullBufferStream</tt>
     * @throws IOException if anything goes wrong while reading media data from
     * this <tt>PullBufferStream</tt> into the specified <tt>buffer</tt>
     * @see AbstractVideoPullBufferStream#doRead(Buffer)
     */
    protected void doRead(Buffer buffer)
        throws IOException
    {
        Format format = buffer.getFormat();

        if (!(format instanceof AVFrameFormat))
            format = null;
        if (format == null)
        {
            format = getFormat();
            if (format != null)
                buffer.setFormat(format);
        }

        if(startInRead)
        {
            startInRead = false;

            long v4l2_buf_type
                = Video4Linux2.v4l2_buf_type_alloc(
                        Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

            if (0 == v4l2_buf_type)
                throw new OutOfMemoryError("v4l2_buf_type_alloc");
            try
            {
                if (Video4Linux2.ioctl(fd, Video4Linux2.VIDIOC_STREAMON,
                        v4l2_buf_type) == -1)
                {
                    throw new IOException("ioctl: request= VIDIOC_STREAMON");
                }
            }
            finally
            {
                Video4Linux2.free(v4l2_buf_type);
            }
        }

        if (Video4Linux2.ioctl(fd, Video4Linux2.VIDIOC_DQBUF, v4l2_buffer)
                == -1)
            throw new IOException("ioctl: request= VIDIOC_DQBUF");

        long timeStamp = System.nanoTime();

        try
        {
            ByteBuffer data = null;
            int index = Video4Linux2.v4l2_buffer_getIndex(v4l2_buffer);
            long mmap = mmaps[index];
            int bytesused = Video4Linux2.v4l2_buffer_getBytesused(v4l2_buffer);

            if(nativePixelFormat == Video4Linux2.V4L2_PIX_FMT_MJPEG ||
                    nativePixelFormat == Video4Linux2.V4L2_PIX_FMT_JPEG)
            {
                /* initialize FFmpeg's MJPEG decoder if not already done */
                if(mjpeg_context == 0)
                {
                    long avcodec = FFmpeg.avcodec_find_decoder(
                            FFmpeg.CODEC_ID_MJPEG);

                    mjpeg_context = FFmpeg.avcodec_alloc_context();
                    FFmpeg.avcodeccontext_set_workaround_bugs(mjpeg_context,
                        FFmpeg.FF_BUG_AUTODETECT);

                    if (FFmpeg.avcodec_open(mjpeg_context, avcodec) < 0)
                    {
                        throw new RuntimeException("" +
                                "Could not open codec CODEC_ID_MJPEG");
                    }

                    avframe = FFmpeg.avcodec_alloc_frame();
                }

                if(FFmpeg.avcodec_decode_video(mjpeg_context, avframe, mmap,
                        bytesused) != -1)
                {
                    Object out = buffer.getData();

                    if (!(out instanceof AVFrame) ||
                            (((AVFrame) out).getPtr() != avframe))
                    {
                        buffer.setData(new AVFrame(avframe));
                    }
                }
            }
            else
            {
                data = byteBufferPool.getFreeBuffer(bytesused);

                if (data != null)
                {
                    Video4Linux2.memcpy(data.ptr, mmap, bytesused);
                }
                data.setLength(bytesused);
                FinalizableAVFrame.read(buffer, format, data, byteBufferPool);
            }
        }
        finally
        {
            if (Video4Linux2.ioctl(fd, Video4Linux2.VIDIOC_QBUF, v4l2_buffer)
                    == -1)
                throw new IOException("ioctl: request= VIDIOC_QBUF");
        }

        buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
        buffer.setTimeStamp(timeStamp);
    }

    /**
     * Gets the <tt>Format</tt> of the media data captured by the Video for
     * Linux Two API Specification device represented by the <tt>fd</tt> of this
     * instance.
     *
     * @return the <tt>Format</tt> of the media data captured by the Video for
     * Linux Two API Specification device represented by the <tt>fd</tt> of this
     * instance
     */
    private Format getFdFormat()
    {
        Format format = null;

        if (-1 != fd)
        {
            long v4l2_format
                = Video4Linux2.v4l2_format_alloc(
                        Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

            if (v4l2_format == 0)
                throw new OutOfMemoryError("v4l2_format_alloc");
            else
            {
                try
                {
                    if (Video4Linux2.ioctl(
                                fd,
                                Video4Linux2.VIDIOC_G_FMT,
                                v4l2_format)
                            != -1)
                    {
                        long fmtPix
                            = Video4Linux2.v4l2_format_getFmtPix(v4l2_format);
                        int pixelformat
                            = Video4Linux2.v4l2_pix_format_getPixelformat(
                                    fmtPix);
                        int ffmpegPixFmt
                            = DataSource.getFFmpegPixFmt(pixelformat);

                        if (FFmpeg.PIX_FMT_NONE != ffmpegPixFmt)
                        {
                            int width
                                = Video4Linux2.v4l2_pix_format_getWidth(fmtPix);
                            int height
                                = Video4Linux2.v4l2_pix_format_getHeight(
                                        fmtPix);

                            format
                                = new AVFrameFormat(
                                        new Dimension(width, height),
                                        Format.NOT_SPECIFIED,
                                        ffmpegPixFmt,
                                        pixelformat);
                        }
                    }
                }
                finally
                {
                    Video4Linux2.free(v4l2_format);
                }
            }
        }
        return format;
    }

    /**
     * Unmaps the buffers through which the Video for Linux Two API
     * Specification device provides the captured media data to this instance
     * when {@link #requestbuffersMemory} is equal to <tt>V4L2_MEMORY_MMAP</tt>
     * i.e. breaks the buffers' mappings between the driver's and the
     * application's address spaces.
     */
    private void munmap()
    {
        try
        {
            if (mmaps != null)
            {
                for (int i = 0; i < mmaps.length; i++)
                {
                    long mmap = mmaps[i];

                    if (mmap != 0)
                    {
                        Video4Linux2.munmap(mmap, mmapLengths[i]);
                        mmaps[i] = 0;
                        mmapLengths[i] = 0;
                    }
                }
            }
        }
        finally
        {
            mmaps = null;
            mmapLengths = null;
        }
    }

    /**
     * Negotiates the input method with the Video for Linux Two API
     * Specification device represented by the <tt>fd</tt> of this instance.
     *
     * @throws IOException if anything goes wrong while negotiating the input
     * method with the Video for Linux Two API Specification device represented
     * by the <tt>fd</tt> of this instance
     */
    private void negotiateFdInputMethod()
        throws IOException
    {
        long v4l2_capability = Video4Linux2.v4l2_capability_alloc();

        if (0 == v4l2_capability)
            throw new OutOfMemoryError("v4l2_capability_alloc");
        try
        {
            if (Video4Linux2.ioctl(
                        fd,
                        Video4Linux2.VIDIOC_QUERYCAP,
                        v4l2_capability)
                    == -1)
                throw new IOException("ioctl: request= VIDIOC_QUERYCAP");
            capabilities
                = Video4Linux2.v4l2_capability_getCapabilities(v4l2_capability);
        }
        finally
        {
            Video4Linux2.free(v4l2_capability);
        }
        if ((capabilities & Video4Linux2.V4L2_CAP_STREAMING)
                != Video4Linux2.V4L2_CAP_STREAMING)
            throw new IOException("Non-streaming V4L2 device not supported.");

        long v4l2_requestbuffers
            = Video4Linux2.v4l2_requestbuffers_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

        if (0 == v4l2_requestbuffers)
            throw new OutOfMemoryError("v4l2_requestbuffers_alloc");
        try
        {
            requestbuffersMemory = Video4Linux2.V4L2_MEMORY_MMAP;
            Video4Linux2.v4l2_requestbuffers_setMemory(
                    v4l2_requestbuffers,
                    requestbuffersMemory);
            Video4Linux2.v4l2_requestbuffers_setCount(v4l2_requestbuffers, 2);
            if (Video4Linux2.ioctl(
                        fd,
                        Video4Linux2.VIDIOC_REQBUFS,
                        v4l2_requestbuffers)
                    == -1)
            {
                throw
                    new IOException(
                            "ioctl: request= VIDIOC_REQBUFS, memory= "
                                + requestbuffersMemory);
            }
            requestbuffersCount
                = Video4Linux2.v4l2_requestbuffers_getCount(
                        v4l2_requestbuffers);
        }
        finally
        {
            Video4Linux2.free(v4l2_requestbuffers);
        }
        if (requestbuffersCount < 1)
            throw new IOException("Insufficient V4L2 device memory.");


        long v4l2_buffer
            = Video4Linux2.v4l2_buffer_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

        if (0 == v4l2_buffer)
            throw new OutOfMemoryError("v4l2_buffer_alloc");

        try
        {
            Video4Linux2.v4l2_buffer_setMemory(
                    v4l2_buffer,
                    Video4Linux2.V4L2_MEMORY_MMAP);

            mmaps = new long[requestbuffersCount];
            mmapLengths = new int[requestbuffersCount];

            boolean munmap = true;

            try
            {
                for (int i = 0; i < requestbuffersCount; i++)
                {
                    Video4Linux2.v4l2_buffer_setIndex(v4l2_buffer, i);
                    if (Video4Linux2.ioctl(
                                fd,
                                Video4Linux2.VIDIOC_QUERYBUF,
                                v4l2_buffer)
                            == -1)
                    {
                        throw
                            new IOException("ioctl: request= VIDIOC_QUERYBUF");
                    }

                    int length
                        = Video4Linux2.v4l2_buffer_getLength(v4l2_buffer);
                    long offset
                        = Video4Linux2.v4l2_buffer_getMOffset(v4l2_buffer);
                    long mmap
                        = Video4Linux2.mmap(
                                0,
                                length,
                                Video4Linux2.PROT_READ
                                    | Video4Linux2.PROT_WRITE,
                                Video4Linux2.MAP_SHARED,
                                fd,
                                offset);

                    if (-1 == mmap)
                        throw new IOException("mmap");
                    mmaps[i] = mmap;
                    mmapLengths[i] = length;
                }
                munmap = false;
            }
            finally
            {
                if (munmap)
                    munmap();
            }
        }
        finally
        {
            Video4Linux2.free(v4l2_buffer);
        }
    }

    /**
     * Sets the file descriptor of the Video for Linux Two API Specification
     * device which is to be read through this <tt>PullBufferStream</tt>.
     *
     * @param fd the file descriptor of the Video for Linux Two API
     * Specification device which is to be read through this
     * <tt>PullBufferStream</tt>
     * @throws IOException if anything goes wrong while setting the file
     * descriptor of the Video for Linux Two API Specification device which is
     * to be read through this <tt>PullBufferStream</tt>
     */
    void setFd(int fd)
        throws IOException
    {
        if (this.fd != fd)
        {
            if (this.fd != -1)
            {
                try
                {
                    stop();
                }
                catch (IOException ioex)
                {
                }
                munmap();
            }

            /*
             * Before a Video for Linux Two API Specification device can be
             * read, an attempt to set its format must be made and its cropping
             * must be reset. We can only learn about the format to be set from
             * formatControl. But since this AbstractPullBufferStream exists
             * already, formatControl will ask it about its format. So pretend
             * that there is no device prior to asking formatControl about the
             * format in order to get the format that has been set by the user.
             */
            this.fd = -1;
            this.capabilities = 0;
            this.requestbuffersMemory = 0;
            this.requestbuffersCount = 0;

            if (fd != -1)
            {
                Format format = getFormat();

                this.fd = fd;
                if (format != null)
                    setFdFormat(format);
                setFdCropToDefault();
                negotiateFdInputMethod();
            }
        }
    }

    /**
     * Sets the crop of the Video for Linux Two API Specification device
     * represented by the <tt>fd</tt> of this instance to its default value so
     * that this <tt>PullBufferStream</tt> reads media data without cropping.
     */
    private void setFdCropToDefault()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the <tt>Format</tt> in which the Video for Linux Two API
     * Specification device represented by the <tt>fd</tt> of this instance is
     * to capture media data.
     *
     * @param format the <tt>Format</tt> of the media data to be captured by the
     * Video for Linux Two API Specification device represented by the
     * <tt>fd</tt> of this instance
     * @throws IOException if anything goes wrong while setting the
     * <tt>Format</tt> of the media data to be captured by the Video for Linux
     * Two API Specification device represented by the <tt>fd</tt> of this
     * instance
     */
    private void setFdFormat(Format format)
        throws IOException
    {
        int pixelformat = 0;

        if (format instanceof AVFrameFormat)
        {
            pixelformat = ((AVFrameFormat) format).getDevicePixFmt();
            nativePixelFormat = pixelformat;
        }
        if (Video4Linux2.V4L2_PIX_FMT_NONE == pixelformat)
            throw new IOException("Unsupported format " + format);

        long v4l2_format
            = Video4Linux2.v4l2_format_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

        if (v4l2_format == 0)
            throw new OutOfMemoryError("v4l2_format_alloc");
        try
        {
            if (Video4Linux2.ioctl(fd, Video4Linux2.VIDIOC_G_FMT, v4l2_format)
                    == -1)
                throw new IOException("ioctl: request= VIDIO_G_FMT");

            VideoFormat videoFormat = (VideoFormat) format;
            Dimension size = videoFormat.getSize();
            long fmtPix = Video4Linux2.v4l2_format_getFmtPix(v4l2_format);
            int width = Video4Linux2.v4l2_pix_format_getWidth(fmtPix);
            int height = Video4Linux2.v4l2_pix_format_getHeight(fmtPix);
            boolean setFdFormat = false;

            if (size == null)
            {
                // if there is no size in the format, respect settings
                size = NeomediaActivator
                        .getMediaServiceImpl()
                            .getDeviceConfiguration()
                                .getVideoSize();
            }

            if ((size != null)
                    && ((size.width != width) || (size.height != height)))
            {
                Video4Linux2.v4l2_pix_format_setWidthAndHeight(
                        fmtPix,
                        size.width, size.height);
                setFdFormat = true;
            }
            if (Video4Linux2.v4l2_pix_format_getPixelformat(v4l2_format)
                    != pixelformat)
            {
                Video4Linux2.v4l2_pix_format_setPixelformat(
                        fmtPix,
                        pixelformat);

                setFdFormat = true;
            }

            if (setFdFormat)
                setFdFormat(v4l2_format, fmtPix, size, pixelformat);
        }
        finally
        {
            Video4Linux2.free(v4l2_format);
        }
    }

    /**
     * Sets the <tt>Format</tt> in which the Video for Linux Two API
     * Specification device represented by the <tt>fd</tt> of this instance is
     * to capture media data.
     *
     * @param v4l2_format native format to set on the Video for Linux Two API
     * Specification device
     * @param fmtPix native pixel format of the device
     * @param size size to set on the device
     * @param pixelformat requested pixel format
     * @throws IOException if anything goes wrong while setting the
     * native format of the media data to be captured by the Video for Linux
     * Two API Specification device represented by the <tt>fd</tt> of this
     * instance
     */
    private void setFdFormat(
            long v4l2_format,
            long fmtPix,
            Dimension size,
            int pixelformat)
        throws IOException
    {
        Video4Linux2.v4l2_pix_format_setField(
                fmtPix,
                Video4Linux2.V4L2_FIELD_NONE);
        Video4Linux2.v4l2_pix_format_setBytesperline(fmtPix, 0);

        if (Video4Linux2.ioctl(
                    fd,
                    Video4Linux2.VIDIOC_S_FMT,
                    v4l2_format)
                == -1)
        {
            throw
                new IOException(
                        "ioctl: request= VIDIOC_S_FMT"
                            + ((size == null)
                                ? ""
                                : (", width= "
                                    + size.width
                                    + ", height= "
                                    + size.height))
                            + ", pixelformat= "
                            + pixelformat);
        }
        else if (Video4Linux2.v4l2_pix_format_getPixelformat(fmtPix)
                != pixelformat)
        {
            throw
                new IOException(
                        "Failed to change the format of the V4L2 device to "
                            + pixelformat);
        }
    }

    /**
     * Starts the transfer of media data from this <tt>PullBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>PullBufferStream</tt>
     * @see AbstractPullBufferStream#start()
     */
    @Override
    public void start()
        throws IOException
    {
        super.start();

        long v4l2_buffer
            = Video4Linux2.v4l2_buffer_alloc(
                    Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

        if (0 == v4l2_buffer)
            throw new OutOfMemoryError("v4l2_buffer_alloc");
        try
        {
            Video4Linux2.v4l2_buffer_setMemory(
                    v4l2_buffer,
                    Video4Linux2.V4L2_MEMORY_MMAP);
            for (int i = 0; i < requestbuffersCount; i++)
            {
                Video4Linux2.v4l2_buffer_setIndex(v4l2_buffer, i);
                if (Video4Linux2.ioctl(
                            fd,
                            Video4Linux2.VIDIOC_QBUF,
                            v4l2_buffer)
                        == -1)
                {
                    throw
                        new IOException(
                                "ioctl: request= VIDIOC_QBUF, index= " + i);
                }
            }
        }
        finally
        {
            Video4Linux2.free(v4l2_buffer);
        }

        /* we will start capture in read() method (i.e do the VIDIOC_STREAMON
         * ioctl) because for some couple of fps/resolution the captured image
         * will be weird (shift, not a JPEG for JPEG/MJPEG format, ...)
         * if it is done here. Maybe it is due because sometime JMF do the
         * sequence start/stop/start too quickly...
         */
        startInRead = true;
    }

    /**
     * Stops the transfer of media data from this <tt>PullBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>PullBufferStream</tt>
     * @see AbstractPullBufferStream#stop()
     */
    @Override
    public void stop()
        throws IOException
    {
        try
        {
            long v4l2_buf_type
                = Video4Linux2.v4l2_buf_type_alloc(
                        Video4Linux2.V4L2_BUF_TYPE_VIDEO_CAPTURE);

            if (0 == v4l2_buf_type)
                throw new OutOfMemoryError("v4l2_buf_type_alloc");
            try
            {
                if (Video4Linux2.ioctl(
                            fd,
                            Video4Linux2.VIDIOC_STREAMOFF,
                            v4l2_buf_type)
                        == -1)
                {
                    throw new IOException("ioctl: request= VIDIOC_STREAMOFF");
                }
            }
            finally
            {
                Video4Linux2.free(v4l2_buf_type);
            }
        }
        finally
        {
            super.stop();

            if(mjpeg_context > 0)
            {
                FFmpeg.avcodec_close(mjpeg_context);
                FFmpeg.av_free(mjpeg_context);
            }
            mjpeg_context = 0;

            if(avframe > 0)
            {
                FFmpeg.av_free(avframe);
            }
            avframe = 0;

        }
    }
}
