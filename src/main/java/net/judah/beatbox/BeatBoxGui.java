package net.judah.beatbox;
/** Inspiration: https://github.com/void-false/sequencer */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.JudahClock;
import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;
import net.judah.util.BeatLabel;
import net.judah.util.Constants.Pastels;
import net.judah.util.CurrentBeat;

/** A Drum Grid Sequencer Table */
public class BeatBoxGui extends JPanel implements TimeListener, Pastels {

	private final JudahClock clock = JudahClock.getInstance();
	private final BeatBox tracks;
	private CurrentBeat current;

	public BeatBoxGui(BeatBox box) {
	    this.tracks = box;
	    setLayout(new BorderLayout());
	    initialize();
	    clock.addListener(this);
	}

	public void initialize() {
	    removeAll();
        BeatButtons buttonBox = new BeatButtons(tracks);
	    int count = tracks.size();

        JPanel nameBox = new JPanel();
        GridLayout nameLayout = new GridLayout(0, 1, 0, 0);
        nameBox.setLayout(nameLayout);

        nameBox.add(new JLabel(""));
        Dimension slide = new Dimension(65, 25);
        Dimension nombre = new Dimension(128, 25);
        Dimension rigid = new Dimension(5, 1);
        for(int i = 0; i < count; i++) {
            final DrumTrack drum = tracks.get(i);
            Slider slider = new Slider(e -> {
                drum.setVelocity(((Slider)e.getSource()).getValue() * .01f);});
            slider.setValue((int)(drum.getVelocity() * 100));
            slider.setSize(slide);
            slider.setMaximumSize(slide);
            slider.setSize(slide);
            slider.setPreferredSize(slide);
            JComboBox<GMDrum> combo = new JComboBox<>(GMDrum.values());
            combo.setSize(nombre);
            combo.setMaximumSize(nombre);
            combo.setSelectedItem(drum.getDrum());
            combo.addActionListener(e -> { drum.setDrum((GMDrum)combo.getSelectedItem()); });

            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
            pnl.add(Box.createRigidArea(rigid));
            pnl.add(slider);
            pnl.add(Box.createRigidArea(rigid));
            pnl.add(combo);

            JToggleButton mute = new JToggleButton("▷");
            mute.setBackground(drum.isMute() ? RED : GREEN);
            mute.setSelected(drum.isMute());
            mute.addActionListener(e -> {
                drum.setMute(mute.isSelected());
                mute.setBackground(drum.isMute() ? RED : GREEN);
            });

            pnl.add(mute);
            nameBox.add(pnl);
        }
        GridLayout gridLayout = new GridLayout(count + 1,
                clock.getSubdivision() + 1, 1, 0);
        JPanel grid = new JPanel(gridLayout);

        // top beat labels
        current = new CurrentBeat(clock);
        for (BeatLabel l : current.getLabels())
            grid.add(l);

        for (int y = 0; y < count; y++) {
            DrumTrack track = tracks.get(y);
            track.getTicks().clear();
            for(int x = 0; x < clock.getSteps(); x++) {
                JToggleButton btn = new JToggleButton();
                btn.setSelected(track.hasStep(x));
                btn.addActionListener(e -> {clicked(btn, track);});
                if (x % clock.getSubdivision() == 0)
                    btn.setBackground(btn.isSelected() ? Color.LIGHT_GRAY: BLUE);
                grid.add(btn);
                track.getTicks().add(btn);
            }
        }

        add(BorderLayout.EAST, buttonBox);
        add(BorderLayout.WEST, nameBox);
        add(BorderLayout.CENTER, grid);

        doLayout();

	}

    private void clicked(JToggleButton btn, DrumTrack track) {
        if (btn.isSelected()) {
            for (int step = 0; step < track.getTicks().size(); step++)
                if (track.getTicks().get(step).equals(btn)) {
                    track.getBeats().add(new Beat(step));
                    if (step % clock.getSubdivision() == 0)
                        btn.setBackground(Color.LIGHT_GRAY);
                }
        }
        else {
            int step = track.getTicks().indexOf(btn);
            for (Beat b : new ArrayList<>(track.getBeats()))
                if (b.getStep() == step)
                    track.getBeats().remove(b);
            if (step % clock.getSubdivision() == 0)
                btn.setBackground(BLUE);
        }
    }

	@Override
	public void update(Property prop, Object value) {

        if (Property.STEP == prop && ((int)value) % 2 == 0) {
            current.setActive((int)value);
        }
	}




}
