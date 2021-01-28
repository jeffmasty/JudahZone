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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;
import net.judah.util.Constants;
import net.judah.util.FileChooser;

/** A Drum Grid Sequencer Table */
public class BeatBoxGui extends JPanel implements TimeListener {

	public static final int INSET = 15;
	public static final int WIDTH = 800 + 2 * INSET;
	public static final int HEIGHT = 200 + 2 * INSET;
	private static final Color DOWNBEAT = Color.GREEN;
	private static final Color SELECTED = Color.LIGHT_GRAY;

	public enum Type { Drums, Melodic }

	private final JudahClock clock;

	CurrentBeat current;

	private final JComboBox<GMKit> kits = new JComboBox<>(GMKit.values());
    private JLabel beat = new JLabel("current: ??");
    @Getter private JLabel tempo = new JLabel("tempo: ??");

	final ArrayList<DrumTrack> tracks = new ArrayList<>();
	//Slider/Knob volume, swing;

	public BeatBoxGui(JudahClock clock) {
	    this.clock = clock;
	    setLayout(new BorderLayout());
	    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    initialize();
	    clock.addListener(this);
	}

	public void initialize() {
	    removeAll();

	    List<DrumTrack> tracks = clock.getBeatBox();
	    int count = tracks.size();

        JPanel nameBox = new JPanel();
        GridLayout nameLayout = new GridLayout(0, 1, 0, 0);
        nameBox.setLayout(nameLayout);

        JLabel name = new JLabel(clock.getName(), JLabel.CENTER);
        name.setFont(Constants.Gui.BOLD);
        nameBox.add(name);
        Dimension slide = new Dimension(65, 22);
        Dimension nombre = new Dimension(130, 22);
        Dimension rigid = new Dimension(5, 1);
        for(int i = 0; i < count; i++) {

            final DrumTrack drum = tracks.get(i);
            Slider slider = new Slider(e -> {
                drum.setVelocity(((Slider)e.getSource()).getValue() * .01f);});
            slider.setValue(100);
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
            pnl.add(Box.createRigidArea(rigid));
            nameBox.add(pnl);
        }

        GridLayout gridLayout = new GridLayout(count + 1,
                clock.getSubdivision(), 1, 0);
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

        JPanel buttonBox = new JPanel();
        buttonBox.setLayout(new GridLayout(0, 1));

        buttonBox.setOpaque(false);
        buttonBox.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 5));

        JButton start = new JButton("Start");
        start.addActionListener(e -> {clock.begin();});
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> {clock.end();});
        buttonBox.add(stop);

        JButton load = new JButton("Load");
        load.addActionListener(e -> {load();});
        buttonBox.add(load);

        JButton save = new JButton("Save");
        save.addActionListener(e -> {save();});
        buttonBox.add(save);

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> {
            clock.getBeatBox().initialize();
            initialize();
        });
        buttonBox.add(clear);

        JButton random = new JButton("Randomize");
        buttonBox.add(random);

        kits.addActionListener(e -> {
            clock.getBeatBox().setKit(((GMKit)kits.getSelectedItem()));});
        Dimension d = new Dimension(100, 20);
        kits.setSize(d); kits.setPreferredSize(d); kits.setMaximumSize(d);
        buttonBox.add(new JLabel("drumkit"));
        buttonBox.add(kits);

        buttonBox.add(beat);
        buttonBox.add(tempo);

        add(BorderLayout.WEST, nameBox);
        add(BorderLayout.CENTER, grid);
        add(BorderLayout.EAST, buttonBox);
        doLayout();
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

    public void load() {
        FileChooser.setCurrentDir(new File(Constants.ROOT, "patterns"));
        File f = FileChooser.choose();
        if (f != null)
            clock.parse(f);
    }

    public void save() {
        FileChooser.setCurrentDir(new File(Constants.ROOT, "patterns"));
        File f = FileChooser.choose();
        if (f != null)
            clock.write(f);
    }


	@Override
	public void update(Property prop, Object value) {

        if (Property.STEP == prop && ((int)value) % 2 == 0) {
            current.setActive((int)value);
        } else if (Property.BEAT == prop) {
			beat.setText("current: " + value);
		}
	}




}
