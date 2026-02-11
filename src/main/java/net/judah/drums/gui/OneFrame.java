package net.judah.drums.gui;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import judahzone.gui.Gui;


public abstract class OneFrame extends JFrame {

	public static interface SZ {
		int SLIDER_WIDTH = 120;
		int LABEL_WIDTH = 70;
		int VALUE_WIDTH = 45;
		int SPACING = 8;
		int ROW_HEIGHT = 25;
		Dimension VAL_DIM = new Dimension(VALUE_WIDTH, ROW_HEIGHT);
		Dimension SLIDER_DIM = new Dimension(SLIDER_WIDTH, ROW_HEIGHT);
		Dimension LABEL_DIM = new Dimension(LABEL_WIDTH, ROW_HEIGHT);
	}


	protected static final boolean LEFT = true;
	protected static final boolean RIGHT = false;

	protected NotePad tap;

	public OneFrame() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Capture ESC + Enter keys globally
		getRootPane().registerKeyboardAction(e -> dispose(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(e -> tap.sound(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		// left/right keys
		getRootPane().registerKeyboardAction(e -> rotary(LEFT),
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(e -> rotary(RIGHT),
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		// up/down can be ProgChange when implemented

	}

	protected abstract void rotary(boolean direction);

	protected static class LBL extends JLabel {
		public LBL(String txt) {
			super(txt, JLabel.CENTER);
			Gui.resize(this, SZ.LABEL_DIM);
		}
	}

}
