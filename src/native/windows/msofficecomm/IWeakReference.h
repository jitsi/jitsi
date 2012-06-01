/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_IWEAKREFERENCE_H_
#define _JMSOFFICECOMM_IWEAKREFERENCE_H_

#include <unknwn.h>

EXTERN_C const IID IID_IWeakReference;

#undef INTERFACE /* Silence a possible redefinition warning. */
#define INTERFACE IWeakReference
DECLARE_INTERFACE_(IWeakReference,IUnknown)
{
    STDMETHOD(QueryInterface)(THIS_ REFIID, PVOID *) PURE;
    STDMETHOD_(ULONG,AddRef)(THIS) PURE;
    STDMETHOD_(ULONG,Release)(THIS) PURE;
    STDMETHOD(Resolve)(THIS_ REFIID, PVOID *) PURE;
};
#undef INTERFACE

#endif /* #ifndef _JMSOFFICECOMM_IWEAKREFERENCE_H_ */
