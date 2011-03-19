/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "JAWTRenderer.h"

#include <jawt_md.h>
#include <stdlib.h>
#include <string.h>

#import <AppKit/NSView.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>
#import <OpenGL/gl.h>
#import <OpenGL/OpenGL.h>
#import <QuartzCore/CALayer.h>
#import <QuartzCore/CAOpenGLLayer.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_RECTANGLE_EXT
#define JAWT_RENDERER_TEXTURE_FORMAT GL_BGRA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE

@class JAWTRenderer;
@class JAWTRendererCALayer;

@interface JAWTRenderer : NSObject
{
@public
    /**
     * The <tt>CAOpenGLLayer</tt> in which this <tt>JAWTRenderer</tt> paints.
     */
    JAWTRendererCALayer *layer;

    /**
     * The OpenGL context of this <tt>JAWTRenderer</tt> which shares
     * <tt>texture</tt> with the OpenGL context of <tt>layer</tt> and enables
     * this <tt>JAWTRederer</tt> to not directly access the OpenGL context of
     * <tt>layer</tt> because it cannot guarantee synchronized access to it
     * anyway.
     */
    CGLContextObj glContext;

    jint height;
    GLuint texture;
    jint width;

    CGFloat frameHeight;
    CGFloat frameWidth;
    CGFloat frameX;
    CGFloat frameY;

    CGFloat boundsHeight;
    CGFloat boundsWidth;
    CGFloat boundsX;
    CGFloat boundsY;

    NSView *view;
}

- (void)boundsDidChange:(NSNotification *)notification;
- (void)copyCGLContext:(CGLContextObj)glContext
        forPixelFormat:(CGLPixelFormatObj)pixelFormat;
- (void)dealloc;
- (void)frameDidChange:(NSNotification *)notification;
- (id)init;
- (void)reshape;
- (void)setLayer:(JAWTRendererCALayer *)aLayer;
- (void)setView:(NSView *)aView;
- (void)update;
@end /* JAWTRenderer */

@interface JAWTRendererCALayer : CAOpenGLLayer
{
@public
    JAWTRenderer *renderer;
}

- (void)dealloc;
- (id)init;
- (void)setRenderer:(JAWTRenderer *)aRenderer;
@end /* JAWTRendererCALayer */

void
JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    [renderer release];

    [autoreleasePool release];
}

jlong
JAWTRenderer_open(JNIEnv *jniEnv, jclass clazz, jobject component)
{
    NSAutoreleasePool *autoreleasePool;
    JAWTRenderer *renderer;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    renderer = [[JAWTRenderer alloc] init];

    [autoreleasePool release];
    return (jlong) renderer;
}

jboolean
JAWTRenderer_paint
    (JAWT_DrawingSurfaceInfo *dsi, jclass clazz, jlong handle, jobject g)
{
    NSView *component;

    component
        = ((JAWT_MacOSXDrawingSurfaceInfo *) (dsi->platformInfo))->cocoaViewRef;
    if (component)
    {
        JAWTRenderer *renderer;
        NSAutoreleasePool *autoreleasePool;
        CALayer *componentLayer;

        renderer = (JAWTRenderer *) handle;
        autoreleasePool = [[NSAutoreleasePool alloc] init];

        if (renderer->view != component)
                [renderer setView:component];
        else
        {
            // update
            NSRect frame;

            frame = [component frame];
            if ((renderer->frameX != frame.origin.x)
                    || (renderer->frameY != frame.origin.y)
                    || (renderer->frameWidth != frame.size.width)
                    || (renderer->frameHeight != frame.size.height))
                [renderer update];
            else
            {
                // reshape
                NSRect bounds;

                bounds = [component bounds];
                if ((renderer->boundsX != bounds.origin.x)
                        || (renderer->boundsY != bounds.origin.y)
                        || (renderer->boundsWidth != bounds.size.width)
                        || (renderer->boundsHeight != bounds.size.height))
                    [renderer reshape];
            }
        }

        /* Make the component and its layer paint. */
        componentLayer = [component layer];
        if (componentLayer)
            [componentLayer setNeedsDisplay];
        [component displayIfNeededIgnoringOpacity];

        [autoreleasePool release];
    }
    return JNI_TRUE;
}

jboolean
JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    if (data && length)
    {
        @synchronized (renderer)
        {
            if (renderer->glContext)
            {
                CGLSetCurrentContext(renderer->glContext);

                if (renderer->texture
                        && ((width != renderer->width)
                                || (height != renderer->height)))
                {
                    glDeleteTextures(1, &(renderer->texture));
                    renderer->texture = 0;
                }
                if (renderer->texture)
                {
                    glBindTexture(JAWT_RENDERER_TEXTURE, renderer->texture);
                    glTexSubImage2D(
                        JAWT_RENDERER_TEXTURE,
                        0,
                        0, 0, width, height,
                        JAWT_RENDERER_TEXTURE_FORMAT,
                        JAWT_RENDERER_TEXTURE_TYPE,
                        data);
                }
                else
                {
                    glGenTextures(1, &(renderer->texture));
                    glBindTexture(JAWT_RENDERER_TEXTURE, renderer->texture);
                    glTexParameterf(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_PRIORITY,
                        1.0);
                    glTexParameteri(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_WRAP_S,
                        GL_CLAMP_TO_EDGE);
                    glTexParameteri(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_WRAP_T,
                        GL_CLAMP_TO_EDGE);
                    glTexParameteri(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_MAG_FILTER,
                        GL_LINEAR);
                    glTexParameteri(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_MIN_FILTER,
                        GL_LINEAR);

                    glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

                    glTexParameteri(
                        JAWT_RENDERER_TEXTURE,
                        GL_TEXTURE_STORAGE_HINT_APPLE,
                        GL_STORAGE_SHARED_APPLE);

                    glTexImage2D(
                        JAWT_RENDERER_TEXTURE,
                        0,
                        4,
                        width, height,
                        0,
                        JAWT_RENDERER_TEXTURE_FORMAT,
                        JAWT_RENDERER_TEXTURE_TYPE,
                        data);
                }
                renderer->width = width;
                renderer->height = height;
            }
        }
    }

    [autoreleasePool release];
    return JNI_TRUE;
}

@implementation JAWTRenderer
- (void)boundsDidChange:(NSNotification *)notification
{
    if ([notification object] == view)
        [self reshape];
}

- (void)copyCGLContext:(CGLContextObj)glContext
        forPixelFormat:(CGLPixelFormatObj)pixelFormat
{
    @synchronized (self)
    {
        if (self->glContext)
        {
            CGLSetCurrentContext(self->glContext);
            if (texture)
            {
                glDeleteTextures(1, &texture);
                texture = 0;
            }
            CGLSetCurrentContext(0);

            CGLReleaseContext(self->glContext);
            self->glContext = 0;
        }

        if (glContext)
            CGLCreateContext(pixelFormat, glContext, &(self->glContext));
    }
}

- (void)dealloc
{
    [self setView:nil];
    [self setLayer:nil];
    [self copyCGLContext:0 forPixelFormat:0];

    [super dealloc];
}

- (void)frameDidChange:(NSNotification *)notification
{
    if ([notification object] == view)
        [self update];
}

- (id)init
{
    if ((self = [super init]))
    {
        layer = nil;
        glContext = 0;

        height = 0;
        texture = 0;
        width = 0;

        frameHeight = 0;
        frameWidth = 0;
        frameX = 0;
        frameY = 0;

        boundsHeight = 0;
        boundsWidth = 0;
        boundsX = 0;
        boundsY = 0;

        view = nil;
    }
    return self;
}

- (void)reshape
{
    NSRect bounds;

    bounds = [view bounds];
    boundsHeight = bounds.size.height;
    boundsWidth = bounds.size.width;
    boundsX = bounds.origin.x;
    boundsY = bounds.origin.y;
    if ((bounds.size.width > 0) && (bounds.size.height > 0))
    {
//        [glContext makeCurrentContext];
//        glViewport(
//            bounds.origin.x, bounds.origin.y,
//            bounds.size.width, bounds.size.height);
    }
}

- (void)setLayer:(JAWTRendererCALayer *)aLayer
{
    if (layer != aLayer)
    {
        if (layer)
        {
            if (layer->renderer == self)
                [layer setRenderer:nil];

            [layer release];
        }

        layer = aLayer;

        if (layer)
        {
            [layer retain];

            [layer setRenderer:self];

            /*
             * Set the zPosition of the rendererLayer in accord with the
             * z-order of the view. It should not make a difference
             * because there is one JAWTRendererCALayer per JAWTRenderer
             * NSView but anyway.
             */
            if (view)
            {
                NSView *superview = [view superview];

                if (superview)
                {
                    NSUInteger zPosition
                        = [[superview subviews] indexOfObject:view];
                
                    if (NSNotFound != zPosition)
                        [layer setZPosition:zPosition];
                }
            }
        }
    }
}

- (void)setView:(NSView *)aView
{
    if (view != aView)
    {
        if (view)
        {
            CALayer *viewLayer;

#ifdef JAWT_RENDERER_USE_NSNOTIFICATIONCENTER
            NSNotificationCenter *notificationCenter;

            notificationCenter = [NSNotificationCenter defaultCenter];
            if (notificationCenter)
            {
                [notificationCenter
                    removeObserver:self
                    name:NSViewBoundsDidChangeNotification
                    object:view];
                [notificationCenter
                    removeObserver:self
                    name:NSViewFrameDidChangeNotification
                    object:view];
            }
#endif /* JAWT_RENDERER_USE_NSNOTIFICATIONCENTER */

            viewLayer = [view layer];
            if (self->layer == viewLayer)
                [self setLayer:nil];

            [view release];
        }

        view = aView;

        if (view)
        {
            CALayer *viewLayer;
#ifdef JAWT_RENDERER_USE_NSNOTIFICATIONCENTER
            NSNotificationCenter *notificationCenter;
#endif /* JAWT_RENDERER_USE_NSNOTIFICATIONCENTER */

            [view retain];

            /* Host a JAWTRendererCALayer in the view. */
            viewLayer = [view layer];
            if ((viewLayer
                    && [viewLayer isKindOfClass:[JAWTRendererCALayer class]]))
                [self setLayer:(JAWTRendererCALayer *) viewLayer];
            else
            {
                JAWTRendererCALayer *rendererLayer = [JAWTRendererCALayer layer];

                [self setLayer:rendererLayer];
                if (rendererLayer)
                {
                    [view setLayer:rendererLayer];
                    [view setWantsLayer:YES];

                    if ([view layer] != rendererLayer)
                        [self setLayer:nil];
                }
            }

#ifdef JAWT_RENDERER_USE_NSNOTIFICATIONCENTER
            notificationCenter = [NSNotificationCenter defaultCenter];
            if (notificationCenter)
            {
                [view setPostsBoundsChangedNotifications:YES];
                [notificationCenter
                    addObserver:self
                    selector:@selector(boundsDidChange:)
                    name:NSViewBoundsDidChangeNotification
                    object:view];
                [view setPostsFrameChangedNotifications:YES];
                [notificationCenter
                    addObserver:self
                    selector:@selector(frameDidChange:)
                    name:NSViewFrameDidChangeNotification
                    object:view];
            }
#endif /* JAWT_RENDERER_USE_NSNOTIFICATIONCENTER */

            [self update];
        }
    }
}

- (void)update
{
    NSRect frame;

    frame = [view frame];
    frameHeight = frame.size.height;
    frameWidth = frame.size.width;
    frameX = frame.origin.x;
    frameY = frame.origin.y;
    //[glContext update];

    [self reshape];
}
@end /* JAWTRenderer */

@implementation JAWTRendererCALayer
- (CGLContextObj)copyCGLContextForPixelFormat:(CGLPixelFormatObj)pixelFormat
{
    CGLContextObj glContext = [super copyCGLContextForPixelFormat:pixelFormat];

    if (glContext)
    {
        GLint surfaceOpacity;
        GLclampf color;

        surfaceOpacity = 1;
        CGLSetParameter(glContext, kCGLCPSurfaceOpacity, &surfaceOpacity);

        CGLSetCurrentContext(glContext);

        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDisable(GL_CULL_FACE);

        color = 0.0f;
        glClearColor(color, color, color, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    if (renderer)
        [renderer copyCGLContext:glContext forPixelFormat:pixelFormat];

    return glContext;
}

- (void)dealloc
{
    [self setRenderer:nil];

    [super dealloc];
}

- (void)drawInCGLContext:(CGLContextObj)glContext
             pixelFormat:(CGLPixelFormatObj)pixelFormat
            forLayerTime:(CFTimeInterval)timeInterval
             displayTime:(const CVTimeStamp *)timeStamp
{
    CGLSetCurrentContext(glContext);

    glClear(GL_COLOR_BUFFER_BIT);

    if (renderer)
    {
        @synchronized (renderer)
        {
            if (renderer->texture)
            {
                /*
                 * It may be a misunderstanding of OpenGL context sharing but
                 * JAWT_RENDERER_TEXTURE does not seem to work in glContext
                 * unless it is explicitly bound to renderer->texture while
                 * glContext is current.
                 */
                glBindTexture(JAWT_RENDERER_TEXTURE, renderer->texture);

                glEnable(JAWT_RENDERER_TEXTURE);
                glBegin(GL_QUADS);
                glTexCoord2f(0, 0);
                glVertex2f(-1.0, 1.0);
                glTexCoord2f(renderer->width, 0);
                glVertex2f(1.0, 1.0);
                glTexCoord2f(renderer->width, renderer->height);
                glVertex2f(1.0, -1.0);
                glTexCoord2f(0, renderer->height);
                glVertex2f(-1.0, -1.0);
                glEnd();
                glDisable(JAWT_RENDERER_TEXTURE);
            }
        }
    }

    [super drawInCGLContext:glContext
                pixelFormat:pixelFormat
               forLayerTime:timeInterval
                displayTime:timeStamp];
}

- (id)init
{
    if ((self = [super init]))
    {
        renderer = nil;

        /* AWT will be driving the painting. */
        [self setAsynchronous:YES];
    }
    return self;
}

- (void)releaseCGLContext:(CGLContextObj)glContext
{
    if (renderer)
        [renderer copyCGLContext:0 forPixelFormat:0];

    [super releaseCGLContext:glContext];
}

- (void)setRenderer:(JAWTRenderer *)aRenderer
{
    if (renderer != aRenderer)
    {
        if (renderer)
            [renderer release];

        renderer = aRenderer;

        if (renderer)
            [renderer retain];
    }
}
@end /* JAWTRendererCALayer */
