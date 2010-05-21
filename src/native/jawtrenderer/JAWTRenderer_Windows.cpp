/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "jawt_md.h"
#include "JAWTRenderer.h"

#include "windows/d3d_context.h"
#include "windows/d3d_device.h"
#include "windows/d3d_surface.h"

struct D3DBlitter
{
    D3DContext* d3d;
    D3DDevice* device;
    D3DSurface* surface;
    HWND hwnd;
    size_t width;
    size_t height;
    bool lost;
};

void JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    D3DBlitter* blitter = reinterpret_cast<D3DBlitter*>(handle);

    if(!blitter)
    {
        return;
    }

    if(blitter->surface)
    {
        delete blitter->surface;
        blitter->surface = NULL;
    }

    if(blitter->device)
    {
        delete blitter->device;
        blitter->device = NULL;
    }

    if(blitter->d3d)
    {
        delete blitter->d3d;
        blitter->d3d = NULL;
    }

    delete blitter;
}

jlong JAWTRenderer_open(JNIEnv *jniEnv, jclass clazz, jobject component)
{
    D3DBlitter* ret = new D3DBlitter();

    ret->d3d = D3DContext::createD3DContext();
    ret->surface = NULL;
    ret->device = NULL; 
    ret->width = 0;
    ret->height = 0;
    ret->lost = false;

    /* failed to initialize Direct3D */
    if(!ret->d3d)
    {
        delete ret;
        ret = NULL;
    }

    return (jlong)ret;
}

jboolean JAWTRenderer_paint
    (JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle, jobject g)
{ 
    JAWT_Win32DrawingSurfaceInfo* dsi_win = 
        reinterpret_cast<JAWT_Win32DrawingSurfaceInfo*>(dsi->platformInfo);
    D3DBlitter* blitter = reinterpret_cast<D3DBlitter*>(handle);
    HWND hwnd = WindowFromDC(dsi_win->hdc);
    
    if(!blitter)
        return JNI_FALSE;

    if(blitter->device == NULL || blitter->hwnd != hwnd)
    {
        blitter->hwnd = hwnd;
        blitter->lost = true;
        return JNI_TRUE;
    }

    if(!blitter->device->validate())
    {
        blitter->lost = true;
        return JNI_TRUE;
    }

    blitter->device->render(blitter->surface);
    return JNI_TRUE;
}

jboolean JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height)
{
    D3DBlitter* blitter = reinterpret_cast<D3DBlitter*>(handle);

    if(!blitter)
    {
        return JNI_FALSE;
    }

    if(!blitter->device || blitter->width != width || blitter->height != height
            || blitter->lost)
    {
        blitter->lost = false;

        /* size has changed, recreate our device and surface */
        if(blitter->surface)
        {
            delete blitter->surface;
            blitter->surface = NULL;
        }

        if(blitter->device)
        {
            delete blitter->device;
            blitter->device = NULL;
        }

        blitter->device = blitter->d3d->createDevice(blitter->hwnd, width,
                height);

        if(!blitter->device)
        {
            /* device creation failed */

            /* maybe we go fullscreen and/or hwnd of the window has changed
             * so we return true to force native method to be called and so
             * update blitter->hwnd for the next call
             */
            return JNI_TRUE;
        }

        blitter->surface = blitter->device->createSurface(width, height);

        if(!blitter->surface)
        {
            return JNI_FALSE;
        }

        blitter->width = width;
        blitter->height = height;
    }

    if(blitter->surface)
    {
        blitter->surface->loadData(reinterpret_cast<char*>(data),
                width, height);
    }
    return JNI_TRUE;
}

