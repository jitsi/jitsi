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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Panel showing the current status of the search of the strategies
 *
 * @author ROTH Damien
 */
public class ProgressPanel
    extends TransparentPanel
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JPanel rightPanel;

    private JLabel messageLabel;
    private JLabel progressLabel;
    private JButton cancelButton;

    private int currentStep;
    private int totalSteps;

    private boolean isBuild;

    private ThreadManager searchThread;

    /**
     * Create an instance of <tt>ProgressPanel</tt>
     * @param searchThread The thread manager
     */
    public ProgressPanel(ThreadManager searchThread)
    {
        super(new BorderLayout());

        // Element creation
        this.messageLabel = new JLabel(" ");
        this.progressLabel = new JLabel(" ");
        this.cancelButton
            = new JButton(Resources.getString("service.gui.CANCEL"));
        this.cancelButton.addActionListener(this);

        // Right panel init
        this.rightPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        this.rightPanel.add(this.progressLabel);
        this.rightPanel.add(this.cancelButton);

        this.searchThread = searchThread;

        init();
        this.totalSteps = ThreadManager.NB_STEPS;
    }

    /**
     * Init the values
     */
    private void init()
    {
        this.isBuild = false;
        this.currentStep = 1;

        this.add(this.messageLabel, BorderLayout.CENTER);
    }

    /**
     * Build the UI
     */
    private void build()
    {
        if (this.isBuild)
        {
            return;
        }

        this.add(this.messageLabel, BorderLayout.CENTER);
        this.add(this.rightPanel, BorderLayout.EAST);

        this.isBuild = true;
    }

    /**
     * Move to the next step without updating the message
     */
    public void nextStep()
    {
        nextStep(this.messageLabel.getText());
    }

    /**
     * Mode to the next step with a new message
     * @param message Message
     */
    public void nextStep(String message)
    {
        if (this.currentStep > this.totalSteps)
        {
            finish();
        }

        build();
        this.messageLabel.setText(message);
        this.progressLabel.setText(currentStep + "/" + totalSteps);

        this.currentStep++;
    }

    /**
     * Informs the end of the progress. Remove all the components and
     * reset the values
     */
    public void finish()
    {
        // Remove all elements
        this.removeAll();

        // Re-init the panel
        this.messageLabel.setText(" ");
        this.progressLabel.setText(" ");
        init();

        this.repaint();
        this.validate();
    }

    public void actionPerformed(ActionEvent arg0)
    {
        this.searchThread.cancel();
        this.finish();
    }
}
