/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * This class is based on code extracted from the ANT Project, property of the
 * Apache Software Foundation. It originally included the following license
 *
 * Copyright  2000-2004 The Apache Software Foundation
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

import javax.xml.parsers.*;

import junit.framework.*;
import junit.textui.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;


/**
 * Prints XML output of the test to a specified Writer.
 *
 * @author Emil Ivov
 * @see Element
 */

public class XmlFormatter extends ResultPrinter implements XMLConstants {

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    /**
     * The XML document.
     */
    private Document doc;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element rootElement;
    /**
     * Element for the current test.
     */
    private Hashtable<Test, Element> testElements = new Hashtable<Test, Element>();
    /**
     * tests that failed.
     */
    private Hashtable<Test, Test> failedTests = new Hashtable<Test, Test>();
    /**
     * Timing helper.
     */
    private Hashtable<Test, Long> testStarts = new Hashtable<Test, Long>();
    /**
     * Where to write the log to.
     */
    private OutputStream out;

    public XmlFormatter(PrintStream out)
    {
        super(out);
        setOutput(out);
    }

    public void setOutput(OutputStream out)
    {
        this.out = out;
    }

    public void setSystemOutput(String out)
    {
        formatOutput(SYSTEM_OUT, out);
    }

    public void setSystemError(String out)
    {
        formatOutput(SYSTEM_ERR, out);
    }

    /**
     * The whole testsuite started.
     */
    public void startTestSuite(Test suite, Properties props)
    {
        doc = getDocumentBuilder().newDocument();
        rootElement = doc.createElement(TESTSUITE);
        rootElement.setAttribute(ATTR_NAME, suite.toString());

        // Output properties
        Element propsElement = doc.createElement(PROPERTIES);
        rootElement.appendChild(propsElement);

        if (props != null)
        {
            Enumeration<?> e = props.propertyNames();
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                Element propElement = doc.createElement(PROPERTY);
                propElement.setAttribute(ATTR_NAME, name);
                propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
                propsElement.appendChild(propElement);
            }
        }
    }

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite(Test suite,
                             int err_count,
                             int fail_count,
                             long time)
        throws RuntimeException
    {
        rootElement.setAttribute(ATTR_TESTS, "" + suite.countTestCases());
        rootElement.setAttribute(ATTR_FAILURES, "" + fail_count);
        rootElement.setAttribute(ATTR_ERRORS, "" + err_count);
        rootElement.setAttribute(ATTR_TIME, "" + (time / 1000.0));
        rootElement.setAttribute(ATTR_PACKAGE, "SIP Communicator SLICK suites");
        if (out != null)
        {
            Writer wri = null;
            try {
                wri = new BufferedWriter(new OutputStreamWriter(out, "UTF8"));
                wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                (new DOMElementWriter()).write(rootElement, wri, 0, "  ");
                wri.flush();
            } catch (IOException exc)
            {
                throw new RuntimeException("Unable to write log file", exc);
            } finally {
                if (out != System.out && out != System.err)
                {
                    if (wri != null)
                    {
                        try {
                            wri.close();
                        } catch (IOException e)
                        {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    @Override
    public void startTest(Test test)
    {
        testStarts.put(test, new Long(System.currentTimeMillis()));
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    @Override
    public void endTest(Test test)
    {
        // Fix for bug #5637 - if a junit.extensions.TestSetup is
        // used and throws an exception during setUp then startTest
        // would never have been called
        if (!testStarts.containsKey(test))
        {
            startTest(test);
        }

        Element currentTest = null;
        if (!failedTests.containsKey(test))
        {
            currentTest = doc.createElement(TESTCASE);
            if(test instanceof TestCase)
            {
                String className = test.getClass().getName();
                className = className.substring(className.lastIndexOf(".") + 1);
                currentTest.setAttribute(ATTR_NAME, className
                                                    + "."
                                                    +((TestCase)test).getName());
            }
            else
                currentTest.setAttribute(ATTR_NAME, test.getClass().getName());
            // a TestSuite can contain Tests from multiple classes,
            // even tests with the same name - disambiguate them.
            currentTest.setAttribute(ATTR_CLASSNAME,
                                     test.getClass().getName());

            rootElement.appendChild(currentTest);
            testElements.put(test, currentTest);
        } else {
            currentTest = testElements.get(test);
        }

        Long l = testStarts.get(test);
        currentTest.setAttribute(ATTR_TIME,
            "" + ((System.currentTimeMillis() - l.longValue()) / 1000.0));
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t)
    {
        formatError(FAILURE, test, t);
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     */
    @Override
    public void addFailure(Test test, AssertionFailedError t)
    {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     */
    @Override
    public void addError(Test test, Throwable t)
    {
        formatError(ERROR, test, t);
    }

    private void formatError(String type, Test test, Throwable t)
    {
        if (test != null)
        {
            endTest(test);
            failedTests.put(test, test);
        }

        Element nested = doc.createElement(type);
        Element currentTest = null;
        if (test != null)
        {
            currentTest = testElements.get(test);
        } else {
            currentTest = rootElement;
        }

        currentTest.appendChild(nested);

        String message = t.getMessage();
        if (message != null && message.length() > 0)
        {
            nested.setAttribute(ATTR_MESSAGE, t.getMessage());
        }
        nested.setAttribute(ATTR_TYPE, t.getClass().getName());

        String strace = getStackTrace(t);
        Text trace = doc.createTextNode(strace);
        nested.appendChild(trace);
    }

    /**
     * Convenient method to retrieve the full stacktrace from a given exception.
     * @param t the exception to get the stacktrace from.
     * @return the stacktrace from the given exception.
     */
    public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    private void formatOutput(String type, String output)
    {
        Element nested = doc.createElement(type);
        rootElement.appendChild(nested);
        nested.appendChild(doc.createCDATASection(output));
    }

} // XMLJUnitResultFormatter
