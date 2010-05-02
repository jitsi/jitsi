/**
 * \file d3d_surface.cpp
 * \brief Direct3D surface.
 * \author Sebastien Vincent
 * \date 2010
 */

#include "d3d_surface.h"

D3DSurface::D3DSurface(LPDIRECT3DDEVICE9 device, size_t width, size_t height)
{
    HRESULT ret = device->CreateOffscreenPlainSurface(width, height,
            D3DFMT_X8R8G8B8, D3DPOOL_SYSTEMMEM, &m_surface, NULL);

    if(FAILED(ret))
    {
        m_surface = NULL;
        return;
    }

    m_width = width;
    m_height = height;
}

D3DSurface::~D3DSurface()
{
    if(m_surface)
    {
        m_surface->Release();
        m_surface = NULL;
    }
}

LPDIRECT3DSURFACE9 D3DSurface::getSurface() const
{
    return m_surface;
}

size_t D3DSurface::getWidth()
{
    return m_width;
}

size_t D3DSurface::getHeight()
{
    return m_height;
}

bool D3DSurface::loadData(char *data, size_t width, size_t height)
{
    HRESULT ret = 0;
    RECT rect;

    rect.left = 0;
    rect.top = 0;
    rect.right = width;
    rect.bottom = height;

    if(!m_surface)
    {
        return false;
    }

    /* load image from memory */
    ret = D3DXLoadSurfaceFromMemory(m_surface, NULL, NULL, data, 
            D3DFMT_A8R8G8B8, width * 4, NULL, &rect, D3DX_FILTER_NONE, NULL);
    return !FAILED(ret);
}

