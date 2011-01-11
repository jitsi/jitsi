/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file d3d_device.cpp
 * \brief Direct3D device.
 * \author Sebastien Vincent
 * \date 2010
 */

#include "d3d_device.h"

D3DDevice::D3DDevice(HWND hwnd, LPDIRECT3D9 d3d, size_t width, size_t height,
        bool fullscreen)
{
    HRESULT ret = 0;
    D3DPRESENT_PARAMETERS settings;
    D3DDISPLAYMODE dmode;

    ZeroMemory(&settings, sizeof(D3DPRESENT_PARAMETERS));

    ret = d3d->GetAdapterDisplayMode(D3DADAPTER_DEFAULT, &dmode);
    if(FAILED(ret))
    {
        return;
    }

    settings.BackBufferWidth = width;
    settings.BackBufferHeight = height;
    settings.BackBufferFormat = fullscreen ? dmode.Format : D3DFMT_UNKNOWN;
    settings.BackBufferCount = 1;
    //settings.hDeviceWindow = hwnd;
    settings.Windowed = !fullscreen;
    settings.SwapEffect = D3DSWAPEFFECT_DISCARD;

    if(SUCCEEDED(d3d->CheckDeviceMultiSampleType(D3DADAPTER_DEFAULT, 
                    D3DDEVTYPE_HAL, settings.BackBufferFormat, !fullscreen,
                    D3DMULTISAMPLE_2_SAMPLES, NULL)))
    {
        settings.MultiSampleQuality = D3DMULTISAMPLE_2_SAMPLES;
    }
    else
    {
        settings.MultiSampleQuality = D3DMULTISAMPLE_NONE;
    }

    settings.EnableAutoDepthStencil = false;
    settings.AutoDepthStencilFormat = D3DFMT_D16;
    settings.FullScreen_RefreshRateInHz = 0;
    settings.PresentationInterval = D3DPRESENT_INTERVAL_IMMEDIATE; //0;
    settings.Flags = D3DPRESENTFLAG_LOCKABLE_BACKBUFFER;

    ret = d3d->CreateDevice(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, hwnd,
            D3DCREATE_SOFTWARE_VERTEXPROCESSING, &settings, &m_device);

    if(FAILED(ret))
    {
        m_device = NULL;
        /* TODO */
        return;
    }

    /* copy settings */
    memcpy(&m_settings, &settings, sizeof(settings));
}

D3DDevice::~D3DDevice()
{
    if(m_backSurface)
    {
        //m_backSurface->Release();
    }

    if(m_device)
    {
        m_device->Release();
        m_device = NULL;
    }
}

LPDIRECT3DDEVICE9 D3DDevice::getDevice() const
{
    return m_device;
}

bool D3DDevice::validate()
{
    HRESULT ret = 0;

    ret = m_device->TestCooperativeLevel();

    if(FAILED(ret))
    {
        /* device is lost (window not shown) */
        if(ret == D3DERR_DEVICELOST)
        {
            return false;
        }

        /* ready to reset */
        if(ret == D3DERR_DEVICENOTRESET)
        {
            /* XXX in this case we simply delete and recreate device since
             * it always failed to Reset in JAWT but not in pure Windows
             * binary
             */

#if 0
            if(m_backSurface)
            {
                m_backSurface->Release();
                m_backSurface = NULL;
            }

            ret = m_device->Reset(&m_settings);

            if(FAILED(ret))
            {
                return false;
            }

            ret = m_device->GetBackBuffer(0, 0, D3DBACKBUFFER_TYPE_MONO,
                    &m_backSurface);

            if(FAILED(ret))
            {
                return false;
            }
#endif
        }
    }
    return true;
}

D3DSurface* D3DDevice::createSurface(size_t width, size_t height)
{
    D3DSurface* ret = NULL;

    ret = new D3DSurface(getDevice(), width, height);

    if(ret->getSurface())
    {
        return ret;
    }
    else
    {
        /* problem allocating surface */
        delete ret;
        return NULL;
    }
}

void D3DDevice::render(D3DSurface* surface)
{
    HRESULT ret = 0;
    LPDIRECT3DSURFACE9 surfacePointer = surface->getSurface();

    if(!surfacePointer)
    {
        return;
    }

    /* clear the back buffer */
    m_device->Clear(0, 0, D3DCLEAR_TARGET, D3DCOLOR_XRGB(0xff, 0xff, 0xff),
            0.0f, 0);

    /* Get the back buffer */
    ret = m_device->GetBackBuffer(0, 0, D3DBACKBUFFER_TYPE_MONO,
            &m_backSurface);
    if(FAILED(ret))
    {
        return;
    }

    ret = m_device->BeginScene();
    if(FAILED(ret))
    {
        return;
    }

    /* copy content on surface */
    m_device->UpdateSurface(surfacePointer, NULL, m_backSurface, NULL);

    /* finish scene and cleanup */
    m_device->EndScene();
    m_backSurface->Release();
    m_backSurface = NULL;

    /* present the back buffer to the display adapter to be drawn */
    m_device->Present(NULL, NULL, NULL, NULL);
}

