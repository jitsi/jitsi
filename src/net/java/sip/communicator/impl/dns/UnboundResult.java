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
package net.java.sip.communicator.impl.dns;

/**
 * Class that contains the answer to query processed by the native Unbound
 * resolver. Corresponds to the <a
 * href="http://unbound.net/documentation/doxygen/structub__result.html"
 * >ub_result</a> data structure.
 *
 * The fields {@link #data} and {@link #canonname} are not filled.
 * <p>
 * The JavaDoc of these fields is directly copied from libunbound, licensed as
 * follows:
 * <p>
 * Copyright (c) 2007, NLnet Labs. All rights reserved.
 *
 * This software is open source.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the NLNET LABS nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * @author Ingo Bauersachs
 */
public class UnboundResult
{
    /**
     * The original question, name text string.
     */
    String qname;

    /**
     * the type asked for
     */
    int qtype;

    /**
     * the type asked for
     */
    int qclass;


    /**
     * a list of network order DNS rdata items, terminated with a NULL pointer,
     * so that data[0] is the first result entry, data[1] the second, and the
     * last entry is NULL.
     */
    byte[][] data;

    /**
     * canonical name for the result (the final cname).
     */
    String canonname;

    /**
     * DNS RCODE for the result.
     */
    int rcode;

    /**
     * The DNS answer packet.
     */
    byte[] answerPacket;


    /**
     * If there is any data, this is true.
     */
    boolean haveData;

    /**
     * If there was no data, and the domain did not exist, this is true.
     */
    boolean nxDomain;

    /**
     * True, if the result is validated securely.
     */
    boolean secure;

    /**
     * If the result was not secure ({@link #secure} == false), and this result
     * is due to a security failure, bogus is true.
     */
    boolean bogus;

    /**
     * If the result is bogus this contains a string (zero terminated) that
     * describes the failure.
     */
    String whyBogus;
}
