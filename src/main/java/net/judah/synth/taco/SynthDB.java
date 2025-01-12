package net.judah.synth.taco;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import lombok.Getter;
import net.judah.fx.Filter;
import net.judah.gui.MainFrame;
import net.judah.omni.Threads;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/*	-NAME-
 *  [adsr]a/d/s/r
	[filter]vol/hihz/hirez/lohz/lorez
	[dco-0]shape/gain(/detune)
	[dco-1]shape/gain(/detune)
	[dco-2]shape/gain(/detune)
*/
public class SynthDB extends HashMap<String, String> {

	public static final String SPLIT = "/";
	public static final String OPEN = "[";
	public static final String CLOSE = "]";
	public static final String DASH = "-";

	public static final String ENVELOPE = "Envelope";
	public static final String FILTER = "Filter";
	public static final String DCO = "Dco";

	@Getter private File loaded;
	private File file;

	public SynthDB() {
		this(Folders.getSynthPresets());
	}

	public SynthDB(File file) {
		this.file = file;
		loadFile();
	}

	public void loadFile() {
		clear();
        if (file == null || !file.isFile()) {
            RTLogger.log(this, "missing file " + file);
            return;
        }
        ArrayList<String> preset = new ArrayList<>();
        Scanner scanner = null;
        int lineNum = 0;
        try {
            scanner = new Scanner(file);
            String name = "";
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(DASH)) {
                	if (!preset.isEmpty() && name != null)
                		put(name, pack(preset));
                	name = line.split("[-]")[1];
                	preset.clear();
                }
                else {
                	preset.add(line);
                }
            }
            if (!preset.isEmpty() && name != null) // last in file
            	put(name, pack(preset));
            if (keySet().isEmpty())
            	throw new ExceptionInInitializerError("No Synth Presets loaded");
        } catch (Throwable e) {
            RTLogger.warn(this, file.getName() + " line:" + lineNum + " - " + e.getMessage());
            return;
        } finally {
        	if (scanner != null) scanner.close();
        }
	}

	private String pack(List<String> preset) {
		StringBuffer result = new StringBuffer();
		for (String s : preset)
			result.append(s).append(Constants.NL);
		return result.toString();
	}

	public List<String> keys() {
		ArrayList<String> result = new ArrayList<>(keySet());
		Collections.sort(result);
		return result;
	}

	public int getProg(String s) {
		if (s == null) return 0;
		List<String> names = keys();
		for (int i = 0; i < names.size(); i++)
			if (s.equals(names.get(i)))
					return i;
		return 0;
	}

	public void save(TacoSynth synth, String name) {
		put(name, create(synth));
		StringBuffer buf = new StringBuffer();
		for (String key : keys()) {
			buf.append(DASH).append(key).append(DASH).append(Constants.NL);
			buf.append(get(key));
		}
        try {
            Threads.writeToFile(file, buf.toString());
        } catch (Exception e) {RTLogger.warn(SynthDB.class, e);}
		MainFrame.update(synth);
	}

	public String create(TacoSynth synth) {

		StringBuffer buf = new StringBuffer(OPEN).append(ENVELOPE).append(CLOSE);
		Adsr env = synth.getAdsr();
		buf.append(env.getAttackTime()).append(SPLIT).append(env.getDecayTime()).append(SPLIT);
		buf.append(env.getSustainGain()).append(SPLIT).append(env.getReleaseTime()).append(Constants.NL);
		buf.append(OPEN).append(FILTER).append(CLOSE);
		Filter hi = synth.getHiCut();
		Filter lo = synth.getLoCut();
		buf.append(hi.getFrequency()).append(SPLIT).append(hi.getResonance()).append(SPLIT);
		buf.append(lo.getFrequency()).append(SPLIT).append(lo.getResonance()).append(Constants.NL);

		int length = synth.getShapes().length -1;
		for (int i = 0; i <= length; i++) {
			buf.append(OPEN).append(DCO).append(DASH).append(i).append(CLOSE);
			buf.append(synth.getShapes()[i].name()).append(SPLIT).append(synth.getDcoGain()[i]);
			buf.append(SPLIT).append(synth.getDetune()[i]);
			buf.append(Constants.NL);
		}
		return buf.toString();
	}

	public boolean apply(String name, SynthPresets handler) {
		String preset = get(name);
		if (preset == null)
			return false;
		for (String line : preset.split(Constants.NL)) {
			if (!line.startsWith(OPEN))
            	throw new InvalidParameterException("format: " + OPEN);
            line = line.substring(1);
            if (line.indexOf(CLOSE) == -1)
            	throw new InvalidParameterException("format: " + CLOSE);
            String[] split = line.split(CLOSE);
            if (split.length != 2)
            	throw new InvalidParameterException("format: [?]?");
            String type = split[0];
            String dat = split[1];
            if (type.equals(ENVELOPE)) {
            	handler.adsr(dat.split(SPLIT));
            }
            else if (type.equals(FILTER)) {
            	handler.filter(dat.split(SPLIT));
            }
            else if (type.startsWith(DCO)) {
            	handler.dco(type, dat.split(SPLIT));
            } else
            	throw new InvalidParameterException("type: " + type);
        }
		return true;
	}



}
