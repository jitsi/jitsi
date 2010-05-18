/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file ds_capture_device.cpp
 * \brief DirectShow capture device.
 * \author Sebastien Vincent
 * \date 2010
 */

#include "ds_capture_device.h"

/**
 * \brief Release the format block for a media type.
 * \param mt reference to release
 * \note This function comes from MSDN documentation. It is declared
 * here to avoid use DirectShow Base Classes and link against strmbase.lib.
 */
static void FreeMediaType(AM_MEDIA_TYPE& mt)
{
    if (mt.cbFormat != 0)
    {
        CoTaskMemFree((PVOID)mt.pbFormat);
        mt.cbFormat = 0;
        mt.pbFormat = NULL;
    }
    if (mt.pUnk != NULL)
    {
        // pUnk should not be used.
        mt.pUnk->Release();
        mt.pUnk = NULL;
    }
}

/**
 * \brief Delete a media type structure that was allocated on the heap.
 * \param pmt pointer to release
 * \note This function comes from MSDN documentation. It is declared
 * here to avoid use DirectShow Base Classes and link against strmbase.lib.
 */
static void DeleteMediaType(AM_MEDIA_TYPE *pmt)
{
    if (pmt != NULL)
    {
        FreeMediaType(*pmt); 
        CoTaskMemFree(pmt);
    }
}

/* implementation of ISampleGrabber */
DSGrabberCallback::DSGrabberCallback()
{
}

DSGrabberCallback::~DSGrabberCallback()
{
}

STDMETHODIMP DSGrabberCallback::SampleCB(double time, IMediaSample* sample)
{
	BYTE* data = NULL;
	ULONG length = 0;

	if(FAILED(sample->GetPointer(&data)))
	{
		return E_FAIL;
	}

	length = sample->GetActualDataLength();

	printf("Sample received: %p %u\n", data, length);

	return S_OK;
}

STDMETHODIMP DSGrabberCallback::BufferCB(double time, BYTE* buffer, long len)
{
	/* do nothing */
	return S_OK;
}

HRESULT DSGrabberCallback::QueryInterface(const IID& iid, void** ptr)
{
	if(iid == IID_ISampleGrabberCB || iid == IID_IUnknown)
	{
		*ptr = (void*)reinterpret_cast<ISampleGrabberCB*>(this);
		return S_OK;
	}
	return E_NOINTERFACE;
}

STDMETHODIMP_(ULONG) DSGrabberCallback::AddRef()
{ 
	/* fake reference counting */
	return 1;
}

STDMETHODIMP_(ULONG) DSGrabberCallback::Release()
{
	/* fake reference counting */
	return 1;
}

DSCaptureDevice::DSCaptureDevice(const WCHAR* name)
{
	if(name)
	{
		m_name = wcsdup(name);
	}

    m_callback = NULL;

	m_filterGraph = NULL;
	m_captureGraphBuilder = NULL;
	m_graphController = NULL;

	m_srcFilter = NULL;
	m_sampleGrabberFilter = NULL;
	m_sampleGrabber = NULL;
	m_renderer = NULL;
}

DSCaptureDevice::~DSCaptureDevice()
{
	/* remove all added filters from filter graph */
	m_filterGraph->RemoveFilter(m_srcFilter);
	m_filterGraph->RemoveFilter(m_renderer);
	m_filterGraph->RemoveFilter(m_sampleGrabberFilter);

	/* clean up COM stuff */
	if(m_renderer)
	{
		m_renderer->Release();
	}

	if(m_sampleGrabber)
	{
		m_sampleGrabber->Release();
	}
	
	if(m_sampleGrabberFilter)
	{
		m_sampleGrabberFilter->Release();
	}
	
	if(m_srcFilter)
	{
		m_srcFilter->Release();
	}

	if(m_captureGraphBuilder)
	{
		m_captureGraphBuilder->Release();
	}

	if(m_filterGraph)
	{
		m_filterGraph->Release();
	}

	if(m_name)
	{
		free(m_name);
	}
}

const WCHAR* DSCaptureDevice::getName() const
{
	return m_name;
}

bool DSCaptureDevice::setFormat(const VideoFormat& format)
{
	HRESULT ret;
	IAMStreamConfig* streamConfig = NULL;
	AM_MEDIA_TYPE* mediaType = NULL;

	/* force to stop */
	stop();

	/* get the right interface to change capture settings */
	ret = m_captureGraphBuilder->FindInterface(&PIN_CATEGORY_CAPTURE, &MEDIATYPE_Video,
		m_srcFilter, IID_IAMStreamConfig, (void**)&streamConfig);

	if(!FAILED(ret))
	{
		VIDEOINFOHEADER* videoFormat = NULL;
		GUID fmt;
        size_t bitCount = 0;

		/* get the current format and change resolution */
		streamConfig->GetFormat(&mediaType);
		videoFormat = (VIDEOINFOHEADER*)mediaType->pbFormat;
		videoFormat->bmiHeader.biWidth = (LONG)format.width;
		videoFormat->bmiHeader.biHeight = (LONG)format.height;

		switch(format.format)
		{
		case ARGB32:
			fmt = MEDIASUBTYPE_ARGB32;
            bitCount = 32;
			break;
		case RGB32:
			fmt = MEDIASUBTYPE_RGB32;
            bitCount = 32;
			break;
		case RGB24:
			fmt = MEDIASUBTYPE_RGB24;
            bitCount = 24;
			break;
		default:
			/* try to set resolution with current color space */
			fmt = mediaType->subtype;
            bitCount = videoFormat->bmiHeader.biBitCount;
			break;
		}

		mediaType->subtype = fmt;
		ret = streamConfig->SetFormat(mediaType);

		if(FAILED(ret))
		{
			fprintf(stderr, "Failed to set format\n");
            fflush(stderr);
		}
        else
        {
            m_width = format.width;
            m_height = format.height;
            m_bitPerPixel = bitCount;
        }

		DeleteMediaType(mediaType);
		streamConfig->Release();
	}

	return !FAILED(ret);
}

DSGrabberCallback* DSCaptureDevice::getCallback()
{
    return m_callback;
}

void DSCaptureDevice::setCallback(DSGrabberCallback* callback)
{
    m_callback = callback;
	m_sampleGrabber->SetCallback(callback, 0);
}

bool DSCaptureDevice::initDevice(IMoniker* moniker)
{
	HRESULT ret = 0;

	if(!m_name || !moniker)
	{
		return false;
	}
	
	if(m_filterGraph)
	{
		/* already initialized */
		return false;
	}

	/* create the filter and capture graph */
	ret = CoCreateInstance(CLSID_FilterGraph, NULL, CLSCTX_INPROC_SERVER,
		IID_IFilterGraph2, (void**)&m_filterGraph);

	if(FAILED(ret))
	{
		return false;
	}

	ret = CoCreateInstance(CLSID_CaptureGraphBuilder2, NULL,
		CLSCTX_INPROC_SERVER, IID_ICaptureGraphBuilder2, 
		(void**)&m_captureGraphBuilder);

	if(FAILED(ret))
	{
		return false;
	}

	m_captureGraphBuilder->SetFiltergraph(m_filterGraph);

	/* get graph controller */
	ret = m_filterGraph->QueryInterface(IID_IMediaControl, (void**)&m_graphController);

	if(FAILED(ret))
	{
		return false;
	}

	/* add source filter to the filter graph */
    WCHAR* name = wcsdup(m_name);
	ret = m_filterGraph->AddSourceFilterForMoniker(moniker, NULL, name, &m_srcFilter);
    free(name);

	if(ret != S_OK)
	{
		return false;
	}

	ret = CoCreateInstance(CLSID_SampleGrabber, NULL, CLSCTX_INPROC_SERVER, IID_IBaseFilter,
		(void**)&m_sampleGrabberFilter);

	if(ret != S_OK)
	{
		return false;
	}

	/* get sample grabber */
	ret = m_sampleGrabberFilter->QueryInterface(IID_ISampleGrabber, (void**)&m_sampleGrabber);
	
	if(ret != S_OK)
	{
		return false;
	}

	/* and sample grabber to the filter graph */
	m_filterGraph->AddFilter(m_sampleGrabberFilter, L"SampleGrabberFilter");

	/* set media type */
/*
	AM_MEDIA_TYPE mediaType;
	memset(&mediaType, 0x00, sizeof(AM_MEDIA_TYPE));
	mediaType.majortype = MEDIATYPE_Video;
	mediaType.subtype = MEDIASUBTYPE_RGB24;
	ret = m_sampleGrabber->SetMediaType(&mediaType);
*/
	/* set the callback handler */


	if(ret != S_OK)
	{
		return false;
	}

	/* set renderer */
	ret = CoCreateInstance(CLSID_NullRenderer, NULL, CLSCTX_INPROC_SERVER,
		IID_IBaseFilter, (void**)&m_renderer);
	
	if(ret != S_OK)
	{
		return false;
	}

	/* add renderer to the filter graph */
	m_filterGraph->AddFilter(m_renderer, L"NullRenderer");

	/* initialize the list of formats this device supports */
	initSupportedFormats();

    setFormat(m_formats.front());
	return true;
}

void DSCaptureDevice::initSupportedFormats()
{
	HRESULT ret;
	IAMStreamConfig* streamConfig = NULL;
	AM_MEDIA_TYPE* mediaType = NULL;

	ret = m_captureGraphBuilder->FindInterface(&PIN_CATEGORY_CAPTURE, &MEDIATYPE_Video,
		m_srcFilter, IID_IAMStreamConfig, (void**)&streamConfig);

	/* get to find all supported formats */
	if(!FAILED(ret))
	{
		int nb = 0;
		int size = 0;
		BYTE* allocBytes = NULL;

		streamConfig->GetNumberOfCapabilities(&nb, &size);
		allocBytes = new BYTE[size];
		
		for(int i = 0 ; i < nb ; i++)
		{
			if(streamConfig->GetStreamCaps(i, &mediaType, allocBytes) == S_OK)
			{
				struct VideoFormat format;
				VIDEOINFOHEADER* hdr = (VIDEOINFOHEADER*)mediaType->pbFormat;

				if(hdr)
				{
					format.height = hdr->bmiHeader.biHeight;
					format.width = hdr->bmiHeader.biWidth;

					if(mediaType->subtype == MEDIASUBTYPE_ARGB32)
					{
						format.format = ARGB32;
					}
					else if(mediaType->subtype == MEDIASUBTYPE_RGB32)
					{
						format.format = RGB32;
					}
					else if(mediaType->subtype == MEDIASUBTYPE_RGB24)
					{
						format.format = RGB24;
					}
					
                    m_formats.push_back(format);
				}
			}
		}

		delete allocBytes;
	}

}


std::list<VideoFormat> DSCaptureDevice::getSupportedFormats() const
{
	return m_formats;
}

bool DSCaptureDevice::buildGraph()
{
	REFERENCE_TIME start = 0;
	REFERENCE_TIME stop = MAXLONGLONG;
	HRESULT ret = 0;

#ifndef RENDERER_DEBUG
	ret = m_captureGraphBuilder->RenderStream(&PIN_CATEGORY_PREVIEW, &MEDIATYPE_Video,
		m_srcFilter, m_sampleGrabberFilter,
		m_renderer);
#else
	ret = m_captureGraphBuilder->RenderStream(&PIN_CATEGORY_PREVIEW, &MEDIATYPE_Video,
		m_srcFilter, m_sampleGrabberFilter,
		NULL);
#endif

	if(FAILED(ret))
	{
		/* fprintf(stderr, "problem render stream\n"); */
		return false;
	}

	/* start capture */
	ret = m_captureGraphBuilder->ControlStream(&PIN_CATEGORY_PREVIEW, &MEDIATYPE_Video,
		m_srcFilter, &start, &stop, 1, 2);

	/* we need this to finalize graph (maybe other filter will be added) */
	//m_graphController->Run();
	//this->stop();

	return !FAILED(ret);
}

bool DSCaptureDevice::start()
{	
	if(!m_renderer || !m_sampleGrabberFilter || !m_srcFilter || !m_graphController)
	{
		return false;
	}

	m_graphController->Run();
	m_renderer->Run(0);
	m_sampleGrabberFilter->Run(0);
	m_srcFilter->Run(0);
	return true;
}

bool DSCaptureDevice::stop()
{
	if(!m_renderer || !m_sampleGrabberFilter || !m_srcFilter || !m_graphController)
	{
		return false;
	}

	m_srcFilter->Stop();
	m_sampleGrabberFilter->Stop();
	m_renderer->Stop();
	m_graphController->Stop();
    
	return true;
}

size_t DSCaptureDevice::getWidth()
{
    return m_width;
}

size_t DSCaptureDevice::getHeight()
{
    return m_height;
}

size_t DSCaptureDevice::getBitPerPixel()
{
    return m_bitPerPixel;
}

