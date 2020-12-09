package net.judah.metronome;

import javax.sound.midi.MidiUnavailableException;

import net.judah.api.TimeListener;

public interface Player extends TimeListener {

	void start() throws MidiUnavailableException;
	void stop();
	void close();
	// void setGain(float gain);
	/**@param intro Beats until Time Transport starts (null, the default, indicates Transport won't be triggered)
	 * @param duration Beats until clicks end. (null, the default, indicates clicktrack has no set ending.)*/
	void setDuration(Integer intro, Integer duration);

	
	boolean isRunning();
	
}
