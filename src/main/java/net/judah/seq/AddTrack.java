package net.judah.seq;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;

import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import judahzone.widgets.Integers;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.drumkit.DrumKit;
import net.judah.gui.Size;
import net.judah.gui.widgets.ModalDialog;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.Channels;
import net.judah.seq.chords.ChordPro;
import net.judah.synth.ZoneMidi;
import net.judah.synth.fluid.FluidAssistant;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.TacoTruck;

public class AddTrack extends JPanel {
	public static final Dimension SIZE = new Dimension(300, 300);
	private static final Dimension MIDIOUT = new Dimension(150, 3 * Size.STD_HEIGHT);
	private static final Dimension AUTOMATION = new Dimension(200, 175);

	enum TrackType {Synth, Chord, FX} // Drum,

	private final JudahZone zone;
	private final Channels channels;
	private final Seq seq;
	private final JTabbedPane tabs = new JTabbedPane();
	private final Box drum = new Box(BoxLayout.Y_AXIS);
	private final Box synth = new Box(BoxLayout.Y_AXIS);
	private final Box chord = new Box(BoxLayout.Y_AXIS);
	private final Box fx = new Box(BoxLayout.Y_AXIS);

	final JTextField drumName = new JTextField(8);
	final JTextField synthName = new JTextField(8);
	final JComboBox<Channels.RegisteredDrums> drumType = new JComboBox<Channels.RegisteredDrums>(Channels.RegisteredDrums.values());
	final JComboBox<Channels.RegisteredSynths> synthType = new JComboBox<Channels.RegisteredSynths>(Channels.RegisteredSynths.values());
	final JList<ZoneMidi> drumOut = new JList<ZoneMidi>();
	final JList<ZoneMidi> synthOut = new JList<ZoneMidi>();
	final JComboBox<Integer> drumCh = new JComboBox<Integer>(Integers.generate(0, 16));
	final JToggleButton joinSynth = new JToggleButton("Connect");
	final JList<Channel> fxChannel;
	final JTextArea chordPreview = new JTextArea(25, 7);
	final JLabel chordFile = new JLabel(" <-- Open File  ");
	File chords;

	final JButton cancel = new Btn(" Cancel ", e->ModalDialog.getInstance().dispose());
	final JButton ok = new Btn("  OK  ", e-> ok()); // import track into Seq

	public AddTrack(JudahZone judahZone) {
		this.zone = judahZone;
		this.channels = zone.getChannels();
		this.seq = zone.getSeq();

		setName("New Track");


		ArrayList<Channel> automation = new ArrayList<Channel>();
		fxChannel = new JList<Channel>(automation.toArray(new Channel[0]));

		List<Channel> all = channels.getAll();
		all.forEach(channel-> {
			if (channel instanceof ZoneMidi == false && channel instanceof DrumKit == false)
				automation.add(channel);});

 		JToggleButton createSynth = new JToggleButton("Create");
 		ButtonGroup synths = new ButtonGroup();
 		synths.add(createSynth);
 		synths.add(joinSynth);
 		createSynth.setSelected(true);
 		createSynth.addActionListener(l->activateSynth());
 		joinSynth.addActionListener(l->activateSynth());
		synthType.setSelectedIndex(0);
		refillSynth();
		synthType.addActionListener(l->refillSynth());
		synth.add(Gui.wrap(new JLabel("Name: "), synthName, synthType));
 		synth.add(Gui.wrap(new JLabel("Engine:"), joinSynth, createSynth));
 		synth.add(Gui.resize(new JScrollPane(synthOut), MIDIOUT));
		synthOut.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		synthName.addKeyListener(new KeyAdapter() {
			@Override public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) ok();
				else if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
						ModalDialog.getInstance().dispose();}});
		drumType.setSelectedIndex(0);
 		refillDrums();
		drumCh.setSelectedIndex(MidiConstants.DRUM_CH);
		drumType.addActionListener(l->refillDrums());
 		drum.add(Gui.wrap(new JLabel("Name: "), drumName, drumType));
		drum.add(Gui.resize(new JScrollPane(drumOut), MIDIOUT));
 		drum.add(Gui.wrap(new JLabel("Midi CH:"), drumCh));

		fxChannel.setSelectedIndex(0);
 		fx.add(Gui.resize(fxChannel, AUTOMATION));

		Btn openFile = new Btn(Icons.SAVE, e->openChords());
		chord.add(Gui.wrap(openFile, new JLabel("  "), chordFile));
		chord.add(chordPreview);

		tabs.add("Synth", centered(synth));
		tabs.add("Chord", centered(chord));
		tabs.add("Effects", centered(fx));
		// tabs.add("Drum", centered(drum)); // TODO
		tabs.setSelectedIndex(0);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(tabs);
		JPanel btns = new JPanel();
		btns.add(ok); btns.add(cancel);
		add(btns);
		add(Box.createVerticalStrut(Size.STD_HEIGHT));
		new ModalDialog(this, SIZE);

	}

	public void openChords() {
		chords = Folders.choose(Folders.getChordPro());
		if (chords == null)
			return;
		ChordPro result = zone.getChords().load(chords);
		if (result == null)
			return;
		chordPreview.setText(result.toString());
		chordFile.setText(chords.getName());
	}

	private JPanel centered(Component inside) {
		JPanel result = new JPanel(new BorderLayout());
		result.add(new JLabel(" "), BorderLayout.PAGE_START);
		result.add(inside, BorderLayout.CENTER);
		return result;
	}

	private void refillSynth() {
		DefaultListModel<ZoneMidi> model = new DefaultListModel<>();
		switch((Channels.RegisteredSynths)synthType.getSelectedItem()) {
			case Taco:
				for (TacoTruck engine : seq.getTacos())
					model.addElement(engine);
				break;
			case Fluid:
				for (FluidSynth engine: seq.getFluids())
					model.addElement(engine);
				break;
			case External:
				for (MidiInstrument engine : seq.getOther())
					model.addElement(engine);
				break;
		}
		synthOut.setModel(model);
		activateSynth();
	}
	private void activateSynth() {
		synthOut.setEnabled(joinSynth.isSelected());
		if(synthOut.isEnabled() && synthOut.getSelectedValue() == null)
			synthOut.setSelectedIndex(0);
	}

	private void refillDrums() {
		DefaultListModel<ZoneMidi> model = new DefaultListModel<>();
		drumOut.setModel(model);
	}

	// TODO  4 and 5: post processing
	// TACO or Fluid or Automation (or Crave/Etc) (separate channel?)
	// 1. gather info from user
	// 2. create T
	// 3. post MetaInfo
	// 4. post TrackInfo
	// 5. update Gui
	private void ok() {
		TrackType type = TrackType.values()[tabs.getSelectedIndex()];
		switch(type) {
			case Synth : createSynth(); break;
			case Chord : addChords(); break;
 			case FX : createChannel(); break;
		}
		ModalDialog.getInstance().dispose();
	}

	private void createSynth() {
		Channels.RegisteredSynths type = Channels.RegisteredSynths.values()[synthType.getSelectedIndex()];
		boolean join = synthOut.isEnabled();

		if (join) {
			seq.addTrack(synthName.getText(), type, synthOut.getSelectedIndex());
		} else {// TODO extern handling
			String name = synthName.getText();
			if (name.isBlank()) {
				RTLogger.warn(this, "Name was blank.");
				return;
			}
			RTLogger.debug(this, "creating " + type.name() + " engine with track " + name);
			if (type == Channels.RegisteredSynths.Fluid) {
				new FluidAssistant(name, zone);
			} else if (type == Channels.RegisteredSynths.Taco) {
				int idx = seq.getTacos().length + 1;
				zone.getChannels().accept(new TacoTruck("T + " + idx));
			} // else ?
		}
	}

	private void createChannel() {
		Channel ch = fxChannel.getSelectedValue();
		if (ch != null)
			seq.adaptChannel(ch, ch.getName());
	}

	private void addChords() {
		if (chords == null)
			return;
		zone.getChords().load(chords);
		zone.getOverview().refill();
	}

//	private void createDrum() {
		// RegisteredDrums type = RegisteredDrums.values()[drumOut.getSelectedIndex()];
		//	jackclient.registerPort("left", AUDIO, JackPortIsOutput);
//	}


}
