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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP constraint-failure element. Indicates that the requested operation
 * would result in a document that failed a data constraint defined by the
 * application usage, but not enforced by the schema or a uniqueness constraint.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class ConstraintFailureType extends BaseXCapError
{
    /**
     * Creates the XCAP constraint-failure with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public ConstraintFailureType(String phrase)
    {
        super(phrase);
    }
}
