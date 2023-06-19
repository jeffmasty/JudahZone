package net.judah.api;

import lombok.Data;

@Data
public class Notification {
	
	public static enum Property {
		/**current tempo*/
		TEMPO,
		/**TimeProvider's current status */
		STATUS,
		/** beats per measure or subdivision change */
		SIGNATURE,
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

	public final Property prop;
	public final Object value;

}
