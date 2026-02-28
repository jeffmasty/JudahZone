package net.judah.drums.pluck;

import judahzone.api.Hz;
import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.fx.op.Noise.Colour;
import net.judah.drums.Drama.Freqs;
import net.judah.drums.Drum;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.DrumSetup;
import net.judah.drums.DrumType;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.KitDB.OscSetup;
import net.judah.drums.pluck.PluckSetup.Pick;


public class PluckKit extends DrumKit {

	public static final String[] PARAMS = {"Burst", "Amp", "Tap", "Feedback", "Damp", "Harmony", "Combs", "Colour"};
	public static final int VOICES = DrumType.values().length;

	private final PluckOsc[] drums = new PluckOsc[VOICES]; // preallocated voice holder

	GainT gainT = new GainT(1.5f);
	Postage kickPost = new Postage(1, 256);
	Postage stickPost = new Postage(1, 128);
	Postage snarePost = new Postage(1, 82);
	Postage clapPost = new Postage(2, 95);
	Postage chatPost = new Postage(6, 100);
	Postage ohatPost = new Postage(15, 512);
	Postage ridePost = new Postage(73, 1024);
	Postage bongoPost = new Postage(7, 140);

    // deep body coord, tighter low-pass to emphasize thump, upper-body resonance
	DrumSetup kickD = new DrumSetup(DrumType.Kick, gainT, kickPost,
	        new Freqs(new Coord(32, 4.5f), new FilterT(202, -6f, 0.6f), new Coord(420, 6f)),
	        new Hz(36), PARAMS);

    // percussive mid/high emphasis for stick transients
	DrumSetup stickD = new DrumSetup(DrumType.Stick, gainT, stickPost,
	        new Freqs(new Coord(137, 1f), new FilterT(215, 9, 0.5f), new Coord(1482, 3f)),
	        new Hz(174), PARAMS);

    // body coord + sharper mid/air for crack
	DrumSetup snareD = new DrumSetup(DrumType.Snare, gainT, snarePost,
	        new Freqs(new Coord(230, 6f), new FilterT(316, 3f, 1.5f), new Coord(8411, 6f)),
	        new Hz(415), PARAMS);

	// broader transient, extra top-end for sheen and slap
	DrumSetup clapD = new DrumSetup(DrumType.Clap, gainT, clapPost,
	        new Freqs(new Coord(360, 1f), new FilterT(1580, 15, 1.1f), new Coord(6935, 6f)),
	        new Hz(391), PARAMS);

    // focused high partials, tight decay
	DrumSetup chatD = new DrumSetup(DrumType.CHat, gainT, chatPost,
	        new Freqs(new Coord(730, 2f), new FilterT(3206, 3, 0.35f), new Coord(3646, 3f)),
	        new Hz(739), PARAMS);

    // shimmer and upper harmonic content
	DrumSetup ohatD = new DrumSetup(DrumType.OHat, gainT, ohatPost,
	        new Freqs(new Coord(1200, 6f), new FilterT(4000, 1, 0.5f), new Coord(7000, 3f)),
	        new Hz(440), PARAMS);

    // broader, bright metal character and long tail
	DrumSetup rideD = new DrumSetup(DrumType.Ride, gainT, ridePost,
	        new Freqs(new Coord(498, 0f), new FilterT(2479, -6f, 0.6f), new Coord(3419, 6f)),
	        new Hz(523), PARAMS);

    // tight low-mid body with a little high click
	DrumSetup bongoD = new DrumSetup(DrumType.Bongo, gainT, bongoPost,
	        new Freqs(new Coord(177, 1f), new FilterT(178, 6, 1.1f), new Coord(1390, 0.5f)),
	        new Hz(246), PARAMS);
				  //	 burst, amp, tap(hz),  fb, bright, room, combs, color
	Pick kick  = new Pick(15f, 1.20f,   70f,  PluckOsc.MIN_FB,  0.67f, 10,  0.60f, Colour.BROWN);
	Pick stick = new Pick(11f, 1.00f,  220f,  0.98f, 0.15f, 47, 0.80f, Colour.VELVET);
	Pick snare = new Pick(30f, 1.10f,  880f,  0.98f, 0.47f, 28, 0.07f, Colour.WHITE);
	Pick clap  = new Pick(27f, 1.00f,  880f,  0.95f, 0.22f, 90, 0.77f, Colour.VELVET);
	Pick chat  = new Pick(8f,  0.80f, 1500f,  0.91f, 0.60f, 50, 0.35f, Colour.GREY);
	Pick ohat  = new Pick(26f, 1.00f, 4000f,  0.9f,  0.70f, 60, 0.75f, Colour.WHITE);
	Pick ride  = new Pick(12f, 0.23f, 1046f,  0.99f, 0.60f, 73, 0.73f, Colour.GREY);
	Pick bongo = new Pick(20f, 0.95f,  440f,  0.99f, 0.50f, 22, 0.78f, Colour.BROWN);

	public PluckKit(String name, DrumMachine engine, DrumInit kit) {
		super(name, engine, kit);

		int i = 0;
		drums[i++] = new PluckOsc(kickD, kick, actives);
		drums[i++] = new PluckOsc(stickD, stick, actives);
		drums[i++] = new PluckOsc(snareD, snare, actives);
		drums[i++] = new PluckOsc(clapD, clap, actives);
		drums[i++] = new PluckOsc(chatD, chat, actives);
		drums[i++] = new PluckOsc(ohatD, ohat, actives);
		drums[i++] = new PluckOsc(rideD, ride, actives);
		drums[i] = new PluckOsc(bongoD, bongo, actives);

	}

	@Override
	public KitSetup get() {
		OscSetup[] pitch = new OscSetup[VOICES];
		Pick[] setup = new Pick[VOICES];

		for (int i = 0; i < drums.length; i++) {
			setup[i] = drums[i].get();
			pitch[i] = new OscSetup(drums[i].isSync(), drums[i].getFundamental());
		}
		return super.get().withPluck(new PluckSetup(setup), pitch);
	}

	@Override
	public Drum get(int idx) {
		return drums[idx];
	}

}
