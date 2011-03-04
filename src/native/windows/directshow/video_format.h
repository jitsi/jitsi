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
 * \struct VideoFormat
 * \brief Information about video format
 */
struct VideoFormat
{
    size_t width; /**< Video width */
    size_t height; /**< Video height */
    unsigned long pixelFormat; /**< Pixel format */
    GUID mediaType; /**< Media type */
};

#endif /* VIDEO_FORMAT_H */

