package net.judah.notebox;
/** Inspiration: https://github.com/void-false/sequencer */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.JudahClock;
import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;
import net.judah.util.BeatLabel;
import net.judah.util.CurrentBeat;
import net.judah.util.Constants.Pastels;

/** A Drum Grid Sequencer Table */
public class NoteBoxGui extends JPanel implements TimeListener, Pastels {

	private final JudahClock clock = JudahClock.getInstance();
	private final NoteBox tracks;
	private CurrentBeat current;

	public NoteBoxGui(NoteBox box) {
	    this.tracks = box;
	    setLayout(new BorderLayout());
	    initialize();
	    clock.addListener(this);
	}

	public void initialize() {
	    removeAll();
        NoteButtons buttonBox = new NoteButtons(tracks);
	    int count = tracks.size();

        JPanel nameBox = new JPanel();
        GridLayout nameLayout = new GridLayout(0, 1, 0, 0);
        nameBox.setLayout(nameLayout);

        nameBox.add(new JLabel(""));
        Dimension slide = new Dimension(65, 25);
        Dimension nombre = new Dimension(128, 25);
        Dimension rigid = new Dimension(5, 1);
        for(int i = 0; i < count; i++) {
            final NoteTrack note = tracks.get(i);

            Slider slider = new Slider(e -> {
                note.setVelocity(((Slider)e.getSource()).getValue() * .01f);});
            slider.setValue((int)(note.getVelocity() * 100));
            slider.setSize(slide);
            slider.setMaximumSize(slide);
            slider.setSize(slide);
            slider.setPreferredSize(slide);

            JLabel noteLbl = new JLabel("" + note.getMidi());
            noteLbl.setSize(nombre);
            noteLbl.setMaximumSize(nombre);
            // combo.setSelectedItem(note);
            // combo.addActionListener(e -> { note.setMidi(combo.getSelectedIndex()); });

            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.X_AXIS));
            pnl.add(Box.createRigidArea(rigid));
            pnl.add(slider);
            pnl.add(Box.createRigidArea(rigid));
            pnl.add(noteLbl);

            JToggleButton mute = new JToggleButton("▷");
            mute.setBackground(note.isMute() ? RED : GREEN);
            mute.setSelected(note.isMute());
            mute.addActionListener(e -> {
                note.setMute(mute.isSelected());
                mute.setBackground(note.isMute() ? RED : GREEN);
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
            NoteTrack track = tracks.get(y);
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

    private void clicked(JToggleButton btn, NoteTrack track) {
        if (btn.isSelected()) {
            for (int step = 0; step < track.getTicks().size(); step++)
                if (track.getTicks().get(step).equals(btn)) {
                    track.getBeats().add(new Note(step));
                    if (step % clock.getSubdivision() == 0)
                        btn.setBackground(Color.LIGHT_GRAY);
                }
        }
        else {
            int step = track.getTicks().indexOf(btn);
            for (Note b : new ArrayList<>(track.getBeats()))
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
