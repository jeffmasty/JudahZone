package net.judah.synth.fluid;

import lombok.AllArgsConstructor;

enum ValueType {
	NONE,
	INT,
	FLOAT,
	/** 2 integers */ POINT
}

@AllArgsConstructor
public enum FluidCommand {

		CHANNELS	("channels -verbose", ValueType.NONE),
		GAIN		("gain ", ValueType.FLOAT, 0f, 5f),
		INST		("inst ", ValueType.INT, 0, 127),
		PROG_CHANGE ("prog ", ValueType.POINT),
		QUIT		("quit", ValueType.NONE),
		RESET		("reset", ValueType.NONE),

		CHORUS_DELAY_LINES ("cho_set_nr ", ValueType.INT, 0, 99),
		CHORUS_OUTPUT      ("cho_set_level ", ValueType.FLOAT, 0, 1.25),
		CHORUS_SPEED       ("cho_set_speed ", ValueType.FLOAT, 0.3f, 5f), // hz
		CHORUS_DEPTH	   ("cho_set_depth ", ValueType.INT, 0, 42) // ms
		;

		final String code;
		final ValueType type;
		final Number min;
		final Number max;

		FluidCommand(String code, ValueType type) {
			this.code = code;
			this.type = type;
			min = max = null;
		}

	}