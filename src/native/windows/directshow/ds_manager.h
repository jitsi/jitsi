/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * \file ds_manager.h
 * \brief DirectShow capture devices manager.
 * \author Sebastien Vincent
 * \date 2010
 */

#ifndef DS_MANAGER_H
#define DS_MANAGER_H

#pragma comment(lib, "strmiids")

#include <list>

#include <dshow.h>

class DSCaptureDevice;

/**
 * \class DSManager
 * \brief DirectShow capture device manager (singleton).
 */
class DSManager
{
public:
    /**
     * \brief Destructor.
     */
    ~DSManager();

    /**
     * \brief Initialize DirectShow manager.
     *
     * Call this method to initialize DirectShow capture devices.
     * It can also be used to reinitialize and update list of current
     * devices but be sure to not use any of previous DSCaptureDevice after.
     *
     * \return true if initialize succeed, false otherwise
     * \note You MUST call before any use of DirectShow.
     */
    static bool initialize();

    /**
     * \brief Destroy DirectShow manager.
     * \note You MUST call this method when you have finished 
     * using DirectShow.
     */
    static void destroy();

    /**
     * \brief Get unique instance.
     * \return DirectShow manager instance
     * \note You MUST call DSManager::initialize before
     * any use of this method or you will get NULL as return value.
     */
    static DSManager* getInstance();

    /**
     * \brief Get all available capture video devices.
     * \return devices list
     */
    std::list<DSCaptureDevice*> getDevices() const;

    /**
     * \brief Get number of devices.
     * \return number of available devices
     */
    size_t getDevicesCount();

private:
    /**
     * \brief Unique instance of DirectShow manager.
     */
    static DSManager* m_instance;

    /**
     * \brief Constructor.
     */
    DSManager();

    /**
     * \brief Get and initialize video capture devices.
     */
    void initCaptureDevices();

    /**
     * \brief Available devices list.
     */
    std::list<DSCaptureDevice*> m_devices;

    /**
     * \brief Easy use of template-based list iterator.
     */
    typedef std::list<DSCaptureDevice*>::iterator DeviceListIterator;

    /**
     * If COM backend is initialized.
     */
    bool comInited;
};

#endif /* DS_MANAGER_H */

