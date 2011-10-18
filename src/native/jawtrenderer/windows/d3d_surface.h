/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file d3d_surface.h
 * \brief Direct3D surface.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef D3D_SURFACE_H
#define D3D_SURFACE_H

#include <cstdlib>

#include <d3d9.h>
#include <d3dx9.h>

class D3DDevice;

/**
 * \class D3DSurface
 * \brief Direct3D surface.
 */
class D3DSurface
{
    public:
        /**
         * \brief Constructor.
         * \param device device that will create surface
         * \param width width of the surface
         * \param height height of the surface
         */
        D3DSurface(LPDIRECT3DDEVICE9 device, size_t width, size_t height);

        /**
         * \brief Destructor.
         */
        ~D3DSurface();

        /**
         * \brief Get raw pointer of Direct3D surface.
         * \return Direct3D surface.
         */
        LPDIRECT3DSURFACE9 getSurface() const;

        /**
         * \brief Load data into surface.
         * \param data array of bytes
         * \param width width of image
         * \param height height of image
         * \return true if data is loaded, false otherwise
         */
        bool loadData(char* data, size_t width, size_t height);

        /**
         * \brief Get surface width.
         * \return surface width
         */
        size_t getWidth();

        /**
         * \brief Get surface height.
         * \return surface height
         */
        size_t getHeight();

    private:
        /**
         * \brief Raw Direct3D surface.
         */
        LPDIRECT3DSURFACE9 m_surface;

        /**
         * \brief Width of surface.
         */
        size_t m_width;

        /**
         * \brief Height of surface.
         */
        size_t m_height;
};

#endif /* D3D_SURFACE_H */

