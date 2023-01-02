package net.judah.widgets;


import java.awt.Insets;

import javax.swing.JButton;

import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;

public class FxButton extends JButton {
	private static final Insets ZERO = new Insets(0, 0, 0, 0);
	
	private FxButton() {
		setIcon(Icons.get("fx.png"));
		setMargin(ZERO);
	}
	
	public FxButton(Channel ch) {
		this();
		addActionListener(e -> MainFrame.setFocus(ch));
	}

//	public FxButton(MidiReceiver midiOut) {
//		this();
//		addActionListener(e ->{
//			for (Channel ch : JudahZone.getNoizeMakers()) {
//				if (ch instanceof Midi)
//			}
//		});
//		// TODO Auto-generated constructor stub
//	}

	
}
