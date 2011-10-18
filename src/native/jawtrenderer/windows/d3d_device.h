/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file d3d_device.h
 * \brief Direct3D device.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef D3D_DEVICE_H
#define D3D_DEVICE_H

#include <cstdlib>

#include <d3d9.h>
#include <d3dx9.h>

#include "d3d_surface.h"

/**
 * \class D3DDevice
 * \brief Direct3D device.
 *
 * It is used to render contents on screen.
 */
class D3DDevice
{
    public:
        /**
         * \brief Constructor.
         * \param hwnd Handle of the Window
         * \param d3d raw Direct3D context
         * \param width width of the future device
         * \param height height of the future device
         * \param fullscreen create device for fullscreen mode
         */
        D3DDevice(HWND hwnd, LPDIRECT3D9 d3d, size_t width, size_t height, bool fullscreen);

        /**
         * \brief Destructor.
         */
        ~D3DDevice();

        /**
         * \brief Get raw Direct3D device pointer.
         * \return Direct3D device pointer
         */
        LPDIRECT3DDEVICE9 getDevice() const;

        /**
         * \brief Validate the device.
         * \return true if validation succeed, false otherwise
         */
        bool validate();

        /**
         * \brief Create a surface.
         * \param width width of the surface
         * \param height height of the surface
         * \return surface or NULL if problem
         */
        D3DSurface* createSurface(size_t width, size_t height);

        /**
         * \brief Render a surface on the screen.
         * \param surface surface to render on the screen
         */
        void render(D3DSurface* surface);

    private:
        /**
         * \brief Raw Direct3D device.
         */
        LPDIRECT3DDEVICE9 m_device;

        /**
         * \brief Settings of the device.
         */
        D3DPRESENT_PARAMETERS m_settings;

        /**
         * \brief Back surface.
         */
        LPDIRECT3DSURFACE9 m_backSurface;

        /**
         * \brief Width of the device.
         */
        size_t m_width;

        /**
         * \brief Height of the device.
         */
        size_t m_height;
};

#endif /* D3D_DEVICE_H */

