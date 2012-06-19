/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.video4linux2;

/**
 * Provides the interface to the native Video for Linux Two API Specification
 * (http://v4l2spec.bytesex.org/spec/) implementation.
 *
 * @author Lubomir Marinov
 */
public class Video4Linux2
{
    public static final int MAP_SHARED = 0x01;

    public static final int O_NONBLOCK = 00004000;

    public static final int O_RDWR = 00000002;

    public static final int PROT_READ = 0x1;

    public static final int PROT_WRITE = 0x2;

    public static final int V4L2_BUF_TYPE_VIDEO_CAPTURE = 1;

    public static final int V4L2_CAP_STREAMING = 0x04000000;

    public static final int V4L2_CAP_VIDEO_CAPTURE = 0x00000001;

    public static final int V4L2_FIELD_NONE = 1;

    public static final int V4L2_MEMORY_MMAP = 1;

    public static final int V4L2_MEMORY_USERPTR = 2;

    public static final int V4L2_PIX_FMT_NONE = 0;

    public static final int V4L2_PIX_FMT_RGB24
        = v4l2_fourcc('R', 'G', 'B', '3');

    public static final int V4L2_PIX_FMT_BGR24
        = v4l2_fourcc('B', 'G', 'R', '3');

    public static final int V4L2_PIX_FMT_UYVY
        = v4l2_fourcc('U', 'Y', 'V', 'Y');

    public static final int V4L2_PIX_FMT_VYUY
        = v4l2_fourcc('V', 'Y', 'U', 'Y');

    public static final int V4L2_PIX_FMT_YUV420
        = v4l2_fourcc('Y', 'U', '1', '2');

    public static final int V4L2_PIX_FMT_YUYV
        = v4l2_fourcc('Y', 'U', 'Y', 'V');

    public static final int V4L2_PIX_FMT_MJPEG
        = v4l2_fourcc('M', 'J', 'P', 'G');

    public static final int V4L2_PIX_FMT_JPEG
        = v4l2_fourcc('J', 'P', 'E', 'G');

    public static final int VIDIOC_DQBUF;

    public static final int VIDIOC_G_FMT;

    public static final int VIDIOC_QBUF;

    public static final int VIDIOC_QUERYBUF;

    public static final int VIDIOC_QUERYCAP;

    public static final int VIDIOC_REQBUFS;

    public static final int VIDIOC_S_FMT;

    public static final int VIDIOC_S_PARM;

    public static final int VIDIOC_STREAMOFF;

    public static final int VIDIOC_STREAMON;

    static
    {
        System.loadLibrary("jnvideo4linux2");

        VIDIOC_DQBUF = VIDIOC_DQBUF();
        VIDIOC_G_FMT = VIDIOC_G_FMT();
        VIDIOC_QBUF = VIDIOC_QBUF();
        VIDIOC_QUERYBUF = VIDIOC_QUERYBUF();
        VIDIOC_QUERYCAP = VIDIOC_QUERYCAP();
        VIDIOC_REQBUFS = VIDIOC_REQBUFS();
        VIDIOC_S_FMT = VIDIOC_S_FMT();
        VIDIOC_S_PARM = VIDIOC_S_PARM();
        VIDIOC_STREAMOFF = VIDIOC_STREAMOFF();
        VIDIOC_STREAMON = VIDIOC_STREAMON();
    }

    public static native int close(int fd);

    public static native void free(long ptr);

    public static native int ioctl(int fd, int request, long argp);

    public static native long memcpy(long dest, long src, int n);

    public static native long mmap(
            long start,
            int length,
            int prot,
            int flags,
            int fd,
            long offset);

    public static native int munmap(long start, int length);

    public static native int open(String deviceName, int flags);

    public static native long v4l2_buffer_alloc(int type);

    public static native int v4l2_buffer_getBytesused(long v4l2_buffer);

    public static native int v4l2_buffer_getIndex(long v4l2_buffer);

    public static native int v4l2_buffer_getLength(long v4l2_buffer);

    public static native long v4l2_buffer_getMOffset(long v4l2_buffer);

    public static native void v4l2_buffer_setIndex(
            long v4l2_buffer,
            int index);

    public static native void v4l2_buffer_setMemory(
            long v4l2_buffer,
            int memory);

    public static native long v4l2_buf_type_alloc(int type);

    public static native long v4l2_capability_alloc();

    public static native int v4l2_capability_getCapabilities(
            long v4l2_capability);

    public static native String v4l2_capability_getCard(
            long v4l2_capability);

    private static int v4l2_fourcc(char a, char b, char c, char d)
    {
        return
            (a & 0xFF)
                | ((b & 0xFF) << 8)
                | ((c & 0xFF) << 16)
                | ((d & 0xFF) << 24);
    }

    public static native long v4l2_format_alloc(int type);

    public static native long v4l2_format_getFmtPix(long v4l2_format);

    public static native int v4l2_pix_format_getHeight(
            long v4l2_pix_format);

    public static native int v4l2_pix_format_getPixelformat(
            long v4l2_pix_format);

    public static native int v4l2_pix_format_getWidth(long v4l2_pix_format);

    public static native void v4l2_pix_format_setBytesperline(
            long v4l2_pix_format,
            int bytesperline);

    public static native void v4l2_pix_format_setField(
            long v4l2_pix_format,
            int field);

    public static native void v4l2_pix_format_setPixelformat(
            long v4l2_pix_format,
            int pixelformat);

    public static native void v4l2_pix_format_setWidthAndHeight(
            long v4l2_pix_format,
            int width, int height);

    public static native long v4l2_requestbuffers_alloc(int type);

    public static native int v4l2_requestbuffers_getCount(
            long v4l2_requestbuffers);

    public static native void v4l2_requestbuffers_setCount(
            long v4l2_requestbuffers,
            int count);

    public static native void v4l2_requestbuffers_setMemory(
            long v4l2_requestbuffers,
            int memory);

    public static native long v4l2_streamparm_alloc(int type);

    public static native void v4l2_streamparm_setFps(long v4l2_streamparm, int fps);

    private static native int VIDIOC_DQBUF();

    private static native int VIDIOC_G_FMT();

    private static native int VIDIOC_QBUF();

    private static native int VIDIOC_QUERYBUF();

    private static native int VIDIOC_QUERYCAP();

    private static native int VIDIOC_REQBUFS();

    private static native int VIDIOC_S_FMT();

    private static native int VIDIOC_S_PARM();

    private static native int VIDIOC_STREAMOFF();

    private static native int VIDIOC_STREAMON();
}
