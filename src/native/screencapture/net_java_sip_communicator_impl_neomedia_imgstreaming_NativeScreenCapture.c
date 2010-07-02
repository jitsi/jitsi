/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture.c
 * \brief X11 screen capture.
 * \author Sebastien Vincent
 * \date 2009
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if defined(_WIN32) || defined(_WIN64)

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <wingdi.h>

typedef __int32 int32_t; 
typedef unsigned __int32 uint32_t; 
typedef unsigned __int8 uint8_t; 

#elif defined(__APPLE__)

#include <stdint.h>

#include <ApplicationServices.h>

#else /* Unix */

#include <stdint.h>

#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XShm.h>

#endif

#include "net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture.h" 

#if defined(_WIN32) || defined (_WIN64)

/**
 * \struct monitor_data
 * \brief Used with monitorCallback to pass index and receive DC
 */
struct monitor_data
{
    unsigned int  monitor_index; /**< Index of the specific monitor */
    unsigned int current_index; /**< Internal index of monitor in callback */
    HDC monitor_dc; /**< DC of the monitor (set by monitorCallback if any) */
};

/**
 * \brief Callback when using EnumDisplayMonitors function.
 * \param hMonitor monitor object
 * \param hdcMonitor DC passed from EnumDisplayMonitors
 * \param lpcrMonitor clipping rectangle
 * \param dwData additionnal data
 * \return true if enumeration have to be stopped, false otherwise
 */
BOOL CALLBACK monitorCallback(HMONITOR hMonitor, HDC hdcMonitor, 
        LPRECT lprcMonitor, LPARAM dwData)
{
    struct monitor_data* data = (struct monitor_data*)dwData;
  	MONITORINFOEX info;

    /* avoid warnings */
    hdcMonitor = hdcMonitor;
    lprcMonitor = lprcMonitor;

    if(data->current_index == data->monitor_index)
    {
	    info.cbSize = sizeof(MONITORINFOEX);
	    GetMonitorInfo(hMonitor, (LPMONITORINFO)&info);

        /* create the DC for this monitor */
        data->monitor_dc = CreateDC(info.szDevice, info.szDevice, NULL, NULL);

        /* found the right monitor, breaks */
        return FALSE;
    }

    data->current_index++;
    return TRUE;
}

/**
 * \brief Get DC pointer for a specific display.
 * \param display display index.
 * \return DC pointer or NULL if failure
 */
static HDC get_dc(unsigned int index)
{
  HDC hDC = NULL;
  struct monitor_data data;

  data.monitor_index = index;
  data.current_index = 0;
  data.monitor_dc = NULL;

  EnumDisplayMonitors(NULL, NULL, &monitorCallback, (LPARAM)&data);
  
  hDC = data.monitor_dc;

  return hDC;
}

/**
 * \brief Grab Windows screen.
 * \param data array that will contain screen capture
 * \param display display index
 * \param x x position to start capture
 * \param y y position to start capture
 * \param w capture width
 * \param h capture height 
 * \return 0 if success, -1 otherwise
 */
static int windows_grab_screen(jbyte* data, unsigned int display, int x, int y, int w, int h)
{
  static const RGBQUAD redColor = {0x00, 0x00, 0xFF, 0x00};
  static const RGBQUAD greenColor = {0x00, 0xFF, 0x00, 0x00};
  static const RGBQUAD blueColor = {0xFF, 0x00, 0x00, 0x00};
  HDC desktop = NULL;
  HDC dest = NULL;
  HBITMAP bitmap;
  HBITMAP oldBitmap;
  int width = 0;
  int height = 0;
  size_t size = 0;
  BITMAPINFO* bitmap_info = NULL;
  BITMAPINFOHEADER* bitmap_hdr = NULL;
  RGBQUAD *pixels = NULL;
  size_t i = 0;
  size_t off = 0;
  uint32_t test = 1;
  int little_endian = *((uint8_t*)&test);

  /* get handle corresponding to the display specified */
  desktop = get_dc(display);

  if(!desktop)
  {
    fprintf(stderr, "GetDC failed!\n"); 
    return -1;
  }

  /* get resolution */
  width = GetDeviceCaps(desktop, HORZRES);
  height = GetDeviceCaps(desktop, VERTRES);

  /* check that user-defined parameters are in image */
  if((w + x) > width || (h + y) > height)
  {
    ReleaseDC(NULL, desktop);
    return -1;
  }

  size = w * h;

  /* fprintf(stderr, "Resolution: %dx%d\n", width, height); */

  dest = CreateCompatibleDC(desktop);
  
  if(!dest)
  {
    fprintf(stderr, "CreateCompatibleDC failed!\n");
    ReleaseDC(NULL, desktop);
    return -1;
  } 

  bitmap = CreateCompatibleBitmap(desktop, width, height);

  if(!bitmap)
  {
    fprintf(stderr, "CreateCompatibleBitmap failed!\n"); 
    ReleaseDC(NULL, desktop);
    return -1;
  }

  /* select bitmap to be used by DC */
  oldBitmap = SelectObject(dest, bitmap);

  if(BitBlt(dest, 0, 0, w, h, desktop, x, y, SRCCOPY | CAPTUREBLT) == FALSE)
  {
    fprintf(stderr, "BitBlt failed\n");
    SelectObject(dest, oldBitmap); /* restore old state before delete */
    DeleteDC(dest);
    DeleteObject(bitmap);
    ReleaseDC(NULL, desktop);
    return -1;
  }

  /* allocate memory for bitmap header, it consists
   * of header size and the uncompressed pixels
   * GetDiBits with BI_BITFIELDS requires array of 3 RBGQUAD
   * structures and BITMAPINFO structure has just one allocated
   * RGBQUAD array
   */
  bitmap_info = malloc(sizeof(BITMAPINFO) + 3 * sizeof(RGBQUAD) + (size * 4));

  if(!bitmap_info)
  {
    fprintf(stderr, "malloc failed\n");
    SelectObject(dest, oldBitmap); /* restore old state before delete */
    DeleteDC(dest);
    DeleteObject(bitmap);
    ReleaseDC(NULL, desktop);
    return -1;
  }

  bitmap_hdr = &bitmap_info->bmiHeader;

  bitmap_hdr->biSize = sizeof(BITMAPINFOHEADER);
  bitmap_hdr->biWidth = w;
  bitmap_hdr->biHeight = -h; /* otherwise inverse screen */
  bitmap_hdr->biPlanes = 1;
  bitmap_hdr->biCompression = BI_BITFIELDS;
  bitmap_hdr->biBitCount = 32;
  bitmap_hdr->biSizeImage = 0;
  bitmap_hdr->biXPelsPerMeter = 0;
  bitmap_hdr->biYPelsPerMeter = 0;
  bitmap_hdr->biClrImportant = 0;
  bitmap_hdr->biClrUsed = 0;

  /* set up color */
  /* red */
  bitmap_info->bmiColors[0] = redColor;
  /* green */
  bitmap_info->bmiColors[1] = greenColor;
  /* blue */
  bitmap_info->bmiColors[2] = blueColor;

  /* first data pixel begins after the array of color mask */
  pixels = &bitmap_info->bmiColors[2];
  pixels++;

  /* get raw bytes */
  if(GetDIBits(dest, bitmap, 0, height, pixels, bitmap_info, DIB_RGB_COLORS) == 0)
  {
    fprintf(stderr, "GetDIBits failed!\n");
    free(bitmap_info);
    SelectObject(dest, oldBitmap); /* restore old state before delete */
    DeleteDC(dest);
    DeleteObject(bitmap);
    ReleaseDC(NULL, desktop);
    return -1;
  }

  for(i = 0 ; i < size ; i++)
  {
    RGBQUAD* quad = &pixels[i];
    uint32_t pixel = 0xFF000000 | quad->rgbRed << 16 | quad->rgbGreen << 8 | quad->rgbBlue;
    
    /* Java int is always big endian so output as ARGB */
    if(little_endian)
    {
      /* ARGB is BGRA in little-endian */
      uint8_t r = (pixel >> 16) & 0xff;
      uint8_t g = (pixel >> 8) & 0xff;
      uint8_t b = pixel & 0xff;
      pixel = b << 24 | g << 16 | r << 8 | 0xff;
    }

    memcpy(data + off, &pixel, 4);
    off += 4;
  }

  /* cleanup */
  free(bitmap_info);
  SelectObject(dest, oldBitmap); /* restore old state before delete */
  DeleteDC(dest);
  DeleteObject(bitmap);
  ReleaseDC(NULL, desktop);

  return 0; 
}

#elif defined(__APPLE__)

/**
 * \brief Grab Mac OS X screen (with Quartz API).
 * \param data array that will contain screen capture
 * \param display display index
 * \param x x position to start capture
 * \param y y position to start capture
 * \param w capture width
 * \param h capture height
 * \return 0 if success, -1 otherwise
 */
static int quartz_grab_screen(jbyte* data, unsigned int display, int x, int y, int w, int h)
{
  CGImageRef img = NULL;
  CGDataProviderRef provider = NULL;
  CFDataRef dataRef = NULL;
  uint8_t* pixels = NULL;
  size_t len = 0;
  size_t off = 0;
  size_t i = 0;
  CGRect rect;
  uint32_t test = 1;
  int little_endian = *((uint8_t*)&test);
  CGDirectDisplayID displayIds[16];
  CGDisplayCount displayNb = 0;

  /* find display */
  if(CGGetActiveDisplayList(display + 1, displayIds, &displayNb) != kCGErrorSuccess)
  {
    return -1;
  }

  if(displayNb < (display + 1))
  {
    /* request a non existent display */
    return -1;
  }
  
  rect = CGDisplayBounds(displayIds[display]);
  rect.size.width = w;
  rect.size.height = h;
  rect.origin.x += x;
  rect.origin.y += y;
  img = CGWindowListCreateImage(rect, kCGWindowListOptionOnScreenOnly, kCGNullWindowID, kCGWindowImageDefault);

  if(img == NULL)
  {
    fprintf(stderr, "CGWindowListCreateImage failed\n!");
    return -1;
  }

  /* get pixels */
  provider = CGImageGetDataProvider(img);
  dataRef = CGDataProviderCopyData(provider);
  pixels = (uint8_t*)CFDataGetBytePtr(dataRef);

  len = CFDataGetLength(dataRef);

  for(i = 0 ; i < len ; i+=4)
  {
    uint32_t pixel = *((uint32_t*)&pixels[i]);
    
    pixel |= (0xff << 24);

    /* Java int is always big endian so output as ARGB */
    if(little_endian)
    {
      /* ARGB is BGRA in little-endian */
      uint8_t r = (pixel >> 16) & 0xff;
      uint8_t g = (pixel >> 8) & 0xff;
      uint8_t b = pixel & 0xff;
      pixel = b << 24 | g << 16 | r << 8 | 0xff;
    }

    memcpy(data + off, &pixel, 4);
    off += 4;
  }

  /* cleanup */
  CGImageRelease(img);
  CFRelease(dataRef);
  return 0;
}

#else /* Unix */

/**
 * \brief Grab X11 screen.
 * \param x11display display string (i.e. :0.0), if NULL getenv("DISPLAY") is used
 * \param data array that will contain screen capture
 * \param displayIndex display index
 * \param x x position to start capture
 * \param y y position to start capture
 * \param w capture width
 * \param h capture height 
 * \return 0 if success, -1 otherwise
 */
static int x11_grab_screen(jbyte* data, unsigned int displayIndex, int x, int y, int w, int h)
{
  const char* display_str; /* display string */
  Display* display = NULL; /* X11 display */
  Visual* visual = NULL;
  int screen = 0; /* X11 screen */
  Window root_window = 0; /* X11 root window of a screen */
  int width = 0;
  int height = 0;
  int depth = 0;
  int shm_support = 0;
  XImage* img = NULL;
  XShmSegmentInfo shm_info;
  size_t off = 0;
  int i = 0;
  int j = 0;
  size_t size = 0;
  uint32_t test = 1;
  int little_endian = *((uint8_t*)&test);
  char buf[16];
  
  snprintf(buf, sizeof(buf), ":0.%u", displayIndex);
  display_str = buf;

  /* open current X11 display */
  display = XOpenDisplay(display_str);

  if(!display)
  {
    /* fprintf(stderr, "Cannot open X11 display!\n"); */
    return -1;
  }
  
  screen = DefaultScreen(display);
  root_window = RootWindow(display, screen);
  visual = DefaultVisual(display, screen);
  width = DisplayWidth(display, screen);
  height = DisplayHeight(display, screen);
  depth = DefaultDepth(display, screen);

  /* check that user-defined parameters are in image */
  if((w + x) > width || (h + y) > height)
  {
    XCloseDisplay(display);
    return -1;
  }

  size = w * h;

  /* test is XServer support SHM */
  shm_support = XShmQueryExtension(display);

  /* fprintf(stderr, "Display=%s width=%d height=%d depth=%d SHM=%s\n", display_str, width, height, depth, shm_support ? "true" : "false"); */

  if(shm_support)
  {
    /* fprintf(stderr, "Use XShmGetImage\n"); */

    /* create image for SHM use */
    img = XShmCreateImage(display, visual, depth, ZPixmap, NULL, &shm_info, w, h);

    if(!img)
    {
      /* fprintf(stderr, "Image cannot be created!\n"); */
      XCloseDisplay(display);
      return -1;
    }

    /* setup SHM stuff */
    shm_info.shmid = shmget(IPC_PRIVATE, img->bytes_per_line * img->height, IPC_CREAT | 0777);
    shm_info.shmaddr = (char*)shmat(shm_info.shmid, NULL, 0);
    img->data = shm_info.shmaddr;
    shmctl(shm_info.shmid, IPC_RMID, NULL);
    shm_info.readOnly = 0;

    /* attach segment and grab screen */
    if((shm_info.shmaddr == (void*)-1) || !XShmAttach(display, &shm_info))
    {
      /* fprintf(stderr, "Cannot use shared memory!\n"); */
      if(shm_info.shmaddr != (void*)-1)
      {
        shmdt(shm_info.shmaddr);
      }

      img->data = NULL;
      XDestroyImage(img);
      img = NULL;
      shm_support = 0;
    }
    else if(!XShmGetImage(display, root_window, img, x, y, 0xffffffff))
    {
      /* fprintf(stderr, "Cannot grab image!\n"); */
      XShmDetach(display, &shm_info);
      shmdt(shm_info.shmaddr);
      XDestroyImage(img);
      img = NULL;
      shm_support = 0;
    }
  }

  /* if XSHM is not available or has failed 
   * use XGetImage
   */
  if(!img)
  {
    /* fprintf(stderr, "Use XGetImage\n"); */
    img = XGetImage(display, root_window, x, y, w, h, 0xffffffff, ZPixmap);

    if(!img)
    {
      /* fprintf(stderr, "Cannot grab image!\n"); */
      XCloseDisplay(display);
      return -1;
    }
  }

  /* convert to bytes but keep ARGB */
  for(j = 0 ; j < h ; j++)
  {
    for(i = 0 ; i < w ; i++)
    {
      /* do not care about high 32-bit for Linux 64 bit 
       * machine (sizeof(unsigned long) = 8)
       */
      uint32_t pixel = (uint32_t)XGetPixel(img, i, j) | (0xff << 24);
      
      /* Java int is always big endian so output as ARGB */
      if(little_endian)
      {
        /* ARGB is BGRA in little-endian */
        uint8_t r = (pixel >> 16) & 0xff;
        uint8_t g = (pixel >> 8) & 0xff;
        uint8_t b = pixel & 0xff;
        pixel = b << 24 | g << 16 | r << 8 | 0xff;
      }
      
      memcpy(data + off, &pixel, 4);
      off += 4;
    }
  }

  /* free X11 resources and close display */
  XDestroyImage(img);
  
  if(shm_support)
  {
    XShmDetach(display, &shm_info);
    shmdt(shm_info.shmaddr);
  }

  XCloseDisplay(display);

  /* return array */
  return 0;
}

#endif

/**
 * \brief JNI native method to grab desktop screen and retrieve ARGB pixels.
 * \param env JVM environment
 * \param clazz NativeScreenCapture Java class
 * \param display display index
 * \param x x position to start capture
 * \param y y position to start capture
 * \param width capture width
 * \param height capture height
 * \param output output buffer, screen bytes will be stored in
 * \return true if success, false otherwise
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture_grabScreen__IIIII_3B
  (JNIEnv* env, jclass clazz, jint display, jint x, jint y, jint width, jint height, jbyteArray output)
{
  jint size = width * height * 4;
  jbyte* data = NULL;

  clazz = clazz; /* not used */

  if(!output || (*env)->GetArrayLength(env, output) < size)
  {
    return JNI_FALSE;
  }

  data = (*env)->GetPrimitiveArrayCritical(env, output, 0);

  if(!data)
  {
    return JNI_FALSE;
  }

#if defined (_WIN32) || defined(_WIN64)
  if(windows_grab_screen(data, display, x, y, width, height) == -1)
#elif defined(__APPLE__)
    if(quartz_grab_screen(data, display, x, y, width, height) == -1)
#else /* Unix */
  if(x11_grab_screen(data, display, x, y, width, height) == -1)
#endif
  {
    (*env)->ReleasePrimitiveArrayCritical(env, output, data, 0);
    return JNI_FALSE;
  }

  (*env)->ReleasePrimitiveArrayCritical(env, output, data, 0);
  return JNI_TRUE;
}

/**
 * \brief JNI native method to grab desktop screen and retrieve ARGB pixels.
 * \param env JVM environment
 * \param clazz NativeScreenCapture Java class
 * \param display display index
 * \param x x position to start capture
 * \param y y position to start capture
 * \param width capture width
 * \param height capture height
 * \param output native output buffer
 * \return true if success, false otherwise
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_impl_neomedia_imgstreaming_NativeScreenCapture_grabScreen__IIIIIJI
  (JNIEnv* env, jclass clazz, jint display, jint x, jint y, jint width, jint height, jlong output, jint outputLength)
{
  jint size = width * height * 4;
  jbyte* data = (jbyte*)output;

  /* not used */
  clazz = clazz;
  env = env;

  if(!data || outputLength < size)
  {
    return JNI_FALSE;
  }

#if defined (_WIN32) || defined(_WIN64)
  if(windows_grab_screen(data, display, x, y, width, height) == -1)
#elif defined(__APPLE__)
    if(quartz_grab_screen(data, display, x, y, width, height) == -1)
#else /* Unix */
  if(x11_grab_screen(data, display, x, y, width, height) == -1)
#endif
  {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

