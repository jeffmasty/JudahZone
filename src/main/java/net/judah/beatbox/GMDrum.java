package net.judah.beatbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Midi;

@RequiredArgsConstructor @Getter
public enum GMDrum {

    AcousticBassDrum(35, "Acoustic Bass Drum"),
    BassDrum(36, "Bass Drum"),
    SideStick(37, "Side Stick"),
    AcousticSnare(38, "Acoustic Snare"),
    HandClap(39, "Hand Clap"),
    ElectricSnare(40, "Electric Snare"),
    LowFloorTom(41, "Low Floor Tom"),
    ClosedHiHat(42, "Closed Hi-Hat"),
    HighFloorTom(43, "High Floor Tom"),
    PedalHiHat(44, "Pedal Hi-Hat"),
    LowTom(45, "Low Tom"),
    OpenHiHat(46, "Open Hi-Hat"),
    LowMidTom(47, "Low-Mid Tom"),
    HiMidTom(48, "Hi-Mid Tom"),
    CrashCymbal1(49, "Crash Cymbal 1"),
    HighTom(50, "High Tom"),
    RideCymbal(51, "Ride Cymbal 1"),
    ChineseCymbal(52, "Chinese Cymbal"),
    RideBell(53, "Ride Bell"),
    Tambourine(54, "Tambourine"),
    SplashCymbal(55, "Splash Cymbal"),
    Cowbell(56, "Cowbell"),
    CrashCymbal2(57, "Crash Symbol 2"),
    Vibraslap(58, "Vibraslap"),
    RideCymbal2(59, "Ride Cymbal 2"),
    HiBongo(60, "Hi Bongo"), // middle C
    LowBongo(61, "Low Bongo"),
    MuteHiConga(62, "Mute Hi Conga"),
    OpenHiConga(63, "Open Hi Conga"),
    LowConga(64, "Low Conga"),
    HighTimbale(65, "High Timbale"),
    LowTimbale(66, "Low Timbale"),
    HighAgogo(67, "High Agogo"),
    LowAgogo(68, "Low Agogo"),
    Cabasa(69, "Cabasa"),
    Maracas(70, "Maracas"),
    ShortWhistle(71, "Short Whistle"),
    LongWhistle(72, "Long Whistle"),
    ShortGuiro(73, "Short Guiro"),
    LongGuiro(74, "Long Guiro"),
    Claves(75, "Claves"),
    HiWoodBlock(76, "Hi Wood Block"),
    LowWoodBlock(77, "Low Wood Block"),
    MuteCuica(78, "Mute Cuica"),
    OpenCuica(79, "Open Cuica"),
    MuteTriangle(80, "Mute Triangle"),
    OpenTriangle(81, "Open Triangle"),
    Shaker(82, "Shaker");

    private final int midi;
    private final String display;

	public static String format(Midi midi) {
		if (midi == null) return "null";
		int val = midi.getData1();
		for (GMDrum d : values()) {
		    if (d.midi == val)
		        return d.display + " ("+ midi + ")";
		}
		return midi.toString();
	}

	@Override
	public String toString() {
	    return display;
	}

}

