/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/*
 * Various utilities for command line tools
 * Copyright (c) 2000-2003 Fabrice Bellard
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

#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <math.h>

/* Include only the enabled headers since some compilers (namely, Sun
   Studio) will not omit unused inline functions and create undefined
   references to libraries that are not being built. */

#include "libavcodec/opt.h"
#include "libavformat/avformat.h"
#include "libavutil/pixdesc.h"
#include "libavfilter/avfilter.h"
#include "libavfilter/graphparser.h"
#include "libavfilter/vsrc_buffer.h"
#include "libswscale/swscale.h"

#include "ffmpeg_utils.h"

static int ffsink_init(AVFilterContext *ctx, const char *args, void *opaque)
{
    FFSinkContext *priv = ctx->priv;

    if (!opaque)
        return AVERROR(EINVAL);
    *priv = *(FFSinkContext *)opaque;

    return 0;
}

static void null_end_frame(AVFilterLink *inlink) { }

static int ffsink_query_formats(AVFilterContext *ctx)
{
    FFSinkContext *priv = ctx->priv;
    enum PixelFormat pix_fmts[] = { priv->pix_fmt, PIX_FMT_NONE };

    avfilter_set_common_formats(ctx, avfilter_make_format_list(pix_fmts));
    return 0;
}

AVFilter ffsink = {
    .name      = "ffsink",
    .priv_size = sizeof(FFSinkContext),
    .init      = ffsink_init,

    .query_formats = ffsink_query_formats,

    .inputs    = (AVFilterPad[]) {{ .name          = "default",
                                    .type          = AVMEDIA_TYPE_VIDEO,
                                    .end_frame     = null_end_frame,
                                    .min_perms     = AV_PERM_READ, },
                                  { .name = NULL }},
    .outputs   = (AVFilterPad[]) {{ .name = NULL }},
};

int get_filtered_video_frame(AVFilterContext *ctx, AVFrame *frame,
                             struct AVFilterBufferRef **picref_ptr, AVRational *tb)
{
    int ret;
    AVFilterBufferRef *picref;

    if ((ret = avfilter_request_frame(ctx->inputs[0])) < 0)
    {
        return ret;
    }
    if (!(picref = ctx->inputs[0]->cur_buf))
    {
        return AVERROR(ENOENT);
    }

    *picref_ptr = picref;
    ctx->inputs[0]->cur_buf = NULL;
    *tb = ctx->inputs[0]->time_base;

    memcpy(frame->data,     picref->data,     sizeof(frame->data));
    memcpy(frame->linesize, picref->linesize, sizeof(frame->linesize));
    frame->interlaced_frame = picref->video->interlaced;
    frame->top_field_first  = picref->video->top_field_first;
    
    return 1;
}

int configure_filters(AVInputStream *ist, int pix_fmt, int w, int h,
    AVFilterGraph *graph, const char* vfilters)
{
    AVFilterContext *last_filter;
    /** filter graph containing all filters including input & output */
    //AVCodecContext *codec = ost->st->codec;
    //AVCodecContext *icodec = ist->st->codec;
    FFSinkContext ffsink_ctx = { .pix_fmt = pix_fmt };
    char args[255];
    int ret;
    int width = w;
    int height = h;

    //graph = av_mallocz(sizeof(AVFilterGraph));

    if ((ret = avfilter_open(&ist->input_video_filter, avfilter_get_by_name("buffer"), "src")) < 0)
    {
        return ret;
    }
    if ((ret = avfilter_open(&ist->output_video_filter, &ffsink, "out")) < 0)
    {
        return ret;
    }

    snprintf(args, 255, "%d:%d:%d:%d:%d", width,
             height, pix_fmt, 1, AV_TIME_BASE);

    if ((ret = avfilter_init_filter(ist->input_video_filter, args, NULL)) < 0)
    {
        return ret;
    }
    if ((ret = avfilter_init_filter(ist->output_video_filter, NULL, &ffsink_ctx)) < 0)
    {
        return ret;
    }

    /* add input and output filters to the overall graph */
    avfilter_graph_add_filter(graph, ist->input_video_filter);
    avfilter_graph_add_filter(graph, ist->output_video_filter);

    last_filter = ist->input_video_filter;

/*
    if (codec->width  != icodec->width || codec->height != icodec->height) {
        AVFilterContext *filter;
        snprintf(args, 255, "%d:%d:flags=0x%X",
                 codec->width,
                 codec->height,
                 (int)av_get_int(sws_opts, "sws_flags", NULL));
        if ((ret = avfilter_open(&filter, avfilter_get_by_name("scale"), NULL)) < 0)
            return ret;
        if ((ret = avfilter_init_filter(filter, args, NULL)) < 0)
            return ret;
        if ((ret = avfilter_link(last_filter, 0, filter, 0)) < 0)
            return ret;
        last_filter = filter;
        avfilter_graph_add_filter(graph, last_filter);
    }
*/
    //snprintf(args, sizeof(args), "flags=0x%X", (int)av_get_int(sws_opts, "sws_flags", NULL));
    snprintf(args, sizeof(args), "flags=0x%X", SWS_BICUBIC);
    graph->scale_sws_opts = av_strdup(args);


    if (vfilters) {
        AVFilterInOut *outputs = av_malloc(sizeof(AVFilterInOut));
        AVFilterInOut *inputs  = av_malloc(sizeof(AVFilterInOut));

        outputs->name    = av_strdup("in");
        outputs->filter  = last_filter;
        outputs->pad_idx = 0;
        outputs->next    = NULL;

        inputs->name    = av_strdup("out");
        inputs->filter  = ist->output_video_filter;
        inputs->pad_idx = 0;
        inputs->next    = NULL;

        if ((ret = avfilter_graph_parse(graph, vfilters, inputs, outputs, NULL)) < 0)
        {
            return ret;
        }

        //av_freep(&vfilters);
    } else {
        if ((ret = avfilter_link(last_filter, 0, ist->output_video_filter, 0)) < 0)
        {
            return ret;
        }
    }
    if ((ret = avfilter_graph_config(graph, NULL)) < 0)
    {
        return ret;
    }
/*
    codec->width  = ist->output_video_filter->inputs[0]->w;
    codec->height = ist->output_video_filter->inputs[0]->h;
*/
    return 0;
}

