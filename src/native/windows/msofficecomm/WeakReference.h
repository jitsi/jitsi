/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_WEAKREFERENCE_H_
#define _JMSOFFICECOMM_WEAKREFERENCE_H_

#include "UnknownImpl.h"

class WeakReference
    : public UnknownImpl<IWeakReference, IID_IWeakReference>
{
public:
    WeakReference(LPUNKNOWN iUnknown) : _iUnknown(iUnknown) {}

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID, PVOID *);

    // IWeakReference
    STDMETHODIMP Resolve(REFIID, PVOID *);

    void invalidate() { _iUnknown = NULL; }

protected:
    virtual ~WeakReference() {}

private:
    LPUNKNOWN _iUnknown;
};

#endif /* #ifndef _JMSOFFICECOMM_WEAKREFERENCE_H_ */
