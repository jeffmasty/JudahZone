package net.judah.gui.widgets;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;

public class ModalDialog extends JDialog {

	@Getter private static ModalDialog instance;
	@Getter final JPanel view;

	public ModalDialog(JPanel view, Dimension size) {
		instance = this;
		this.view = view;
		setModal(true);
        setSize(size);
        if (view.getName() != null)
        	setTitle(view.getName());
        setLocation(JudahZone.getInstance().getFrame().getSize().width / 2 - size.width / 2, 50); // multi-screen?
		add(view);
        setVisible(true);
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				instance = null;
			}
		});
	}

}
