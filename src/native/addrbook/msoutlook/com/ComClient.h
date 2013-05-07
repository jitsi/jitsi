/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_COMCLIENT_H
#define __MSOUTLOOKADDRBOOKCOM_COMCLIENT_H

#include "IMsOutlookAddrBookServer.h"

/**
 * Starts and stops registration for the COM client.
 *
 * @author Vincent Lucas
 */
void ComClient_start(void);
void ComClient_stop(void);

IMsOutlookAddrBookServer * ComClient_getIServer();

#endif
