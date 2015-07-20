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
 * Wrapper for the JUnbound JNI wrapper.
 * <p>
 * The JavaDoc of these methods is directly copied from libunbound, licensed as
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
 *
 * @author Ingo Bauersachs
 */
public class UnboundApi
{
    private static boolean isAvailable;
    private static final Object syncRoot = new Object();

    static
    {
        tryLoadUnbound();
    }

    /**
     * Attempts to load the Unbound native library. When successful,
     * {@link #isAvailable()} returns true.
     */
    public static void tryLoadUnbound()
    {
        synchronized(syncRoot)
        {
            try
            {
                System.loadLibrary("junbound");
                isAvailable = true;
            }
            catch(UnsatisfiedLinkError e)
            {
                isAvailable = false;
            }
        }
    }

    /**
     * Indicates whether the Unbound library is loaded.
     * @return True when the JNI wrapper could be loaded, false otherwise.
     */
    public static boolean isAvailable()
    {
        return isAvailable;
    }

    /**
     * Set debug verbosity for the context. Output is directed to stderr. Higher
     * debug level gives more output.
     *
     * @param context context.
     * @param level The debug level.
     */
    public static native void setDebugLevel(long context, int level);

    /**
     * Create a resolving and validation context.
     * @return a new context. default initialization. returns NULL on error.
     */
    public static native long createContext();

    /**
     * Destroy a validation context and free all its resources. Outstanding
     * async queries are killed and callbacks are not called for them.
     *
     * @param context context to delete
     */
    public static native void deleteContext(long context);

    /**
     * Set machine to forward DNS queries to, the caching resolver to use.
     * <p>
     * IP4 or IP6 address. Forwards all DNS requests to that machine, which is
     * expected to run a recursive resolver. If the proxy is not DNSSEC-capable,
     * validation may fail. Can be called several times, in that case the
     * addresses are used as backup servers.
     *
     * @param context context. At this time it is only possible to set
     *            configuration before the first resolve is done.
     * @param server address, IP4 or IP6 in string format. If the server is
     *            NULL, forwarding is disabled.
     */
    public static native void setForwarder(long context, String server);

    /**
     * Add a trust anchor to the given context.
     * <p>
     * The trust anchor is a string, on one line, that holds a valid DNSKEY or
     * DS RR.
     *
     * @param context context. At this time it is only possible to add trusted
     *            keys before the first resolve is done.
     * @param anchor string, with zone-format RR on one line. [domainname] [TTL
     *            optional] [type] [class optional] [rdata contents]
     */
    public static native void addTrustAnchor(long context, String anchor);

    /**
     * Perform resolution and validation of the target name.
     *
     * @param context context. The context is finalized, and can no longer
     *            accept config changes.
     * @param name domain name in text format (a zero terminated text string).
     * @param rrtype type of RR in host order, 1 is A (address).
     * @param rrclass class of RR in host order, 1 is IN (for internet).
     * @return the result data is returned in a newly allocated result
     *         structure. May be NULL on return, return value is set to an error
     *         in that case (out of memory).
     * @throws UnboundException when an error occurred.
     */
    public static native UnboundResult resolve(long context, String name,
        int rrtype, int rrclass) throws UnboundException;

    /**
     * Perform resolution and validation of the target name.
     * <p>
     * Asynchronous, after a while, the callback will be called with your data
     * and the result.
     *
     * @param context context. If no thread or process has been created yet to
     *            perform the work in the background, it is created now. The
     *            context is finalized, and can no longer accept config changes.
     * @param name domain name in text format (a string).
     * @param rrtype type of RR in host order, 1 is A.
     * @param rrclass class of RR in host order, 1 is IN (for internet).
     * @param data this data is your own data (you can pass null), and is passed
     *            on to the callback function.
     * @param cb this is called on completion of the resolution.
     * @return an identifier number is returned for the query as it is in
     *         progress. It can be used to cancel the query.
     * @throws UnboundException when an error occurred.
     */
    public static native int resolveAsync(long context, String name,
        int rrtype, int rrclass, Object data, UnboundCallback cb)
        throws UnboundException;

    /**
     * Cancel an async query in progress. Its callback will not be called.
     *
     * @param context context.
     * @param asyncId which query to cancel.
     * @throws UnboundException This routine can error if the async_id passed
     *             does not exist or has already been delivered. If another
     *             thread is processing results at the same time, the result may
     *             be delivered at the same time and the cancel fails with an
     *             error. Also the cancel can fail due to a system error, no
     *             memory or socket failures.
     */
    public static native void cancelAsync(long context, int asyncId)
        throws UnboundException;

    /**
     * Convert error value to a human readable string.
     *
     * @param code error code from one of the Unbound functions.
     * @return text string of the error code.
     */
    public static native String errorCodeToString(int code);

    /**
     * Wait for a context to finish with results. Call this routine to continue
     * processing results from the validating resolver. After the wait, there
     * are no more outstanding asynchronous queries.
     *
     * @param context context.
     * @throws UnboundException when an error occurred.
     */
    public static native void processAsync(long context)
        throws UnboundException;

    /**
     * Interface for the async resolve callback.
     */
    public interface UnboundCallback
    {
        /**
         * Called on completion of the async resolution.
         *
         * @param data the same object as passed to
         *            {@link UnboundApi#resolveAsync(long, String, int, int,
         *             Object, UnboundCallback)}
         * @param err 0 when a result has been found, an Unbound error code
         *            otherwise
         * @param result a newly allocated result structure. The result may be
         *            null, in that case err is set.
         */
        public void UnboundResolveCallback(Object data, int err,
            UnboundResult result);
    }
}
