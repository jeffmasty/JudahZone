package net.judah.gui.widgets;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import lombok.Getter;
import net.judah.mixer.Channel;
import net.judah.util.Constants;


/* see also: https://github.com/yoda-jm/pitch-detection.lv2 */
public class GuitarTuner extends JPanel {

    public static final PitchEstimationAlgorithm algo = PitchEstimationAlgorithm.MPM;
    public static final float[] GUITAR_FREQUENCIES = {82, 110, 147, 196, 247, 330};
    public static final String[] GUITAR_STRINGS = new String[] {"E", "A", "D", "G", "B", "E"};
    
    @Getter private static Channel channel;

    private final PitchDetector pitchDetector = 
    		algo.getDetector(Constants.sampleRate(), Constants.bufSize());

    private final JLabel note;
    private final JSlider tuning;
    @Getter private float frequency;
    
    public GuitarTuner()  {
    	setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        note = new JLabel(" ");
        Dimension d = new Dimension(35, 30);
        note.setMinimumSize(d);
        note.setPreferredSize(d);
        tuning = new RainbowFader(0, 80, e ->{/*no-op*/});
        tuning.setOrientation(JSlider.HORIZONTAL);
        
        //tuning = new JSlider(0, 80);
        d = new Dimension(90, 30);
        tuning.setPreferredSize(d);
        tuning.setMinimumSize(d);
        tuning.setMajorTickSpacing(20);
        tuning.setMinorTickSpacing(27);
        tuning.setPaintTicks(true);
        add(Box.createHorizontalStrut(10));
        add(note);
        add(Box.createHorizontalStrut(5));
        add(tuning);
        add(Box.createHorizontalStrut(10));
        doLayout();
    }

    public void process(float[] data) {
    	PitchDetectionResult info = pitchDetector.getPitch(data);
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
    
    /**@param ch set to null to make invisible and disable audio processing*/
    public void setChannel(Channel ch) {
    	channel = ch;
    	setVisible(ch != null);
    }
	
}
