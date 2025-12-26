package net.judah.synth.taco;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import lombok.Getter;
import net.judah.fx.MonoFilter;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.omni.Threads;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class SynthDB extends ArrayList<TacoSauce> {

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
        ArrayList<String> raw = new ArrayList<>();
        Scanner scanner = null;
        int lineNum = 0;
        try {
            scanner = new Scanner(file);
            String header = null;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(DASH)) {
                	if (!raw.isEmpty() && header != null)
                		add(new TacoSauce(header, raw));
                	header = line;
                	raw.clear();
                }
                else
                	raw.add(line);
            }
            if (!raw.isEmpty() && header != null) // last in file
            	add(new TacoSauce(header, raw));
            if (isEmpty())
            	throw new ExceptionInInitializerError("No Synth Presets loaded");
        } catch (Throwable e) {
            RTLogger.warn(this, file.getName() + " line:" + lineNum + " - " + e.getMessage());
            return;
        } finally {
        	if (scanner != null) scanner.close();
        }
        RTLogger.debug(this, "loaded " + size() + " presets");
        // reindex();
	}

	@SuppressWarnings("unused")
	private void reindex() {
		for (int i = 0; i < size(); i++)
			get(i).index = i;
		save();
	}

	public TacoSauce get(String name) {
		for (TacoSauce p : this)
			if (p.name.equals(name))
				return p;
		return null;
	}

	public List<String> keys() {
		ArrayList<String> result = new ArrayList<>();
		for (TacoSauce p : this)
			result.add(p.name);
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

	/** save new preset or replace old preset */
	public void save(TacoSynth synth, String name) {
		TacoSauce preset = new TacoSauce(name, synth, size());
		if (keys().contains(name)) {
			TacoSauce old = get(name);
			preset.index = old.index;
			set(indexOf(old), preset);
		}
		else
			add(preset);
		save();
		MainFrame.update(synth);
	}

	private void save() {
		StringBuffer buf = new StringBuffer();
		for (String key : keys())
			buf.append(get(key).toString());
        try {
            Threads.writeToFile(file, buf.toString());
        } catch (Exception e) {RTLogger.warn(this, e);}
	}

	public String create(TacoSynth synth) {
		StringBuffer buf = new StringBuffer(OPEN).append(ENVELOPE).append(CLOSE);
		Adsr env = synth.getAdsr();
		buf.append(env.getAttackTime()).append(SPLIT).append(env.getDecayTime()).append(SPLIT);
		buf.append(env.getSustainGain()).append(SPLIT).append(env.getReleaseTime()).append(Constants.NL);
		buf.append(OPEN).append(FILTER).append(CLOSE);
		MonoFilter hc = synth.getLowPass();
		MonoFilter lc = synth.getHighPass();
		buf.append(hc.getFrequency()).append(SPLIT).append(hc.getResonance()).append(SPLIT);
		buf.append(lc.getFrequency()).append(SPLIT).append(lc.getResonance()).append(Constants.NL);

		int length = synth.getShapes().length -1;
		for (int i = 0; i <= length; i++) {
			buf.append(OPEN).append(DCO).append(DASH).append(i).append(CLOSE);
			buf.append(synth.getShapes()[i].name()).append(SPLIT).append(synth.getDcoGain()[i]);
			buf.append(SPLIT).append(synth.getDetune()[i]);
			buf.append(Constants.NL);
		}
		return buf.toString();
	}

	public boolean apply(String name, TacoSynth synth) {
		TacoSauce p = get(name);
		if (p == null)
			return false;
		synth.getAdsr().set(p.env);
		synth.getLowPass().setFrequency(p.filter[0]);
		synth.getLowPass().setResonance(p.filter[1]);
		synth.getHighPass().setFrequency(p.filter[2]);
		synth.getHighPass().setResonance(p.filter[3]);
		for (int i = 0; i < p.osc.length; i++) {
			synth.getShapes()[i] = p.osc[i].shape;
			synth.getDetune()[i] = p.osc[i].detune;
			synth.getDcoGain()[i] = p.osc[i].gain;
		}
		if (MainFrame.getKnobMode() == KnobMode.Taco)
			MainFrame.update(MainFrame.getKnobs());
		return true;
	}


}
