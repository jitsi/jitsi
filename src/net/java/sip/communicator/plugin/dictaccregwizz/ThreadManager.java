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
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.util.*;

import net.java.dict4j.*;
import net.java.sip.communicator.util.*;

/**
 * Class manager the thread called for searching the strategies' list
 *
 * @author ROTH Damien
 */
public class ThreadManager
{
    protected static Logger logger = Logger.getLogger(ThreadManager.class);
    public static int NB_STEPS = 4;

    private StrategyThread thread = null;
    private FirstWizardPage wizard = null;

    /**
     * Create an instance of <tt>ThreadManager</tt>
     * @param wiz Wizard
     */
    public ThreadManager(FirstWizardPage wiz)
    {
        this.wizard = wiz;
    }

    /**
     * Submit a request to launch the thread
     * @param host Server host
     * @param port Server port
     * @return TRUE if the thread is started - FALSE otherwise
     */
    public boolean submitRequest(String host, int port)
    {
        if (this.thread != null)
        {
            return false;
        }

        this.thread = new StrategyThread(this.wizard, host, port);
        this.thread.start();

        return true;
    }

    /**
     * Stop the thread
     */
    public void cancel()
    {
        if (this.thread != null)
        {
            this.thread.interrupt();
            this.thread = null;
        }
    }

    /**
     * Wait for the searching thread to stop
     * @return true if success, false otherwise
     */
    public boolean waitThread()
    {
        if (this.thread == null)
        {
            return false;
        }

        try
        {
            this.thread.join();
        }
        catch (InterruptedException e)
        {
            if (logger.isInfoEnabled())
                logger.info(e);
            return false;
        }
        return true;
    }

    /**
     * Thread used to search the strategies
     *
     * @author ROTH Damien, LITZELMANN Cedric
     */
    static class StrategyThread
        extends Thread
    {
        private final FirstWizardPage wizard;
        private final String host;
        private final int port;

        /**
         * Informations messages
         */
        private String[] messages = new String[] {
                Resources.getString(
                    "plugin.dictaccregwizz.THREAD_CONNECT"),
                Resources.getString(
                    "plugin.dictaccregwizz.THREAD_CONNECT_FAILED"),
                Resources.getString(
                    "plugin.dictaccregwizz.RETRIEVING_STRATEGIES"),
                Resources.getString(
                    "plugin.dictaccregwizz.NO_STRATEGIES_FOUND"),
                Resources.getString(
                    "plugin.dictaccregwizz.POPULATE_LIST"),
                Resources.getString(
                    "plugin.dictaccregwizz.CLOSING_CONNECTION")
        };

        /**
         * Create an instance of the thread
         * @param wizard The wizard who started the thread
         * @param host Server host
         * @param port Server port
         */
        public StrategyThread(FirstWizardPage wizard, String host, int port)
        {
            this.wizard = wizard;
            this.host = host;
            this.port = port;
        }

        @Override
        public void run()
        {
            List<Strategy> strategies = null;

            DictConnection dictConnection = new DictConnection(host, port);

            // Open the connection to the server
            this.wizard.progressMessage(messages[0]);
            try
            {
                dictConnection.connect();
            }
            catch (DictException e)
            {
                this.wizard.strategiesSearchFailure(this.messages[1], e);
                return;
            }

            // Get the strategies
            this.wizard.progressMessage(messages[2]);
            try
            {
                strategies = dictConnection.getStrategies();
            }
            catch (DictException e)
            {
                this.wizard.strategiesSearchFailure(this.messages[3], e);
                return;
            }

            // Store the strategies
            this.wizard.progressMessage(messages[4]);
            this.wizard.setStrategies(strategies);
            this.wizard.autoSelectStrategy();

            // Close the connection
            this.wizard.progressMessage(messages[5]);
            try
            {
                dictConnection.close();
            }
            catch (DictException e)
            {
                // An error while closing the connection isn't very important
                // We just log it
                ThreadManager.logger.info("DICT search strategies thread : " +
                        "Error while closing connection", e);
            }

            // End of the search
            this.wizard.strategiesSearchComplete();
        }
    }
}
