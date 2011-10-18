/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file d3d_context.cpp
 * \brief Direct3D context.
 * \author Sebastien Vincent
 * \date 2010
 */

#include "d3d_context.h"

D3DContext* D3DContext::createD3DContext()
{
    D3DContext* ret = new D3DContext();

    if(ret->getDirect3D())
    {
        return ret;
    }
    else
    {
        /* fail to initialize Direct3D */
        delete ret;
        return NULL;
    }
}

D3DContext::D3DContext()
{
    m_d3d = Direct3DCreate9(D3D_SDK_VERSION);
}

D3DContext::~D3DContext()
{
    if(m_d3d)
    {
        m_d3d->Release();
        m_d3d = NULL;
    }
}

LPDIRECT3D9 D3DContext::getDirect3D() const
{
    return m_d3d;
}

D3DDevice* D3DContext::createDevice(HWND hwnd, size_t width, size_t height)
{
    D3DDevice* ret = NULL;

    ret = new D3DDevice(hwnd, getDirect3D(), width, height, false);
    if(ret->getDevice())
    {
        return ret;
    }
    else
    {
        /* problem to create Direct3D device */
        delete ret;
        return NULL;
    }
}

