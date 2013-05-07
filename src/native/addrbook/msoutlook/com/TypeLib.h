/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_TYPELIB_H
#define __MSOUTLOOKADDRBOOKCOM_TYPELIB_H

#include <objbase.h>

/**
 * Un/Register the typeLib for the COM server and client.
 *
 * @author Vincent Lucas
 */

LPTYPELIB TypeLib_loadRegTypeLib(WCHAR* path);
void TypeLib_releaseTypeLib(LPTYPELIB iTypeLib);

#endif
