package net.judah.tracks;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.clock.BeatBuddy;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.settings.Channels;
import net.judah.util.CenteredCombo;
import net.judah.util.Click;

public class MidiOut extends JPanel {

	private JComboBox<String> midiOut;
	private JComboBox<String> instruments;
//	boolean inUpdate;
	Track track;
	
	public MidiOut(Track t) {
		track = t;
        midiOut = new CenteredCombo<>();
        for (JackPort p : JudahMidi.getInstance().getOutPorts())
            midiOut.addItem(p.getShortName());
        midiOut.setSelectedItem(t.midiOut == null ? t.initial : t.getMidiOut().getShortName());
        midiOut.addActionListener(e -> {
//            if (inUpdate) return;
            JackPort out = JudahMidi.getByName(
                    midiOut.getSelectedItem().toString());
            if (!track.getMidiOut().equals(out)) {
            	track.setMidiOut(out);
            	fillInstruments();
            }
            // volume.setValue(sequencer.getCurrent().getVolume());
        });
        Click outLbl = new Click("<html> Midi <br/>"
                + " Out </html");
        outLbl.addActionListener( e -> {
            JackPort out = JudahMidi.getByName(
                midiOut.getSelectedItem().toString());
            MainFrame.setFocus(
                    JudahZone.getChannels().byName(Channels.volumeTarget(out)));});

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(outLbl);
        add(midiOut);
        
        instruments = new JComboBox<>();
        fillInstruments();
        
        instruments.addActionListener(e -> {});
// setInstrument(instruments.getSelectedIndex());});

        add(new JLabel("Instrument", JLabel.CENTER));
        add(instruments);
    }

        
        // vol folder file pattern
        // midiout instrument latch tempo
	
//    public void setInstrument(int index) {
//        if (index < 0) return;
//        ArrayList<FluidInstrument> list = (type == Type.Drums)
//                ? FluidSynth.getInstruments().getDrumkits()
//                : FluidSynth.getInstruments().getInstruments();
//        FluidInstrument next = list.get(index);
//        if (next.equals(getInstrument())) return;
//        noteOff();
//        setInstrument(list.get(index));
//
//    }
//    public void setInstrument(FluidInstrument patch) {
//        instrument = patch;
//        try {
//            Midi midi = new Midi(ShortMessage.PROGRAM_CHANGE, midiChannel > 9 ? 9 : midiChannel, instrument.index);
//            queue.add(midi);
//        } catch (InvalidMidiDataException e) { Console.warn(e); }
//    }
//        
	private void fillInstruments() {
        instruments.removeAllItems();
        	
        if (track.getMidiOut().equals(JudahMidi.getInstance().getDrumsOut()))
        	for (int i = 1; i < BeatBuddy.Drumset.values().length; i++)
        		instruments.addItem(BeatBuddy.Drumset.values()[i].name());
        else {
        	ArrayList<FluidInstrument> list; 
        	if (track.isDrums()) 
        		list = FluidSynth.getInstruments().getDrumkits();
        	else 
        		list = FluidSynth.getInstruments().getInstruments();
        for (int i = 0; i < list.size(); i++)
            instruments.addItem(list.get(i).name);
        }
	}
    
	
	
}



