package net.judah.gui.widgets;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;

public class ModalDialog extends JDialog {

	@Getter private static ModalDialog instance;
	@Getter final JPanel view;
	private final KnobMode previousMode;
	
	public ModalDialog(JPanel view, Dimension size, KnobMode mode) {
		instance = this;
		this.view = view;
		setModal(true);
        setSize(size);
        if (view.getName() != null)
        	setTitle(view.getName());
        setLocation(JudahZone.getFrame().getSize().width / 2 - size.width / 2, 50); // multi-screen?
        previousMode = MainFrame.getKnobMode();
		if (mode != null)
			MainFrame.setFocus(mode);
		add(view);
        setVisible(true);
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				instance = null;
				if (MainFrame.getKnobMode() != previousMode)
					MainFrame.setFocus(previousMode);
			}
		});
	}

}
