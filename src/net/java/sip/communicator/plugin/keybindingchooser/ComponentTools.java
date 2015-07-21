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
package net.java.sip.communicator.plugin.keybindingchooser;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Common tools for Swing gui components.
 * @author Damian Johnson (atagar1@gmail.com)
 * @version September 1, 2007
 */
public class ComponentTools {
  /**
   * Applies a highly customized visual scheme based on the reference color. Background is of a
   * gradient hue (similar to the Java 6 default ocean theme) and uses a raised bevel border. If
   * designated as being pressed then this uses a lowered bevel border and the background darkens,
   * unless the background is already very dark, in which case it will lighten. Unfortunately Swing
   * doesn't directly support setting background gradients, requiring that the component's paint
   * method is overridden. For instance, to apply this to a JButton:<br>
   * <code>
   * JButton button = new JButton(text) {
   *   private static final long serialVersionUID = 0;
   *
   *   public void paintComponent(Graphics g) {
   *     Graphics2D g2 = (Graphics2D) g;
   *     g2.setPaint(applyThemedScheme(this, reference, getModel().isArmed()));
   *     g2.fillRect(0, 0, getWidth(), getHeight()); // Draws gradient background
   *     super.paintComponent(g); // Draws button content
   *   }
   * };
   * button.setContentAreaFilled(false); // Disables default background
   * </code>
   * @param component component to which custom foreground and border is applied, if null then these
   *        attributes aren't applied
   * @param reference color on which background gradient and border are based
   * @param isPressed determines if toggled scheme is applied for components that can be pressed
   * @return component background gradient
   */
  public static GradientPaint applyThemedScheme(JComponent component, Color reference,
      boolean isPressed) {
    int r = reference.getRed();
    int g = reference.getGreen();
    int b = reference.getBlue();
    Color lightened = new Color(getValidRgb(r + 75), getValidRgb(g + 75), getValidRgb(b + 75));
    Color darkened = new Color(getValidRgb(r - 75), getValidRgb(g - 75), getValidRgb(b - 75));
    boolean isVeryDark = (r + g + b) / 3 < 25; // If contrast should be provided by lightening

    if (isPressed) {
      if (isVeryDark) {
        reference = reference.brighter();
        lightened = lightened.brighter();
        darkened = darkened.brighter();
      } else {
        reference = reference.darker();
        lightened = lightened.darker();
        darkened = darkened.darker();
      }
    }

    if (component != null) {
      int borderType = !isPressed ? BevelBorder.RAISED : BevelBorder.LOWERED;
      Border border = BorderFactory.createBevelBorder(borderType, lightened, darkened);
      component.setBorder(border);

      Color foreground = isVeryDark ? new Color(224, 224, 224) : Color.BLACK;
      component.setForeground(foreground);
    }

    Point p1 = new Point(0, 20);
    Point p2 = new Point(0, 5);
    return new GradientPaint(p1, reference, p2, lightened, false);
  }

  /**
   * Provides a visually customized button utilizing the applyThemedScheme method that will update
   * its theme accordingly when pressed. Any content is anti-aliased.
   * @param text message displayed by the button
   * @param reference color on which background gradient and border are based
   * @return button with a color scheme matching the provided color
   */
  public static JButton makeThemedButton(String text, final Color reference) {
    JButton button = new JButton(text) {
      private static final long serialVersionUID = 0;

      @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(applyThemedScheme(this, reference, getModel().isArmed()));
        g2.fillRect(0, 0, getWidth(), getHeight()); // Draws gradient background
        super.paintComponent(g); // Draws button content
      }
    };
    button.setContentAreaFilled(false); // Disables default background
    return button;
  }

  /**
   * Centers a window within the center of the screen.
   * @param mover window to be centered
   */
  public static void center(Window mover) {
    Dimension moverSize = mover.getPreferredSize();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screenSize.width - moverSize.width) / 2;
    int y = (screenSize.height - moverSize.height) / 2;
    mover.setLocation(x, y);
  }

  /**
   * Moves a window to be concentric with another.
   * @param mover window to be centered
   * @param target to be centered within
   */
  public static void center(Window mover, Component target) {
    Dimension moverSize = mover.getSize();
    Dimension targetSize = target.getSize();
    int x = (targetSize.width - moverSize.width) / 2;
    x += target.getLocation().x;
    int y = (targetSize.height - moverSize.height) / 2;
    y += target.getLocation().y;
    mover.setLocation(x, y);
  }

  /**
   * Binds a given keystroke to click the button when the button's in the focused window.
   * @param button button to be bound to keystroke
   * @param event type of keyboard event that triggers the button
   */
  public static void setKeyBinding(JButton button, KeyStroke event) {
    String eventName = "do" + button.getText();
    button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(event, eventName);
    button.getActionMap().put(eventName, new AbstractAction() {
      private static final long serialVersionUID = 0;

      public void actionPerformed(ActionEvent event) {
        ((JButton) event.getSource()).doClick();
      }
    });
  }

  /**
   * Generates a modal dialog capable of having either a Frame or Dialog parent.
   * @param parent the parent component of the dialog
   * @param title title of dialog
   * @return dialog with the specified parent
   */
  public static JDialog makeDialog(Component parent, String title) {
    /*
     * The method of doing this was exemplified by the source of the GCJ JColorChooser (with some
     * changes to handle what I suspect is a bug in order to handle subclasses).
     */
    parent = findParent(parent);
    if (parent == null) throw new AWTError("No suitable parent found for Component.");
    else if (parent instanceof Frame) return new JDialog((Frame) parent, title, true);
    else return new JDialog((Dialog) parent, title, true);
  }

  /**
   * This is a helper method to recursively find the first instance of a Frame or Dialog within the
   * component's hierarchy.
   * @param comp The component in which to check for Frame or Dialog
   * @return Frame or Dialog ancestor, null if none is found
   */
  private static Component findParent(Component comp) {
    if (comp instanceof Frame || comp instanceof Dialog) return comp;
    Component parent = comp.getParent();
    if (parent != null) return findParent(parent);
    else return null;
  }

  // Provides an rgb value within valid bounds (0-255).
  private static int getValidRgb(int value) {
    value = Math.max(value, 0);
    value = Math.min(value, 255);
    return value;
  }
}
