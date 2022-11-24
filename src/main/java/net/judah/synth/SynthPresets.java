package net.judah.synth;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Scanner;

import lombok.Getter;
import net.judah.effects.CutFilter;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/*	[adsr]a/d/s/r
	[filter]vol/hihz/hirez/lohz/lorez
	[dco-0]shape/gain(/detune)
	[dco-1]shape/gain
	[dco-2]shape/gain
*/
public class SynthPresets extends ArrayList<String> { 
	
	public static final String ENVELOPE = "Envelope";
	public static final String FILTER = "Filter";
	public static final String DCO = "Dco";

	private static final String SPLIT = "/";
	private static final String OPEN = "[";
	private static final String CLOSE = "]";
	private static final String DASH = "-";
	
	private final JudahSynth synth;
	@Getter private File loaded;
	
	public SynthPresets(JudahSynth synth) {
		this.synth = synth;
		for (String s : Constants.SYNTH.list()) 
			add(s);
	}
	
	public int getProg() {
		if (loaded == null) return 0;
		String search = loaded.getName();
		for (int i = 0; i < size(); i++)
			if (get(i).equals(search))
				return i;
		return 0;
	}
	
	public void save(File f) {
		StringBuffer buf = new StringBuffer(OPEN).append(ENVELOPE).append(CLOSE);
		Adsr env = synth.getAdsr();
		buf.append(env.getAttackTime()).append(SPLIT).append(env.getDecayTime()).append(SPLIT);
		buf.append(env.getSustainGain()).append(SPLIT).append(env.getReleaseTime()).append(Constants.NL);
		buf.append(OPEN).append(FILTER).append(CLOSE).append(synth.getGain().getGain()).append(SPLIT);
		CutFilter hi = synth.getHiCut();
		CutFilter lo = synth.getLoCut();
		buf.append(hi.getFrequency()).append(SPLIT).append(hi.getResonance()).append(SPLIT);
		buf.append(lo.getFrequency()).append(SPLIT).append(lo.getResonance()).append(Constants.NL);
		int length = synth.getShapes().length -1;
		for (int i = 0; i <= length; i++) {
			buf.append(OPEN).append(DCO).append(DASH).append(i).append(CLOSE);
			buf.append(synth.getShapes()[i].name()).append(SPLIT).append(synth.getDcoGain()[i]);
			buf.append(SPLIT).append(synth.getDetune()[i]);
			if (i < length)
				buf.append(Constants.NL);
		}
        try {
            Constants.writeToFile(f, buf.toString());
            loaded = f;
            SynthView.update(synth);
        } catch (Exception e) {RTLogger.warn(SynthPresets.class, e);}
	}
	
	public boolean load(String name) {
		return load(new File(Constants.SYNTH, name));
	}
	
	public boolean load(File f) {
        if (f == null || !f.isFile()) {
            RTLogger.log(this, "missing file " + f);
            return false;
        }
        Scanner scanner = null;
        int lineNum = 0;
        try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
            	lineNum++;
                String line = scanner.nextLine();
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
                	adsr(dat.split(SPLIT), synth.getAdsr());
                }
                else if (type.equals(FILTER)) {
                	filter(dat.split(SPLIT));
                }
                else if (type.startsWith(DCO)) {
                	dco(type, dat.split(SPLIT));
                } else 
                	throw new InvalidParameterException("type: " + type); 
            }
            loaded = f;
            SynthView.update(synth);
        } catch (Throwable e) {
            RTLogger.warn(this, f.getName() + " line:" + lineNum + " - " + e.getMessage());
            return false;
        } finally {
        	if (scanner != null) scanner.close();
        	SynthView.update(synth);
        }
        return true;
	
	}
	
	private void adsr(String[] dat, Adsr adsr) {
		adsr.setAttackTime(Integer.parseInt(dat[0]));
		adsr.setDecayTime(Integer.parseInt(dat[1]));
		adsr.setSustainGain(Float.parseFloat(dat[2]));
		adsr.setReleaseTime(Integer.parseInt(dat[3]));
	}
	private void filter(String[] dat) {
		synth.getGain().setGain(Float.parseFloat(dat[0]));
		synth.getHiCut().setFrequency(Float.parseFloat(dat[1]));
		synth.getHiCut().setResonance(Float.parseFloat(dat[2]));
		synth.getLoCut().setFrequency(Float.parseFloat(dat[3]));
		synth.getLoCut().setResonance(Float.parseFloat(dat[4]));
	}
	private void dco(String name, String[] dat) {
		int idx = Integer.parseInt(name.split(DASH)[1]);
		synth.getShapes()[idx] = Shape.valueOf(dat[0]);
		synth.getDcoGain()[idx] = Float.parseFloat(dat[1]);
		synth.getDetune()[idx] = dat.length > 2 ?
			synth.getDetune()[idx] = Float.parseFloat(dat[2]) : 1f;
	}


}
