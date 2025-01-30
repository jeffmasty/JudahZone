package net.judah.gui.widgets;

import static be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm.MPM;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import net.judah.gui.Gui;
import net.judah.util.Constants;

// Legacy, PitchDetetector now an Effect of Channel
public class GuitarTuner extends JPanel {

    public static final float[] GUITAR_FREQUENCIES = {82, 110, 147, 196, 247, 330};
    public static final String[] GUITAR_STRINGS = new String[] {"E", "A", "D", "G", "B", "E"};
    private static final Dimension SLIDER = new Dimension(220, 50);
    private static final Dimension LBL = new Dimension(35, 40);

	private static float probablity = 0.8f;
	private PitchDetector pitchDetector;
    private final JLabel note;
    private final JSlider tuning;
    // private final JComboBox<PitchEstimationAlgorithm> algo = new JComboBox<>(PitchEstimationAlgorithm.values());
    // private final JComboBox<Float> chance = new JComboBox<>();

    public GuitarTuner()  {
    	JComponent tuner = Box.createHorizontalBox();

    	setupPitch(MPM);
        note = new JLabel(" ");
        note.setFont(Gui.BOLD);
        note.setBorder(Gui.SUBTLE);
        tuning = new RainbowFader(0, 80, e ->{/*no-op*/});
        tuning.setOrientation(JSlider.HORIZONTAL);
        tuning.setMajorTickSpacing(20);
        tuning.setMinorTickSpacing(27);
        tuning.setPaintTicks(true);
        tuning.setEnabled(false);
        Gui.resize(tuning, SLIDER);
        Gui.resize(note, LBL);

        int strut = 10;
        tuner.add(Box.createHorizontalStrut(2 * strut));
        tuner.add(note);
        tuner.add(Box.createHorizontalStrut(strut));
        tuner.add(tuning);
        tuner.add(Box.createHorizontalStrut(2 * strut));

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(tuner);
    	// algo.addActionListener(e->setupPitch((PitchEstimationAlgorithm) algo.getSelectedItem()));
    	// algo.setSelectedItem(MPM);
        // add(Gui.wrap(algo));
        doLayout();
    }

    private void setupPitch(PitchEstimationAlgorithm model) {
    	pitchDetector = model.getDetector(Constants.sampleRate(), Constants.bufSize());
    }
    /** @return computed hz or -1 */
	float detectPitch(float[] data) {
		PitchDetectionResult info = pitchDetector.getPitch(data);
    	return info.getProbability() < probablity ? -1 : info.getPitch();
    }

	/** https://github.com/dylstuart/GuitarTunerAndroid/blob/master/GuitarTuner%20-%20Copy/app/src/main/java/com/example/android/guitartuner/FrequencyDetector.java */
    public static int detectString(float freq) {
        float min = 12345;
        for(int i = 0; i < GUITAR_FREQUENCIES.length; i++) {
            float dFromCurrentToDesired = Math.abs(GUITAR_FREQUENCIES[i] - freq);
            if (dFromCurrentToDesired < min) {
                min = dFromCurrentToDesired;
                return i;
            }
        }
        return -1;
    }

	public void process(float[][] stereo) {
		float frequency = detectPitch(stereo[0]);
        if (frequency <= 0) {
            note.setText("X");
            return;
        }

        int idx = detectString(frequency);
        if (idx == -1) {
            note.setText("-1");
            tuning.setEnabled(false);
            return;
        }
        note.setText(GUITAR_STRINGS[idx]);
        tuning.setEnabled(true);
        // slider shows +/- 4 hz of frequencies
        tuning.setValue(40 - Math.round((GUITAR_FREQUENCIES[idx] - frequency) * 10));
    }

}
