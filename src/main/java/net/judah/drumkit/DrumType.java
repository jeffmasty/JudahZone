package net.judah.drumkit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DrumType {

	Kick(GMDrum.BassDrum.getData1()), 
	Snare(GMDrum.AcousticSnare.getData1()), 
	Stick(GMDrum.SideStick.getData1()), 
	Bongo(GMDrum.LowBongo.getData1()), // or Shaker/RimShot
	CHat(GMDrum.ClosedHiHat.getData1()), 
	OHat(GMDrum.OpenHiHat.getData1()), 
	Ride(GMDrum.RideCymbal.getData1()), 
	Clap(GMDrum.HandClap.getData1());

	@Getter private final int data1;
	
	public static int index(int data1) {
		for (int i = 0; i < values().length; i++)
			if (data1 == values()[i].getData1())
				return i;
		return -1;
	}
	
}
