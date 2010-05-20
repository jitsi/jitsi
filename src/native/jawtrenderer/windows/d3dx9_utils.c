/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/*
 * Copyright (C) 2009 Tony Wasserka
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 */

/**
 * \file d3dx9_utils.c
 * \brief DirectX functions implementation coming from Wine project.
 * \author Wine project
 */

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <d3d9.h>
#include <d3dx9.h>

#ifdef __cplusplus
extern "C" { /* } */
#endif

/* Following structures and functions functions comes from the Wine project.
 * There are used to avoid linking to d3dx9_xx.dll since this DLL cannot be
 * redistributed directly (it must be installed using DirectXSetup).
 */

typedef enum _FormatType
{
    FORMAT_ARGB,   /* unsigned */
    FORMAT_UNKNOWN

} FormatType;

typedef struct _PixelFormatDesc 
{
    D3DFORMAT format;
    BYTE bits[4];
    BYTE shift[4];
    UINT bytes_per_pixel;
    FormatType type;
} PixelFormatDesc;

/************************************************************
 * pixel format table providing info about number of bytes per pixel,
 * number of bits per channel and format type.
 *
 * Call get_format_info to request information about a specific format.
 */
static const PixelFormatDesc formats[] =
{
   /* format                    bits per channel        shifts per channel   bpp   type        */
    { D3DFMT_R8G8B8,          {  0,   8,   8,   8 },  {  0,  16,   8,   0 },   3,  FORMAT_ARGB },
    { D3DFMT_A8R8G8B8,        {  8,   8,   8,   8 },  { 24,  16,   8,   0 },   4,  FORMAT_ARGB },
    { D3DFMT_X8R8G8B8,        {  0,   8,   8,   8 },  {  0,  16,   8,   0 },   4,  FORMAT_ARGB },
    { D3DFMT_A8B8G8R8,        {  8,   8,   8,   8 },  { 24,   0,   8,  16 },   4,  FORMAT_ARGB },
    { D3DFMT_X8B8G8R8,        {  0,   8,   8,   8 },  {  0,   0,   8,  16 },   4,  FORMAT_ARGB },
    { D3DFMT_R5G6B5,          {  0,   5,   6,   5 },  {  0,  11,   5,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_X1R5G5B5,        {  0,   5,   5,   5 },  {  0,  10,   5,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_A1R5G5B5,        {  1,   5,   5,   5 },  { 15,  10,   5,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_R3G3B2,          {  0,   3,   3,   2 },  {  0,   5,   2,   0 },   1,  FORMAT_ARGB },
    { D3DFMT_A8R3G3B2,        {  8,   3,   3,   2 },  {  8,   5,   2,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_A4R4G4B4,        {  4,   4,   4,   4 },  { 12,   8,   4,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_X4R4G4B4,        {  0,   4,   4,   4 },  {  0,   8,   4,   0 },   2,  FORMAT_ARGB },
    { D3DFMT_A2R10G10B10,     {  2,  10,  10,  10 },  { 30,  20,  10,   0 },   4,  FORMAT_ARGB },
    { D3DFMT_A2B10G10R10,     {  2,  10,  10,  10 },  { 30,   0,  10,  20 },   4,  FORMAT_ARGB },
    { D3DFMT_G16R16,          {  0,  16,  16,   0 },  {  0,   0,  16,   0 },   4,  FORMAT_ARGB },
    { D3DFMT_A8,              {  8,   0,   0,   0 },  {  0,   0,   0,   0 },   1,  FORMAT_ARGB },
    { D3DFMT_UNKNOWN,         {  0,   0,   0,   0 },  {  0,   0,   0,   0 },   0,  FORMAT_UNKNOWN }, /* marks last element */
};

/************************************************************
 * get_format_info
 *
 * Returns information about the specified format.
 * If the format is unsupported, it's filled with the D3DFMT_UNKNOWN desc.
 *
 * PARAMS
 *   format [I] format whose description is queried
 *   desc   [O] pointer to a StaticPixelFormatDesc structure
 *
 */
const PixelFormatDesc *get_format_info(D3DFORMAT format)
{
    unsigned int i = 0;
    while(formats[i].format != format && formats[i].format != D3DFMT_UNKNOWN) i++;
    return &formats[i];
}

/************************************************************
 * copy_simple_data
 *
 * Copies the source buffer to the destination buffer, performing
 * any necessary format conversion and color keying.
 * Works only for ARGB formats with 1 - 4 bytes per pixel.
 */
static void copy_simple_data(CONST BYTE *src,  UINT  srcpitch, POINT  srcsize, CONST PixelFormatDesc  *srcformat,
                             CONST BYTE *dest, UINT destpitch, POINT destsize, CONST PixelFormatDesc *destformat,
                             DWORD dwFilter)
{
    DWORD srcshift[4], destshift[4];
    DWORD srcmask[4], destmask[4];
    BOOL process_channel[4];
    DWORD channels[4];
    DWORD channelmask = 0;

    UINT minwidth, minheight;
    BYTE *srcptr, *destptr;
    UINT i, x, y;

    ZeroMemory(channels, sizeof(channels));
    ZeroMemory(process_channel, sizeof(process_channel));

    for(i = 0;i < 4;i++) {
        /* srcshift is used to extract the _relevant_ components */
        srcshift[i]  =  srcformat->shift[i] + max( srcformat->bits[i] - destformat->bits[i], 0);

        /* destshift is used to move the components to the correct position */
        destshift[i] = destformat->shift[i] + max(destformat->bits[i] -  srcformat->bits[i], 0);

        srcmask[i]  = ((1 <<  srcformat->bits[i]) - 1) <<  srcformat->shift[i];
        destmask[i] = ((1 << destformat->bits[i]) - 1) << destformat->shift[i];

        /* channelmask specifies bits which aren't used in the source format but in the destination one */
        if(destformat->bits[i]) {
            if(srcformat->bits[i]) process_channel[i] = TRUE;
            else channelmask |= destmask[i];
        }
    }

    minwidth  = (srcsize.x < destsize.x) ? srcsize.x : destsize.x;
    minheight = (srcsize.y < destsize.y) ? srcsize.y : destsize.y;

    for(y = 0;y < minheight;y++) {
        srcptr  = (BYTE*)( src + y *  srcpitch);
        destptr = (BYTE*)(dest + y * destpitch);
        for(x = 0;x < minwidth;x++) {
            /* extract source color components */
            if(srcformat->type == FORMAT_ARGB) {
                const DWORD col = *(DWORD*)srcptr;
                for(i = 0;i < 4;i++)
                    if(process_channel[i])
                        channels[i] = (col & srcmask[i]) >> srcshift[i];
            }

            /* recombine the components */
            if(destformat->type == FORMAT_ARGB) {
                DWORD* const pixel = (DWORD*)destptr;
                *pixel = 0;

                for(i = 0;i < 4;i++) {
                    if(process_channel[i]) {
                        /* necessary to make sure that e.g. an X4R4G4B4 white maps to an R8G8B8 white instead of 0xf0f0f0 */
                        signed int shift;
                        for(shift = destshift[i]; shift > destformat->shift[i]; shift -= srcformat->bits[i]) *pixel |= channels[i] << shift;
                        *pixel |= (channels[i] >> (destformat->shift[i] - shift)) << destformat->shift[i];
                    }
                }
                *pixel |= channelmask;   /* new channels are set to their maximal value */
            }
            srcptr  +=  srcformat->bytes_per_pixel;
            destptr += destformat->bytes_per_pixel;
        }
    }
}

/************************************************************
 * D3DXLoadSurfaceFromMemory
 *
 * Loads data from a given memory chunk into a surface,
 * applying any of the specified filters.
 *
 * PARAMS
 *   pDestSurface [I] pointer to the surface
 *   pDestPalette [I] palette to use
 *   pDestRect    [I] to be filled area of the surface
 *   pSrcMemory   [I] pointer to the source data
 *   SrcFormat    [I] format of the source pixel data
 *   SrcPitch     [I] number of bytes in a row
 *   pSrcPalette  [I] palette used in the source image
 *   pSrcRect     [I] area of the source data to load
 *   dwFilter     [I] filter to apply on stretching
 *   Colorkey     [I] colorkey
 *
 * RETURNS
 *   Success: D3D_OK, if we successfully load the pixel data into our surface or
 *                    if pSrcMemory is NULL but the other parameters are valid
 *   Failure: D3DERR_INVALIDCALL, if pDestSurface, SrcPitch or pSrcRect are NULL or
 *                                if SrcFormat is an invalid format (other than D3DFMT_UNKNOWN)
 *            D3DXERR_INVALIDDATA, if we fail to lock pDestSurface
 *            E_FAIL, if SrcFormat is D3DFMT_UNKNOWN or the dimensions of pSrcRect are invalid
 *
 * NOTES
 *   pSrcRect specifies the dimensions of the source data;
 *   negative values for pSrcRect are allowed as we're only looking at the width and height anyway.
 *
 */
HRESULT WINAPI D3DXLoadSurfaceFromMemory(LPDIRECT3DSURFACE9 pDestSurface,
                                         CONST PALETTEENTRY *pDestPalette,
                                         CONST RECT *pDestRect,
                                         LPCVOID pSrcMemory,
                                         D3DFORMAT SrcFormat,
                                         UINT SrcPitch,
                                         CONST PALETTEENTRY *pSrcPalette,
                                         CONST RECT *pSrcRect,
                                         DWORD dwFilter,
                                         D3DCOLOR Colorkey)
{
    CONST PixelFormatDesc *srcformatdesc, *destformatdesc;
    D3DSURFACE_DESC surfdesc;
    D3DLOCKED_RECT lockrect;
    POINT srcsize, destsize;
    HRESULT hr;

    //TRACE("(%p, %p, %p, %p, %x, %u, %p, %p %u, %#x)\n", pDestSurface, pDestPalette, pDestRect, pSrcMemory,
    //    SrcFormat, SrcPitch, pSrcPalette, pSrcRect, dwFilter, Colorkey);

    if( !pDestSurface || !pSrcMemory || !pSrcRect ) return D3DERR_INVALIDCALL;
    if(SrcFormat == D3DFMT_UNKNOWN || pSrcRect->left >= pSrcRect->right || pSrcRect->top >= pSrcRect->bottom) return E_FAIL;

    if(dwFilter != D3DX_FILTER_NONE) return E_NOTIMPL;

    IDirect3DSurface9_GetDesc(pDestSurface, &surfdesc);

    srcformatdesc = get_format_info(SrcFormat);
    destformatdesc = get_format_info(surfdesc.Format);
    if( srcformatdesc->type == FORMAT_UNKNOWN ||  srcformatdesc->bytes_per_pixel > 4) return E_NOTIMPL;
    if(destformatdesc->type == FORMAT_UNKNOWN || destformatdesc->bytes_per_pixel > 4) return E_NOTIMPL;

    srcsize.x = pSrcRect->right - pSrcRect->left;
    srcsize.y = pSrcRect->bottom - pSrcRect->top;
    if( !pDestRect ) {
        destsize.x = surfdesc.Width;
        destsize.y = surfdesc.Height;
    } else {
        destsize.x = pDestRect->right - pDestRect->left;
        destsize.y = pDestRect->bottom - pDestRect->top;
    }

    hr = IDirect3DSurface9_LockRect(pDestSurface, &lockrect, pDestRect, 0);
    if(FAILED(hr)) return D3DXERR_INVALIDDATA;

    copy_simple_data((CONST BYTE*)pSrcMemory, SrcPitch, srcsize, srcformatdesc,
                     (CONST BYTE*)lockrect.pBits, lockrect.Pitch, destsize, destformatdesc,
                     dwFilter);

    IDirect3DSurface9_UnlockRect(pDestSurface);
    return D3D_OK;
}

#ifdef __cplusplus
}
#endif

