/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _ORG_JITSI_WINDOWS_SETUP_BSPATCH_H_
#define _ORG_JITSI_WINDOWS_SETUP_BSPATCH_H_

#ifdef BSPATCH_API_STATIC
int bspatch_main(int argc, const char * argv[]);
#endif /* #ifdef BSPATCH_API_STATIC */

#endif /* #ifndef _ORG_JITSI_WINDOWS_SETUP_BSPATCH_H_ */
