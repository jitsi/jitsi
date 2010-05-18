/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
                renderer->dataLength = 0;
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
            memcpy(rendererData, data, dataLength);
            renderer->dataLength = dataLength;
            renderer->dataWidth = width;
            renderer->dataHeight = height;
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
    if (image && ((image->width != width) || (image->height != height)))
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
        if (image && ((image->width != width) || (image->height != height)))
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
            image->data = renderer->data;
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
