package net.judah.drumz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.tracker.GMDrum;

@RequiredArgsConstructor
public enum DrumType {
	
	Kick(GMDrum.BassDrum), 
	Snare(GMDrum.AcousticSnare), 
	Stick(GMDrum.SideStick), 
	Bongo(GMDrum.LowBongo), // or Shaker/RimShot
	CHat(GMDrum.ClosedHiHat), 
	OHat(GMDrum.OpenHiHat), 
	Ride(GMDrum.RideCymbal), 
	Clap(GMDrum.HandClap);

	@Getter private final GMDrum dat;
	
}
