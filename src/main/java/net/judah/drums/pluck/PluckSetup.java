package net.judah.drums.pluck;

import judahzone.fx.op.Noise.Colour;
import judahzone.util.Constants;

public record PluckSetup(

		Pick[] plucks
//	long burst,      // excitation length MS
//	float amp,       // burst amplitude
//	float tap,		 // pickup position (0..1)
//
//	// body
//	float feedback,  // feedback scalar
//	float damp,      // damping filter cutoff (normalized 0..1)
//	float harmony,   // all-pass detune amount
//	float combs,     // combs knob (0..1)
//	Colour colour	 // noise colour

)
{

	public static record Pick (
		float burst,     // excitation length 1.0 - 25.0 in fractional milliseconds
		float amp,       // burst amplitude
		float tap,		 // pickup center position (in Hz.)

		// body
		float feedback,  // feedback scalar
		float bright,      // damping filter cutoff (normalized 0..1)
		int room,   		// TODO: rework as a more intuitive "harmony" control (all-pass detune amount)
		float combs,     // combs knob (0..1)
		Colour colour	 // noise colour
		) {
	}

	public static enum Params {

		BURST,
		AMP,
		TAP,

		FEEDBACK, // loop gain
		DAMP, // damping filter cutoff
		HARMONY, // allpass
		COMBS, // combs knob
		COLOUR
		;



	}


	public static int normalizedFeedback(float fb) {
		return (int)((fb - PluckOsc.MIN_FB) / (PluckOsc.MAX_FB - PluckOsc.MIN_FB) * Constants.NORMAL);
	}

	public static int normalizedDamp(float damp) {
		return (int)((damp - PluckOsc.MIN_DAMP) / (PluckOsc.MAX_DAMP - PluckOsc.MIN_DAMP) * Constants.NORMAL);
	}

	public static int normalizedFeedback 	(PluckOsc osc)	{ return normalizedFeedback(osc.getFeedback()); }
	public static int normalizedDamp		(PluckOsc osc)	{ return normalizedDamp(osc.getDamp()); }



}



// pluck
	//pitch


// filter

// envelope

// pickup


// gain/pan





