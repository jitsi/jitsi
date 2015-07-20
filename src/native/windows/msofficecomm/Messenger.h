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
#ifndef _JMSOFFICECOMM_MESSENGER_H_
#define _JMSOFFICECOMM_MESSENGER_H_

#include "DispatchImpl.h"
#include "DMessengerEventsConnectionPoint.h"
#include <msgrua.h>

/**
 * Implements the <tt>IMessenger</tt>, <tt>IMessenger2</tt>,
 * <tt>IMessenger3</tt>, <tt>IMessengerAdvanced</tt> and
 * <tt>IMessengerContactResolution</tt> interfaces.
 *
 * @author Lyubomir Marinov
 */
class Messenger
    : public DispatchImpl<IMessengerAdvanced, IID_IMessengerAdvanced>,
      public IConnectionPointContainer,
      public IMessengerContactResolution
{
public:
    static void CALLBACK onContactStatusChange(ULONG_PTR dwParam);
    static HRESULT start(JNIEnv *env);
    static HRESULT stop(JNIEnv *env);

    Messenger();

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID, PVOID *);
    STDMETHODIMP_(ULONG) AddRef(THIS) { return DispatchImpl::AddRef(); };
    STDMETHODIMP_(ULONG) Release(THIS) { return DispatchImpl::Release(); };

    // IDispatch
    STDMETHODIMP GetTypeInfoCount(UINT *pctinfo)
        { return DispatchImpl::GetTypeInfoCount(pctinfo); };
    STDMETHODIMP GetTypeInfo(UINT iTInfo, LCID lcid, LPTYPEINFO *ppTInfo)
        { return DispatchImpl::GetTypeInfo(iTInfo, lcid, ppTInfo); };
    STDMETHODIMP GetIDsOfNames(REFIID riid, LPOLESTR *rgszNames, UINT cNames, LCID lcid, DISPID *rgDispId)
        { return DispatchImpl::GetIDsOfNames(riid, rgszNames, cNames, lcid, rgDispId); };
    STDMETHODIMP Invoke(DISPID dispIdMember, REFIID riid, LCID lcid, WORD wFlags, DISPPARAMS *pDispParams, VARIANT *pVarResult, EXCEPINFO *pExcepInfo, UINT *puArgErr)
        { return DispatchImpl::Invoke(dispIdMember, riid, lcid, wFlags, pDispParams, pVarResult, pExcepInfo, puArgErr); };

    // IMessenger
    STDMETHODIMP get_Window(IDispatch **ppMWindow);
    STDMETHODIMP ViewProfile(VARIANT vContact);
    STDMETHODIMP get_ReceiveFileDirectory(BSTR *bstrPath);
    STDMETHODIMP StartVoice(VARIANT vContact, IDispatch **ppMWindow);
    STDMETHODIMP InviteApp(VARIANT vContact, BSTR bstrAppID, IDispatch **ppMWindow);
    STDMETHODIMP SendMail(VARIANT vContact);
    STDMETHODIMP OpenInbox();
    STDMETHODIMP SendFile(VARIANT vContact, BSTR bstrFileName, IDispatch **ppMWindow);
    STDMETHODIMP Signout();
    STDMETHODIMP Signin(long hwndParent, BSTR bstrSigninName, BSTR bstrPassword);
    STDMETHODIMP GetContact(BSTR bstrSigninName, BSTR bstrServiceId, IDispatch **ppMContact);
    STDMETHODIMP OptionsPages(long hwndParent, MOPTIONPAGE mOptionPage);
    STDMETHODIMP AddContact(long hwndParent, BSTR bstrEMail);
    STDMETHODIMP FindContact(long hwndParent, BSTR bstrFirstName, BSTR bstrLastName, VARIANT vbstrCity, VARIANT vbstrState, VARIANT vbstrCountry);
    STDMETHODIMP InstantMessage(VARIANT vContact, IDispatch **ppMWindow);
    STDMETHODIMP Phone(VARIANT vContact, MPHONE_TYPE ePhoneNumber, BSTR bstrNumber, IDispatch **ppMWindow);
    STDMETHODIMP MediaWizard(long hwndParent);
    STDMETHODIMP Page(VARIANT vContact, IDispatch **ppMWindow);
    STDMETHODIMP AutoSignin();
    STDMETHODIMP get_MyContacts(IDispatch **ppMContacts);
    STDMETHODIMP get_MySigninName(BSTR *pbstrName);
    STDMETHODIMP get_MyFriendlyName(BSTR *pbstrName);
    STDMETHODIMP put_MyStatus(MISTATUS mStatus);
    STDMETHODIMP get_MyStatus(MISTATUS *pmStatus);
    STDMETHODIMP get_UnreadEmailCount(MUAFOLDER mFolder, LONG *plCount);
    STDMETHODIMP get_MyServiceName(BSTR *pbstrServiceName);
    STDMETHODIMP get_MyPhoneNumber(MPHONE_TYPE PhoneType, BSTR *pbstrNumber);
    STDMETHODIMP get_MyProperty(MCONTACTPROPERTY ePropType, VARIANT *pvPropVal);
    STDMETHODIMP put_MyProperty(MCONTACTPROPERTY ePropType, VARIANT vPropVal);
    STDMETHODIMP get_MyServiceId(BSTR *pbstrServiceId);
    STDMETHODIMP get_Services(IDispatch **ppdispServices);

    // IMessenger2
    STDMETHODIMP get_ContactsSortOrder(MUASORT *pSort);
    STDMETHODIMP put_ContactsSortOrder(MUASORT Sort);
    STDMETHODIMP StartVideo(VARIANT vContact, IDispatch **ppMWindow);
    STDMETHODIMP get_MyGroups(IDispatch **ppMGroups);
    STDMETHODIMP CreateGroup(BSTR bstrName, VARIANT vService, IDispatch **ppGroup);

    // IMessenger3
    STDMETHODIMP get_Property(MMESSENGERPROPERTY ePropType, VARIANT *pvPropVal);
    STDMETHODIMP put_Property(MMESSENGERPROPERTY ePropType, VARIANT vPropVal);

    // IMessengerAdvanced
    STDMETHODIMP StartConversation(CONVERSATION_TYPE ConversationType, VARIANT vParticipants, VARIANT vContextualData, VARIANT vSubject, VARIANT vConversationIndex, VARIANT vConversationData, VARIANT *pvWndHnd);
    STDMETHODIMP GetAuthenticationInfo(BSTR *pbstrAuthInfo);

    // IConnectionPointContainer
    STDMETHODIMP EnumConnectionPoints(IEnumConnectionPoints **ppEnum);
    STDMETHODIMP FindConnectionPoint(REFIID riid,IConnectionPoint **ppCP);

    // IMessengerContactResolution
    STDMETHODIMP ResolveContact(ADDRESS_TYPE AddressType, CONTACT_RESOLUTION_TYPE ResolutionType, BSTR bstrAddress, BSTR *pbstrIMAddress);

protected:
    virtual ~Messenger();

private:
    static jclass                    _jclass;
    static jmethodID                 _jctorMethodID;
    static jmethodID                 _jstartConversationMethodID;
    static Messenger *               _singleton;

    HRESULT constructJobject();
    HRESULT createMessengerContact(BSTR signinName, REFIID iid, PVOID *obj);
    HRESULT destructJobject();
    HRESULT getMessengerContact(BSTR signinName, REFIID iid, PVOID *obj);
    HRESULT toString(JNIEnv *env, VARIANT &v, jstring *string);
    HRESULT toStringArray(JNIEnv *env, VARIANT &v, jobjectArray *stringArray);

    DMessengerEventsConnectionPoint *_dMessengerEventsConnectionPoint;
    jobject                          _jobject;
    size_t                           _messengerContactCount;
    IWeakReference **                _messengerContacts;
    IWeakReference *                 _myContacts;
    LPOLESTR                         _myServiceId;
    IWeakReference *                 _services;
};

#endif /* #ifndef _JMSOFFICECOMM_MESSENGER_H_ */
