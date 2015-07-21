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
#ifndef __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENTCLASSFACTORY_H
#define __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENTCLASSFACTORY_H

#include "ClassFactory.h"

#include "MsOutlookAddrBookClient.h"


/**
 * Implements the <tt>IClassFactory</tt> interface for the
 * <tt>IMsOutlookAddrBookClient</tt>
 * interface implementation.
 *
 * @author Lyubomir Marinov
 */
class MsOutlookAddrBookClientClassFactory:
    public ClassFactory
{
    public:
        MsOutlookAddrBookClientClassFactory();

        STDMETHODIMP CreateInstance(LPUNKNOWN, REFIID, PVOID *);

    protected:
        virtual ~MsOutlookAddrBookClientClassFactory();

    private:
        IMsOutlookAddrBookClient *_msOutlookAddrBookClient;
};

#endif /* _JMSOFFICECOMM_MSOUTLOOKADDRBOOKCLASSFACTORY_H_ */
