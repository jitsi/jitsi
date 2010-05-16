/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.configuration;

import java.beans.*;
import java.util.EventListener;

/*
 * This interface uses SC's own ProperteyVetoException.
 */
public interface ConfigVetoableChangeListener extends EventListener
{
  /**
   * Fired before a Bean's property changes.
   *
   * @param e the change (containing the old and new values)
   * @throws ConfigPropertyVetoException if the change is vetoed by the listener
   */
  void vetoableChange(PropertyChangeEvent e) throws ConfigPropertyVetoException;
} 
