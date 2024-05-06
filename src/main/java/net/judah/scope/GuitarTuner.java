package net.judah.scope;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Gui;
import net.judah.gui.widgets.RainbowFader;
import net.judah.scope.Scope.Mode;
import net.judah.util.Constants;


/* see also: https://github.com/yoda-jm/pitch-detection.lv2 */
public class GuitarTuner extends ScopeView {

    public static final float[] GUITAR_FREQUENCIES = {82, 110, 147, 196, 247, 330};
    public static final String[] GUITAR_STRINGS = new String[] {"E", "A", "D", "G", "B", "E"};
    private static final Dimension SLIDER = new Dimension(220, 50);
    private static final Dimension LBL = new Dimension(35, 40);
    
    private static PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.MPM;
	@Setter public static float probablity = 0.8f;
    private static PitchDetector pitchDetector = 
    		algo.getDetector(Constants.sampleRate(), Constants.bufSize());
    public static void setAlgorithm(PitchEstimationAlgorithm algo) {
    	GuitarTuner.algo = algo;
    	pitchDetector = algo.getDetector(Constants.sampleRate(), Constants.bufSize());
    }
    
    
    @Getter private final Mode mode = Mode.Tuner;
    private final JLabel note;
    private final JSlider tuning;
    
//    private final JComboBox<PitchEstimationAlgorithm> algorithms = new JComboBox<>();
//    private final JComboBox<Float> chance = new JComboBox<>();
    
    public GuitarTuner(Scope scope)  {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
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
        add(Box.createHorizontalStrut(2 * strut));
        add(note);
        add(Box.createHorizontalStrut(strut));
        add(tuning);
        add(Box.createHorizontalStrut(2 * strut));
        doLayout();
    }
    
    /** @return computed hz or -1 */
	public static float detectPitch(float[] data) {
    	PitchDetectionResult info = pitchDetector.getPitch(data);
    	if (info.getProbability() < probablity) 
    		return -1;
    	return info.getPitch();
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
	
	
	@Override
	public void process(float[][] stereo) {
		float frequency = detectPitch(stereo[0]);
        if (frequency <= 0) {
            note.setText("X");
            return;
        }
        	
        int idx = detectString(frequency);
        if (idx == -1) {
            note.setText("-1");
            return;
        }
        note.setText(GUITAR_STRINGS[idx]);

        // slider shows +/- 4 hz of frequencies
        tuning.setValue(40 - Math.round((GUITAR_FREQUENCIES[idx] - frequency) * 10));
    }



}
