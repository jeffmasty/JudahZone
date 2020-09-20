package net.judah.metronome;

import javax.sound.midi.MidiUnavailableException;

public interface MetroPlayer {

	void start() throws MidiUnavailableException;
	void stop();
	void close();
	void setGain(float gain);
	boolean isRunning();
	
	/**@param intro Beats until Time Transport starts (null, the default, indicates Transport won't be triggered)
	 * @param duration Beats until clicks end. (null, the default, indicates clicktrack has no set ending.)*/
	void setDuration(Integer intro, Integer duration);
	
}
