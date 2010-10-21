/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef __G722_DECODER_H__
#define __G722_DECODER_H__

void g722_decoder_close(void *decoder);
void *g722_decoder_open();
void g722_decoder_process(
        void *decoder,
        unsigned short *input, short *output, int outputLength);

#endif /* __G722_DECODER_H__ */