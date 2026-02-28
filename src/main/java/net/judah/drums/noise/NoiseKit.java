package net.judah.drums.noise;

import net.judah.drums.Drum;
import net.judah.drums.DrumInit;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.KitDB.KitSetup;
import net.judah.drums.KitDB.NoiseSetup;
import net.judah.gui.MainFrame;

public class NoiseKit extends DrumKit {

	public static final String TOKEN = "_JudahZone_DrumSynth_"; // TODO code smell

	private final Kick kick;
	private final Snare snare;
	private final Stick stick;
	private final Clap clap;
	private final CHat chat;
	private final OHat ohat;
	private final Ride ride;
	private final Bongo bongo;

	private final NoiseOsc[] drums;

	public NoiseKit(DrumMachine engine, DrumInit type) {
		super(type.name(), engine, type);
		kick = new Kick(actives);
		stick = new Stick(actives);
		snare = new Snare(actives);
		clap = new Clap(actives);
		chat = new CHat(actives);
		ohat = new OHat(actives);
		ride = new Ride(actives);
		bongo = new Bongo(actives);
		drums = new NoiseOsc[] {kick, stick, snare, clap, chat, ohat, ride, bongo};
		gain.setPreamp(0.25f);
	}

	@Override
	public void accept(KitSetup kit) {
		super.accept(kit);

		kick.accept(kit.synth().kick());
		snare.accept(kit.synth().snare());
		stick.accept(kit.synth().stick());
		clap.accept(kit.synth().clap());
		chat.accept(kit.synth().chat());
		ohat.accept(kit.synth().ohat());
		ride.accept(kit.synth().ride());
		bongo.accept(kit.synth().bongo());

		if (knobs != null)
			MainFrame.update(knobs);
	}

	@Override public KitSetup get() {
		NoiseSetup mine = new NoiseSetup(kick.get(), snare.get(), stick.get(), clap.get(),
				chat.get(), ohat.get(), ride.get(), bongo.get());
		return super.get().withNoise(mine, pitch(drums));
	}

	@Override public Drum get(int idx) {
		return drums[idx];
	}
}
//    private static KitSetup makeNoiseKit() {
//        Gain.GainT[] g = gains(
//            0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,
//            0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f,0.25f,0.5f,0.5f,1.0f
//        );
//        Postage[] e = env(5,30,9,21,8,25,23,205,15,201,15,169,30,639,15,204);
//        Coord[] lo = coords(45.0,9.0,150.0,1.0,180.0,6.0,400.0,0.0,2000.0,6.0,2100.0,3.0,1100.0,6.0,180.0,9.0);
//        FilterT[] b = bodies(
//            55.0,0.0,0.5,500.0,0.0,0.5,391.0,0.0,0.5,2600.0,9.0,0.5,3278.0,9.0,0.5,4421.576,9.0,0.5,3006.1748,0.0,0.5,296.91833,0.0,0.5
//        );
//        Coord[] hi = coords(180.0,6.0,6666.0,1.0,10000.0,6.0,6666.0,9.0,12500.0,9.0,16000.0,6.0,16000.0,1.0,2000.0,3.0);
//        KitDB.OscSetup[] pitch = new OscSetup[] {
//            new OscSetup(false, 55.0f),
//            new OscSetup(false, 500.0f),
//            new OscSetup(false, 391.0f),
//            new OscSetup(false, 2600.0f),
//            new OscSetup(false, 3278.0f),
//            new OscSetup(false, 4434.922f),
//            new OscSetup(false, 2959.9553f),
//            new OscSetup(false, 293.66476f)
//        }; }
