package net.judah.util;

import java.awt.Dimension;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import lombok.Getter;


/* see also: https://github.com/yoda-jm/pitch-detection.lv2 */
public class Tuner extends JPanel implements PitchDetectionHandler {

    public static final PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.MPM;
    public static final float[] GUITAR_FREQUENCIES = {82, 110, 147, 196, 247, 330};
    public static final String[] GUITAR_STRINGS = new String[] {"E", "A", "D", "G", "B", "E"};

    private AudioDispatcher dispatcher;
    private Mixer mixer;

    private final PitchProcessor dsp = new PitchProcessor(
            algo, Constants.sampleRate(), Constants.bufSize(), this);
    private final JCheckBox activeBtn;
    private final JLabel note;

    private final JSlider tuning;

    @Getter private float frequency;

    public Tuner()  {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        activeBtn = new JCheckBox();
        activeBtn.addActionListener(l -> {listen(activeBtn.isSelected());});
        note = new JLabel(" ");
        Dimension d = new Dimension(35, 20);
        note.setMinimumSize(d);
        note.setPreferredSize(d);
        tuning = new JSlider(0, 80);
        d = new Dimension(125, 30);
        tuning.setPreferredSize(d);
        tuning.setMinimumSize(d);
        tuning.setMajorTickSpacing(20);
        tuning.setMinorTickSpacing(5);
        tuning.setPaintTicks(true);

        add(Box.createHorizontalStrut(5));
        add(new JLabel("tuner"));
        add(Box.createHorizontalStrut(5));
        add(activeBtn);
        add(Box.createHorizontalStrut(5));
        add(tuning);
        add(Box.createHorizontalStrut(5));
        add(note);

        new PitchProcessor(algo, Constants.sampleRate(), Constants.bufSize(), this);

        
        
        for(Mixer.Info info : JavaSound.getMixerInfo(false, true))
            if (info.getName().contains("default")) mixer = AudioSystem.getMixer(info);
    }

    public void listen(boolean active)  {

        if (dispatcher!= null) {
            dispatcher.stop();
        }
        if (!active) {
            note.setText(" ");
            return;
        }

        final AudioFormat format = new AudioFormat(
                Constants.sampleRate(), 24, 1, true, true);
        final DataLine.Info dataLineInfo = new DataLine.Info(
                TargetDataLine.class, format);
        TargetDataLine line;

        try {
            line = (TargetDataLine) mixer.getLine(dataLineInfo);
            final int numberOfSamples = Constants.bufSize();
            line.open(format, numberOfSamples);
            line.start();
            final AudioInputStream stream = new AudioInputStream(line);

            JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
            // create a new dispatcher
            dispatcher = new AudioDispatcher(audioStream, Constants.bufSize(), 0);

            // add a processor
            dispatcher.addAudioProcessor(dsp);

            new Thread(dispatcher,"Audio dispatching").start();
        } catch (LineUnavailableException e) {
            Console.warn(e);
        }

    }

    @Override
    public void handlePitch(PitchDetectionResult info,AudioEvent audioEvent) {
        if (info.getPitch() == -1 || info.getProbability() < 0.9f) {
            note.setText(" ");
            return;
        }
        frequency = info.getPitch();

        int idx = detectString(frequency);
        if (idx == -1) {
            note.setText(" ");
            return;
        }
        note.setText(GUITAR_STRINGS[idx]);

        // slider shows +/- 4 hz of frequencies
        tuning.setValue(40 - Math.round((GUITAR_FREQUENCIES[idx] - frequency) * 10));

    }

    /** https://github.com/dylstuart/GuitarTunerAndroid/blob/master/GuitarTuner%20-%20Copy/app/src/main/java/com/example/android/guitartuner/FrequencyDetector.java */
    public static int detectString(float freq) {
        int result = -1;
        float min = 12345;

        for(int i = 0; i < GUITAR_FREQUENCIES.length; i++) {
            float dFromCurrentToDesired = Math.abs(GUITAR_FREQUENCIES[i] - freq);
            if (dFromCurrentToDesired < min) {
                min = dFromCurrentToDesired;
                result = i;
            }
        }
        return result;
    }

    //from Tarsus DSP examples Shared.java
	public static Vector<Mixer.Info> getMixerInfo(
			final boolean supportsPlayback, final boolean supportsRecording) {
		final Vector<Mixer.Info> infos = new Vector<Mixer.Info>();
		final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		for (final Mixer.Info mixerinfo : mixers) {
			if (supportsRecording
					&& AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
				// Mixer capable of recording audio if target LineWavelet length != 0
				infos.add(mixerinfo);
			} else if (supportsPlayback
					&& AudioSystem.getMixer(mixerinfo).getSourceLineInfo().length != 0) {
				// Mixer capable of audio play back if source LineWavelet length != 0
				infos.add(mixerinfo);
			}
		}
		return infos;
	}
}
