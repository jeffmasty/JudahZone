package net.judah.metronome;

import static net.judah.util.Constants.Gui.*;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import lombok.extern.log4j.Log4j;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.util.FileChooser;
import net.judah.util.Knob;

@Log4j
public class MetroGui extends JPanel

implements TimeListener {

	private final Metronome metro;

	private JButton playBtn;
    private JButton stopBtn;
    private JButton bpbBtn;
    private JButton fileBtn;
    private Knob volume;

	MetroGui(Metronome metronome) {
		metro = metronome;
		gui();
		actionListeners();
		update(null, null);
	}

	private void gui() {
	    setBorder(BorderFactory.createTitledBorder("Metronome"));
		playBtn = new JButton("Play");
		stopBtn = new JButton("Stop");
		stopBtn.setSelected(!metro.isRunning());

		JPanel indicatorPanel = new JPanel();
		// indicatorPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		indicatorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

		bpbBtn = new JButton("BPB");
		bpbBtn.setMargin(BTN_MARGIN);
		bpbBtn.setFont(FONT11);

		fileBtn = new JButton(" File ");
		fileBtn.setMargin(BTN_MARGIN);
		fileBtn.setFont(FONT11);
		indicatorPanel.add(bpbBtn);
		indicatorPanel.add(fileBtn);

		add(playBtn);
		add(stopBtn);
		add(indicatorPanel);

		volume = new Knob( val -> {metro.setVolume(volume.getValue() / 100f);});
		JLabel volumeLbl = new JLabel("Vol", SwingConstants.CENTER);
		volumeLbl.setFont(FONT11);
	    add(volume);
	    add(volumeLbl);
	}

	private void actionListeners() {
		playBtn.addActionListener(event -> {
			try { metro.begin();}
			catch (Exception e) { log.error(e.getMessage(), e);}});
		stopBtn.addActionListener(e -> { metro.end(); });
		fileBtn.addActionListener( e -> loadMidi());
		bpbBtn.addActionListener(e -> {
			int measure = openBeatsDialog();
			if (measure > 0) metro.setMeasure(measure);
		});
	}

	public void loadMidi() {
		// FileChooser.setCurrentDir(new File("/home/judah/Tracks/midi"));
		File file = FileChooser.choose(JFileChooser.FILES_ONLY, "mid", "Midi files (*.mid)");
		if (file == null) ;
		metro.setMidiFile(file);
	}

	private int openBeatsDialog() {
		String input = JOptionPane.showInputDialog(getRootPane(), "Beats per bar?", "4");
		try { return Integer.parseInt(input);
		} catch (Throwable t) { return -1; }
	}

	@Override
	public void update(Property prop, Object value) {
        if (Status.ACTIVE == value) {
			playBtn.setSelected(true);
			stopBtn.setSelected(false);
		}
		else if (Status.TERMINATED == value) {
			playBtn.setSelected(false);
			stopBtn.setSelected(true);
		}
		else
			volume.setValue(Math.round(100f * metro.getGain()));
	}


}
