#include "JAWTRenderer.h"

#include <jawt_md.h>
#include <stdlib.h>
#include <string.h>

#import <AppKit/NSOpenGL.h>
#import <Foundation/NSAutoreleasePool.h>
#import <OpenGL/gl.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_RECTANGLE_EXT
#define JAWT_RENDERER_TEXTURE_FORMAT GL_BGRA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE

typedef struct _JAWTRenderer
{
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
}
JAWTRenderer;

void
JAWTRenderer_close
    (JNIEnv *jniEnv, jclass clazz, jlong handle, jobject component)
{
    JAWTRenderer *renderer;
    NSAutoreleasePool *autoreleasePool;

    renderer = (JAWTRenderer *) handle;
    autoreleasePool = [[NSAutoreleasePool alloc] init];

    [renderer->glContext release];
    free(renderer);

    [autoreleasePool release];
}

jlong
JAWTRenderer_open(JNIEnv *jniEnv, jclass clazz, jobject component)
{
    NSAutoreleasePool *autoreleasePool;
    NSOpenGLPixelFormatAttribute pixelFormatAttribs[]
        = { NSOpenGLPFAWindow, 0 };
    NSOpenGLPixelFormat *pixelFormat;
    JAWTRenderer *renderer;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    pixelFormat
        = [[NSOpenGLPixelFormat alloc] initWithAttributes:pixelFormatAttribs];
    if (pixelFormat)
    {
        NSOpenGLContext *glContext;

        glContext
            = [[NSOpenGLContext alloc]
                    initWithFormat:pixelFormat
                    shareContext:nil];
        if (glContext)
        {
            renderer = malloc(sizeof(JAWTRenderer));
            if (renderer)
            {
                GLint surfaceOpacity;

                renderer->glContext = glContext;
                renderer->height = 0;
                renderer->texture = 0;
                renderer->width = 0;

                renderer->frameHeight = 0;
                renderer->frameWidth = 0;
                renderer->frameX = 0;
                renderer->frameY = 0;

                renderer->boundsHeight = 0;
                renderer->boundsWidth = 0;
                renderer->boundsX = 0;
                renderer->boundsY = 0;

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
        }
        else
            renderer = NULL;
        [pixelFormat release];
    }
    else
        renderer = NULL;

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
        NSAutoreleasePool *autoreleasePool;

        autoreleasePool = [[NSAutoreleasePool alloc] init];

        if ([component lockFocusIfCanDraw])
        {
            JAWTRenderer *renderer;
            NSRect frame;
            NSRect bounds;
            
            renderer = (JAWTRenderer *) handle;

            [renderer->glContext makeCurrentContext];
            if ([renderer->glContext view] != component)
                [renderer->glContext setView:component];

            // update
            frame = [component frame];
            if ((renderer->frameX != frame.origin.x)
                    || (renderer->frameY != frame.origin.y)
                    || (renderer->frameWidth != frame.size.width)
                    || (renderer->frameHeight != frame.size.height))
            {
                renderer->frameHeight = frame.size.height;
                renderer->frameWidth = frame.size.width;
                renderer->frameX = frame.origin.x;
                renderer->frameY = frame.origin.y;
                [renderer->glContext update];
            }

            // reshape
            bounds = [component bounds];
            if ((renderer->boundsX != bounds.origin.x)
                    || (renderer->boundsY != bounds.origin.y)
                    || (renderer->boundsWidth != bounds.size.width)
                    || (renderer->boundsHeight != bounds.size.height))
            {
                renderer->boundsHeight = bounds.size.height;
                renderer->boundsWidth = bounds.size.width;
                renderer->boundsX = bounds.origin.x;
                renderer->boundsY = bounds.origin.y;
                if ((bounds.size.width > 0) && (bounds.size.height > 0))
                {
                    glViewport(
                        bounds.origin.x, bounds.origin.y,
                        bounds.size.width, bounds.size.height);
                }
            }

            // drawRect:
            glClear(GL_COLOR_BUFFER_BIT);
            if (renderer->texture)
            {
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
            glFlush();

            [component unlockFocus];
        }

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

    [renderer->glContext makeCurrentContext];

    if (data && length)
    {
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
    }

    [autoreleasePool release];
    return JNI_TRUE;
}
