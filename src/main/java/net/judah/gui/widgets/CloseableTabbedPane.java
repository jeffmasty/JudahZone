// https://github.com/dimensionv/closeabletabbedpane

/*
 * $Id: CloseableTabbedPane.java,v 1.2 2012/07/14 23:04:06 mjoellnir Exp $
 *
 *     ---------------------------------------------------------
 *
 * Copyright 2011 Volkmar Seifert <vs@dimensionv.de>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY VOLKMAR SEIFERT AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Volkmar Seifert <vs@dimensionv.de>.
 *
 *     ---------------------------------------------------------
 *
 <groupId>de.dimensionv</groupId>
  <artifactId>closeabletabbedpane</artifactId>
  <name>CloseableTabbedPane</name>
  <version>1.0.0</version>
  <packaging>jar</packaging>

  <description>
    A JTabbedPane that can have a close-icon ('X') on each tab.
  </description>
  <licenses>
    <license>
      <name>Simplified BSD or BSD 2-Clause</name>
      <url>http://opensource.org/licenses/BSD-2-Clause</url>
    </license>
  </licenses>
  <url>https://github.com/dimensionv/closeabletabbedpane</url>
  <developers>
    <developer>
      <id>dimensionv</id>
      <name>Volkmar Seifert</name>
      <email>vs@dimensionv.de</email>
      <timezone>GMT+1</timezone>
      <roles></roles>
    </developer>
  </developers>
 */
package net.judah.gui.widgets;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * A JTabbedPane that can have a close-icon ('X') on each tab.
 *
 * <p>
 * Since it is derived from JTabbedPane directly, it is used in exactly the same manner as the JTabbedPane. By default,
 * it even behaves equally, i.e. it does not add the close-icon, thus disabling the closing-capabilities completely for
 * a tab. To enable the closing-capabilities of a tab, add a boolean value (true) the the addTab-method-call.</p>
 *
 * <p>
 * To have an additional extra icon on each tab (e.g. showing the file type), use the method addTab(String, Component,
 * Icon) or addTab(String, Component, Icon, boolean). The first variant goes without closing-capabilities, while the
 * second, if the boolean is set to true, comes with closing-capabilities.</p>
 *
 * <p>
 * Clicking the 'X', of course, closes the tab. If you like to perform action, <b>after</b> the tab has already been
 * closed, implement an event-listener to capture the ComponentRemoved-event. The removed tab can be retrieved by
 * calling java.awt.event.ContainerEvent.getChild(), which is the event the listener will receive.</p>
 *
 * @author mjoellnir
 */
public abstract class CloseableTabbedPane extends JTabbedPane implements MouseListener {

  /**
   * Creates a new instance of ClosableTabbedPane
   */
  public CloseableTabbedPane() {
    super();
    initializeMouseListener();
  }

  /**
   * Appends a tab without closing-capabilities, just as the standard JTabbedPane would do.
   *
   * @see javax.swing.JTabbedPane#addTab(String title, Component component) addTab
   */
  @Override
  public void addTab(String title, Component component) {
    this.addTab(title, component, null, false);
  }

  /**
   * Appends a tab with or without closing-capabilities, depending on the flag isClosable. If isClosable is true, a
   * close-icon ('X') is displayed left of the title.
   *
   * @param title Title of this tab.
   * @param component Contents of this tab.
   * @param isClosable en-/disable closing-capabilities
   * @see javax.swing.JTabbedPane#addTab(String title, Component component) addTab
   */
  public void addTab(String title, Component component, boolean isClosable) {
    this.addTab(title, component, null, isClosable);
  }

  /**
   * Appends a tab with or without closing-capabilities, depending on the flag isClosable. If isClosable is true, a
   * close-icon ('X') is displayed left of the title. If extraIcon is not null, it will be displayed between the closing
   * icon (if present) and the tab's title. The extraIcon will be displayed indepently of the closing-icon.
   *
   * @param title Title of this tab.
   * @param component Contents of this tab.
   * @param extraIcon Extra icon to be displayed.
   * @param isClosable en-/disable closing-capabilities
   * @see javax.swing.JTabbedPane#addTab(String title, Component component) addTab
   */
  public void addTab(String title, Component component, Icon extraIcon, boolean isClosable) {
    if (isClosable) {
      super.addTab(title, new CloseTabIcon(extraIcon), component);
    } else {
      if (extraIcon != null) {
        super.addTab(title, extraIcon, component);
      } else {
        super.addTab(title, component);
      }
    }
  }

  /**@param evt
   * @return tabIndex if mouse click is close action or -1
   */
  public int isCloseable(MouseEvent evt) {
	    int tabIndex = getUI().tabForCoordinate(this, evt.getX(), evt.getY());
	    if (tabIndex < 0)
	      return -1;
	    Icon icon = getIconAt(tabIndex);
	    if ((icon == null) || !(icon instanceof CloseTabIcon))
	      return -1;
	    return tabIndex;
  }

  @Override
  public void mouseClicked(MouseEvent evt) {
    int tabIndex = isCloseable(evt);
    if (tabIndex < 0)
      return;

    if (SwingUtilities.isRightMouseButton(evt)) {
    	detach();
    	return;
    }

    Rectangle rect = ((CloseTabIcon) getIconAt(tabIndex)).getBounds();
    if (rect.contains(evt.getX(), evt.getY())) {
      //the tab is being closed
      this.removeTabAt(tabIndex);
    }
  }

  @Override
  public void mouseEntered(MouseEvent evt) {
  }

  @Override
  public void mouseExited(MouseEvent evt) {
  }

  @Override
  public void mousePressed(MouseEvent evt) {
  }

  @Override
  public void mouseReleased(MouseEvent evt) {
  }

  private void initializeMouseListener() {
    addMouseListener(this);
  }

  public abstract void detach() ;

}