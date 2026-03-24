package net.judah.gui.knobs.synth;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import judahzone.gui.Gui;
import judahzone.synth.Synth;
import judahzone.synth.WavSynth;
import judahzone.widgets.SingleRow;
import net.judah.channel.Generator;
import net.judah.gui.drums.views.DrumView;
import net.judah.gui.drums.views.DrumView.DataLbl;
import net.judah.gui.widgets.Slider;

/**
 * Encapsulates the amp/gain UI previously held inside SynthKnobs.gainStage().
 * Provides a constructor to build the UI and an update() method to sync values
 * from the provided WavSynth.
 */
public class GainStage extends Box {

    // UI components (extracted from SynthKnobs.gainStage())
    private final JSlider preamp;
    private final JSlider volume;
    private final JSlider pan;
    private final JSlider bend;

    private final DataLbl preampFb;
    private final DataLbl volumeFb;
    private final DataLbl bendFb;

    private final Box amp;

    private final WavSynth synth;

    /** Build the gain-stage UI for the given synth and generator line. */
    public GainStage(WavSynth synth, Generator line) {
        super(BoxLayout.Y_AXIS);
        this.synth = synth;

        // feedback labels
        this.preampFb = new DataLbl();
        this.volumeFb = new DataLbl();
        this.bendFb = new DataLbl();

        // sliders (use project Slider wrapper for consistent styling)
        this.preamp = new Slider(0, 200);
        this.volume = new Slider();
        this.pan = new Slider();
        this.bend = new Slider(1, 12);

        // icon column
        Box icon = new Box(BoxLayout.PAGE_AXIS);
        icon.add(new JLabel(line.getIcon(), JLabel.CENTER));
        icon.add(new JLabel(line.getName(), JLabel.CENTER));
        icon.setBorder(Gui.SUBTLE);

        // configure individual rows
        preamp.setMinorTickSpacing(50);
        preamp.setPaintTicks(true);
        Box preampRow = new SingleRow("Preamp", preamp, preampFb);

        volume.setMinorTickSpacing(25);
        volume.setPaintTicks(true);
        Box volumeRow = new SingleRow("Volume", volume, volumeFb);

        JLabel panLbl = new JLabel("Pan    L", JLabel.CENTER);
        Gui.resize(panLbl, DrumView.SZ.LABEL_DIM);
        JLabel rLbl = new JLabel("R", JLabel.CENTER);
        Gui.resize(rLbl, DrumView.SZ.LABEL_DIM);
        pan.setMinorTickSpacing(25);
        pan.setPaintTicks(true);
        Box panRow = new SingleRow(panLbl, pan, rLbl);

        bend.setMinorTickSpacing(1);
        bend.setPaintTicks(true);
        Box bendRow = new SingleRow("Bend Mod", bend, bendFb);

        // assemble gain stage
        Box gainStage = new Box(BoxLayout.Y_AXIS);
        gainStage.add(preampRow);
        gainStage.add(volumeRow);
        gainStage.add(panRow);
        gainStage.add(bendRow);

        amp = new Box(BoxLayout.LINE_AXIS);
        amp.add(Box.createHorizontalStrut(8));
        amp.add(icon);
        amp.add(Box.createHorizontalStrut(10));
        amp.add(gainStage);

        // put together without extra top/bottom struts to avoid double padding
        add(amp);
        listeners();
    }

    private void listeners() {
        preamp.addChangeListener(l-> synth.set(Synth.PREAMP, preamp.getValue()));
        volume.addChangeListener(l-> synth.set(Synth.GAIN, volume.getValue()));
        pan.addChangeListener(l-> synth.set(Synth.PAN, pan.getValue()));
        pan.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) { pan.setValue(50); }
            }});

        bend.addChangeListener(l-> {
            if (synth.getModWheel().getSemitones() != bend.getValue())
                synth.setModSemitones(bend.getValue());
        });
    }

    /** Synchronize UI controls with synth state. Safe to call from EDT. */
    public void update() {
        int pre = synth.get(Synth.PREAMP);
        if (preamp.getValue() != pre)
            preamp.setValue(pre);
        preampFb.setText("" + pre);

        int gain = synth.get(Synth.GAIN);
        if (volume.getValue() != gain)
            volume.setValue(gain);
        volumeFb.setText("" + gain);

        int width = synth.get(Synth.PAN);
        if (pan.getValue() != width)
            pan.setValue(width);

        int bendVal = synth.getModWheel().getSemitones();
        if (bend.getValue() != bendVal)
            bend.setValue(bendVal);
        bendFb.setText("" + bendVal);
    }

    // Expose some controls so parent code can attach listeners if needed
    public JSlider getPreamp() { return preamp; }
    public JSlider getVolume() { return volume; }
    public JSlider getPan() { return pan; }
    public JSlider getBend() { return bend; }
}
