package net.judah.gui.waves;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import lombok.Getter;
import net.judah.api.Key;
import net.judah.api.Key.Note;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.RainbowFader;
import net.judah.util.Constants;

public class Tuner extends Box implements Updateable {
	public static record Tuning(Tuner tuner, float[][] buffer) {}

	public static final int TUNER_HEIGHT = 45;
    private static final Dimension SLIDER = new Dimension(Size.WIDTH_KNOBS - 130, TUNER_HEIGHT);
    private static final Dimension LBL = new Dimension(80, TUNER_HEIGHT - 10);

    private static final float probablity = 0.8f;
    private final PitchDetector tuner;
    @Getter private final int paramCount = 0; // TODO bypass, pitch algo
    @Getter private boolean active;
    private final JSlider tuning = new RainbowFader(e ->{/*no-op*/});
    private final JToggleButton toggle = new JToggleButton("Tuner");

	public Tuner() {
	   	super(BoxLayout.X_AXIS);
	   	tuner = PitchEstimationAlgorithm.MPM.getDetector(
	   			Constants.sampleRate(), Constants.bufSize());

	   	toggle.addActionListener(e->setActive(!isActive()));
	   	toggle.setOpaque(true);
        tuning.setOrientation(JSlider.HORIZONTAL);
        tuning.setMajorTickSpacing(50);
        tuning.setMinorTickSpacing(20);
        tuning.setPaintTicks(true);
        tuning.setOpaque(true);
        tuning.setEnabled(false);

        int strut = 15;
        add(Box.createHorizontalStrut(strut));
        add(Gui.resize(tuning, SLIDER));
        add(Box.createHorizontalStrut(strut));
        add(Gui.resize(toggle, LBL));
        add(Box.createHorizontalStrut(strut));
        doLayout();
        update();
	}
	@Override
	public void update() {
        if (!active) {
        	toggle.setText("Tuner");
        	tuning.setValue(0);
        	toggle.setFont(Gui.FONT11);
        }
        else {
    	   	toggle.setFont(Gui.BOLD12);
        }
        toggle.setBackground(active ? Pastels.GREEN : null);
	}

	public void update(float[][] data) {
		if (!active)
			return;
		float frequency = detectPitch(data[0]);
        if (frequency <= 0)
        	return;

        Note eureka = Key.toNote(frequency);
        if (eureka == null)
        	return;
        float nearest = Key.toFrequency(eureka);
        float diff = frequency - nearest;
        float scaleFactor = calculateScaleFactor(eureka.octave());

        // Calculate slider value based on freq. diff and scale factor
        int sliderValue = 50 + Math.round(diff * scaleFactor);

		tuning.setValue(sliderValue);
        toggle.setText(eureka.toString());
	}

	/** slider less sensitive to frequency diff as octave increases */
	private float calculateScaleFactor(int octave) {
		float baseScaleFactor = 0.8f;  // Base scale factor for octave 0
	    return (float) (baseScaleFactor * Math.pow(2, octave));
	}

    /** @return computed hz or -1 */
	public float detectPitch(float[] workarea) {
		PitchDetectionResult info = tuner.getPitch(workarea);
    	return info.getProbability() < probablity ? -1 : info.getPitch();
    }

	public void setActive(boolean active) {
		this.active = active;
		MainFrame.update(this);
	}


}

