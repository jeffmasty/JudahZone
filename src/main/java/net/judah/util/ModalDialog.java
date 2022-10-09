package net.judah.util;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPKmini;


public class ModalDialog extends JDialog {

	@Getter private static ModalDialog instance;
	
	@Getter final JPanel view;
	private final KnobMode previousMode;
	
	
	public ModalDialog(JPanel view, Dimension size, KnobMode newMode) {
		instance = this;
		this.view = view;
		setModal(true);
        setSize(size);
        
        Dimension screen = JudahZone.getFrame().getSize();
        // multi-screen?
        setLocation(screen.width / 2 - size.width / 2, screen.height / 3 - size.height / 2);
		previousMode = MPKmini.getMode();
		MPKmini.setMode(newMode);
		add(view);
        setVisible(true);
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				RTLogger.log(this, "Yeehaw");
				instance = null;
				MPKmini.setMode(previousMode);
			}
		});
	}

}
