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
