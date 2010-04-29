#include "JAWTRenderer.h"

#include <jawt_md.h>
#include <stdlib.h>
#include <string.h>

#import <AppKit/NSOpenGL.h>
#import <AppKit/NSOpenGLView.h>
#import <Foundation/NSAutoreleasePool.h>
#import <OpenGL/gl.h>

#define JAWT_RENDERER_TEXTURE GL_TEXTURE_RECTANGLE_EXT
#define JAWT_RENDERER_TEXTURE_FORMAT GL_BGRA
#define JAWT_RENDERER_TEXTURE_TYPE GL_UNSIGNED_BYTE

@interface JAWTRenderer : NSObject
{
@public
    jint *data;
    jint dataCapacity;
    jint dataHeight;
    jint dataLength;
    jint dataWidth;
}

- (void)dealloc;
- (id)init;
@end /* JAWTRenderer */

@interface JAWTRendererOpenGLView : NSOpenGLView
{
@private
    JAWTRenderer *renderer;
    GLuint texture;
    jint textureWidth;
    jint textureHeight;
}

- (void)clearGLContext;
- (void)dealloc;
- (void)drawRect:(NSRect)rect;
- (id)initWithFrame:(NSRect)frameRect;
- (BOOL)isOpaque;
- (void)prepareOpenGL;
- (void)reshape;
- (void)setRenderer:(JAWTRenderer *)renderer;
@end /* JAWTRendererOpenGLView */

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

void
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
        NSArray *subviews;
        JAWTRendererOpenGLView *rendererView;

        renderer = (JAWTRenderer *) handle;
        autoreleasePool = [[NSAutoreleasePool alloc] init];

        subviews = [component subviews];
        if ([subviews count])
            rendererView = [subviews objectAtIndex:0];
        else
        {

            rendererView
                = [[JAWTRendererOpenGLView alloc]
                        initWithFrame:[component bounds]];

            [component addSubview:rendererView];
            [rendererView release];

            /*
             * Have the rendererView autosized by the component which contains
             * it. And since the rendererView has just been created to fill the
             * current bounds of the component, it will always fill it this way.
             */
            [component setAutoresizesSubviews:YES];
            [rendererView
                setAutoresizingMask:(NSViewWidthSizable | NSViewHeightSizable)];

            [rendererView setRenderer:renderer];
        }
        [rendererView display];

        [autoreleasePool release];
    }
}

jboolean
JAWTRenderer_process
    (JNIEnv *jniEnv, jclass clazz,
     jlong handle, jobject component,
     jint *data, jint length,
     jint width, jint height)
{
    JAWTRenderer *renderer;
    size_t dataSize;
    jint *rendererData;
    jboolean processed;

    renderer = (JAWTRenderer *) handle;

    @synchronized(renderer)
    {
        dataSize = sizeof(jint) * length;
        if (renderer->dataCapacity < length)
        {
            jint newRendererDataCapacity;
            jint *newRendererData;

            newRendererData = realloc(renderer->data, dataSize);
            if (newRendererData)
            {
                renderer->data = rendererData = newRendererData;
                renderer->dataCapacity = length;
            }
            else
                rendererData = NULL;
        }
        else
            rendererData = renderer->data;
        if (rendererData)
        {
            memcpy(rendererData, data, dataSize);
            renderer->dataHeight = height;
            renderer->dataLength = length;
            renderer->dataWidth = width;
            processed = JNI_TRUE;
        }
        else
            processed = JNI_FALSE;
    }
    return processed;
}

@implementation JAWTRenderer
- (void)dealloc
{
    if (data)
        free(data);

    /*
     * Do not release the view because no reference to it has been previously
     * retained by JAWTRenderer.
     */

    [super dealloc];
}

- (id)init
{
    self = [super init];
    if (self)
    {
        data = NULL;
        dataCapacity = 0;
        dataHeight = 0;
        dataLength = 0;
        dataWidth = 0;
    }
    return self;
}
@end /* JAWTRenderer */

@implementation JAWTRendererOpenGLView
- (void)clearGLContext
{
    [super clearGLContext];
}

- (void)dealloc
{
    [self setRenderer:nil];

    [super dealloc];
}

- (void)drawRect:(NSRect)rect
{
    if (renderer)
    {
        @synchronized(renderer)
        {
            if (renderer->data && renderer->dataLength)
            {
                if (texture
                        && ((textureWidth != renderer->dataWidth))
                                || (textureHeight != renderer->dataHeight))
                {
                    glDeleteTextures(1, &texture);
                    texture = 0;
                }
                if (texture)
                {
                    glBindTexture(JAWT_RENDERER_TEXTURE, texture);
                    glTexSubImage2D(
                        JAWT_RENDERER_TEXTURE,
                        0,
                        0, 0,
                        renderer->dataWidth, renderer->dataHeight,
                        JAWT_RENDERER_TEXTURE_FORMAT,
                        JAWT_RENDERER_TEXTURE_TYPE,
                        renderer->data);
                }
                else
                {
                    glGenTextures(1, &texture);
                    glBindTexture(JAWT_RENDERER_TEXTURE, texture);
                    glTexParameterf(
                        JAWT_RENDERER_TEXTURE, GL_TEXTURE_PRIORITY, 1.0);
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
                        renderer->dataWidth, renderer->dataHeight,
                        0,
                        JAWT_RENDERER_TEXTURE_FORMAT,
                        JAWT_RENDERER_TEXTURE_TYPE,
                        renderer->data);
                }
                textureWidth = renderer->dataWidth;
                textureHeight = renderer->dataHeight;

                /*
                 * We've just created the texture from the data so we don't want
                 * to do it again.
                 */
                renderer->dataLength = 0;
            }
        }
    }

    glClear(GL_COLOR_BUFFER_BIT);

    if (texture)
    {
        glEnable(JAWT_RENDERER_TEXTURE);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(-1.0, 1.0);
        glTexCoord2f(textureWidth, 0);
        glVertex2f(1.0, 1.0);
        glTexCoord2f(textureWidth, textureHeight);
        glVertex2f(1.0, -1.0);
        glTexCoord2f(0, textureHeight);
        glVertex2f(-1.0, -1.0);
        glEnd();
        glDisable(JAWT_RENDERER_TEXTURE);
    }

    glFlush();
}

- (id)initWithFrame:(NSRect)frameRect
{
    self
        = [super
                initWithFrame:frameRect
                pixelFormat:[NSOpenGLView defaultPixelFormat]];
    if (self)
    {
        renderer = nil;
        texture = 0;
        textureWidth = 0;
        textureHeight = 0;
    }
    return self;
}

- (BOOL)isOpaque
{
    return YES;
}

- (void)prepareOpenGL
{
    [super prepareOpenGL];

    glDisable(GL_BLEND);
    glDisable(GL_DEPTH_TEST);
    glDepthMask(GL_FALSE);
    glDisable(GL_CULL_FACE);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
}

- (void)reshape
{
    NSRect bounds;

    [super reshape];

    bounds = [self bounds];
    if ((bounds.size.width > 0) && (bounds.size.height > 0))
    {
        NSOpenGLContext *glContext;

        glContext = [self openGLContext];
        if (glContext)
        {
            [glContext makeCurrentContext];

            glViewport(
                bounds.origin.x,
                bounds.origin.y,
                bounds.size.width,
                bounds.size.height);
        }
    }
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
@end /* JAWTRendererOpenGLView */
