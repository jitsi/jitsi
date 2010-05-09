#include "JAWTRenderer.h"

#include <jawt_md.h>
#include <stdlib.h>
#include <string.h>

#import <AppKit/NSOpenGL.h>
#import <AppKit/NSView.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>
#import <OpenGL/gl.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_RECTANGLE_EXT
#define JAWT_RENDERER_TEXTURE_FORMAT GL_BGRA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE

@interface JAWTRenderer : NSObject
{
@public
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
}

- (void)boundsDidChange:(NSNotification *)notification;
- (void)dealloc;
- (void)frameDidChange:(NSNotification *)notification;
- (id)init;
- (void)paint;
- (void)reshape;
- (void)setView:(NSView *)aView;
- (void)update;
@end /* JAWTRenderer */

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
JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;
    jboolean repaint;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    if (data && length)
    {
        [renderer->glContext makeCurrentContext];

        if (renderer->texture
                && ((width != renderer->width) || (height != renderer->height)))
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
            glTexParameterf(JAWT_RENDERER_TEXTURE, GL_TEXTURE_PRIORITY, 1.0);
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

        /*
         * The component needs repainting now. Upon return, a paint of the
         * component will be scheduled. But #paint and #process both want the
         * lock on the renderer and it may turn out that #process will manage to
         * execute once again without #paint being able to depict the current
         * frame. So try to paint now and don't schedule a paint for later.
         */
        if (renderer->view)
        {
            [renderer paint];
            repaint = JNI_FALSE;
        }
        else
            repaint = JNI_TRUE;
    }
    else
        repaint = JNI_TRUE;

    [autoreleasePool release];
    return repaint;
}

@implementation JAWTRenderer
- (void)boundsDidChange:(NSNotification *)notification
{
    if ([notification object] == view)
        [self reshape];
}

- (void)dealloc
{
    [self setView:nil];
    [glContext release];

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
        NSOpenGLPixelFormatAttribute pixelFormatAttribs[]
            = { NSOpenGLPFAWindow, 0 };
        NSOpenGLPixelFormat *pixelFormat;
        
        pixelFormat
            = [[NSOpenGLPixelFormat alloc]
                    initWithAttributes:pixelFormatAttribs];
        if (pixelFormat)
        {
            glContext
                = [[NSOpenGLContext alloc]
                        initWithFormat:pixelFormat
                        shareContext:nil];
            if (glContext)
            {
                GLint surfaceOpacity;

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

                // prepareOpenGL
                [glContext makeCurrentContext];

                surfaceOpacity = 1;
                [glContext
                     setValues:&surfaceOpacity
                     forParameter:NSOpenGLCPSurfaceOpacity];

                glDisable(GL_BLEND);
                glDisable(GL_DEPTH_TEST);
                glDepthMask(GL_FALSE);
                glDisable(GL_CULL_FACE);
                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
            }
            else
            {
                [self release];
                self = nil;
            }
            [pixelFormat release];
        }
        else
        {
            [self release];
            self = nil;
        }
    }
    return self;
}

- (void)paint
{
    if ([view lockFocusIfCanDraw])
    {
        [glContext makeCurrentContext];

        // drawRect:
        glClear(GL_COLOR_BUFFER_BIT);
        if (texture)
        {
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
        glFlush();

        [view unlockFocus];
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
    if ((bounds.size.width > 0) && (bounds.size.height > 0))
    {
        [glContext makeCurrentContext];

        glViewport(
            bounds.origin.x, bounds.origin.y,
            bounds.size.width, bounds.size.height);
    }
}

- (void)setView:(NSView *)aView
{
    if (view != aView)
    {
        if (view)
        {
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

            [view release];
        }

        view = aView;

        if (view)
        {
            NSNotificationCenter *notificationCenter;

            [view retain];

            if ([glContext view] != view)
                [glContext setView:view];

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
    [glContext update];

    [self reshape];
}
@end /* JAWTRenderer */
