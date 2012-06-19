/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include <stdlib.h>

void * operator new (size_t size) { return ::malloc(size); }
void operator delete (void *ptr) { ::free(ptr); }
void *__cxa_pure_virtual = NULL;
