package net.judah.mixer;

import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;

public class GMSynth extends Instrument {

	@Setter @Getter JackPort midiOut;
	
	public GMSynth(String channelName, String sourceLeft, String sourceRight, JackPort left, JackPort right, ImageIcon icon) {
		super(channelName, sourceLeft, sourceRight, left, right, icon);
	}

}
