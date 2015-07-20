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
#include "DMessengerEventsConnectionPoint.h"

#include <msgruaid.h>

STDMETHODIMP
DMessengerEventsConnectionPoint::OnContactStatusChange
    (LPDISPATCH pMContact, MISTATUS mStatus)
{
    const UINT argc = 2;
    VARIANTARG argv[argc];

    for (UINT i = 0; i < argc; i++)
        ::VariantInit(argv + i);
    argv[1].vt = VT_DISPATCH;
    argv[1].pdispVal = pMContact;
    argv[0].vt = VT_I4;
    argv[0].lVal = (LONG) mStatus;

    DISPPARAMS dispParams;

    ::ZeroMemory(&dispParams, sizeof(DISPPARAMS));
    dispParams.cArgs = argc;
    dispParams.rgvarg = argv;

    return Invoke(DISPID_MUAE_ONUSERSTATECHANGE, &dispParams);
}
