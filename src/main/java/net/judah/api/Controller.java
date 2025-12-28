package net.judah.api;

public interface Controller {

	/** @return true if consumed */
	boolean midiProcessed(Midi midi);

}
