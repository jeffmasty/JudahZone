package net.judah.util;


import java.awt.Insets;

import javax.swing.JButton;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Engine;
import net.judah.mixer.Channel;
import net.judah.mixer.MidiInstrument;
import net.judah.tracker.Track;

public class FxButton extends JButton {
	private static final Insets ZERO = new Insets(0, 0, 0, 0);
	
	private FxButton() {
		setIcon(Icons.load("fx.png"));
		setMargin(ZERO);
	}
	
	public FxButton(Channel ch) {
		this();
		addActionListener(e -> MainFrame.setFocus(ch));
	}

	
	public FxButton(Track track) {
		this();
		addActionListener(e -> {
			for (Channel ch : JudahZone.getMixer().getChannels()) {
				if (ch instanceof MidiInstrument &&
					 ((MidiInstrument)ch).getMidiPort() == track.getMidiOut()) {
						MainFrame.setFocus(ch);
						return;
				}
				if (ch instanceof Engine &&
						((Engine)ch).getMidiPort() == track.getMidiOut()) {
					MainFrame.setFocus(ch);
					return;
				}
			}
		});
	}
}
