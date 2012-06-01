/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
