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
package net.java.sip.communicator.slick.runner;

import java.io.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Detects and runs all Service Implementation Compatibility Kits (SLICKs)inside
 * the current OSGI instance. The SipCommunicatorSlickRunner produces an xml log
 * file following ant format rules (so that it could be used by CruiseControl)
 * and stores it inside the directory indicated in the
 * net.java.sip.communicator.slick.runner.OUTPUT_DIR property (default is
 * test-reports).
 * <p>
 * In order for the SipCommunicatorSlickRunner to detect all SLICKs they
 * needs to be registered as services in the OSGI environment prior to the
 * actication of the runner, and their names need to be specified in a
 * whitespace separated list registered against the
 * net.java.sip.communicator.slick.runner.TEST_LIST system property.
 * <p>
 * After running all unit tests the SipcCommunicatorSlickRunner will try to
 * gracefully shutdown the Felix OSGI framework (if it fails it'll shut it
 * down rudely ;) ) and will System.exit() with an error code in case any
 * test failures occurred or with 0 if all tests passed.
 *
 * @author Emil Ivov
 */
public class SipCommunicatorSlickRunner
    extends TestSuite implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The name of the property indicating the Directory where test reports
     * should be stored.
     */
    private static final String OUTPUT_DIR_PROPERTY_NAME =
        "net.java.sip.communicator.slick.runner.OUTPUT_DIR";

    /**
     * A default name for the Directory where test reports should be stored.
     */
    private static final String DEFAULT_OUTPUT_DIR = "test-reports";

    /**
     * The name of the property indicating the name of the file where test
     * reports should be stored.
     */
    private static final String OUTPUT_FILE_NAME =
        "sip-communicator.unit.test.reports.xml";

    /**
     * The name of the property that contains the list of Service ICKs that
     * we'd have to run.
     */
    private static final String TEST_LIST_PROPERTY_NAME =
        "net.java.sip.communicator.slick.runner.TEST_LIST";

    /**
     * A reference to the bundle context received when activating the test
     * runner.
     */
    private BundleContext bundleContext = null;

    /**
     * The number of failures and errors that occurred during unit testing.
     */
    private int errCount = 0;

    /**
     * The number of unit tests run by the slick runner.
     */
    private int runCount = 0;

    /**
     * Starts the slick runner, runs all unit tests indicated in the
     * TEST_LIST property, and exits with an error code corresponding to whether
     * or there were failure while running the tests.
     * @param bc BundleContext
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        logger.logEntry();
        try
        {
            bundleContext = bc;
            setName(getClass().getName());

            //Let's now see what tests have been scheduled for execution.
            String tests = System.getProperty(TEST_LIST_PROPERTY_NAME);

            if (tests == null || tests.trim().length() == 0)
            {
                tests = "";
            }

            logger.debug("specfied test list is: " + tests);

            StringTokenizer st = new StringTokenizer(tests);
            String[] ids = new String[st.countTokens()];

            int n = 0;
            while (st.hasMoreTokens())
            {
                ids[n++] = st.nextToken().trim();
            }

            //Determine the file specified for storing test results.
            String outputDirName = System.getProperty(OUTPUT_DIR_PROPERTY_NAME);

            if (outputDirName == null || outputDirName.trim().length() == 0)
            {
                outputDirName = DEFAULT_OUTPUT_DIR;
            }

            File outputDir = new File(outputDirName);
            if (!outputDir.exists())
            {
                outputDir.mkdirs();
            }

            for (int i = 0; i < ids.length; i++)
            {
                logger.info("=========== Running tests in : " + ids[i]
                          + " ===========");

                TestSuite slick = getTestSuite(bc, ids[i]);

                logger.debug("with " + slick.countTestCases() + " tests.");

                File outputFile =
                    new File(outputDir, "SC-TEST-" + ids[i] + ".xml");
                if (!outputFile.exists())
                {
                    outputFile.createNewFile();
                }

                logger.debug("specified reports file: "
                         + outputFile.getCanonicalFile());

                OutputStream out =
                    new FileOutputStream(outputFile);

                XmlFormatter fmtr = new XmlFormatter(new PrintStream(out));

                TestResult res = ScTestRunner.run(slick, fmtr);
                errCount += res.errorCount() + res.failureCount();
                runCount += res.runCount();

                out.flush();
                out.close();

            }
            //output results
            logger.info("");
            logger.info("====================================================");
            logger.info("We ran " + runCount
                        + " tests and encountered " + errCount
                        + " errors and failures.");
            logger.info("====================================================");
            logger.info("");

            //in order to shutdown felix we'd first need to wait for it to
            //complete it's start process, so we'll have to implement shutdown
            //in a framework listener.
            bc.addFrameworkListener(new FrameworkListener(){
            public void frameworkEvent(FrameworkEvent event){

                if( event.getType() == FrameworkEvent.STARTED)
                {
                    try
                    {
                        //first stop the system bundle thus causing oscar
                        //to stop all user bundles and shut down.
                        bundleContext.getBundle(0).stop();
                    }
                    catch (BundleException ex)
                    {
                        logger.error("Failed to gently shutdown Felix",ex);
                    }

                    //if everything is ok then the stop call shouldn't have
                    //exited the the program since we must have set the
                    //"felix.embedded.execution" property to true
                    //we could therefore now System.exit() with a code
                    //indicating whether or not all unit tests went wrong

                    // After updating to Felix 3.2.2, System.exit locks
                    // the tests and it never stop, so it has to be removed
                    // or in new thread.
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            System.exit(errCount > 0? -1: 0);
                        }
                    }).start();

                }
            }
            });
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Dummy impl
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        logger.debug("Stopping!");
    }

    /**
     * Looks through the osgi framework for a service with a "service.pid"
     * property set to <tt>id</tt>.
     * @param bc the BundleContext where the service is to be looked for.
     * @param id the value of the "service.pid" property for the specified
     * service.
     * @return a TestSuite service corresponding the specified <tt>id</tt>
     * or a junit TestCase impl wrapping an exception in case we failed to
     * retrieve the service for some reason.
     */
    public TestSuite getTestSuite(BundleContext bc,
                                  final String id)
    {
        Object obj = null;

        try
        {
            ServiceReference[] srl
                = bc.getServiceReferences(
                        (String) null,
                        "(service.pid=" + id + ")");

            if (srl == null || srl.length == 0)
            {
                obj = new TestCase("No id=" + id)
                {
                    @Override
                    public void runTest()
                    {
                        throw new IllegalArgumentException("No test with id=" +
                            id);
                    }
                };
            }
            if (srl != null && srl.length != 1)
            {
                obj = new TestCase("Multiple id=" + id)
                {
                    @Override
                    public void runTest()
                    {
                        throw new IllegalArgumentException(
                            "More than one test with id=" + id);
                    }
                };
            }
            if (obj == null)
            {
                obj = bc.getService(srl[0]);
            }
        }
        catch (Exception e)
        {
            obj = new TestCase("Bad filter syntax id=" + id)
            {
                @Override
                public void runTest()
                {
                    throw new IllegalArgumentException("Bad syntax id=" + id);
                }
            };
        }

        if (! (obj instanceof Test))
        {
            final Object oldObj = obj;
            obj = new TestCase("ClassCastException")
            {
                @Override
                public void runTest()
                {
                    throw new ClassCastException("Service implements " +
                                                 oldObj.getClass().getName() +
                                                 " instead of " +
                                                 Test.class.getName());
                }
            };
        }

        Test test = (Test) obj;

        TestSuite suite;

        if (test instanceof TestSuite)
        {
            suite = (TestSuite) test;
        }
        else
        {
            suite = new TestSuite(id);
            suite.addTest(test);
        }

        return suite;
    }
}
