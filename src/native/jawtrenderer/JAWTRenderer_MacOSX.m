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

#import <AppKit/NSOpenGL.h>
#import <AppKit/NSView.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>
#import <OpenGL/gl.h>
#import <OpenGL/OpenGL.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_RECTANGLE_EXT
#define JAWT_RENDERER_TEXTURE_FORMAT GL_BGRA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE

@interface JAWTRenderer : NSObject
{
@public
    /**
     * The OpenGL context of this <tt>JAWTRenderer</tt> which shares
     * <tt>texture</tt> with the OpenGL contexts of <tt>subrenderers</tt> and
     * enables this <tt>JAWTRederer</tt> to not directly access the OpenGL
     * contexts of <tt>subrenderers</tt> because it cannot guarantee
     * synchronized access to them anyway.
     */
    NSOpenGLContext *glContext;

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

    /**
     * The <tt>JAWTRenderer</tt>s which are to have their <tt>layer</tt>s
     * contained in the <tt>layer</tt> of this <tt>JAWTRenderer</tt>.
     */
    NSMutableArray *subrenderers;
    /**
     * The <tt>JAWTRenderer</tt> which has this <tt>JAWTRenderer</tt> in its
     * <tt>subrenderers</tt>.
     */
    JAWTRenderer *superrenderer;
}

- (void)addSubrenderer:(JAWTRenderer *)subrenderer;
- (void)boundsDidChange:(NSNotification *)notification;
- (void)copyCGLContext:(NSOpenGLContext *)glContext
        forPixelFormat:(CGLPixelFormatObj)pixelFormat;
- (void)dealloc;
- (void)frameDidChange:(NSNotification *)notification;
- (id)init;
- (void)paint;
- (void)removeFromSuperrenderer;
- (void)removeSubrenderer:(JAWTRenderer *)subrenderer;
- (void)removeSubrendererAtIndex:(NSUInteger)index;
- (void)reshape;
- (void)setSuperrenderer:(JAWTRenderer *)aSuperrenderer;
- (void)setView:(NSView *)aView;
- (void)update;
@end /* JAWTRenderer */

void
JAWTRenderer_addNotifyLightweightComponent
    (jlong handle, jobject component, jlong parentHandle)
{
    JAWTRenderer *renderer;
    JAWTRenderer *parentRenderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    parentRenderer = (JAWTRenderer *) parentHandle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    if (parentRenderer)
        [parentRenderer addSubrenderer:renderer];
    [renderer copyCGLContext:nil forPixelFormat:0];

    [autoreleasePool release];
}

void
JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    JAWTRenderer_removeNotifyLightweightComponent(handle, component);
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

        [renderer paint];

        [autoreleasePool release];
    }
    return JNI_TRUE;
}

jboolean
JAWTRenderer_paintLightweightComponent
    (jlong handle, jobject component, jobject g)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    // TODO Auto-generated method stub

    [autoreleasePool release];
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
                [renderer->glContext makeCurrentContext];

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

void
JAWTRenderer_processLightweightComponentEvent
    (jlong handle, jint x, jint y, jint width, jint height)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    @synchronized (renderer)
    {
        renderer->boundsX = x;
        renderer->boundsY = y;
        renderer->boundsWidth = width;
        renderer->boundsHeight = height;
    }

    [autoreleasePool release];
}

void
JAWTRenderer_removeNotifyLightweightComponent(jlong handle, jobject component)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    [renderer removeFromSuperrenderer];

    [autoreleasePool release];
}

@implementation JAWTRenderer
- (void)addSubrenderer:(JAWTRenderer *)subrenderer
{
    @synchronized (self)
    {
        if (!subrenderers)
            subrenderers = [[NSMutableArray arrayWithCapacity:1] retain];
        if (NSNotFound == [subrenderers indexOfObject:subrenderer])
        {
            [subrenderers addObject:subrenderer];
            [subrenderer retain];
            [subrenderer setSuperrenderer:self];
        }
    }
}

- (void)boundsDidChange:(NSNotification *)notification
{
    if ([notification object] == view)
        [self reshape];
}

- (void)copyCGLContext:(NSOpenGLContext *)glContext
        forPixelFormat:(CGLPixelFormatObj)pixelFormat
{
    @synchronized (self)
    {
        if (self->glContext)
        {
            [self->glContext makeCurrentContext];
            if (texture)
            {
                glDeleteTextures(1, &texture);
                texture = 0;
            }
            [NSOpenGLContext clearCurrentContext];

            [self->glContext release];
            self->glContext = nil;
        }

        if (glContext)
        {
            NSOpenGLPixelFormat *format = [NSOpenGLPixelFormat alloc];

            /*
             * Unfortunately, initWithCGLPixelFormatObj: is available starting
             * with Mac OS X 10.6.
             */
            if ([format
                    respondsToSelector:@selector(initWithCGLPixelFormatObj:)])
            {
                format = [format initWithCGLPixelFormatObj:pixelFormat];
            }
            else
            {
                NSOpenGLPixelFormatAttribute pixelFormatAttribs[]
                    = { NSOpenGLPFAWindow, 0 };

                format = [format initWithAttributes:pixelFormatAttribs];
            }

            self->glContext
                = [[NSOpenGLContext alloc]
                        initWithFormat:format
                          shareContext:glContext];
            [format release];
        }
    }
}

- (void)dealloc
{
    /* subrenderers */
    @synchronized (self)
    {
        if (subrenderers)
        {
            NSUInteger subrendererCount = [subrenderers count];

            while (subrendererCount > 0)
            {
                --subrendererCount;
                [self removeSubrendererAtIndex:subrendererCount];
            }
            [subrenderers release];
            subrenderers = nil;
        }
    }

    [self setView:nil];

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

        glContext = nil;

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

        subrenderers = nil;
        superrenderer = nil;
    }
    return self;
}

- (void)paint
{
    if (!glContext)
        return;

    [glContext makeCurrentContext];

    // drawRect:
    glClear(GL_COLOR_BUFFER_BIT);

    @synchronized (self)
    {
        if (texture)
        {
            /*
             * It may be a misunderstanding of OpenGL context sharing but
             * JAWT_RENDERER_TEXTURE does not seem to work in glContext unless
             * it is explicitly bound to texture while glContext is current.
             */
            glBindTexture(JAWT_RENDERER_TEXTURE, texture);
            glEnable(JAWT_RENDERER_TEXTURE);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);
            glVertex2f(-1.0, 1.0);
            glTexCoord2f(width, 0);
            glVertex2f(1.0, 1.0);
            glTexCoord2f(width, height);
            glVertex2f(1.0, -1.0);
            glTexCoord2f(0, height);
            glVertex2f(-1.0, -1.0);
            glEnd();
            glDisable(JAWT_RENDERER_TEXTURE);
        }
            
        /* Draw the subrenderers of this JAWTRenderer. */
        if (subrenderers)
        {
            NSUInteger subrendererIndex = 0;
            NSUInteger subrendererCount = [subrenderers count];
            CGLPixelFormatObj pixelFormat = 0;

            for (;
                    subrendererIndex < subrendererCount;
                    subrendererIndex++)
            {
                JAWTRenderer *subrenderer
                    = [subrenderers objectAtIndex:subrendererIndex];

                @synchronized (subrenderer)
                {
                    GLfloat subrendererBoundsHeight;
                    GLfloat subrendererBoundsWidth;

                    /*
                     * Make sure the subrenderer has an NSOpenGLContext
                     * which is compatible with glContext and shares its
                     * object state.
                     */
                    if (!(subrenderer->glContext))
                    {
                        if (!pixelFormat)
                            pixelFormat
                                = CGLGetPixelFormat([glContext CGLContextObj]);
                        [subrenderer copyCGLContext:glContext
                                     forPixelFormat:pixelFormat];
                    }

                    subrendererBoundsHeight = subrenderer->boundsHeight;
                    subrendererBoundsWidth = subrenderer->boundsWidth;
                    if (subrenderer->texture
                            && (subrendererBoundsHeight > 0)
                            && (subrendererBoundsWidth > 0))
                    {
                        GLfloat x_1
                            = -1.0 + 2 * subrenderer->boundsX / boundsWidth;
                        GLfloat y1
                            = 1 - 2 * subrenderer->boundsY / boundsHeight;
                        GLfloat x1
                            = x_1 + 2 * subrendererBoundsWidth / boundsWidth;
                        GLfloat y_1
                            = y1 - 2 * subrendererBoundsHeight / boundsHeight;

                        glBindTexture(
                            JAWT_RENDERER_TEXTURE,
                            subrenderer->texture);
                        glEnable(JAWT_RENDERER_TEXTURE);
                        glBegin(GL_QUADS);
                        glTexCoord2f(0, 0);
                        glVertex2f(x_1, y1);
                        glTexCoord2f(subrenderer->width, 0);
                        glVertex2f(x1, y1);
                        glTexCoord2f(subrenderer->width, subrenderer->height);
                        glVertex2f(x1, y_1);
                        glTexCoord2f(0, subrenderer->height);
                        glVertex2f(x_1, y_1);
                        glEnd();
                        glDisable(JAWT_RENDERER_TEXTURE);
                    }
                }
            }
        }
    }

    glFlush();
}

- (void)removeFromSuperrenderer
{
    if (superrenderer)
        [superrenderer removeSubrenderer:self];
    [self copyCGLContext:nil forPixelFormat:nil];
}

- (void)removeSubrenderer:(JAWTRenderer *)subrenderer
{
    @synchronized (self)
    {
        if (subrenderers)
        {
            NSUInteger index = [subrenderers indexOfObject:subrenderer];

            if (NSNotFound != index)
                [self removeSubrendererAtIndex:index];
        }
    }
}

- (void)removeSubrendererAtIndex:(NSUInteger)index
{
    @synchronized (self)
    {
        if (subrenderers)
        {
            JAWTRenderer *subrenderer = [subrenderers objectAtIndex:index];

            [subrenderers removeObjectAtIndex:index];
            [subrenderer setSuperrenderer:nil];
            [subrenderer release];
        }
    }
}

- (void)reshape
{
    NSRect bounds;

    bounds = [view bounds];
    boundsHeight = bounds.size.height;
    boundsWidth = bounds.size.width;
    boundsX = bounds.origin.x;
    boundsY = bounds.origin.y;
    if ((bounds.size.width > 0) && (bounds.size.height > 0) && glContext)
    {
        [glContext makeCurrentContext];

        glViewport(
            bounds.origin.x, bounds.origin.y,
            bounds.size.width, bounds.size.height);
    }
}

- (void)setSuperrenderer:(JAWTRenderer *)aSuperrenderer
{
    if (superrenderer != aSuperrenderer)
    {
        if (superrenderer)
            [superrenderer release];

        superrenderer = aSuperrenderer;

        if (superrenderer)
            [superrenderer retain];
    }
}

- (void)setView:(NSView *)aView
{
    if (view != aView)
    {
        if (view)
        {
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

            [self copyCGLContext:nil forPixelFormat:0];

            [view release];
        }

        view = aView;

        if (view)
        {
            NSOpenGLPixelFormatAttribute pixelFormatAttribs[]
                = { NSOpenGLPFAWindow, 0 };
            NSOpenGLPixelFormat *pixelFormat;

#ifdef JAWT_RENDERER_USE_NSNOTIFICATIONCENTER
            NSNotificationCenter *notificationCenter;
#endif /* JAWT_RENDERER_USE_NSNOTIFICATIONCENTER */

            [view retain];
            
            pixelFormat
                = [[NSOpenGLPixelFormat alloc]
                        initWithAttributes:pixelFormatAttribs];
            if (pixelFormat)
            {
                glContext
                    = [[NSOpenGLContext alloc] initWithFormat:pixelFormat
                                                 shareContext:nil];
                if (glContext)
                {
                    GLint surfaceOpacity;

                    // prepareOpenGL
                    [glContext makeCurrentContext];

                    surfaceOpacity = 1;
                    [glContext setValues:&surfaceOpacity
                            forParameter:NSOpenGLCPSurfaceOpacity];

                    glDisable(GL_BLEND);
                    glDisable(GL_DEPTH_TEST);
                    glDepthMask(GL_FALSE);
                    glDisable(GL_CULL_FACE);
                    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT);
                }
                [pixelFormat release];
            }

            if (glContext && ([glContext view] != view))
                [glContext setView:view];

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
    if (glContext)
        [glContext update];

    [self reshape];
}
@end /* JAWTRenderer */
