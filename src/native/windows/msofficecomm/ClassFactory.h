/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
