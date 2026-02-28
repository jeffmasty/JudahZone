package net.judah.drums.fm;

import judahzone.data.Postage;
import judahzone.data.Shape;





public record FMSetup(
		FMOscSetup[] oscillators
		)
{

	public static record FMOscSetup(
		Algorithm algorithm, // per oscillator/drum
		OpSetup[] operators
		)
		{
		}


	public static record OpSetup (

		float level, 	// often called opIndex
		float fb,   	// we can have f/b on any operator
		float ratio,  	// usage based on mode
		Postage env,    // attack and decay
		// Mode mode, 		// fixed/ratio flag
		Shape shape 	// sine/noise/saw/square table
		)
		{

		public OpSetup(OpSetup copy) {
			this(copy.level, copy.fb, copy.ratio, copy.env, copy.shape);
		}
		}

}
