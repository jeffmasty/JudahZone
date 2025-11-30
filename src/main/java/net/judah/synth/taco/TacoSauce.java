package net.judah.synth.taco;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** Synth Preset */
public class TacoSauce {

	public static final String SPLIT = "/";
	public static final String OPEN = "[";
	public static final String CLOSE = "]";
	public static final String DASH = "-";

	public static final String ENVELOPE = "Envelope";
	public static final String FILTER = "Filter";
	public static final String DCO = "Dco";

	@AllArgsConstructor
	class Osc {
		Shape shape;
		float gain;
		float detune;
	}

	String name = "??";
	int index = 127;
	Adsr env = new Adsr();
	float[] filter = new float[4];
	Osc[] osc = new Osc[TacoSynth.DCO_COUNT];

	public TacoSauce(String header, ArrayList<String> raw) {

		for (String line : raw) {
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
            	adsr(dat.split(SPLIT));
            }
            else if (type.equals(FILTER)) {
            	filter(dat.split(SPLIT));
            }
            else if (type.startsWith(DCO)) {
            	dco(type, dat.split(SPLIT));
            } else
            	throw new InvalidParameterException("type: " + type);
        }

    	String[] split = header.split("[-]");
		name = split[1];
		if (split.length > 2 && split[2].isBlank() == false)
			try { index = Integer.parseInt(split[2]);
			} catch (NumberFormatException e) { RTLogger.log(header, e.getMessage()); }
		}

		public TacoSauce(String name2, TacoSynth synth, int idx) {
			name = name2;
			index = idx;
			env = new Adsr(synth.getAdsr());

			filter[0] = synth.getLowPass().getFrequency();
			filter[1] = synth.getLowPass().getResonance();
			filter[2] = synth.getHighPass().getFrequency();
			filter[3] = synth.getHighPass().getResonance();

			for (int i = 0; i < TacoSynth.DCO_COUNT; i++)
				osc[i] = new Osc(synth.getShapes()[i], synth.getDcoGain()[i], synth.getDetune()[i]);
		}

		public void adsr(String[] dat) {
			env.setAttackTime(Integer.parseInt(dat[0]));
			env.setDecayTime(Integer.parseInt(dat[1]));
			env.setSustainGain(Float.parseFloat(dat[2]));
			env.setReleaseTime(Integer.parseInt(dat[3]));
		}
		public void filter(String[] dat) {
			filter[0] = Float.parseFloat(dat[0]);
			filter[1] = Float.parseFloat(dat[1]);
			filter[2] = Float.parseFloat(dat[2]);
			filter[3] = Float.parseFloat(dat[3]);
		}
		public void dco(String name, String[] dat) {
			int idx = Integer.parseInt(name.split(DASH)[1]);
			float detune = dat.length > 2 ? Float.parseFloat(dat[2]) : 1f;
			osc[idx] = new Osc(Shape.valueOf(dat[0]), Float.parseFloat(dat[1]), detune);
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(DASH).append(name).append(DASH).append(index).append(Constants.NL);
			buf.append(OPEN).append(ENVELOPE).append(CLOSE);
			buf.append(env.getAttackTime()).append(SPLIT).append(env.getDecayTime()).append(SPLIT);
			buf.append(env.getSustainGain()).append(SPLIT).append(env.getReleaseTime()).append(Constants.NL);

			buf.append(OPEN).append(FILTER).append(CLOSE);
			for (int i = 0; i < filter.length; i++)
				buf.append(filter[i]).append(SPLIT);
			buf.append(Constants.NL);

			for (int i = 0; i < osc.length ; i++) {
				buf.append(OPEN).append(DCO).append(DASH).append(i).append(CLOSE);
				buf.append(osc[i].shape).append(SPLIT).append(osc[i].gain).append(SPLIT).append(osc[i].detune);
				buf.append(Constants.NL);
			}
			return buf.toString();
		}


}
