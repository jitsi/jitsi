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
package net.java.sip.communicator.service.protocol;

/**
 * An Operation Set defining option
 * to unconditional auto answer incoming calls.
 *
 * @author Damian Minkov
 */
public interface OperationSetBasicAutoAnswer
    extends OperationSet
{
    /**
     * Auto answer unconditional account property.
     */
    public static final String AUTO_ANSWER_UNCOND_PROP =
        "AUTO_ANSWER_UNCONDITIONAL";

    /**
     * Auto answer video calls with video account property.
     */
    public static final String AUTO_ANSWER_WITH_VIDEO_PROP =
        "AUTO_ANSWER_WITH_VIDEO";

    /**
     * Sets the auto answer option to unconditionally answer all incoming calls.
     */
    public void setAutoAnswerUnconditional();

    /**
     * Is the auto answer option set to unconditionally
     * answer all incoming calls.
     * @return is auto answer set to unconditional.
     */
    public boolean isAutoAnswerUnconditionalSet();

    /**
     * Clear any previous settings.
     */
    public void clear();

    /**
     * Sets the auto answer with video to video calls.
     *
     * @param answerWithVideo A boolean set to true to activate the auto answer
     * with video when receiving a video call. False otherwise.
     */
    public void setAutoAnswerWithVideo(boolean answerWithVideo);

    /**
     * Returns if the auto answer with video to video calls is activated.
     *
     * @return A boolean set to true if the auto answer with video when
     * receiving a video call is activated. False otherwise.
     */
    public boolean isAutoAnswerWithVideoSet();

}
