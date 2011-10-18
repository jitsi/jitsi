/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "JAWTRenderer.h"

#include <jawt_md.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <X11/extensions/Xvlib.h>

typedef struct _JAWTRenderer
{
    Display *display;
    Drawable drawable;

    XvPortID port;
    int imageFormatID;
    XvImage *image;

    char *data;
    size_t dataCapacity;
    jint dataHeight;
    jint dataLength;
    int dataOffsets[3];
    int dataPitches[3];
    jint dataWidth;
}
JAWTRenderer;

static XvImage *_JAWTRenderer_createImage(JAWTRenderer *renderer);
static int _JAWTRenderer_freeImage(JAWTRenderer *renderer);
static XvPortID _JAWTRenderer_grabPort
    (JAWTRenderer *renderer, JAWT_X11DrawingSurfaceInfo *x11dsi);
static int _JAWTRenderer_ungrabPort(JAWTRenderer *renderer);

void
JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer *renderer;

    renderer = (JAWTRenderer *) handle;
    if (-1 != renderer->port)
        _JAWTRenderer_ungrabPort(renderer);
    if (renderer->data)
        free(renderer->data);
    free(renderer);
}

jlong
JAWTRenderer_open(JNIEnv *jniEnv, jclass clazz, jobject component)
{
    Display *display;
    JAWTRenderer *renderer;

    display = XOpenDisplay(NULL);
    if (display)
    {
        unsigned int ver, rev, req, ev, err;

        if (Success == XvQueryExtension(display, &ver, &rev, &req, &ev, &err))
        {
            renderer = malloc(sizeof(JAWTRenderer));
            if (renderer)
            {
                renderer->display = NULL;
                renderer->drawable = 0;

                renderer->port = -1;
                renderer->image = NULL;

                renderer->data = NULL;
                renderer->dataHeight = 0;
                renderer->dataLength = 0;
                renderer->dataWidth = 0;
            }
        }
        else
            renderer = NULL;
        XCloseDisplay(display);
    }
    else
        renderer = NULL;
    return (jlong) renderer;
}

jboolean
JAWTRenderer_paint
    (JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle, jobject g)
{
    JAWT_X11DrawingSurfaceInfo *x11dsi;
    JAWTRenderer *renderer;
    Display *display;
    Drawable drawable;
    XvPortID port;

    x11dsi = (JAWT_X11DrawingSurfaceInfo *) (dsi->platformInfo);
    renderer = (JAWTRenderer *) handle;

    display = x11dsi->display;
    drawable = x11dsi->drawable;
    if ((renderer->display != display) || (renderer->drawable != drawable))
    {
        if (-1 != renderer->port)
            _JAWTRenderer_ungrabPort(renderer);

        renderer->display = display;
        renderer->drawable = drawable;

        port = _JAWTRenderer_grabPort(renderer, x11dsi);
    }
    else
        port = renderer->port;
    if (-1 != port)
    {
        XvImage *image;

        if (renderer->data && renderer->dataLength)
            image = _JAWTRenderer_createImage(renderer);
        else
            image = renderer->image;
        if (image)
        {
            Window root;
            int x, y;
            unsigned int width, height;
            unsigned int borderWidth;
            unsigned int depth;

            if (XGetGeometry(
                    display,
                    drawable,
                    &root,
                    &x, &y,
                    &width, &height,
                    &borderWidth,
                    &depth))
            {
                GC gc;

                gc = XCreateGC(display, drawable, 0, NULL);
                /* XXX How does one check that XCreateGC has succeeded? */
                XvPutImage(
                    display,
                    port,
                    drawable,
                    gc,
                    image,
                    0, 0, image->width, image->height,
                    0, 0, width, height);
                XFreeGC(display, gc);
            }
        }
    }
    return JNI_TRUE;
}

jboolean
JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height)
{
    if (data && length)
    {
        JAWTRenderer *renderer;
        char *rendererData;
        jint dataLength;

        renderer = (JAWTRenderer *) handle;
        rendererData = renderer->data;
        dataLength = sizeof(jint) * length;
        if (!rendererData || (renderer->dataCapacity < dataLength))
        {
            char *newData;

            newData = realloc(rendererData, dataLength);
            if (newData)
            {
                renderer->data = rendererData = newData;
                renderer->dataCapacity = dataLength;
            }
            else
                rendererData = NULL;
        }
        if (rendererData)
        {
            /*
             * If the image has already told us the offsets and the pitches with
             * which the data should be, apply them now so that we end up with
             * one copying.
             */
            if ((renderer->dataWidth == width)
                    && (renderer->dataHeight == height))
            {
                int *rendererDataPitches;
                int *rendererDataOffsets;
                char *dataAsChars;
                int planeIndex;

                rendererDataPitches = renderer->dataPitches;
                rendererDataOffsets = renderer->dataOffsets;
                dataAsChars = (char *) data;
                for (planeIndex = 0; planeIndex < 3; planeIndex++)
                {
                    int dataPitch;
                    int rendererDataPitch;
                    int planeHeight;
                    int rendererDataOffset;

                    dataPitch = planeIndex ? (width / 2) : width;
                    rendererDataPitch = rendererDataPitches[planeIndex];
                    planeHeight = planeIndex ? (height / 2) : height;
                    rendererDataOffset = rendererDataOffsets[planeIndex];
                    if (dataPitch == rendererDataPitch)
                    {
                        int planeSize;

                        planeSize = dataPitch * planeHeight;
                        memcpy(
                            rendererData + rendererDataOffset,
                            dataAsChars,
                            planeSize);
                        dataAsChars += planeSize;
                    }
                    else
                    {
                        char *rendererDataRow;
                        int rowIndex;

                        rendererDataRow = rendererData + rendererDataOffset;
                        for (rowIndex = 0;
                                rowIndex < planeHeight;
                                rowIndex++)
                        {
                            memcpy(rendererDataRow, dataAsChars, dataPitch);
                            rendererDataRow += rendererDataPitch;
                            dataAsChars += dataPitch;
                        }
                    }
                }
            }
            else
            {
                /*
                 * Since the image has not told us the offsets and the pitches
                 * with which the data should be, assume the defaults.
                 */
                int *dataPitches;
                int pitchY;
                int pitchUV;
                int *dataOffsets;
                int offsetU;

                memcpy(rendererData, data, dataLength);

                renderer->dataWidth = width;
                renderer->dataHeight = height;

                dataPitches = renderer->dataPitches;
                pitchY = width;
                dataPitches[0] = pitchY;
                pitchUV = width / 2;
                dataPitches[1] = pitchUV;
                dataPitches[2] = pitchUV;
                dataOffsets = renderer->dataOffsets;
                dataOffsets[0] = 0;
                offsetU = pitchY * height;
                dataOffsets[1] = offsetU;
                dataOffsets[2] = offsetU + pitchUV * height / 2;
            }
            renderer->dataLength = dataLength;
        }
        else
            return JNI_FALSE;
    }
    return JNI_TRUE;
}

static XvImage *
_JAWTRenderer_createImage(JAWTRenderer *renderer)
{
    XvImage *image;
    jint width;
    jint height;

    image = renderer->image;
    width = renderer->dataWidth;
    height = renderer->dataHeight;

    /* XvCreateImage is limited to 2048x2048 image so do not drop image
     * if size exceed the limit
     */
    if (image && ((image->width != width) || (image->height != height)) &&
          width < 2048 && height < 2048)
    {
        XFree(image);
        image = NULL;
    }
    if (!image)
    {
        image
            = XvCreateImage(
                renderer->display,
                renderer->port,
                renderer->imageFormatID,
                NULL,
                width, height);

        /*
         * XvCreateImage is documented to enlarge width and height for some YUV
         * formats. But I don't know how to handle such a situation.
         */
        /* XvCreateImage is limited to 2048x2048 image so do not drop image
         * if size exceed this limit
         */
        if (image && ((image->width != width) || (image->height != height)) &&
                width < 2048 && height < 2048)
        {
            XFree(image);
            image = NULL;
        }
    }
    if (image)
    {
        size_t imageDataSize;

        imageDataSize = image->data_size;
        if (imageDataSize > renderer->dataCapacity)
        {
            size_t newDataCapacity;
            char *newData;

            newDataCapacity = imageDataSize;
            newData = realloc(renderer->data, newDataCapacity);
            if (newData)
            {
                renderer->data = newData;
                renderer->dataCapacity = newDataCapacity;
            }
            else
            {
                XFree(image);
                image = NULL;
            }
        }
        if (image)
        {
            char *data;
            int planeCount;
            int *dataOffsets;
            int *imageOffsets;
            int planeIndexIncrement;
            int planeIndex;
            int beginPlaneIndex;
            int endPlaneIndex;

            data = renderer->data;
            image->data = data;

            /*
             * The data may have different offsets and/or pitches than the image
             * so the data has to be moved according to the offsets and the
             * pitches of the image. Start by determining the direction in which
             * the planes will be moving in order to begin either from the first
             * plane or from the last plane so that no pixels get overwritten.
             */
            planeCount = image->num_planes;
            dataOffsets = renderer->dataOffsets;
            imageOffsets = image->offsets;
            planeIndexIncrement = 0;
            for (planeIndex = 0; planeIndex < planeCount; planeIndex++)
            {
                int dataOffset;
                int imageOffset;

                dataOffset = dataOffsets[planeIndex];
                imageOffset = imageOffsets[planeIndex];
                if (dataOffset == imageOffset)
                    continue;
                else
                {
                    if (dataOffset < imageOffset)
                    {
                        beginPlaneIndex = planeCount - 1;
                        endPlaneIndex = -1;
                        planeIndexIncrement =  -1;
                    }
                    else
                    {
                        beginPlaneIndex = 0;
                        endPlaneIndex = planeCount;
                        planeIndexIncrement = 1;
                    }
                    break;
                }
            }
            if (planeIndexIncrement)
            {
                int *dataPitches;
                int *imagePitches;

                dataPitches = renderer->dataPitches;
                imagePitches = image->pitches;
                for (planeIndex = beginPlaneIndex;
                        planeIndex != endPlaneIndex;
                        planeIndex += planeIndexIncrement)
                {
                    int dataPitch;
                    int imagePitch;
                    int dataOffset;
                    int imageOffset;
                    int planeHeight;

                    dataPitch = dataPitches[planeIndex];
                    imagePitch = imagePitches[planeIndex];
                    dataOffset = dataOffsets[planeIndex];
                    imageOffset = imageOffsets[planeIndex];
                    planeHeight = planeIndex ? (height / 2) : height;
                    if (dataPitch == imagePitch)
                    {
                        if (dataOffset != imageOffset)
                        {
                            memmove(
                                data + imageOffset,
                                data + dataOffset,
                                dataPitch * planeHeight);
                            dataOffsets[planeIndex] = imageOffset;
                        }
                    }
                    else
                    {
                        int planeWidth;
                        char *dataRow;
                        char *imageRow;
                        int rowIndex;

                        planeWidth = planeIndex ? (width / 2) : width;
                        /*
                         * The direction of the moving has to be applied to the
                         * moving of the rows as well in order to avoid
                         * overwriting.
                         */
                        if (planeIndexIncrement < 0)
                        {
                            dataRow
                                = data
                                    + (dataOffset + dataPitch * planeHeight);
                            imageRow
                                = data
                                    + (imageOffset + imagePitch * planeHeight);
                            for (rowIndex = planeHeight - 1;
                                    rowIndex > -1;
                                    rowIndex--)
                            {
                                dataRow -= dataPitch;
                                imageRow -= imagePitch;
                                memmove(imageRow, dataRow, planeWidth);
                            }
                        }
                        else
                        {
                            dataRow = data + dataOffset;
                            imageRow = data + imageOffset;
                            for (rowIndex = 0;
                                    rowIndex < planeHeight;
                                    rowIndex++)
                            {
                                memmove(imageRow, dataRow, planeWidth);
                                dataRow += dataPitch;
                                imageRow += imagePitch;
                            }
                        }
                        dataPitches[planeIndex] = imagePitch;
                        dataOffsets[planeIndex] = imageOffset;
                    }
                }
            }

            /*
             * We've just turned data into image and we don't want to do it
             * again.
             */
            renderer->dataLength = 0;
        }
    }
    renderer->image = image;
    return image;
}

static int
_JAWTRenderer_freeImage(JAWTRenderer *renderer)
{
    int ret;

    ret = XFree(renderer->image);
    renderer->image = NULL;
    return ret;
}

static XvPortID
_JAWTRenderer_grabPort
    (JAWTRenderer *renderer, JAWT_X11DrawingSurfaceInfo *x11dsi)
{
    Display *display;
    unsigned int ver, rev, req, ev, err;
    Drawable drawable;
    unsigned int adaptorInfoCount;
    XvAdaptorInfo *adaptorInfos;
    XvPortID grabbedPort;

    display = renderer->display;
    drawable = renderer->drawable;
    grabbedPort = -1;
    if ((Success == XvQueryExtension(display, &ver, &rev, &req, &ev, &err))
            && (Success
                    == XvQueryAdaptors(
                            display,
                            (Window) drawable,
                            &adaptorInfoCount, &adaptorInfos))
            && adaptorInfoCount)
    {
        int depth;
        VisualID visualID;
        unsigned int adaptorInfoIndex;

        depth = x11dsi->depth;
        visualID = x11dsi->visualID;
        for (adaptorInfoIndex = 0;
                adaptorInfoIndex < adaptorInfoCount;
                adaptorInfoIndex++)
        {
            XvAdaptorInfo *adaptorInfo;
            char type;

            unsigned long formatCount;
            XvFormat *formats;
            unsigned long formatIndex;
            Bool formatIsFound;

            unsigned long portCount;
            XvPortID basePortID;
            unsigned long portIndex;

            adaptorInfo = adaptorInfos + adaptorInfoIndex;
            type = adaptorInfo->type;
            if (!(type & XvInputMask) || !(type & XvImageMask))
                continue;

            formatCount = adaptorInfo->num_formats;
            formats = adaptorInfo->formats;
            formatIsFound = False;
            for (formatIndex = 0; formatIndex < formatCount; formatIndex++)
            {
                XvFormat *format;

                format = formats + formatIndex;
                if ((depth == format->depth) && (visualID == format->visual_id))
                {
                    formatIsFound = True;
                    break;
                }
            }
            if (!formatIsFound)
                continue;

            portCount = adaptorInfo->num_ports;
            basePortID = adaptorInfo->base_id;
            for (portIndex = 0; portIndex < portCount; portIndex++)
            {
                XvPortID port;
                XvImageFormatValues *imageFormats;
                int imageFormatCount;

                port = basePortID + portIndex;
                imageFormats
                    = XvListImageFormats(display, port, &imageFormatCount);
                if (imageFormats && imageFormatCount)
                {
                    int imageFormatIndex;

                    for (imageFormatIndex = 0;
                            imageFormatIndex < imageFormatCount;
                            imageFormatIndex++)
                    {
                        XvImageFormatValues *imageFormat;
                        const char *guid;

                        imageFormat = imageFormats + imageFormatIndex;
                        guid = imageFormat->guid;
                        /* I420 */
                        if (('I' == guid[0])
                                && ('4' == guid[1])
                                && ('2' == guid[2])
                                && ('0' == guid[3]))
                        {
                            if (Success
                                    == XvGrabPort(display, port, CurrentTime))
                            {
                                grabbedPort = port;
                                renderer->imageFormatID = imageFormat->id;
                            }
                            break;
                        }
                    }
                    XFree(imageFormats);
                    if (-1 != grabbedPort)
                        break;
                }
            }
            if (-1 != grabbedPort)
                break;
        }
        XvFreeAdaptorInfo(adaptorInfos);
    }
    renderer->port = grabbedPort;
    return grabbedPort;
}

static int
_JAWTRenderer_ungrabPort(JAWTRenderer *renderer)
{
    int ret;

    /* The XvImage is created on the XvPortID. */
    if (renderer->image)
        _JAWTRenderer_freeImage(renderer);

    ret = XvUngrabPort(renderer->display, renderer->port, CurrentTime);
    renderer->port = -1;
    return ret;
}
