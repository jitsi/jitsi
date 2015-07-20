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

/* @(#)net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream.c

 Author: lorchat
 Created : 21 Nov 2006

 */

#include <stdio.h>
#include <stdlib.h>

#include <alsa/asoundlib.h>

#include "net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream.h"

snd_pcm_t *capture;
snd_pcm_uframes_t buffersize = 160;
snd_pcm_uframes_t periodsize = 80;

/**
   Init the alsa sub-system

   TODO: add support for on-demand sample size adjustment and different bitrates

 * Class:     net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream
 * Method:    jni_alsa_init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream_jni_1alsa_1init
  (JNIEnv *env, jobject obj)
{
  int err;
  
  snd_pcm_hw_params_t *capture_params;
  
  char *device = "plughw:0,0";
  
  int rate = 8000;
  int exact_rate;
    
  printf("init called\n");

  if ((err = snd_pcm_open(&capture, device, SND_PCM_STREAM_CAPTURE, 0)) < 0) {
    fprintf(stderr, "alsa: can not open device %s for capture\n", device);
    return;
  }

  snd_pcm_hw_params_alloca(&capture_params);

  /* Init hwparams with full configuration space */
  if (snd_pcm_hw_params_any(capture, capture_params) < 0) {
    fprintf(stderr, "Can not configure this PCM device.\n");
    return;
  }
  
  if (snd_pcm_hw_params_set_access(capture, capture_params, SND_PCM_ACCESS_RW_INTERLEAVED) < 0) {
    fprintf(stderr, "Error setting access.\n");
    return;
  }
  
  /* Set sample format */
  if (snd_pcm_hw_params_set_format(capture, capture_params, SND_PCM_FORMAT_S16_LE) < 0) {
    fprintf(stderr, "Error setting format.\n");
    return;
  }
  
  /* Set sample rate. If the exact rate is not supported */
  /* by the hardware, use nearest possible rate.         */ 
  exact_rate = rate;
  if (snd_pcm_hw_params_set_rate_near(capture, capture_params, &exact_rate, 0) < 0) {
    fprintf(stderr, "Error setting rate.\n");
    snd_pcm_close(capture);
    return;
  }
  if (rate != exact_rate) {
    fprintf(stderr, "alsa: The rate %d Hz is not supported by your hardware.\n", rate); 
    snd_pcm_close(capture);
    return;
  }
  
  /* Set number of channels */
  if (snd_pcm_hw_params_set_channels(capture, capture_params, 1) < 0) {
    fprintf(stderr, "Error setting channels.\n");
    snd_pcm_close(capture);
    return;
  }

    /* Set number of periods. Periods used to be called fragments. */ 
  if (snd_pcm_hw_params_set_buffer_size_near(capture, capture_params, &buffersize) < 0) {
    fprintf(stderr, "Error setting buffer_size\n");
    snd_pcm_close(capture);
    return;
  }
	
  periodsize = buffersize / 2;
  if (snd_pcm_hw_params_set_period_size_near(capture, capture_params, &periodsize, 0) < 0) {
    fprintf(stderr, "Error setting period size\n");
    snd_pcm_close(capture);
    return;
  }

  if (snd_pcm_hw_params(capture, capture_params) < 0) {
    fprintf(stderr, "can not commit hw parameters\n");
    snd_pcm_close(capture);
    return;
  }

  printf("Rate is %d, buffer size is %d and period size is %d\n", rate, buffersize, periodsize);

  return;
}

/**
 * Read data from the ALSA device. This is a blocking call.
 *
 * Class:     net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream
 * Method:    jni_alsa_read
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream_jni_1alsa_1read
  (JNIEnv *env, jobject obj, jbyteArray arr)
{
  int err;
  signed short capture_data[80];

  while ((err = snd_pcm_readi(capture, (void *) capture_data, periodsize)) < 0) {
    printf("prepare(%d), ", snd_pcm_prepare(capture));
    fprintf(stderr, "%d.", err);
  }

  printf("."); fflush(stdout);

  //  printf("[%d]", (*env)->GetArrayLength(env, arr));
  (*env)->SetByteArrayRegion(env, arr, 0, 160, (const jbyte *) capture_data);
  
  return;
}

/**
 * Free the ALSA-related stuff and close the devices, to be implemented
 *
 * Class:     net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream
 * Method:    jni_alsa_delete
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream_jni_1alsa_1delete
  (JNIEnv *env, jobject obj)
{
  printf("killing buffer\n");
  return;
}
