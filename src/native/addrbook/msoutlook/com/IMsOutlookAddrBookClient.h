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
/* at Thu May 08 16:38:42 2014
 */
/* Compiler settings for IMsOutlookAddrBookClient.idl:
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

#ifndef __IMsOutlookAddrBookClient_h__
#define __IMsOutlookAddrBookClient_h__

#if defined(_MSC_VER) && (_MSC_VER >= 1020)
#pragma once
#endif

/* Forward Declarations */ 

#ifndef __IMsOutlookAddrBookClient_FWD_DEFINED__
#define __IMsOutlookAddrBookClient_FWD_DEFINED__
typedef interface IMsOutlookAddrBookClient IMsOutlookAddrBookClient;
#endif 	/* __IMsOutlookAddrBookClient_FWD_DEFINED__ */


#ifndef __IMsOutlookAddrBookClient_FWD_DEFINED__
#define __IMsOutlookAddrBookClient_FWD_DEFINED__
typedef interface IMsOutlookAddrBookClient IMsOutlookAddrBookClient;
#endif 	/* __IMsOutlookAddrBookClient_FWD_DEFINED__ */


/* header files for imported files */
#include "Unknwn.h"
#include "oaidl.h"

#ifdef __cplusplus
extern "C"{
#endif 


#ifndef __IMsOutlookAddrBookClient_INTERFACE_DEFINED__
#define __IMsOutlookAddrBookClient_INTERFACE_DEFINED__

/* interface IMsOutlookAddrBookClient */
/* [oleautomation][dual][uuid][object] */ 


EXTERN_C const IID IID_IMsOutlookAddrBookClient;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D579E840-B1A6-11E2-9E96-0800200C9A66")
    IMsOutlookAddrBookClient : public IUnknown
    {
    public:
        virtual HRESULT STDMETHODCALLTYPE foreachMailUserCallback( 
            /* [in] */ BSTR id,
            /* [in] */ long callback) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE foreachCalendarItemCallback( 
            /* [in] */ BSTR id,
            /* [in] */ long callback) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE deleted( 
            /* [in] */ BSTR id,
            /* [in] */ ULONG type) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE inserted( 
            /* [in] */ BSTR id,
            /* [in] */ ULONG type) = 0;
        
        virtual HRESULT STDMETHODCALLTYPE updated( 
            /* [in] */ BSTR id,
            /* [in] */ ULONG type) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMsOutlookAddrBookClientVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMsOutlookAddrBookClient * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMsOutlookAddrBookClient * This);
        
        HRESULT ( STDMETHODCALLTYPE *foreachMailUserCallback )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ BSTR id,
            /* [in] */ long callback);
        
        HRESULT ( STDMETHODCALLTYPE *foreachCalendarItemCallback )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ BSTR id,
            /* [in] */ long callback);
        
        HRESULT ( STDMETHODCALLTYPE *deleted )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ BSTR id,
            /* [in] */ ULONG type);
        
        HRESULT ( STDMETHODCALLTYPE *inserted )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ BSTR id,
            /* [in] */ ULONG type);
        
        HRESULT ( STDMETHODCALLTYPE *updated )( 
            IMsOutlookAddrBookClient * This,
            /* [in] */ BSTR id,
            /* [in] */ ULONG type);
        
        END_INTERFACE
    } IMsOutlookAddrBookClientVtbl;

    interface IMsOutlookAddrBookClient
    {
        CONST_VTBL struct IMsOutlookAddrBookClientVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMsOutlookAddrBookClient_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMsOutlookAddrBookClient_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMsOutlookAddrBookClient_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMsOutlookAddrBookClient_foreachMailUserCallback(This,id,callback)	\
    ( (This)->lpVtbl -> foreachMailUserCallback(This,id,callback) ) 

#define IMsOutlookAddrBookClient_foreachCalendarItemCallback(This,id,callback)	\
    ( (This)->lpVtbl -> foreachCalendarItemCallback(This,id,callback) ) 

#define IMsOutlookAddrBookClient_deleted(This,id,type)	\
    ( (This)->lpVtbl -> deleted(This,id,type) ) 

#define IMsOutlookAddrBookClient_inserted(This,id,type)	\
    ( (This)->lpVtbl -> inserted(This,id,type) ) 

#define IMsOutlookAddrBookClient_updated(This,id,type)	\
    ( (This)->lpVtbl -> updated(This,id,type) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMsOutlookAddrBookClient_INTERFACE_DEFINED__ */



#ifndef __IMsOutlookAddrBookClientTypeLib_LIBRARY_DEFINED__
#define __IMsOutlookAddrBookClientTypeLib_LIBRARY_DEFINED__

/* library IMsOutlookAddrBookClientTypeLib */
/* [helpstring][version][uuid] */ 



EXTERN_C const IID LIBID_IMsOutlookAddrBookClientTypeLib;
#endif /* __IMsOutlookAddrBookClientTypeLib_LIBRARY_DEFINED__ */

/* Additional Prototypes for ALL interfaces */

unsigned long             __RPC_USER  BSTR_UserSize(     unsigned long *, unsigned long            , BSTR * ); 
unsigned char * __RPC_USER  BSTR_UserMarshal(  unsigned long *, unsigned char *, BSTR * ); 
unsigned char * __RPC_USER  BSTR_UserUnmarshal(unsigned long *, unsigned char *, BSTR * ); 
void                      __RPC_USER  BSTR_UserFree(     unsigned long *, BSTR * ); 

/* end of Additional Prototypes */

#ifdef __cplusplus
}
#endif

#endif


