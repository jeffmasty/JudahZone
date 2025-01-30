package net.judah.api;

public record Notification (Property prop, Object value) {

	public static enum Property {
		/**current tempo*/
		TEMPO,
		/**TimeProvider's current status */
		STATUS,
		/** beats per measure or subdivision change */
		SIGNATURE,
		/** transport change */
		TRANSPORT,
		/** current beat of TimeProvider */
		BEAT,
		/** current measure count */
		BARS,
		/** when bars / length == 0 */
		BOUNDARY,
		/** current loop count */
		LOOP,
		/** current step in step sequencer */
		STEP,
		/** Scene change */
		SCENE;
	}

}
