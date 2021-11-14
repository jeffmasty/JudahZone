package net.judah.beatbox;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;

import org.jaudiolibs.jnajack.JackPort;

import com.illposed.osc.OSCSerializeException;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.beatbox.BeatBox.Type;
import net.judah.clock.JudahClock;
import net.judah.effects.gui.Slider;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.plugin.Carla;
import net.judah.settings.Channels;
import net.judah.util.CenteredCombo;
import net.judah.util.Click;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.FileChooser;
import net.judah.util.Pastels;
import net.judah.util.Size;

    // GUI                  // MODEL
    ///////////////////////////////////////
    // channel              // beatBox
    // pattern#             // beatBox pattern CRUD Line 1
    // pattern              // beatBox pattern CRUD Line 2
    // file/mute            // beatBox
    // save/delete          // save pattern/all
    // midi out             // beatBox
    // lineIn volume        // external
    // instrument/drumset   // beatBox
    // grid volume          // grid
    // note off default     // transient
    // drum reverb          // transient
    // Tracks               // scale key octave/drumkit in tuner area
    // Steps/Division       // clock

public class Buttons extends JPanel implements Size {
    public static final File DRUM_FOLDER = new File(Constants.ROOT, "patterns");
    public static final File MELODIC_FOLDER = new File(Constants.ROOT, "sequences");

    private BeatBox sequencer;
    private final JudahClock clock = JudahClock.getInstance();

    @Getter private JComboBox<Integer> channelCombo;
    @Getter private JComboBox<Integer> gate;

    private JComboBox<Integer> patternCombo;
    private JComboBox<String> fileCombo;
    private JComboBox<String> midiOut;
    private Slider volume, gridVolume;
    private JComboBox<String> instruments;
    private JToggleButton calf;
    private JComboBox<Integer> steps;
    private JComboBox<Integer> div;
    private JCheckBox mute;
    private boolean inUpdate;
    private JPanel gatePnl;

    public Buttons(BeatBox box) {
        this.sequencer = box;
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        setLayout(new GridLayout(0, 1, 2, 3));
        createChannelLabels();
        createChannelCombos();
        createChannelNav();
        createFile1();
        createFile2();
        createGridVolume();
        createMidiOut();
        createChannelVolume();
        createInstrument();
        createGate();
        add(new JLabel(" "));
        add(new JLabel(" "));
        add(new JLabel(" "));
        createReverb();
        createSteps();
        update();
    }

    public void setSequencer(BeatBox sequencer) {

        boolean fillInstruments = false;

        if (this.sequencer.getType() != sequencer.getType()) {
            // redo File list
            fillInstruments = true;
            inUpdate = true;
            fileCombo.removeAllItems();
            fileCombo.addItem(""); // clear/new
            File folder = (sequencer.getType() == Type.Drums)
                    ? DRUM_FOLDER : MELODIC_FOLDER;
                for (File f : folder.listFiles())
                    if (f.isFile())
                        fileCombo.addItem(f.getName());
           inUpdate = false;
        }
        this.sequencer = sequencer;
        inUpdate = true;
        fillPatterns();
        if (fillInstruments)
            fillInstruments(instruments);
        update();
        calf.setVisible(sequencer.getType() == Type.Drums);
        gatePnl.setVisible(!calf.isVisible());
        inUpdate = false;
    }

    public void update() {
        inUpdate = true;
        channelCombo.setSelectedItem(sequencer.getMidiChannel());
        patternCombo.setSelectedIndex(sequencer.indexOf(sequencer.getCurrent()));
        if (sequencer.getFile() != null)
            fileCombo.setSelectedItem(sequencer.getFile().getName());
        midiOut.setSelectedItem(sequencer.getMidiOut().getShortName());
        volume.setValue(Channels.getVolume(sequencer.getMidiOut()));
        if (sequencer.getInstrument() != null)
            instruments.setSelectedItem(sequencer.getInstrument().name);
        gridVolume.setValue(sequencer.getCurrent().getVolume());

        if (clock.getSteps() != (Integer)steps.getSelectedItem())
            steps.setSelectedItem(clock.getSteps());
        if (clock.getSubdivision() != div.getSelectedIndex())
        div.setSelectedItem(clock.getSubdivision());
        mute.setSelected((sequencer.getCurrent().isMute()));
        for (ItemListener l : mute.getItemListeners()) {
            l.itemStateChanged(new ItemEvent(mute, 1, Boolean.TRUE, ItemEvent.RESERVED_ID_MAX));
        }
        inUpdate = false;
    }

    private void createChannelLabels() {
        JLabel chlbl = new JLabel("Channel", JLabel.CENTER);
        chlbl.setFont(Constants.Gui.BOLD);
        JLabel lbl = new JLabel("Pattern", JLabel.CENTER);
        lbl.setFont(Constants.Gui.BOLD);
        JPanel pnl = new JPanel(new GridLayout(1, 2));
        pnl.add(chlbl);
        pnl.add(lbl);

        add(pnl);
    }

    private void createChannelCombos() {
        channelCombo = new CenteredCombo<>();
        channelCombo.setFont(Constants.Gui.BOLD);
        for (int i = 0; i < 16; i++)
            channelCombo.addItem(i);

        // Change the channel!
        channelCombo.addActionListener(e -> {
            if (inUpdate) return;
            int channel = channelCombo.getSelectedIndex();
            if (sequencer.getMidiChannel() == channel) return;
            BeatsView.getInstance().initialize(clock.getSequencer(channel));});

        patternCombo = new CenteredCombo<>();
        for (int i = 0; i < sequencer.size(); i++)
            patternCombo.addItem(i);
        patternCombo.addActionListener(e -> {
            if (inUpdate) return;
            sequencer.setCurrent(sequencer.get(patternCombo.getSelectedIndex()));
            BeatsView.getInstance().updateKit(sequencer, sequencer.getCurrent().createTracks());});
        JPanel pnl = new JPanel(new GridLayout(1, 2));
        pnl.add(channelCombo);
        pnl.add(patternCombo);
        add(pnl);
    }

    private void createChannelNav() {

        JPanel result = new JPanel(new GridLayout(1, 4, 3, 3));

        Click chBack = new Click("<");
        chBack.addActionListener(e -> {
            int channel = channelCombo.getSelectedIndex() - 1;
            if (channel < 0) channel = channelCombo.getItemCount() - 1;
            BeatsView.getInstance().initialize(clock.getSequencer(channel));});

        Click chNext = new Click(">");
        chNext.addActionListener(e -> {
            int channel = channelCombo.getSelectedIndex() + 1;
            if (channel == channelCombo.getItemCount()) channel = 0;
            BeatsView.getInstance().initialize(clock.getSequencer(channel));
        });
        Click patternBack = new Click("<");
        patternBack.addActionListener(e -> {sequencer.next(false);});
        Click patternNext = new Click(">");
        patternNext.addActionListener(e -> {sequencer.next(true);});

        result.add(chBack);
        result.add(chNext);
        result.add(patternBack);
        result.add(patternNext);
        add(result);
    }

    private void createFile1() {
        fileCombo = new CenteredCombo<>();
        fileCombo.setAlignmentX(0.5f);
        fileCombo.setToolTipText("Step Sequence File");
        fileCombo.addItem(""); // clear/new
        File folder = (sequencer.getType() == Type.Drums)
                ? DRUM_FOLDER : MELODIC_FOLDER;
            for (File f : folder.listFiles())
                if (f.isFile())
                    fileCombo.addItem(f.getName());
        if (sequencer.getFile() != null)
            fileCombo.setSelectedItem(sequencer.getFile().getName());
        fileCombo.addActionListener(e -> {
            if (inUpdate) return;
            if (fileCombo.getSelectedItem() == null) return;
            String selected = fileCombo.getSelectedItem().toString();
            if (selected.equals("")) {
                sequencer.getCurrent().initialize();
                BeatsView.getInstance().initialize(sequencer);
            }
            else {
                File dir = sequencer.getType() == Type.Drums ?
                        DRUM_FOLDER : MELODIC_FOLDER;
                sequencer.load(new File(dir, selected));
            }
        });
        add(fileCombo);
    }

    private void createFile2() {

        Click saveBtn = new Click("Save");
        saveBtn.addActionListener( e -> {
            File folder = (sequencer.getType() == Type.Drums)
                    ? DRUM_FOLDER : MELODIC_FOLDER;
            FileChooser.setCurrentDir(folder);
            File f = FileChooser.choose();
            if (f != null)
                sequencer.write(f);
        });

        class MuteCheckBox extends JCheckBox {
            MuteCheckBox() {
                addItemListener(e -> {
                    if (isSelected()) {
                        setBackground(Pastels.RED);
                        setOpaque(true);
                    }
                    else
                        setOpaque(false);
                    if (inUpdate) return;
                });
            }
        };

        mute = new MuteCheckBox();
        mute.setText(" mute");
        mute.setHorizontalTextPosition(JCheckBox.LEFT);
        mute.addActionListener(e -> {
            sequencer.getCurrent().setMute(mute.isSelected());});

        JPanel mutePnl = new JPanel(new GridLayout(1, 2));
        mutePnl.add(saveBtn);
        mutePnl.add(mute);
        add(mutePnl);

    }


    private void createMidiOut() {
        midiOut = new CenteredCombo<>();
        for (JackPort p : JudahMidi.getInstance().getOutPorts())
            midiOut.addItem(p.getShortName());
        midiOut.addActionListener(e -> {
            if (inUpdate) return;
            JackPort out = JudahMidi.getByName(
                    midiOut.getSelectedItem().toString());
            sequencer.setMidiOut(out);
            volume.setValue(sequencer.getCurrent().getVolume());
        });
        Click outLbl = new Click("<html> Midi <br/>"
                + " Out </html");
        outLbl.addActionListener( e -> {
            JackPort out = JudahMidi.getByName(
                midiOut.getSelectedItem().toString());
            MainFrame.get().getMixer().setFocus(
                    JudahZone.getChannels().byName(Channels.volumeTarget(out)));});

        JPanel midi = new JPanel();
        midi.setLayout(new BoxLayout(midi, BoxLayout.X_AXIS));

        midi.add(outLbl);
        midi.add(midiOut);
        add(midi);
    }
    private void createGridVolume() {
        gridVolume = new Slider(e -> { if (inUpdate) return;
            sequencer.getCurrent().setVolume(gridVolume.getValue());});
        gridVolume.setPreferredSize(CLOCK_SLIDER);
        JPanel vel = new JPanel();
        JLabel velLbl = new JLabel("Vel.", JLabel.LEFT);
        vel.add(velLbl);
        vel.add(gridVolume);
        add(vel);
    }

    private void createChannelVolume() {
        volume = new Slider(e -> { if (inUpdate) return;
            Channels.setVolume(volume.getValue(), sequencer.getMidiOut());});
        volume.setPreferredSize(CLOCK_SLIDER);
        JPanel vol = new JPanel();
        JLabel volLbl = new JLabel("Vol.", JLabel.LEFT);
        vol.add(volLbl);
        vol.add(volume);
        add(vol);

    }

    protected final void createInstrument() {
        instruments = new JComboBox<>();
        fillInstruments(instruments);
        instruments.addActionListener(e -> {
            if (inUpdate) return;
            sequencer.setInstrument(instruments.getSelectedIndex());});
        add(new JLabel("Instrument", JLabel.CENTER));
        add(instruments);
    }

    private void fillPatterns() {
        patternCombo.removeAllItems();
        for (int i = 0; i < sequencer.size(); i++)
            patternCombo.addItem(i);
    }

    private void fillInstruments(JComboBox<String> combo) {
        combo.removeAllItems();
        ArrayList<FluidInstrument> list = sequencer.getType() == Type.Drums
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
        for (int i = 0; i < list.size(); i++)
            combo.addItem(list.get(i).name);
    }

    private void createSteps() {
        steps = new JComboBox<>();
        for (int i = 4; i < 33; i++) steps.addItem(i);
        steps.setSelectedItem(clock.getSteps());
        steps.setFont(Constants.Gui.FONT11);
        steps.addActionListener(e ->{
            if (inUpdate) return;
            clock.setSteps((int)steps.getSelectedItem());
            BeatsView.getInstance().getGrid2().repaint();
            });

        div = new JComboBox<>();
        for (int i = 2; i < 7; i++) div.addItem(i);
        div.setSelectedItem(clock.getSubdivision());
        div.setFont(Constants.Gui.FONT11);
        div.addActionListener(e -> {
            if (inUpdate) return;
            clock.setSubdivision((int)div.getSelectedItem());
            BeatsView.getInstance().getGrid2().repaint();
            });

        JPanel stepsPnl = new JPanel(new GridLayout(1, 2));
        stepsPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        stepsPnl.add(new JLabel("steps", JLabel.CENTER));
        stepsPnl.add(steps);
        JPanel divPnl = new JPanel(new GridLayout(1, 2));
        divPnl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        divPnl.add(new JLabel("div.", JLabel.CENTER));
        divPnl.add(div);
        add(stepsPnl);
        add(divPnl);
    }

    private void createReverb() {
        calf = new JToggleButton("Calf Reverb");
        calf.addActionListener(e -> {
            try {
                Carla.getInstance().setParameterValue(0, 2,
                        calf.isSelected() ? 1 : 0);
            } catch (OSCSerializeException | IOException e1) {
                Console.warn(e1);
            }
        });
        calf.setVisible(false);
        add(calf);
    }

    private void createGate() {
        gate = new JComboBox<>();
        for (int i = 0; i < clock.getSteps(); i++)
            gate.addItem(i);
        gate.setSelectedItem(2);
        gatePnl = new JPanel(new GridLayout(1, 2));
        gatePnl.add(new JLabel("Note Off", JLabel.CENTER));
        gatePnl.add(gate);
        gatePnl.setVisible(false);
        add(gatePnl);

    }

}
