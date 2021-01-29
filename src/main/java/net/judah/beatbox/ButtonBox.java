package net.judah.beatbox;

import static net.judah.util.Constants.Gui.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;
import net.judah.util.FileChooser;

public class ButtonBox extends JPanel implements TimeListener {
// output midi port combo box + channel volume

    private final JudahClock clock;
    private final BeatBoxGui gui;
    private final JToggleButton start;
    private final JLabel beats;
    private Slider tempo;
    private final JLabel tempoLbl;

    public ButtonBox(JudahClock clock, BeatBoxGui gui) {
        this.clock = clock;
        this.gui = gui;

        Slider tempo = new Slider(30, 230, e -> {
            clock.setTempo(((Slider)e.getSource()).getValue());});
        tempo.setValue(Math.round(clock.getTempo()));
        tempo.setFont(FONT9);
        tempo.setMajorTickSpacing(50);
        tempo.setPaintTicks(false);
        tempo.setPaintLabels(true);
        tempo.setPreferredSize(new Dimension(110, 22));

        setLayout(new GridLayout(0, 1));
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 5));

        start = new JToggleButton("Start");
        start.setSelected(clock.isActive());
        JButton load = new JButton("Load");
        JButton save = new JButton("Save");
        JButton clear = new JButton("Clear");
        JButton random = new JButton("Randomize");

        start.addActionListener(e -> {
            if (clock.isActive()) {
                clock.end();
                start.setText("Start");
            } else {
                clock.begin();
                start.setText("Stop");
            }
        });
        load.addActionListener(e -> {load();});
        save.addActionListener(e -> {save();});
        clear.addActionListener(e -> {clear();});


        add(tempo);
        add(start);
        add(load);
        add(save);
        add(random);
        add(clear);

        add(new JLabel("Drumkit", JLabel.CENTER));
        add(kitPanel());
        add(stepsPanel());
        add(midiPanel());

        beats = new JLabel("current beat: " + clock.getCount(), JLabel.CENTER);
        tempoLbl = new JLabel("tempo: " + clock.getTempo(), JLabel.CENTER);

        add(beats);
        add(tempoLbl);
        doLayout();
        clock.addListener(this);
    }

    private JPanel midiPanel() {
        JPanel midi = new JPanel();
        midi.setLayout(new GridLayout(4, 1));
        midi.add(new JLabel("Midi Out"));
        midi.add(new JLabel("[CarlaFluid]"));
        midi.add(new JLabel("Chanel"));
        midi.add(new JLabel("Aux2"));
        return midi;
    }

    private JPanel stepsPanel() {
        JPanel setup = new JPanel();
        setup.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        setup.setLayout(new GridLayout(2, 2));
        setup.add(new JLabel("steps", JLabel.CENTER));
        setup.add(new JLabel("div.", JLabel.CENTER));

        JComboBox<Integer> steps = new JComboBox<>();
        for (int i = 4; i < 25; i++) steps.addItem(i);
        steps.setSelectedItem(clock.getSteps());
        steps.addActionListener(e ->{
            clock.setSteps((int)steps.getSelectedItem());
            clear();});
        setup.add(steps);

        JComboBox<Integer> div = new JComboBox<>();
        for (int i = 2; i < 7; i++) div.addItem(i);
        div.setSelectedItem(clock.getSubdivision());
        div.addActionListener(e -> {
            clock.setSubdivision((int)div.getSelectedItem());
            clear();});
        setup.add(div);
        return setup;
    }

    private JPanel kitPanel() {
        JComboBox<GMKit> kits = new JComboBox<>(GMKit.values());
        kits.setSelectedItem(clock.getBeatBox().getKit());
        kits.addActionListener(e -> {
            clock.getBeatBox().setKit(((GMKit)kits.getSelectedItem()));});

        JPanel kitPanel = new JPanel();
        kitPanel.setMaximumSize(new Dimension(105, 23));
        kitPanel.setPreferredSize(new Dimension(105, 23));
        kitPanel.add(kits);
        kits.setPreferredSize(new Dimension(105, 22));
        return kitPanel;
    }

    private void clear() {
        clock.setFile(null);
        clock.getBeatBox().initialize();
        gui.initialize();
    }

    public void load() {
        FileChooser.setCurrentDir(BeatBox.FOLDER);
        File f = FileChooser.choose();
        if (f != null)
            clock.parse(f);
    }

    public void save() {
        FileChooser.setCurrentDir(BeatBox.FOLDER);
        File f = FileChooser.choose();
        if (f != null)
            clock.write(f);
    }

    @Override
    public void update(Property prop, Object value) {
        if (Property.BEAT == prop) {
            beats.setText("current: " + value);
        }
        else if (Property.TEMPO == prop) {
            tempoLbl.setText("Tempo: " + value);
            if (tempo.getValue() != Math.round((float)value))
                tempo.setValue(Math.round((float)value));
        }
        else if (Property.TRANSPORT == prop) {
            start.setSelected((boolean)value);
        }
    }


}
