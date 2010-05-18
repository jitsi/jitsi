/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file video_format.h
 * \brief Useful structures and enumerations for video format.
 * \author Sebastien Vincent
 */

#ifndef VIDEO_FORMAT_H
#define VIDEO_FORMAT_H

/**
 * \struct ColorSpace
 * \brief Color space (RGB, YUV,...).
 */
enum ColorSpace
{
	ARGB32 = 0, /**< ARGB color */
	RGB32, /**< RGB on 32-bit (alpha not used) */
	RGB24, /**< RGB on 24 bit */
	UNKNOWN, /**< Unknown color space */
	ANY = -1
};

/**
 * \struct VideoFormat
 * \brief Information about video format
 */
struct VideoFormat
{
	size_t width; /**< Video width */
	size_t height; /**< Video height */
    enum ColorSpace format; /**< Format */
};

#endif /* VIDEO_FORMAT_H */

