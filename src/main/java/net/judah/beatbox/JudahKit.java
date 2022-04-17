package net.judah.beatbox;

import java.util.HashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Midi;

@RequiredArgsConstructor @Getter
public enum JudahKit {

    AcousticBassDrum(35, "A.Bass"),
    BassDrum(36, "Bass"),
    AcousticSnare(38, "Snare"),
    ElectricSnare(40, "E.Snare"),
    SideStick(37, "Stick"),
    HandClap(39, "Clap"),

    LowFloorTom(41, "Tom LF"),
    HighFloorTom(43, "Tom HF"),
    LowTom(45, "Tom L"),
    LowMidTom(47, "Tom LM"),
    HiMidTom(48, "Tom HM"),
    HighTom(50, "Tom H"),
    
    ClosedHiHat(42, "Hat-Clsd"),
    PedalHiHat(44, "Hat-Pdl"),
    OpenHiHat(46, "Hat-Open"),
    MuteTriangle(80, "TriaMute"),
    OpenTriangle(81, "TriaOpen"),
    Shaker(82, "Shaker"),
    Claves(75, "Claves"),

    Tambourine(54, "Tambo."),
    RideCymbal(51, "Ride1"),
    RideCymbal2(59, "Ride2"),

    RideBell(53, "RideBell"),
    Cowbell(56, "Cowbell"),
    CrashCymbal1(49, "Crash1"),
    CrashCymbal2(57, "Crash2"),
    ChineseCymbal(52, "ChinaCym"),
    SplashCymbal(55, "Splash"),

    HiBongo(60, "Bongo H"), // middle C
    LowBongo(61, "Bongo L"),
    MuteHiConga(62, "CongaMut"),
    OpenHiConga(63, "CongaOpn "),
    LowConga(64, "CongaLow"),
    Vibraslap(58, "Vib-slap"),
    HighTimbale(65, "Timba H"),
    LowTimbale(66, "Timba L"),
    HighAgogo(67, "Agogo H"),
    LowAgogo(68, "Agogo L"),
    Cabasa(69, "Cabasa"),
    Maracas(70, "Maracas"),
    ShortWhistle(71, "Whistle1"),
    LongWhistle(72, "Whistle2"),
    ShortGuiro(73, "Guiro1"),
    LongGuiro(74, "Guiro2"),
    HiWoodBlock(76, "Block H"),
    LowWoodBlock(77, "Block L"),
    MuteCuica(78, "CuicaMte"),
    OpenCuica(79, "CuicaOpn")
    ;

    private final int midi;
    private final String display;

    public static JudahKit lookup(int midi) {
    	for (JudahKit x : JudahKit.values())
    		if (x.midi ==  midi)
    			return x;
    	return null;
    }
    
	public static String format(Midi midi) {
		if (midi == null) return "null";
		int val = midi.getData1();
		for (JudahKit d : values()) {
		    if (d.midi == val)
		        return d.display + " ("+ midi + ")";
		}
		return midi.toString();
	}

	public byte toByte() {
		return Integer.valueOf(midi).byteValue();
	}
	
	@Override
	public String toString() {
	    return display;
	}

    public static final JudahKit[] Standard = new JudahKit[] {
            BassDrum, AcousticSnare, SideStick, HandClap,
            ClosedHiHat, PedalHiHat, OpenHiHat, Shaker,
            HiBongo, LowBongo, OpenHiConga, HighTimbale,
    };

    public static final JudahKit[] Acoustic = new JudahKit[] {
            BassDrum, SideStick, HandClap, LongGuiro,
            ClosedHiHat, PedalHiHat, OpenHiHat, ChineseCymbal,
            Cabasa, Shaker, HiWoodBlock, Claves,
    };

    public static final HashMap<String, JudahKit[]> KITS = new HashMap<>();
    static {KITS.put("Standard", Standard); KITS.put("Acoustic", Acoustic);}

}

