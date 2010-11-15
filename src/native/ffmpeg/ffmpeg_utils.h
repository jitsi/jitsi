/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/*
 * Various utilities for command line tools
 * copyright (c) 2003 Fabrice Bellard
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#ifndef FFMPEG_CMDUTILS_H
#define FFMPEG_CMDUTILS_H

#include <inttypes.h>
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavfilter/avfilter.h"
#include "libavfilter/avfiltergraph.h"

typedef struct {
    enum PixelFormat pix_fmt;
} FFSinkContext;

extern AVFilter ffsink;

struct AVInputStream;

typedef struct AVOutputStream {
    int file_index;          /* file index */
    int index;               /* stream index in the output file */
    int source_index;        /* AVInputStream index */
    AVStream *st;            /* stream in the output file */
    int encoding_needed;     /* true if encoding needed for this stream */
    int frame_number;
    /* input pts and corresponding output pts
       for A/V sync */
    //double sync_ipts;        /* dts from the AVPacket of the demuxer in second units */
    struct AVInputStream *sync_ist; /* input stream to sync against */
    int64_t sync_opts;       /* output frame counter, could be changed to some true timestamp */ //FIXME look at frame_number
    AVBitStreamFilterContext *bitstream_filters;
    /* video only */
    int video_resample;
    AVFrame pict_tmp;      /* temporary image for resampling */
//    struct SwsContext *img_resample_ctx; /* for image resampling */
//    int resample_height;
//    int resample_width;
//    int resample_pix_fmt;

    /* full frame size of first frame */
    int original_height;
    int original_width;

    /* forced key frames */
    int64_t *forced_kf_pts;
    int forced_kf_count;
    int forced_kf_index;
} AVOutputStream;

typedef struct AVInputStream {
    int file_index;
    int index;
    AVStream *st;
    int discard;             /* true if stream data should be discarded */
    int decoding_needed;     /* true if the packets must be decoded in 'raw_fifo' */
    int64_t sample_index;      /* current sample */

    int64_t       start;     /* time when read started */
    int64_t       next_pts;  /* synthetic pts for cases where pkt.pts
                                is not defined */
    int64_t       pts;       /* current pts */
    //PtsCorrectionContext pts_ctx;
    int is_start;            /* is 1 at the start and after a discontinuity */
    int showed_multi_packet_warning;
    int is_past_recording_time;

    AVFilterContext *output_video_filter;
    AVFilterContext *input_video_filter;
    AVFrame *filter_frame;
    int has_filter_frame;
    AVFilterBufferRef *picref;
} AVInputStream;

/**
 * Extract a frame from sink.
 *
 * @return a negative error in case of failure, 1 if one frame has
 * been extracted successfully.
 */
int get_filtered_video_frame(AVFilterContext *sink, AVFrame *frame,
                             AVFilterBufferRef **picref, AVRational *pts_tb);

int configure_filters(AVInputStream *ist, int pix_fmt, int width, int height,
    AVFilterGraph *graph, const char* vfilters);

#endif /* FFMPEG_CMDUTILS_H */
