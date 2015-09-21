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
package net.java.sip.communicator.slick.protocol.icq;

import junit.framework.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Phoney tests to signal specific problems with the
 * accounts.properties file
 * @author Brian Burch
 */
public class TestAccountInvalidNotification extends TestCase
{
    ProtocolProviderFactory icqProviderFactory  = null;

    public TestAccountInvalidNotification(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * It is not meaningful to define a test suite. Each of the
     * pseudo-tests reports a different setup failure, so the
     * appropriate test should be added individually.
     * <p>
     * As a safety measure, we add an empty test suite which
     * will generate a "no tests found" failure.
     *
     * @return an empty test suite.
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        // will generate a jUnit "no tests found" error condition
        return suite;
    }

    /**
     * The icq test suites MUST have an accounts.properties file
     * that defines two icq test accounts. This test is ONLY
     * executed when icqProtocolProviderSlick.start() has failed
     * to load the Properties and it deliberately fails with a
     * meaningful message.
     */
    public void failIcqTesterAgentMissing()
    {
        fail("The IcqTesterAgent on icq was not defined. "
            +"Possible reasons: account.properties file not found "
            +"in lib directory. Please see wiki for advice on unit "
            +"test setup.");
    }

    /**
     * This test is ONLY executed when icqProtocolProviderSlick.start()
     * has failed to register with the icq service when providing
     * the username and password defined in the account.properties file.
     * It deliberately fails with a meaningful message.
     */
    public void failIcqTesterAgentRegisterRejected()
    {
        fail("Registering the IcqTesterAgent on icq has failed. "
            +"Possible reasons: authentification failure (wrong ICQ "
            +"account number, no password, wrong password), "
            +"or Connection rate limit exceeded.");
    }
}
