/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_CLASSFACTORY_H_
#define _JMSOFFICECOMM_CLASSFACTORY_H_

#include "OutOfProcessServer.h"
#include "UnknownImpl.h"

/**
 * Represents a base implementation of the <tt>IClassFactory</tt> interface.
 *
 * @author Lyubomir Marinov
 */
class ClassFactory
    : public UnknownImpl<IClassFactory, IID_IClassFactory>
{
public:
    // IClassFactory
    STDMETHOD(LockServer)(BOOL lock)
        {
            lock ? OutOfProcessServer::addRef() : OutOfProcessServer::release();
            return S_OK;
        };

    HRESULT registerClassObject()
        {
            return
                ::CoRegisterClassObject(
                        _clsid,
                        this,
                        CLSCTX_LOCAL_SERVER,
                        REGCLS_MULTIPLEUSE | REGCLS_SUSPENDED,
                        &_registration);
        };

    HRESULT revokeClassObject() { return ::CoRevokeClassObject(_registration); };

protected:
    ClassFactory(REFCLSID clsid) : _clsid(clsid), _registration(0) {};
    virtual ~ClassFactory() {};

    const CLSID _clsid;

private:
    DWORD _registration;
};

#endif /* #ifndef _JMSOFFICECOMM_CLASSFACTORY_H_ */
