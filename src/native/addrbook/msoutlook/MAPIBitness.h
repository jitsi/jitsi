/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPIBITNESS_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPIBITNESS_H_

/**
 * Checks the bitness of the Outlook installation and of the Jitsi executable.
 *
 * @author Vincent Lucas
 */

int MAPIBitness_getOutlookBitnessVersion(void);

int MAPIBitness_getOutlookVersion(void);

#endif
