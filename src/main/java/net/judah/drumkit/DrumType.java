package net.judah.drumkit;

import lombok.Getter;

public enum DrumType {

//	Kick(GMDrum.BassDrum, GMDrum.AcousticBassDrum), 
//	Snare(GMDrum.AcousticSnare, GMDrum.Cabasa, GMDrum.ShortWhistle, GMDrum.HiWoodBlock, GMDrum.LowTimbale, GMDrum.HiMidTom), 
//	Stick(GMDrum.SideStick, GMDrum.OpenHiConga, GMDrum.LowMidTom, GMDrum.MuteHiConga, GMDrum.Maracas, GMDrum.HighTimbale), 
//	Bongo(GMDrum.LowBongo, GMDrum.LowConga, GMDrum.LowTom, GMDrum.LowFloorTom), 
//	CHat(GMDrum.ClosedHiHat, GMDrum.PedalHiHat, GMDrum.MuteTriangle, GMDrum.MuteCuica), 
//	OHat(GMDrum.OpenHiHat, GMDrum.OpenTriangle, GMDrum.RideCymbal2), 
//	Ride(GMDrum.RideCymbal, GMDrum.CrashCymbal1, GMDrum.Vibraslap, GMDrum.CrashCymbal2, GMDrum.LongWhistle, GMDrum.ChineseCymbal), 
//	Clap(GMDrum.HandClap, GMDrum.Claves, GMDrum.HiBongo, GMDrum.HighFloorTom, GMDrum.Cowbell, GMDrum.HighTom);
	// data1 36, 38, 37, 61, 42, 46, 51, 39

	Kick(GMDrum.BassDrum, GMDrum.AcousticBassDrum), 
	Stick(GMDrum.SideStick, GMDrum.OpenHiConga, GMDrum.LowMidTom, GMDrum.MuteHiConga, GMDrum.Maracas, GMDrum.HighTimbale), 
	Snare(GMDrum.AcousticSnare, GMDrum.Cabasa, GMDrum.ShortWhistle, GMDrum.HiWoodBlock, GMDrum.LowTimbale, GMDrum.HiMidTom), 
	Clap(GMDrum.HandClap, GMDrum.Claves, GMDrum.HiBongo, GMDrum.HighFloorTom, GMDrum.Cowbell, GMDrum.HighTom),
	CHat(GMDrum.ClosedHiHat, GMDrum.PedalHiHat, GMDrum.MuteTriangle, GMDrum.MuteCuica), 
	OHat(GMDrum.OpenHiHat, GMDrum.OpenTriangle, GMDrum.RideCymbal2), 
	Ride(GMDrum.RideCymbal, GMDrum.CrashCymbal1, GMDrum.Vibraslap, GMDrum.CrashCymbal2, GMDrum.LongWhistle, GMDrum.ChineseCymbal), 
	Bongo(GMDrum.LowBongo, GMDrum.LowConga, GMDrum.LowTom, GMDrum.LowFloorTom);
	
	
	
	@Getter private final int data1;
	@Getter private final GMDrum primary;
	@Getter private final GMDrum[] alt;

	DrumType(GMDrum gmdrum, GMDrum ...drums) {
		this.primary = gmdrum;
		this.data1 = primary.getData1();
		this.alt = drums;
	}
	
	public static int index(int data1) {
		for (int i = 0; i < values().length; i++)
			if (data1 == values()[i].getData1())
				return i;
		return -1;
	}
	
	public static int alt(int data1) {
		for (int i = 0; i < values().length; i++)
			for (GMDrum d : values()[i].alt) 
				if (d.getData1() == data1)
					return d.getData1();
		return -1;
	}
	
	public static int translate(DrumType t, int delta) {
		return translate(t.getData1(), delta);
	}

	public static int translate(int source, int delta) {
		int idx = index(source) + delta;
		while (idx >= values().length)
			idx -= values().length;
		while (idx < 0)
			idx += values().length;
		return values()[idx].data1;
	}
	
}
