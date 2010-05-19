/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice.cpp
 * \brief JNI part of DSCaptureDevice.
 * \author Sebastien Vincent
 */

#include "ds_capture_device.h"
#include "video_format.h"

#ifdef __cplusplus
extern "C" { /* } */
#endif

#include "net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice.h"

/**
 * \class Grabber.
 * \brief Frame grabber.
 */
class Grabber : public DSGrabberCallback
{
public:
    /**
     * \brief Constructor.
     * \param vm Java Virtual Machine
     * \param delegate delegate Java object
     */
    Grabber(JavaVM* vm, jobject delegate, DSCaptureDevice* dev)
    {
        this->m_vm = vm;
        this->m_delegate = delegate;
        this->m_dev = dev;
        m_bytes = NULL;
        m_bytesLength = 0;
    }

    ~Grabber()
    {
        m_vm = NULL;
        m_delegate = NULL;
        m_dev = NULL;

        if(m_bytes != NULL)
        {
            delete[] m_bytes;
        }
    }

   	/**
	 * \brief Method callback when device capture a frame.
	 * \param time time when frame was received
	 * \param sample media sample
	 * \see ISampleGrabberCB
	 */
	virtual STDMETHODIMP SampleCB(double time, IMediaSample* sample)
    {
        jclass delegateClass = NULL;
        JNIEnv* env = NULL;

        if(m_vm->AttachCurrentThreadAsDaemon((void**)&env, NULL) != 0)
        {
            return E_FAIL;
        }

        delegateClass = env->GetObjectClass(m_delegate);
        if(delegateClass)
        {
            jmethodID methodid = NULL;

            methodid =env->GetMethodID(delegateClass,"frameReceived",
                    "(JI)V");
            if(methodid)
            {
                BYTE* data = NULL;
                size_t length = 0;
                bool flipImage = false;
                size_t width = 0;
                size_t height = 0;
                size_t bytesPerPixel = 0;
                VideoFormat format = m_dev->getFormat();
                /* get width and height */
                width = format.width;
                height = format.height;
                bytesPerPixel = m_dev->getBitPerPixel() / 8;

                /* flip image for RGB content */
                flipImage = (format.mediaType == MEDIASUBTYPE_ARGB32 ||
                    format.mediaType == MEDIASUBTYPE_RGB32 || 
                    format.mediaType == MEDIASUBTYPE_RGB24);

                sample->GetPointer(&data);
                length = sample->GetActualDataLength();

                if(!m_bytes || m_bytesLength < length)
                {
                    if(m_bytes)
                    {
                        delete[] m_bytes;
                    }

                    m_bytes = new BYTE[length];
                    m_bytesLength = length;
                }

                /* it seems that images is always inversed, 
                 * the following code from lti-civil is always used to flip
                 * images
                 */
        		if(flipImage)
                {
                    for(size_t row = 0 ; row < height ; row++)
            		{
                        memcpy((m_bytes + row * width * bytesPerPixel), data + (height - 1 - row) * width * bytesPerPixel,
                                width * bytesPerPixel);
                    }
                }
                else
                {
                    memcpy(m_bytes, data, length);
                }

                env->CallVoidMethod(m_delegate, methodid, (jlong)m_bytes, (jlong)length);
            }
        }
        return S_OK;
    }
    
    /**
     * \brief Java VM.
     */
    JavaVM* m_vm;

    /**
     * \brief Delegate Java object.
     */
    jobject m_delegate;

    /**
     * \brief Internal buffer.
     */
    BYTE* m_bytes; 

    /**
     * \brief Length of internal buffer.
     */
    size_t m_bytesLength;

    /**
     * \brief DirectShow device.
     */
    DSCaptureDevice* m_dev;
};

/**
 * \brief Open native capture device.
 * \param env JNI environment
 * \param obj DSCaptureDevice object
 * \param ptr native pointer of DSCaptureDevice
 */ 
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_open
  (JNIEnv* env, jobject obj, jlong ptr)
{
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);

    dev->buildGraph();
    dev->start();
}

/**
 * \brief Close native capture device.
 * \param env JNI environment
 * \param obj DSCaptureDevice object
 * \param ptr native pointer of DSCaptureDevice
 */ 
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_close
  (JNIEnv* env, jobject obj, jlong ptr)
{
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    dev->stop();
}
 
/**
 * \brief Get name of native capture device.
 * \param env JNI environment
 * \param obj DSCaptureDevice object
 * \param ptr native pointer of DSCaptureDevice
 * \return name of the native capture device
 */ 
JNIEXPORT jstring JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_getName
  (JNIEnv* env, jobject obj, jlong ptr)
{
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    jstring ret = NULL;
    jsize len = static_cast<jsize>(wcslen(dev->getName()));
    jchar* name = new jchar[len];

    /* jchar is two bytes! */
    memcpy((void*)name, (void*)dev->getName(), len * 2);

    ret = env->NewString(name, len);
    delete[] name;

    return ret;
}

/**
 * \brief Set format of native capture device.
 * \param env JNI environment
 * \param obj DSCaptureDevice object
 * \param ptr native pointer of DSCaptureDevice
 * \param format DSFormat to set
 */ 
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_setFormat
  (JNIEnv* env, jobject obj, jlong ptr, jobject format)
{
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    VideoFormat fmt;
    jclass clazz = env->GetObjectClass(format);
    
    if(clazz)
    {
        jfieldID fieldH = env->GetFieldID(clazz, "height", "I");
        jfieldID fieldW = env->GetFieldID(clazz, "width", "I");
        jfieldID fieldF = env->GetFieldID(clazz, "pixelFormat", "J");
        jlong f = env->GetLongField(format, fieldF);
        jint w = env->GetIntField(format, fieldW);
        jint h = env->GetIntField(format, fieldH);
    
        fmt.width = w;
        fmt.height = h;
        fmt.pixelFormat = (unsigned long)f;
    
        dev->setFormat(fmt);
    }
}

/**
 * \brief Get current format.
 * \param env JNI environment
 * \param obj object
 * \param native pointer
 * \return current format
 */
JNIEXPORT jobject JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_getFormat
  (JNIEnv* env, jobject obj, jlong ptr)
{
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    VideoFormat fmt = dev->getFormat();
    jclass clazzDSFormat = NULL;
    jmethodID initDSFormat = NULL;
    jobject ret = NULL;

    /* get DSFormat class to instantiate some object */
    clazzDSFormat = env->FindClass("net/java/sip/communicator/impl/neomedia/directshow/DSFormat");
    if(clazzDSFormat == NULL)
    {
        return NULL;
    }

    initDSFormat = env->GetMethodID(clazzDSFormat, "<init>", "(IIJ)V");

    if(initDSFormat == NULL)
    {
        return NULL;
    }

    ret = env->NewObject(clazzDSFormat, initDSFormat, static_cast<size_t>(fmt.width),
            static_cast<size_t>(fmt.height), static_cast<jlong>(fmt.pixelFormat));
    return ret;
}

/**
 * \brief Get formats supported by native capture device.
 * \param env JNI environment
 * \param obj DSCaptureDevice object
 * \param ptr native pointer of DSCaptureDevice
 * \return array of DSFormat object
 */ 
JNIEXPORT jobjectArray JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_getSupportedFormats
  (JNIEnv* env, jobject obj, jlong ptr)
{
    jobjectArray ret = NULL;
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    std::list<VideoFormat> formats;
    jclass clazzDSFormat = NULL;
    jmethodID initDSFormat = NULL;
    jsize i = 0;

    /* get DSFormat class to instantiate some object */
    clazzDSFormat = env->FindClass("net/java/sip/communicator/impl/neomedia/directshow/DSFormat");
    if(clazzDSFormat == NULL)
    {
        return NULL;
    }

    initDSFormat = env->GetMethodID(clazzDSFormat, "<init>", "(IIJ)V");

    if(initDSFormat == NULL)
    {
        return NULL;
    }

    formats = dev->getSupportedFormats();

    ret = env->NewObjectArray(static_cast<jsize>(formats.size()), clazzDSFormat, NULL);
    for(std::list<VideoFormat>::iterator it = formats.begin() ; it != formats.end() ; ++it)
    {
        VideoFormat tmp = (*it);
        jobject o = env->NewObject(clazzDSFormat, initDSFormat, static_cast<size_t>(tmp.width),
            static_cast<size_t>(tmp.height), static_cast<jlong>(tmp.pixelFormat));

        if(o == NULL)
        {
            fprintf(stderr, "failed!!\n");
            fflush(stderr);
        }
        else
        {

            env->SetObjectArrayElement(ret, i, o);
            env->DeleteLocalRef(o);
            i++;
        }
    }

    return ret;
}

/**
 * \brief Set delegate.
 * \param env JNI environment
 * \param obj object
 * \param ptr native pointer on DSCaptureDevice
 * \param delegate delegate object
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_setDelegate
  (JNIEnv* env, jobject obj, jlong ptr, jobject delegate)
{
    Grabber* grab = NULL;
    DSCaptureDevice* dev = reinterpret_cast<DSCaptureDevice*>(ptr);
    DSGrabberCallback* prev = dev->getCallback();
   
    if(delegate != NULL)
    {
        delegate = env->NewGlobalRef(delegate);
        if(delegate)
        {
            JavaVM* vm = NULL;
            /* get JavaVM */
            env->GetJavaVM(&vm);
            grab = new Grabber(vm, delegate, dev);
            dev->setCallback(grab);
        }
    }
    else
    {
        dev->setCallback(NULL);
    }
   
    if(prev)
    {
        jobject tmp_delegate = ((Grabber*)prev)->m_delegate;
        if(tmp_delegate)
        {
            env->DeleteGlobalRef(tmp_delegate);
        }
        delete prev;
    }
}


JNIEXPORT jint JNICALL Java_net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice_getBytes
  (JNIEnv* env, jclass clazz, jlong ptr, jlong buf, jint len)
{
    /* copy data */
    memcpy((void*)buf, (void*)ptr, len);
    return len;
}

#ifdef __cplusplus
}
#endif

