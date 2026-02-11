package net.judah.drums;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import judahzone.data.Env;
import judahzone.data.Filter;
import judahzone.data.Stage;
import judahzone.util.Folders;
import judahzone.util.JsonUtil;
import judahzone.util.RTLogger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.drums.synth.Bongo.BongoParams;
import net.judah.drums.synth.CHat.CHatParams;
import net.judah.drums.synth.Clap.ClapParams;
import net.judah.drums.synth.Kick.KickParams;
import net.judah.drums.synth.OHat.OHatParams;
import net.judah.drums.synth.Ride.RideParams;
import net.judah.drums.synth.Snare.SnareParams;
import net.judah.drums.synth.Stick.StickParams;

/** drum kit parameters to/from disk. */
@Data @AllArgsConstructor @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KitDB {
	public static enum BaseParam { Vol, Pan, Attack, Decay }

	@JsonIgnore
	static final int KITS = DrumInit.values().length;

	@JsonIgnore
    private static final List<KitSetup> synthKits = new CopyOnWriteArrayList<>();
	@JsonIgnore
	private static final List<KitSetup> sampleKits = new CopyOnWriteArrayList<>();

	static {
	    try {
	        KitDB synths = JsonUtil.readJson(Folders.getDrumSynth(), KitDB.class);
	        if (synths != null && synths.kits != null)
	        	synthKits.addAll(dedupeByName(synths.kits));
	    } catch (IOException e) {
	        RTLogger.warn(Folders.getDrumSynth().getAbsolutePath(), e);
	    }
	    try {
	        KitDB samples = JsonUtil.readJson(Folders.getOldschool(), KitDB.class);
	        if (samples != null && samples.kits != null)
	        	sampleKits.addAll(dedupeByName(samples.kits));
	    } catch (IOException e) {
	        RTLogger.warn(Folders.getOldschool().getAbsolutePath(), e);
	    }
	}

	private static List<KitSetup> dedupeByName(List<KitSetup> kits) {
	    return new ArrayList<>(kits.stream()
	        .collect(Collectors.toMap(KitSetup::name, k -> k, (first, second) -> first))
	        .values());
	}

    public static String[] getSampleKits() {
		return patches(sampleKits);
    }
    public static String[] getSynthKits() {
		return patches(synthKits);
	}

    // JSON
    List<KitSetup> kits = new ArrayList<>();

    public static record KitSetup (
    		String name,
    		boolean choke,
    		Stage[] gains,
    		Env[] env,
    		SynthKit synth/*OldSkool:null*/) {}

    // Synth-specific
    public static record SynthCommon(Filter[] lowCut, Filter[] pitch, Filter[] hiCut) {}
    public static record SynthKit(SynthCommon common,
    		KickParams kick, SnareParams snare, StickParams stick, ClapParams clap,
    		CHatParams chat, OHatParams ohat, RideParams ride, BongoParams bongo) {}

    private static String[] patches(List<KitSetup> kits) {
        return kits.stream()
                   .map(KitSetup::name)
                   .sorted(String.CASE_INSENSITIVE_ORDER)
                   .toArray(String[]::new);
    }

    /** for ProgChanges */
    public static int indexOf(KitSetup kit, boolean synth) {
    	List<KitSetup> kits = synth ? synthKits : sampleKits;
    	return kits.indexOf(kit);
    }

    // save json
	public static void addOrReplace(KitSetup setup, boolean synth) throws IOException {
		List<KitSetup> kits = synth ? synthKits : sampleKits;
		for (int i = 0; i < kits.size(); i++) {
			if (kits.get(i).name().equals(setup.name())) {
				kits.set(i, setup);
				save(new ArrayList<KitSetup>(kits), synth);
				return;
			}
		}
		kits.add(setup);
		save(kits, synth);
		// TODO refill DropDowns in GUIs  // TODO notify subscribers of type
	}

	public static void remove(String name, boolean synth) throws IOException {
		List<KitSetup> kits = synth ? synthKits : sampleKits;
		for (int i = 0; i < kits.size(); i++) {
			if (kits.get(i).name().equals(name)) {
				kits.remove(i);
				save(kits, synth);
				return;
			}
		}
		// TODO refill DropDowns in GUIs  // TODO notify subscribers of type
	}

	static void save(List<KitSetup> dat, boolean synth) throws IOException {
		File file = synth ? Folders.getDrumSynth() : Folders.getOldschool();
		KitDB db = new KitDB(dat);
		JsonUtil.writeJson(db, file);
	}

	public static KitSetup get(String name, boolean synth) {
		if (synth) {
			for (KitSetup k : synthKits)
				if (k.name().equals(name))
					return k;
		}
		else {
			for (KitSetup k : sampleKits)
				if (k.name().equals(name))
					return k;
		}
		return null;
	}

	public static KitSetup get(int data1, boolean synth) {
		if (synth) {
			if (data1 < 0 || data1 >= synthKits.size())
				return null;
			return synthKits.get(data1);
		}
		else {
			if (data1 < 0 || data1 >= sampleKits.size())
				return null;
			return sampleKits.get(data1);
		}
	}

}
