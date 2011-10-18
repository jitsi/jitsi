/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file d3d_context.h
 * \brief Direct3D context.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef D3D_CONTEXT_H
#define D3D_CONTEXT_H

#include <cstdlib>

#include <d3d9.h>
#include <d3dx9.h>

#include "d3d_device.h"

/**
 * \class D3DContext
 * \brief Direct3D context.
 */
class D3DContext
{
    public:
        /**
         * \briefCreate a Direct3D context.
         * \return Direct3D context pointer or NULL if failed
         */
        static D3DContext* createD3DContext();

        /**
         * \brief Constructor.
         */
        D3DContext();

        /**
         * \brief Destructor.
         */
        ~D3DContext();

        /**
         * \brief Get raw Direct3D context pointer.
         * \return Direct3D
         */
        LPDIRECT3D9 getDirect3D() const;

        /**
         * \brief Create Direct3D device.
         * \param hwnd handle of a window
         * \param width width of the device
         * \param height height of the device
         * \return Direct3D device
         */
        D3DDevice* createDevice(HWND hwnd, size_t width, size_t height);

    private:
        /**
         * \brief Direct3D context pointer.
         */
        LPDIRECT3D9 m_d3d;
};

#endif /* D3D_CONTEXT_H */

