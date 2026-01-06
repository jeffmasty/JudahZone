package net.judah.gui.midiimport;

import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

import judahzone.api.MidiConstants;
import judahzone.gui.Icons;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.seq.track.MidiTrack;

public class ImportView extends KnobPanel implements MidiConstants {

	private static ImportView instance;

	@Getter public final KnobMode knobMode = KnobMode.Import;
	@Getter JComponent title = new Box(BoxLayout.LINE_AXIS);

	MidiTrack target;
	File file;
	JLabel fileName = new JLabel(" <-- Open File  ");
	Sequence sequence;

	public static ImportView getInstance() {
		if (instance == null)
			instance = new ImportView();
		return instance;
	}

	private ImportView() {
		Btn openFile = new Btn(Icons.SAVE, e->openFile());
		title.add(openFile);
		title.add(fileName);
	}

	public ImportView(File f) {
		this();
		openFile(f);
	}

	public boolean openFile() {
		File folder = file == null ? Folders.getImportMidi() : file.getParentFile();
		File choice = Folders.choose(".mid", "Midi Files", folder);
		if (choice == null || choice.isFile() == false)
			return false;
		return openFile(choice);
	}

	public boolean openFile(File f) {
		try {
			sequence = MidiSystem.getSequence(f);
		} catch(Exception e) {
			RTLogger.warn(f, e);
			return false;
		}

		fileName.setText(f.getName());
		install(new ImportTable(sequence));
		return true;
	}

}
