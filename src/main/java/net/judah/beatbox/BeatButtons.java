package net.judah.beatbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jaudiolibs.jnajack.JackPort;

import com.illposed.osc.OSCSerializeException;

import net.judah.JudahClock;
import net.judah.MainFrame;
import net.judah.api.TimeListener;
import net.judah.effects.gui.Slider;
import net.judah.midi.JudahMidi;
import net.judah.plugin.Carla;
import net.judah.util.Console;
import net.judah.util.Constants.Gui;
import net.judah.util.FileChooser;
import net.judah.util.TapTempo;

public class BeatButtons extends JPanel implements TimeListener {

    private final Dimension SLIDER = new Dimension(110, 24);
    private final Dimension COMBO = new Dimension(105, 24);
    private final Dimension PANEL = new Dimension(105, 48);

    private final JudahClock clock = JudahClock.getInstance();
    private final BeatBox box;
    private JToggleButton start;
    private JLabel beats;
    private JLabel tempoLbl;
    private Slider tempo;
    private Slider volume;
    private JLabel volumeLabel;

    public BeatButtons(BeatBox box) {
        this.box = box;

        setMaximumSize(new Dimension(115, MainFrame.HEIGHT_TABS));
        setLayout(new GridLayout(0, 1, 2, 0));
        // setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 3));

        add(namePanel());
        add(savePanel());
        add(stepsPanel());
        add(midiPanel());
        add(kitPanel());
        add(bottomPanel());
        add(bottomPanel2());

        doLayout();
        clock.addListener(this);
    }

    private JPanel namePanel() {
        JPanel result = new JPanel(new GridLayout(3, 1));
        JComboBox<String> name = new JComboBox<>();
        name.setAlignmentX(0.5f);
        name.setToolTipText("Step Sequence File");
        name.addItem(""); // clear/new

        for (File f : BeatBox.FOLDER.listFiles())
            name.addItem(f.getName());
        if (box.getFile() != null)
            name.setSelectedItem(box.getFile().getName());
        name.addActionListener(e -> {
            String selected = name.getSelectedItem().toString();
            if (selected.equals(""))
                box.clear();
            else
                box.load(new File(BeatBox.FOLDER, selected));});
        start = new JToggleButton("Start");
        start.setSelected(clock.isActive());
        start.addActionListener(e -> {
            if (clock.isActive()) {
                clock.end();
                start.setText("Start");
            } else {
                clock.begin();
                start.setText("Stop");
            }
        });
        result.add(new JLabel("Pattern File", JLabel.CENTER));
        result.add(name);
        result.add(start);
        return result;
    }

    private JPanel savePanel() {
        JButton save = new JButton("Save");
        save.addActionListener(e -> {save();});
        JButton random = new JButton("Randomize");
        random.setEnabled(false);

        TapTempo tapButton = new TapTempo("Tempo:", msec -> {
            clock.setTempo(60000 / msec);
        });
        tempoLbl = new JLabel("" + clock.getTempo(), JLabel.CENTER);
        JPanel bpm = new JPanel(new GridLayout(1, 2));
        bpm.add(tapButton);
        bpm.add(tempoLbl);

        JPanel result = new JPanel(new GridLayout(3, 1));

        result.add(save);
        result.add(random);
        result.add(bpm);

        return result;
    }

    private JPanel stepsPanel() {

        tempo = new Slider(40, 200, e -> {
            clock.setTempo(((Slider)e.getSource()).getValue());});
        tempo.setValue(Math.round(clock.getTempo()));
        tempo.setFont(Gui.FONT9);
        tempo.setPreferredSize(SLIDER);

        JPanel row1 = new JPanel(new GridLayout(1, 2));
        row1.add(new JLabel("steps", JLabel.CENTER));
        row1.add(new JLabel("div.", JLabel.CENTER));
        JComboBox<Integer> steps = new JComboBox<>();
        for (int i = 4; i < 33; i++) steps.addItem(i);
        steps.setSelectedItem(clock.getSteps());
        steps.addActionListener(e ->{
            clock.setSteps((int)steps.getSelectedItem());
            box.clear();});

        JComboBox<Integer> div = new JComboBox<>();
        for (int i = 2; i < 7; i++) div.addItem(i);
        div.setSelectedItem(clock.getSubdivision());
        div.addActionListener(e -> {
            clock.setSubdivision((int)div.getSelectedItem());
            box.clear();});
        JPanel row2 = new JPanel(new GridLayout(1, 2));
        row2.add(steps);
        row2.add(div);

        // JPanel result = new JPanel(new GridLayout(3, 1));
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.add(tempo);
        result.add(row1);
        result.add(row2);
        return result;
    }
    private JPanel midiPanel() {
        JComboBox<String> midiOut = new JComboBox<>();
        for (JackPort p : JudahMidi.getInstance().getOutPorts())
            midiOut.addItem(p.getShortName());
        midiOut.setSelectedItem(BeatBox.getMidiOut().getShortName());
        midiOut.addActionListener(e -> {
            BeatBox.setMidiOut(JudahMidi.getByName(
                    midiOut.getSelectedItem().toString()));
            volumeLabel.setText(BeatBox.volumeTarget() + " Volume");
            volume.setValue(BeatBox.getVolume());
        });
        midiOut.setPreferredSize(COMBO);

        volumeLabel = new JLabel(BeatBox.volumeTarget() + " Volume", JLabel.CENTER);

        JPanel result = new JPanel();
        result.setMaximumSize(PANEL);
        result.setPreferredSize(PANEL);
        result.add(new JLabel("Midi Out", JLabel.CENTER));
        result.add(midiOut);
        result.add(volumeLabel);
        return result;
    }


    private JPanel kitPanel() {
        JComboBox<GMKit> kits = new JComboBox<>(GMKit.values());
        kits.setSelectedItem(box.getKit());
        kits.addActionListener(e -> {
            box.setKit(((GMKit)kits.getSelectedItem()));});
        kits.setPreferredSize(COMBO);
        volume = new Slider(e -> {BeatBox.setVolume(volume.getValue());});
        volume.setValue(BeatBox.getVolume());
        volume.setPreferredSize(SLIDER);

        JPanel result = new JPanel();
        result.setMaximumSize(PANEL);
        result.setPreferredSize(PANEL);
        result.add(volume);
        result.add(new JLabel("DrumKit", JLabel.CENTER));
        result.add(kits);

        return result;
    }

    private JPanel bottomPanel() {

        final JPanel result = new JPanel();
        result.setLayout(new GridLayout(3, 1));// BoxLayout(result, BoxLayout.Y_AXIS));
        JToggleButton calf = new JToggleButton("Calf Reverb");
        calf.addActionListener(e -> {
            try {
                Carla.getInstance().setParameterValue(0, 2,
                        calf.isSelected() ? 1 : 0);
            } catch (OSCSerializeException | IOException e1) {
                Console.warn(e1);
            }
        });
        result.add(new JLabel(""));
        result.add(calf);
        result.add(new JLabel(""));

        return result;
    }
    private JPanel bottomPanel2() {
        beats = new JLabel("Current Beat: " + clock.getCount(), JLabel.CENTER);
        beats.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        final JPanel result = new JPanel();
        result.setLayout(new GridLayout(2, 1));
        result.add(new JLabel(""));
        result.add(beats);
        return result;
    }

    public void load() {
        FileChooser.setCurrentDir(BeatBox.FOLDER);
        File f = FileChooser.choose();
        if (f != null)
            box.load(f);
    }

    public void save() {
        FileChooser.setCurrentDir(BeatBox.FOLDER);
        File f = FileChooser.choose();
        if (f != null)
            box.write(f);
    }

    @Override
    public void update(Property prop, Object value) {
        if (Property.BEAT == prop)
            beats.setText("Current Beat:  " + value);
        else if (Property.TEMPO == prop) {
            tempoLbl.setText("" + value);
            int val = Math.round((float)value);
            if (tempo.getValue() != val)
                tempo.setValue(val);
        }
        else if (Property.TRANSPORT == prop)
            start.setSelected((boolean)value);
    }


}
