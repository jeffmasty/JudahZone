package net.judah.drums;

import judahzone.api.Hz;
import judahzone.data.Postage;
import judahzone.fx.Gain.GainT;
import net.judah.drums.Drama.Freqs;

public record DrumSetup(
	DrumType t,
	GainT gainStage,
	Postage env, // TODO Postage?
	Freqs freqs,
	Hz fundamental,
	String[] names  // custom param names for each drum type
	)
{ }