package net.judah.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public interface Gui {

	int STD_HEIGHT = 18;
	Insets BTN_MARGIN = new Insets(1,1,1,1);
	// Insets ZERO_MARGIN = new Insets(0,0,0,0);
	Border RED = BorderFactory.createLineBorder(Color.RED, 1);
	Border HIGHLIGHT = BorderFactory.createLineBorder(Color.BLACK, 1);
	Border SELECTED = BorderFactory.createLineBorder(Pastels.BLUE, 1);
	Border SUBTLE = BorderFactory.createLineBorder(Pastels.MY_GRAY, 1);
	Border NO_BORDERS = new EmptyBorder(BTN_MARGIN);

	String FACE = "Arial";
	Font FONT9 = new Font(FACE, Font.PLAIN, 9);
	Font FONT10 = new Font(FACE, Font.PLAIN, 10);
	Font FONT11 = new Font(FACE, Font.PLAIN, 11);
	Font FONT12 = new Font(FACE, Font.PLAIN, 12);
	Font BOLD10 = new Font(FACE, Font.BOLD, 10);
	Font BOLD = new Font(FACE, Font.BOLD, 11);
	Font BOLD12 = new Font(FACE, Font.BOLD, 12);
	Font BOLD13 = new Font(FACE, Font.BOLD, 13);

	public static JComponent font(JComponent c) {
		c.setFont(FONT9);
		return c;
	}

	static JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(1, 2));
		result.add(left);
		result.add(right);
		return result;
	}

	static Box box(Component... items) {
		Box result = new Box(BoxLayout.X_AXIS);
		for (Component p : items)
			result.add(p);
		return result;
	}

	static JPanel wrap(Component... items) {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		for (Component p : items)
			result.add(p);
		return result;
	}

	static Component resize(Component c, Dimension d) {
		c.setMaximumSize(d);
		c.setPreferredSize(d);
		return c;
	}

	static void infoBox(String infoMessage, String titleBar) {
	    JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
	}

	static String inputBox(String infoMessage) {
	    return JOptionPane.showInputDialog(infoMessage);
	}

	static class Opaque extends JPanel {
		public Opaque(LayoutManager mgr) {
			this();
			setLayout(mgr);
		}

		public Opaque() {
			setOpaque(true);
		}
	}

	public static interface Mouser extends MouseListener, MouseMotionListener, MouseWheelListener {
		@Override default void mouseClicked(MouseEvent e) { }
		@Override default void mousePressed(MouseEvent e) { }
		@Override default void mouseReleased(MouseEvent e) { }
		@Override default void mouseEntered(MouseEvent e) { }
		@Override default void mouseExited(MouseEvent e) { }
		@Override default void mouseDragged(MouseEvent e) { }
	    @Override default void mouseMoved(MouseEvent e) { }
	    @Override default void mouseWheelMoved(MouseWheelEvent e) { }
	}

}
