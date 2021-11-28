package net.judah.api;

public interface TimeListener {

	public enum Property {
		/**current tempo*/
		TEMPO,
		/**TimeProvider's current status */
		STATUS,
		/** beats per measure */
		MEASURE,
		/** volume change */
		VOLUME,
		/** transport change */
		TRANSPORT,
		/** current beat of TimeProvider */
		BEAT,
		/** current measure count */
		BARS,
		/** current loop count */
		LOOP,
		/** current step in step sequencer */
		STEP;
	}


	void update(Property prop, Object value);

}
