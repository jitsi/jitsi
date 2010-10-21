/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef __G722_ENCODER_H__
#define __G722_ENCODER_H__

void g722_encoder_close(void *encoder);
void *g722_encoder_open();
void g722_encoder_process(
        void *encoder,
        short *input, unsigned short *output, int outputLength);

#endif /* __G722_ENCODER_H__ */
