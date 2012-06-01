/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
