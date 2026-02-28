package net.judah.drums.fm;

import judahzone.api.Hz;
import judahzone.data.Postage;
import judahzone.data.Shape;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.prism.Prism;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.KitDB.OscSetup;
import net.judah.drums.fm.FMSetup.FMOscSetup;
import net.judah.drums.fm.FMSetup.OpSetup;

/** Directed Operator Graph */
@Prism
// build an FM patch:  1. Pick algo/assign components  2. envelopes  3 feedback

public class FMKit extends DrumKit { // <-- generics?
	private static final int VOICES = DrumType.values().length; // 8
    private final FMOsc[] drums = new FMOsc[VOICES]; // DrumTypes

    // super.lfo/super.lfo2 TODO modify LFO.java for our needs or use as template.
    static GainT gainT = new GainT(0.5f); // TODO per-drum preamps
	private static Postage kickPost = new Postage(1, 90);
	private static Postage stickPost = new Postage(1, 100);
	private static Postage snarePost = new Postage(1, 90);
	private static Postage clapPost = new Postage(2, 1024);
	private static Postage chatPost = new Postage(22, 200);
	private static Postage ohatPost = new Postage(4, 1024);
	private static Postage ridePost = new Postage(12, 1024);
	private static Postage bongoPost = new Postage(7, 140);

    static final String[] custom = new String[] {"Algo"};

    static DrumSetup kickSetup = new DrumSetup(DrumType.Kick, gainT, kickPost,
			new Freqs(new Coord(25, 9), new FilterT(338, -9f, 0.5f), new Coord(550, 6)),
			new Hz(30), custom);
    static DrumSetup stickSetup = new DrumSetup(DrumType.Stick, gainT, stickPost,
    		new Freqs(new Coord(113, 1), new FilterT(779, 12, 1f), new Coord(5719, 12)),
    		new Hz(92), custom);
    static DrumSetup snareSetup = new DrumSetup(DrumType.Snare, gainT, snarePost,
    		new Freqs(new Coord(35, 1), new FilterT(440, 1, 0.5f), new Coord(440, 1)),
    		new Hz(40), custom);
    static DrumSetup clapSetup = new DrumSetup(DrumType.Clap, gainT, clapPost,
    		new Freqs(new Coord(685, 6), new FilterT(6503, 9, 1.6f), new Coord(9565, 1)),
    		new Hz(493), custom);

    static DrumSetup ohatSetup = new DrumSetup(DrumType.OHat, gainT, ohatPost,
    		new Freqs(new Coord(530, 1), new FilterT(2324, 6, 0.5f), new Coord(8411, 6)),
    		new Hz(329), custom);
    static DrumSetup chatSetup = new DrumSetup(DrumType.CHat, gainT, chatPost,
    		new Freqs(new Coord(945, 1), new FilterT(779, -3, 0.5f), new Coord(4422, 12)),
    		new Hz(1046), custom);
    static DrumSetup rideSetup = new DrumSetup(DrumType.Ride, gainT, ridePost,
    		new Freqs(new Coord(1390, 6), new FilterT(1917, 3, 1f), new Coord(2819, 1)),
    		new Hz(1479), custom);
    static DrumSetup bongoSetup = new DrumSetup(DrumType.Bongo, gainT, bongoPost,
    		new Freqs(new Coord(110, 6), new FilterT(220, 6, 0.5f), new Coord(880, 6)),
    		new Hz(220), custom);

    static final DrumSetup[] setup = new DrumSetup[] {
    		kickSetup, stickSetup, snareSetup, clapSetup,
    		chatSetup, ohatSetup, rideSetup, bongoSetup};

    record op4(OpSetup op1, OpSetup op2, OpSetup op3, OpSetup op4, Algorithm algo) {}

    private static op4 init(DrumType type) {
		switch(type) {
		case Kick: return kickInit();
		case Stick: return stickInit();
		case Snare: return snareInit();
		case Clap: return clapInit();
		case CHat: return chatInit();
		case OHat: return ohatInit();
		case Ride: return rideInit();
		case Bongo: return bongoInit();
		default: throw new IllegalArgumentException("Unknown DrumType " + type);
		}
	}

			    // OpSetup:     level,   fb,   ratio,    postage,           r_d:mode, SIN
    private static op4 kickInit() { // Kick: FB carrier see youtubes
        OpSetup o1 = new OpSetup(0.95f, 0.20f, 0.71f, new Postage(1, 120), Shape.SIN); // deep carrier with pitch wobble
        OpSetup o2 = new OpSetup(0.85f, 0.01f, 4.00f, new Postage(0, 60), Shape.SIN); // fast bright mod
        OpSetup o3 = new OpSetup(0.60f, 0.00f, 1.41f, new Postage(2, 88), Shape.SAW); // body detune
        OpSetup o4 = new OpSetup(0.40f, 0.85f, 0.79f, new Postage(0, 8), Shape.SIN); // self-fb harsh transient
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[4]);
    }

    private static op4 stickInit() { // Single Car tight clear click
        OpSetup o1 = new OpSetup(1.00f, 0.00f, 1.00f, new Postage(0, 12), Shape.SIN); // tight carrier click
        OpSetup o2 = new OpSetup(0.30f, 0.00f, 9.00f, new Postage(0, 20), Shape.SAW); // metallic sheen
        OpSetup o3 = new OpSetup(0.20f, 0.00f, 12.00f, new Postage(0, 10), Shape.SIN); // high inharmonic partials
        OpSetup o4 = new OpSetup(0.10f, 0.00f, 3.00f, new Postage(0, 30), Shape.SIN); // subtle mid
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[10]);
    }

    private static op4 snareInit() { // Chain fb noisy metallic body rich transient/grit
        OpSetup o1 = new OpSetup(0.98f, 0.47f, 1.00f, new Postage(1, 180), Shape.SIN); // body carrier
        OpSetup o2 = new OpSetup(0.33f, 0.38f, 9.00f, new Postage(0, 20), Shape.SAW);
        OpSetup o3 = new OpSetup(0.10f, 0.57f, 12.00f, new Postage(0, 60), Shape.SIN);
        OpSetup o4 = new OpSetup(0.75f, 0.21f, 3.00f, new Postage(0, 100), Shape.SIN); // low ambience
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[11]);
    }

    private static op4 clapInit() { // Clap: Split2 two independent pairs for layered staggered clap voices
        OpSetup o1 = new OpSetup(0.75f, 0.20f, 1.00f, new Postage(20, 500), Shape.SIN); // mid carrier
        OpSetup o2 = new OpSetup(0.45f, 0.25f, 1.41f, new Postage(0, 800), Shape.TRI); // stacked carrier
        OpSetup o3 = new OpSetup(0.34f, 0.00f, 8.66f, new Postage(10, 400), Shape.TRI); // bright transient
        OpSetup o4 = new OpSetup(0.34f, 0.01f, 12.12f, new Postage(0, 200), Shape.SIN); // high noise-like
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[7]);
    }

    private static op4 chatInit() { // Chat: 4Cars multiple parallel carriers for focused mid/high partials and tight decay
        OpSetup o1 = new OpSetup(0.54f, 0.20f, 1.00f, new Postage(1, 120), Shape.SIN); // strong carrier
        OpSetup o2 = new OpSetup(0.44f, 0.01f, 9.00f, new Postage(5, 80), Shape.SIN); // inharmonic shimmer
        OpSetup o3 = new OpSetup(0.40f, 0.01f, 4.00f, new Postage(20, 200), Shape.SAW); // rich mid partials
        OpSetup o4 = new OpSetup(0.25f, 0.00f, 13.00f, new Postage(1, 50), Shape.SIN); // top air
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[3]);
    }

    private static op4 ohatInit() { // OHat: Ring4 cross-modulation / ringy topology creates metallic, inharmonic cymbal-like texture
        OpSetup o1 = new OpSetup(0.66f, 0.12f, 1.00f, new Postage(0, 500), Shape.SIN); // core carrier
        OpSetup o2 = new OpSetup(0.52f, 0.46f, 5.66f, new Postage(20, 478), Shape.SIN); // ringy mod
        OpSetup o3 = new OpSetup(0.26f, 0.1f, 19.8f, new Postage(18, 460), Shape.SAW); // metallic with some fb
        OpSetup o4 = new OpSetup(0.42f, 0.12f, 9.90f, new Postage(35, 450), Shape.SIN); // extreme high partials
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[9]);
    }

    private static op4 rideInit() { // Ride: Mod3 single carrier with multiple modulators for sustained shimmer complexity
        OpSetup o1 = new OpSetup(0.60f, 0.10f, 1.00f, new Postage(2, 600), Shape.SIN); // sustained carrier
        OpSetup o2 = new OpSetup(0.35f, 0.00f, 7.00f, new Postage(0, 150), Shape.SIN);
        OpSetup o3 = new OpSetup(0.30f, 0.05f, 2.83f, new Postage(15, 320), Shape.SAW);
        OpSetup o4 = new OpSetup(0.35f, 0.00f, 1.73f, new Postage(0, 120), Shape.SIN);
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[6]);
    }

    private static op4 bongoInit() { // Bongo: EzPair simple 2-op pair (mod→car) for tuned, percussive bongos with clear pitch relationship
        OpSetup o1 = new OpSetup(0.90f, 0.00f, 0.60f, new Postage(0, 160), Shape.SIN); // tuned body
        OpSetup o2 = new OpSetup(0.70f, 0.00f, 2.00f, new Postage(0, 80), Shape.SIN); // harmonic mod
        OpSetup o3 = new OpSetup(0.45f, 0.00f, 3.33f, new Postage(0, 60), Shape.SAW); // percussive snap
        OpSetup o4 = new OpSetup(0.25f, 0.00f, 5.00f, new Postage(0, 40), Shape.SIN); // top ring
        return new op4(o1, o2, o3, o4, DX9.getAlgorithms()[5]);
    }

	public FMKit(
			String name, // DrumInit?
			DrumMachine engine, // Midi Tracks (4)
			DrumInit kit) { // 1 of 4 slots
		super(name, engine, kit);
		gain.setPreamp(0.5f);
		for (int i = 0; i < VOICES; i++) {
			op4 init = init(DrumType.values()[i]);
			OpSetup[] ops = {init.op1(), init.op2(), init.op3(), init.op4()};
			drums[i] = new FMOsc(setup[i], new FMOscSetup(init.algo(), ops), actives);
		}
	}

	@Override
	public void accept(KitSetup t) { // TODO conform KitSetup1
		t.fm(); // TODO load a preset
	}

	@Override
	public KitSetup get() {
		FMOscSetup[] oscSetup = new FMOscSetup[drums.length];
		OscSetup[] pitch = new OscSetup[drums.length];
		for (int i = 0; i < drums.length; i++) {
			oscSetup[i] = drums[i].get();
			pitch[i] = new OscSetup(drums[i].isSync(), drums[i].getFundamental());
		}
		return super.get().withFM(new FMSetup(oscSetup), pitch);
	}

	@Override
	public Drum get(int idx) {
		return drums[idx];
	}


}
