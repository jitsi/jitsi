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
#ifndef _JMSOFFICECOMM_IWEAKREFERENCESOURCE_H_
#define _JMSOFFICECOMM_IWEAKREFERENCESOURCE_H_

#include "IWeakReference.h" 

EXTERN_C const IID IID_IWeakReferenceSource;

#undef INTERFACE /* Silence a possible redefinition warning. */
#define INTERFACE IWeakReferenceSource
DECLARE_INTERFACE_(IWeakReferenceSource,IUnknown)
{
    STDMETHOD(QueryInterface)(THIS_ REFIID, PVOID *) PURE;
    STDMETHOD_(ULONG,AddRef)(THIS) PURE;
    STDMETHOD_(ULONG,Release)(THIS) PURE;
    STDMETHOD(GetWeakReference)(THIS_ IWeakReference **) PURE;
};
#undef INTERFACE

#endif /* #ifndef _JMSOFFICECOMM_IWEAKREFERENCESOURCE_H_ */
