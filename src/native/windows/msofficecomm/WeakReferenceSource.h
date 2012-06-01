/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_WEAKREFERENCESOURCE_H_
#define _JMSOFFICECOMM_WEAKREFERENCESOURCE_H_

#include "IWeakReferenceSource.h"

class WeakReference;

class WeakReferenceSource
    : public IWeakReferenceSource
{
public:
    WeakReferenceSource(LPUNKNOWN iUnknown)
        : _iUnknown(iUnknown), _weakReference(NULL) {}
    virtual ~WeakReferenceSource();

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj)
        { return _iUnknown->QueryInterface(iid, obj); }
    STDMETHODIMP_(ULONG) AddRef()
        { return _iUnknown->AddRef(); }
    STDMETHODIMP_(ULONG) Release()
        { return _iUnknown->Release(); }

    // IWeakReferenceSource
    STDMETHODIMP GetWeakReference(IWeakReference **weakReference);

private:
    const LPUNKNOWN _iUnknown;
    WeakReference *_weakReference;
};

#define STDMETHODIMP_RESOLVE_WEAKREFERENCE_OR_NEW(out,weakReference,clazz,...) \
    { \
        HRESULT hr; \
\
        if (out) \
        { \
            hr = E_FAIL; \
            if (weakReference) \
            { \
                hr = weakReference->Resolve(IID_IDispatch, (PVOID *) out); \
                if (FAILED(hr) && (E_NOINTERFACE != hr)) \
                { \
                    weakReference->Release(); \
                    weakReference = NULL; \
                } \
            } \
            if (FAILED(hr) && (E_NOINTERFACE != hr)) \
            { \
                clazz *obj = new clazz(__VA_ARGS__); \
                IWeakReferenceSource *weakReferenceSource; \
\
                hr \
                    = obj->QueryInterface( \
                            IID_IWeakReferenceSource, \
                            (PVOID *) &weakReferenceSource); \
                obj->Release(); \
                if (SUCCEEDED(hr)) \
                { \
                    hr \
                        = weakReferenceSource->GetWeakReference( \
                                &weakReference); \
                    if (SUCCEEDED(hr)) \
                    { \
                        hr \
                            = weakReference->Resolve( \
                                    IID_IDispatch, \
                                    (PVOID *) out); \
                    } \
                    else \
                        *out = NULL; \
                    weakReferenceSource->Release(); \
                } \
                else \
                    *out = NULL; \
            } \
        } \
        else \
            hr = RPC_X_NULL_REF_POINTER; \
        return hr; \
    }

#endif /* #ifndef _JMSOFFICECOMM_WEAKREFERENCESOURCE_H_ */
