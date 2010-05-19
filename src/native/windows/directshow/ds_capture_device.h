/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file ds_capture_device.h
 * \brief DirectShow capture device.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef DS_CAPTURE_DEVICE_H
#define DS_CAPTURE_DEVICE_H

#include <list>

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <dshow.h>
#include "qedit.h"

#include "video_format.h"

/**
 * \class DSGrabberCallback
 * \brief Callback when DirectShow device capture frames.
 */
class DSGrabberCallback : public ISampleGrabberCB
{
public:
	/**
	 * \brief Constructor.
	 */
	DSGrabberCallback();

	/**
	 * \brief Destructor.
	 */
	~DSGrabberCallback();

	/**
	 * \brief Method callback when device capture a frame.
	 * \param time time when frame was received
	 * \param sample media sample
	 * \see ISampleGrabberCB
	 */
	virtual STDMETHODIMP SampleCB(double time, IMediaSample* sample);

	/**
	 * \brief Method callback when device buffer a frame.
	 * \param time time when frame was received
	 * \param buffer raw buffer
	 * \param len length of buffer
	 * \see ISampleGrabberCB
	 */
	virtual STDMETHODIMP BufferCB(double time, BYTE* buffer, long len);

	/**
	 * \brief Query if this COM object has the interface iid.
	 * \param iid interface requested
	 * \param ptr if method succeed, an object corresponding
	 * to the interface requested will be copied in this pointer
	 */
	virtual HRESULT STDMETHODCALLTYPE QueryInterface(const IID& iid, void** ptr);

	/**
	 * \brief Adding a reference.
	 * \return number of reference hold
	 */
    STDMETHODIMP_(ULONG) AddRef();

	/**
	 * \brief Release a reference.
	 * \return number of reference hold
	 */
    STDMETHODIMP_(ULONG) Release();
};

/**
 * \class DSCaptureDevice
 * \brief DirectShow capture device.
 *
 * Once a DSCapture has been obtained by DSManager, do not
 * forget to build the graph and optionally set a format.
 */
class DSCaptureDevice
{
public:
	/**
	 * \brief Constructor.
	 * \param name name of the capture device
	 */
	DSCaptureDevice(const WCHAR* name);

	/**
	 * \brief Destructor.
	 */
	~DSCaptureDevice();

	/**
	 * \brief Get name of the capture device.
	 * \return name of the capture device
	 */
	const WCHAR* getName() const;

	/**
	 * \brief Initialize the device.
	 * \param moniker moniker of the capture device
	 * \return true if initialization succeed, false otherwise (in this
	 * case the capture device have to be deleted)
	 */
	bool initDevice(IMoniker* moniker);

	/**
	 * \brief Set video format.
	 * \param format video format
	 * \return true if change is successful, false otherwise (format unsupported, ...)
	 * \note This method stop stream so you have to call start() after.
	 */
	bool setFormat(const VideoFormat& format);

	/**
	 * \brief Get list of supported formats.
	 * \return list of supported formats.
	 */
	std::list<VideoFormat> getSupportedFormats() const;

	/**
	 * \brief Build the filter graph for this capture device.
	 * \return true if success, false otherwise
	 * \note Call this method before start().
	 */
	bool buildGraph();

	/**
	 * \brief get callback object.
	 * \return callback
	 */
	DSGrabberCallback* getCallback();

	/**
	 * \brief Set callback object when receiving new frames.
	 * \param callback callback object to set
	 */
	void setCallback(DSGrabberCallback* callback);

	/**
	 * \brief Start capture device.
	 * \return false if problem, true otherwise
	 */
	bool start();

	/**
	 * \brief Stop capture device.
	 * \return false if problem, true otherwise
	 */
	bool stop();

    /**
     * \brief Get current format.
     * \return current format
     */
    VideoFormat getFormat() const;

    /**
     * \brief Get current bit per pixel.
     * \return bit per pixel of images
     */
    size_t getBitPerPixel();

private:
	/**
	 * \brief Initialize list of supported size.
	 */
	void initSupportedFormats();
	
    /**
	 * \brief Name of the capture device.
	 */
	WCHAR* m_name;

    /**
     * \brief Callback.
     */
    DSGrabberCallback* m_callback;

	/**
	 * \brief List of VideoFormat.
	 */
	std::list<VideoFormat> m_formats;
    
	/**
	 * \brief Reference of the filter graph.
	 */
	IFilterGraph2* m_filterGraph;

	/**
	 * \brief Reference of the capture graph builder.
	 */
	ICaptureGraphBuilder2* m_captureGraphBuilder;

	/**
	 * \brief Controller of the graph.
	 */
	IMediaControl* m_graphController;

	/**
	 * \brief Source filter.
	 */
	IBaseFilter* m_srcFilter;
	
	/**
	 * \brief Sample grabber filter.
	 */
	IBaseFilter* m_sampleGrabberFilter;

	/**
	 * \brief The null renderer.
	 */
	IBaseFilter* m_renderer;

	/**
	 * \brief The sample grabber.
	 */
	ISampleGrabber* m_sampleGrabber;

    /**
     * \brief Current format.
     */
    VideoFormat m_format;

    /**
     * \brief Current bit per pixel.
     */
    size_t m_bitPerPixel;
};

#endif /* DS_CAPTURE_DEVICE_H */

