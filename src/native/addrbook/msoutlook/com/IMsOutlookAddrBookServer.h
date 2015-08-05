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


 /* File created by MIDL compiler version 7.00.0555 */
/* at Tue Apr 01 12:24:24 2014
 */
/* Compiler settings for IMsOutlookAddrBookServer.idl:
    Oicf, W1, Zp8, env=Win32 (32b run), target_arch=X86 7.00.0555 
    protocol : dce , ms_ext, c_ext, robust
    error checks: allocation ref bounds_check enum stub_data 
    VC __declspec() decoration level: 
         __declspec(uuid()), __declspec(selectany), __declspec(novtable)
         DECLSPEC_UUID(), MIDL_INTERFACE()
*/
/* @@MIDL_FILE_HEADING(  ) */

//#pragma warning( disable: 4049 )  /* more than 64k source lines */


/* verify that the <rpcndr.h> version is high enough to compile this file*/
#ifndef __REQUIRED_RPCNDR_H_VERSION__
#define __REQUIRED_RPCNDR_H_VERSION__ 475
#endif

#include "rpc.h"
#include "rpcndr.h"

#ifndef __RPCNDR_H_VERSION__
#error this stub requires an updated version of <rpcndr.h>
#endif // __RPCNDR_H_VERSION__

#ifndef COM_NO_WINDOWS_H
#include "windows.h"
#include "ole2.h"
#endif /*COM_NO_WINDOWS_H*/

#ifndef __IMsOutlookAddrBookServer_h__
#define __IMsOutlookAddrBookServer_h__

#if defined(_MSC_VER) && (_MSC_VER >= 1020)
#pragma once
#endif

/* Forward Declarations */ 

#ifndef __IMsOutlookAddrBookServer_FWD_DEFINED__
#define __IMsOutlookAddrBookServer_FWD_DEFINED__
typedef interface IMsOutlookAddrBookServer IMsOutlookAddrBookServer;
#endif 	/* __IMsOutlookAddrBookServer_FWD_DEFINED__ */


#ifndef __IMsOutlookAddrBookServer_FWD_DEFINED__
#define __IMsOutlookAddrBookServer_FWD_DEFINED__
typedef interface IMsOutlookAddrBookServer IMsOutlookAddrBookServer;
#endif 	/* __IMsOutlookAddrBookServer_FWD_DEFINED__ */


/* header files for imported files */
#include "Unknwn.h"
#include "oaidl.h"

#ifdef __cplusplus
extern "C"{
#endif 


#ifndef __IMsOutlookAddrBookServer_INTERFACE_DEFINED__
#define __IMsOutlookAddrBookServer_INTERFACE_DEFINED__

/* interface IMsOutlookAddrBookServer */
/* [oleautomation][dual][uuid][object] */ 


EXTERN_C const IID IID_IMsOutlookAddrBookServer;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("5DDE9FF0-AC48-11E2-9E96-0800200C9A66")
    IMsOutlookAddrBookServer : public IUnknown
    {
    public:
        virtual HRESULT STDMETHODCALLTYPE foreachMailUser( 
            /* [in] */ BSTR query,
            /* [in] */ long callback) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE getAllCalendarItems( 
            /* [in] */ long callback) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE IMAPIProp_GetProps( 
            /* [in] */ BSTR entryId,
            /* [in] */ int nbPropIds,
            /* [in] */ SAFEARRAY * propIds,
            /* [in] */ long flags,
            /* [in] */ GUID UUID_Address,
            /* [out] */ SAFEARRAY * *props,
            /* [out] */ SAFEARRAY * *propsLength,
            /* [out] */ SAFEARRAY * *propsType) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE createContact( 
            /* [out] */ BSTR *id) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE deleteContact( 
            /* [in] */ BSTR id) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE IMAPIProp_DeleteProp( 
            /* [in] */ long propId,
            /* [in] */ BSTR entryId) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE IMAPIProp_SetPropString( 
            /* [in] */ long propId,
            /* [in] */ BSTR value,
            /* [in] */ BSTR entryId) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE compareEntryIds( 
            /* [in] */ BSTR id1,
            /* [in] */ BSTR id2,
            /* [out] */ int *result) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMsOutlookAddrBookServerVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMsOutlookAddrBookServer * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMsOutlookAddrBookServer * This);
        
        HRESULT ( STDMETHODCALLTYPE *foreachMailUser )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ BSTR query,
            /* [in] */ long callback);
        
        HRESULT ( STDMETHODCALLTYPE *getAllCalendarItems )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ long callback);
        
        HRESULT ( STDMETHODCALLTYPE *IMAPIProp_GetProps )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ BSTR entryId,
            /* [in] */ int nbPropIds,
            /* [in] */ SAFEARRAY * propIds,
            /* [in] */ long flags,
            /* [in] */ GUID UUID_Address,
            /* [out] */ SAFEARRAY * *props,
            /* [out] */ SAFEARRAY * *propsLength,
            /* [out] */ SAFEARRAY * *propsType);
        
        HRESULT ( STDMETHODCALLTYPE *createContact )( 
            IMsOutlookAddrBookServer * This,
            /* [out] */ BSTR *id);
        
        HRESULT ( STDMETHODCALLTYPE *deleteContact )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ BSTR id);
        
        HRESULT ( STDMETHODCALLTYPE *IMAPIProp_DeleteProp )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ long propId,
            /* [in] */ BSTR entryId);
        
        HRESULT ( STDMETHODCALLTYPE *IMAPIProp_SetPropString )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ long propId,
            /* [in] */ BSTR value,
            /* [in] */ BSTR entryId);
        
        HRESULT ( STDMETHODCALLTYPE *compareEntryIds )( 
            IMsOutlookAddrBookServer * This,
            /* [in] */ BSTR id1,
            /* [in] */ BSTR id2,
            /* [out] */ int *result);
        
        END_INTERFACE
    } IMsOutlookAddrBookServerVtbl;

    interface IMsOutlookAddrBookServer
    {
        CONST_VTBL struct IMsOutlookAddrBookServerVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMsOutlookAddrBookServer_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMsOutlookAddrBookServer_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMsOutlookAddrBookServer_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMsOutlookAddrBookServer_foreachMailUser(This,query,callback)	\
    ( (This)->lpVtbl -> foreachMailUser(This,query,callback) ) 

#define IMsOutlookAddrBookServer_getAllCalendarItems(This,callback)	\
    ( (This)->lpVtbl -> getAllCalendarItems(This,callback) ) 

#define IMsOutlookAddrBookServer_IMAPIProp_GetProps(This,entryId,nbPropIds,propIds,flags,UUID_Address,props,propsLength,propsType)	\
    ( (This)->lpVtbl -> IMAPIProp_GetProps(This,entryId,nbPropIds,propIds,flags,UUID_Address,props,propsLength,propsType) ) 

#define IMsOutlookAddrBookServer_createContact(This,id)	\
    ( (This)->lpVtbl -> createContact(This,id) ) 

#define IMsOutlookAddrBookServer_deleteContact(This,id)	\
    ( (This)->lpVtbl -> deleteContact(This,id) ) 

#define IMsOutlookAddrBookServer_IMAPIProp_DeleteProp(This,propId,entryId)	\
    ( (This)->lpVtbl -> IMAPIProp_DeleteProp(This,propId,entryId) ) 

#define IMsOutlookAddrBookServer_IMAPIProp_SetPropString(This,propId,value,entryId)	\
    ( (This)->lpVtbl -> IMAPIProp_SetPropString(This,propId,value,entryId) ) 

#define IMsOutlookAddrBookServer_compareEntryIds(This,id1,id2,result)	\
    ( (This)->lpVtbl -> compareEntryIds(This,id1,id2,result) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMsOutlookAddrBookServer_INTERFACE_DEFINED__ */



#ifndef __IMsOutlookAddrBookServerTypeLib_LIBRARY_DEFINED__
#define __IMsOutlookAddrBookServerTypeLib_LIBRARY_DEFINED__

/* library IMsOutlookAddrBookServerTypeLib */
/* [helpstring][version][uuid] */ 



EXTERN_C const IID LIBID_IMsOutlookAddrBookServerTypeLib;
#endif /* __IMsOutlookAddrBookServerTypeLib_LIBRARY_DEFINED__ */

/* Additional Prototypes for ALL interfaces */

unsigned long             __RPC_USER  BSTR_UserSize(     unsigned long *, unsigned long            , BSTR * ); 
unsigned char * __RPC_USER  BSTR_UserMarshal(  unsigned long *, unsigned char *, BSTR * ); 
unsigned char * __RPC_USER  BSTR_UserUnmarshal(unsigned long *, unsigned char *, BSTR * ); 
void                      __RPC_USER  BSTR_UserFree(     unsigned long *, BSTR * ); 

unsigned long             __RPC_USER  LPSAFEARRAY_UserSize(     unsigned long *, unsigned long            , LPSAFEARRAY * ); 
unsigned char * __RPC_USER  LPSAFEARRAY_UserMarshal(  unsigned long *, unsigned char *, LPSAFEARRAY * ); 
unsigned char * __RPC_USER  LPSAFEARRAY_UserUnmarshal(unsigned long *, unsigned char *, LPSAFEARRAY * ); 
void                      __RPC_USER  LPSAFEARRAY_UserFree(     unsigned long *, LPSAFEARRAY * ); 

/* end of Additional Prototypes */

#ifdef __cplusplus
}
#endif

#endif


