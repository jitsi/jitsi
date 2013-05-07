/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_CLASSFACTORY_H
#define __MSOUTLOOKADDRBOOKCOM_CLASSFACTORY_H

#include <Unknwn.h>

/**
 * Represents a base implementation of the <tt>IClassFactory</tt> interface.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
class ClassFactory:
    public IClassFactory
{
    public:
        STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj);
        STDMETHODIMP_(ULONG) AddRef();
        STDMETHODIMP_(ULONG) Release();

        STDMETHOD(LockServer)(BOOL lock);

        HRESULT registerClassObject();
        HRESULT revokeClassObject();

        ClassFactory(REFCLSID clsid);

    protected:
        virtual ~ClassFactory();

    private:
        const CLSID _clsid;
        DWORD _registration;
        ULONG _refCount;
};

#endif
