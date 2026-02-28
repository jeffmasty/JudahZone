package net.judah.drums;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import judahzone.data.Postage;
import judahzone.filter.Coord;
import judahzone.filter.FilterT;
import judahzone.fx.Gain.GainT;
import judahzone.util.Folders;
import judahzone.util.JsonUtil;
import judahzone.util.RTLogger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.drums.fm.FMSetup;
import net.judah.drums.noise.Bongo.BongoParams;
import net.judah.drums.noise.CHat.CHatParams;
import net.judah.drums.noise.Clap.ClapParams;
import net.judah.drums.noise.Kick.KickParams;
import net.judah.drums.noise.OHat.OHatParams;
import net.judah.drums.noise.Ride.RideParams;
import net.judah.drums.noise.Snare.SnareParams;
import net.judah.drums.noise.Stick.StickParams;
import net.judah.drums.pluck.PluckSetup;
import net.judah.mixer.Channels.RegisteredDrums;

/** drum kit parameters to/from disk. */
@Data @AllArgsConstructor @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KitDB {
	public static enum BaseParam { Vol, Pan, Attack, Decay }


//	@JsonIgnore // PHASE II // don't delete comments
//    public static record DrumJson (
//    		GainT g, Postage e, int room,
//    		OscSetup p, Coord loCut, FilterT body, Coord hiCut,
//			RegisteredDrums type, // noise/wav/fm/pluck
//    		Object registration // NoiseJson FMJson PluckJson WavJson
//    		) {    }
//    public static record KitJson (
//			String name,
//			boolean choke,
//			DrumJson[] drums
//	) {}


	// TODO read into lists by type and create List getters: get(RegisteredDrums type)  get(DrumKit kit)
	private static final Map<RegisteredDrums, List<KitSetup>> saved = new HashMap<>();

	@JsonIgnore
	static final int KITS = DrumInit.values().length;
//	@JsonIgnore
//    private static final List<KitSetup> synthKits = new CopyOnWriteArrayList<>();
//	@JsonIgnore
//	private static final List<KitSetup> sampleKits = new CopyOnWriteArrayList<>();

	static {
	    try {
	        KitDB presets = JsonUtil.readJson(Folders.getDrumSynth(), KitDB.class);
	        if (presets == null || presets.kits == null || presets.kits.isEmpty())
	        	RTLogger.warn(KitDB.class, "No drum synth presets found at " + Folders.getDrumSynth().getAbsolutePath());
	        else
	        	presets.kits.forEach(kit -> type(kit));
//
//	        for (int i = 0; i < presets.; i++) {
//	        	saved.put(RegisteredDrums.Synth, dedupeByName(presets.kits));
//
//
//
//	        if (synths != null && synths.kits != null)
//	        	synthKits.addAll(dedupeByName(synths.kits));
	    } catch (IOException e) {
	        RTLogger.warn(Folders.getDrumSynth().getAbsolutePath(), e);
	    }
//	    try {
//	        KitDB samples = JsonUtil.readJson(Folders.getOldschool(), KitDB.class);
//	        if (samples != null && samples.kits != null)
//	        	sampleKits.addAll(dedupeByName(samples.kits));
//	    } catch (IOException e) {
//	        RTLogger.warn(Folders.getOldschool().getAbsolutePath(), e);
//	    }
	}

	private static void type(KitSetup kit) {
		RegisteredDrums type = RegisteredDrums.Wav;
		if (kit.synth() != null)
			type = RegisteredDrums.Noise;
		else if (kit.fm() != null)
			type = RegisteredDrums.FM;
		else if (kit.pluck() != null)
			type = RegisteredDrums.Pluck;

		List<KitSetup> q = saved.get(type);
		if (q == null) {
			q = new CopyOnWriteArrayList<>();
			saved.put(type, q);
		}
		q.add(kit);
	}

	public static List<KitSetup> getKits(RegisteredDrums type) {
		List<KitSetup> q = saved.get(type);
		if (q == null)
			return List.of();
		return dedupeByName(q);
	}

	private static List<KitSetup> dedupeByName(List<KitSetup> kits) {
	    return new ArrayList<>(kits.stream()
	        .collect(Collectors.toMap(KitSetup::name, k -> k, (first, second) -> first))
	        .values());
	}

    // JSON
    List<KitSetup> kits = new ArrayList<>();

    // why cross-section?
    public static record KitSetup (
    		String name,
    		boolean choke,
    		GainT[] gains,
    		Postage[] env,
    		Coord[] lowCut,
    		FilterT[] body,
    		Coord[] hiCut,
    		OscSetup[] pitch, // nullable
    		NoiseSetup synth, // nullable
    		FMSetup fm, // nullable
    		PluckSetup pluck // nullable
    ) {
        public KitSetup withFM(FMSetup with, OscSetup[] pitch) { return new KitSetup(
        		name, choke, gains, env, lowCut, body, hiCut, pitch, synth, with, pluck);
        }

        public KitSetup withPluck(PluckSetup with, OscSetup[] pitch) { return new KitSetup(
        		name, choke, gains, env, lowCut, body, hiCut, pitch, synth, fm, with);
    	}

		public KitSetup withNoise(NoiseSetup with, OscSetup[] pitch) { return new KitSetup(
				name, choke, gains, env, lowCut, body, hiCut, pitch, with, fm, pluck);
		}}

    // TODO
    public static record OscSetup(boolean tracking, float fundamental) {
    	public OscSetup (float fundamental) {
			this(true, fundamental);
		}
    }

    public static record NoiseSetup(
    		KickParams kick, SnareParams snare, StickParams stick, ClapParams clap,
    		CHatParams chat, OHatParams ohat, RideParams ride, BongoParams bongo) {}

    private static String[] patches(List<KitSetup> kits) {
        return kits.stream()
                   .map(KitSetup::name)
                   .sorted(String.CASE_INSENSITIVE_ORDER)
                   .toArray(String[]::new);
    }

    /** for ProgChanges */
    public static int indexOf(KitSetup kit, RegisteredDrums type) {
    	List<KitSetup> kits = type == RegisteredDrums.Noise ? saved.get(RegisteredDrums.Noise) :
				type == RegisteredDrums.FM ? saved.get(RegisteredDrums.FM) :
				type == RegisteredDrums.Pluck ? saved.get(RegisteredDrums.Pluck) :
				saved.get(RegisteredDrums.Wav);
    	return kits.indexOf(kit);
    }

    // save json
	public static void addOrReplace(KitSetup setup, RegisteredDrums type) throws IOException {
		List<KitSetup> kits = saved.get(type);
		for (int i = 0; i < kits.size(); i++) {
			if (kits.get(i).name().equals(setup.name())) {
				kits.set(i, setup);
				save(new ArrayList<KitSetup>(kits));
				return;
			}
		}
		kits.add(setup);
		save(kits);
		// TODO refill DropDowns in GUIs  // TODO notify subscribers of type
	}

	public static void remove(String name, RegisteredDrums type) throws IOException {
		List<KitSetup> kits = saved.get(type);
		for (int i = 0; i < kits.size(); i++) {
			if (kits.get(i).name().equals(name)) {
				kits.remove(i);
				save(kits);
				return;
			}
		}
		// TODO refill DropDowns in GUIs  // TODO notify subscribers of type
	}

	static void save(List<KitSetup> dat) throws IOException {
		File file = Folders.getDrumSynth();
		KitDB db = new KitDB(dat);
		JsonUtil.writeJson(db, file);
	}

	public static KitSetup get(String name, RegisteredDrums type) {
		List<KitSetup> kits = saved.get(type);
		for (KitSetup k : kits)
			if (k.name().equals(name))
				return k;
		return null;
	}

	public static KitSetup get(int data1, RegisteredDrums type) {
		List<KitSetup> kits = saved.get(type);
		if (kits == null || kits.isEmpty() || data1 < 0 || data1 >= kits.size())
			return null;
		return kits.get(data1);
	}

	public static String[] getPatches(RegisteredDrums reg) {
		List<KitSetup> kits = saved.get(reg);
		String[] result = new String[kits.size()];
		for (int i = 0; i < kits.size(); i++)
			result[i] = kits.get(i).name();
		return result;
	}

}
