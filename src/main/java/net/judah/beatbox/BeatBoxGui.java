package net.judah.beatbox;
/** Inspiration: https://github.com/void-false/sequencer */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;

/** A Drum Grid Sequencer Table */
public class BeatBoxGui extends JPanel implements TimeListener {

	private static final Color DOWNBEAT = new Color(0xa4b9cb); // blueish
	private static final Color SELECTED = Color.LIGHT_GRAY;

	public enum Type { Drums, Melodic }

	private final JudahClock clock;
	private CurrentBeat current;

	public BeatBoxGui(JudahClock clock) {
	    this.clock = clock;
	    setLayout(new BorderLayout());
	    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    initialize();
	    clock.addListener(this);
	}

	public void initialize() {
	    removeAll();
        ButtonBox buttonBox = new ButtonBox(clock, this);

        new Thread() { @Override public void run() {

	    List<DrumTrack> tracks = clock.getBeatBox();
	    int count = tracks.size();

        JPanel nameBox = new JPanel();
        GridLayout nameLayout = new GridLayout(0, 1, 0, 0);
        nameBox.setLayout(nameLayout);

        nameBox.add(nameCombo());
        Dimension slide = new Dimension(65, 22);
        Dimension nombre = new Dimension(130, 22);
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
            nameBox.add(pnl);
        }
        GridLayout gridLayout = new GridLayout(count + 1,
                clock.getSubdivision() + 1, 1, 0);
        JPanel grid = new JPanel(gridLayout);

        // top beat labels
        current = new CurrentBeat(clock.getSteps(), clock.getSubdivision());
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
                    btn.setBackground(btn.isSelected() ? SELECTED : DOWNBEAT);
                grid.add(btn);
                track.getTicks().add(btn);
            }
        }

        add(BorderLayout.EAST, buttonBox);
        add(BorderLayout.WEST, nameBox);
        add(BorderLayout.CENTER, grid);

        doLayout();
	    }}.start();
	}

	private JComboBox nameCombo() {
//        JLabel name = new JLabel(clock.getName(), JLabel.CENTER);
//        name.setFont(Constants.Gui.BOLD);

        JComboBox<File> result = new JComboBox<>();
        for (File f : BeatBox.FOLDER.listFiles())
            result.addItem(f);
        return result;



	}

    private void clicked(JToggleButton btn, DrumTrack track) {
        if (btn.isSelected()) {
            for (int step = 0; step < track.getTicks().size(); step++)
                if (track.getTicks().get(step).equals(btn)) {
                    track.getBeats().add(new Beat(step));
                    if (step % clock.getSubdivision() == 0)
                        btn.setBackground(SELECTED);
                }
        }
        else {
            int step = track.getTicks().indexOf(btn);
            for (Beat b : new ArrayList<>(track.getBeats()))
                if (b.getStep() == step)
                    track.getBeats().remove(b);
            if (step % clock.getSubdivision() == 0)
                btn.setBackground(DOWNBEAT);
        }
    }

	@Override
	public void update(Property prop, Object value) {

        if (Property.STEP == prop && ((int)value) % 2 == 0) {
            current.setActive((int)value);
        }
	}




}
