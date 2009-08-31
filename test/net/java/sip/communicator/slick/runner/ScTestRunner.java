package net.java.sip.communicator.slick.runner;
import junit.framework.*;
import junit.runner.*;
import net.java.sip.communicator.util.*;

/**
 * A command line based tool to run tests.
 * <pre>
 * java junit.textui.TestRunner [-wait] TestCaseClass
 * </pre>
 * TestRunner expects the name of a TestCase class as argument.
 * If this class defines a static <tt>suite</tt> method it
 * will be invoked and the returned test is run. Otherwise all
 * the methods starting with "test" having no arguments are run.
 * <p>
 * When the wait command line argument is given TestRunner
 * waits until the users types RETURN.
 * <p>
 * TestRunner prints a trace as the tests are executed followed by a
 * summary at the end.
 */
public class ScTestRunner extends BaseTestRunner {

    private Logger logger = Logger.getLogger(ScTestRunner.class.getName());
    static private XmlFormatter testPrinter;

    /**
     * Constructs a TestRunner using the given ResultPrinter all the output
     */
    public ScTestRunner(XmlFormatter printer)
    {
        testPrinter = printer;
    }

    /**
     * Runs a suite extracted from a TestCase subclass.
     */
    static public void run(Class testClass, XmlFormatter fmtr)
    {
        run(new TestSuite(testClass), fmtr);
    }

    /**
     * Runs a single test and collects its results.
     * This method can be used to start a test run
     * from your program.
     * <pre>
     * public static void main (String[] args) {
     *     test.textui.TestRunner.run(suite());
     * }
     * </pre>
     */
    static public TestResult run(Test test, XmlFormatter printer)
    {
        ScTestRunner runner= new ScTestRunner(printer);
        return runner.doRun(test);
    }

    /**
     * Always use the StandardTestSuiteLoader. Overridden from
     * BaseTestRunner.
     */
    public TestSuiteLoader getLoader()
    {
        return new StandardTestSuiteLoader();
    }

    public void testFailed(int status, Test test, Throwable t)
    {
        logger.debug("test " + test.toString() + " failed.");
    }

    public void testStarted(String testName)
    {
        logger.debug("started testName"+testName);
    }

    public void testEnded(String testName)
    {
        logger.debug("ended testName"+testName);

    }

    /**
     * Creates the TestResult to be used for the test run.
     */
    protected TestResult createTestResult()
    {
        return new TestResult();
    }

    public TestResult doRun(Test test)
    {
        return doRun(test, false);
    }

    public TestResult doRun(Test suite, boolean wait)
    {
        TestResult result= new TestResult();
        result.addListener(testPrinter);
        testPrinter.startTestSuite(suite, System.getProperties());

        long startTime= System.currentTimeMillis();
        suite.run(result);
        long endTime= System.currentTimeMillis();
        long runTime= endTime-startTime;
        testPrinter.endTestSuite(suite, result.errorCount(), result.failureCount(), runTime);

        return result;
    }

    protected void pause(boolean wait)
    {
        if (!wait) return;
//        testPrinter.printWaitPrompt();
        try {
            System.in.read();
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Starts a test run. Analyzes the command line arguments
     * and runs the given test suite.
     */
    protected TestResult start(String args[])
        throws Exception
    {
        String testCase= "";
        boolean wait= false;

        for (int i= 0; i < args.length; i++)
        {
            if (args[i].equals("-wait"))
                wait= true;
            else if (args[i].equals("-c"))
                testCase= extractClassName(args[++i]);
            else if (args[i].equals("-v"))
                System.err.println("JUnit "+Version.id()+" by Kent Beck and Erich Gamma");
            else
                testCase= args[i];
        }

        if (testCase.equals(""))
            throw new Exception("Usage: TestRunner [-wait] testCaseName, where name is the name of the TestCase class");

        try {
            Test suite= getTest(testCase);
            return doRun(suite, wait);
        }
        catch(Exception e)
        {
            throw new Exception("Could not create and run test suite: "+e);
        }
    }

    protected void runFailed(String message)
    {
        System.err.println(message);
        System.exit(junit.textui.TestRunner.FAILURE_EXIT);
    }

//    public void setPrinter(ResultPrinter printer) {
//        fPrinter= printer;
//    }


}
