package net.judah.metronome;

import static net.judah.util.Constants.Gui.*;

import java.awt.Color;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.text.PlainDocument;

import lombok.extern.log4j.Log4j;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.util.FileChooser;

@Log4j
public class MetroGui extends JPanel implements TimeListener {

	private final Metronome metro;
	
	private JToggleButton playBtn;
    private JToggleButton stopBtn;
    private JButton bpbBtn;
    private JButton fileBtn;
    
    private JTextField bpmText;
    private JSlider bpm;
    private JSlider volume;
	
	MetroGui(Metronome metronome) {
		metro = metronome;
		gui();
		actionListeners();
		update(null, null);
	}
	
	private void gui() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel banner = new JPanel();
		playBtn = new JToggleButton("Play");
		playBtn.setFont(BOLD);
		
		stopBtn = new JToggleButton("Stop");
		stopBtn.setFont(BOLD);
		stopBtn.setSelected(!metro.isRunning());
		
		JPanel bpmPnl = new JPanel();
		bpmPnl.setLayout(new BoxLayout(bpmPnl, BoxLayout.Y_AXIS));
		JLabel bpmLbl = new JLabel("BPM");
		bpmLbl.setFont(FONT12);
		bpmText = new JFormattedTextField(NumberFormat.getNumberInstance());
		bpmText.setDocument(new PlainDocument());
		bpmText.setColumns(3); 
		bpmText.setEditable(false);
		bpmText.setFont(FONT12);
		
		bpmPnl.add(bpmLbl);
		bpmPnl.add(bpmText);
		
		JPanel indicatorPanel = new JPanel();
		indicatorPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		
		bpbBtn = new JButton("BPB");
		bpbBtn.setMargin(BTN_MARGIN);
		bpbBtn.setFont(FONT11);
		
		fileBtn = new JButton(" File ");
		fileBtn.setMargin(BTN_MARGIN);
		fileBtn.setFont(FONT11);
		indicatorPanel.add(bpbBtn);
		indicatorPanel.add(fileBtn);
		
		banner.add(playBtn);
		banner.add(stopBtn);
		banner.add(bpmPnl);
		banner.add(indicatorPanel);
		add(banner);

		JPanel tempoPanel = new JPanel();
		JLabel tempoLbl = new JLabel("Tempo");
		tempoLbl.setFont(FONT13);
		tempoPanel.add(tempoLbl);
		
		bpm = new JSlider(JSlider.HORIZONTAL, 30, 230, Math.round(metro.getTempo()));
		bpm.setFont(FONT9);
		bpm.setMajorTickSpacing(40);
		bpm.setPaintTicks(false);
		bpm.setPaintLabels(true);
		tempoPanel.add(bpm);
		add(tempoPanel);

		JPanel volumePanel = new JPanel();
		JLabel volumeLbl = new JLabel("Volume");
		volumeLbl.setFont(FONT13);
		volumePanel.add(volumeLbl);
		
		volume = new JSlider(JSlider.HORIZONTAL, 0, 100, Math.round(metro.getGain() * 100));
		volume.setFont(FONT9);
		volume.setMajorTickSpacing(25);
		volume.setPaintTicks(true);
		volume.setPaintLabels(false);

		volumePanel.add(volume);
		add(volumePanel);
	}
	
	private void actionListeners() {
		playBtn.addActionListener(event -> {
			try { metro.play();} 
			catch (Exception e) { log.error(e.getMessage(), e);}});
		stopBtn.addActionListener(e -> { metro.stop(); });
		fileBtn.addActionListener( e -> loadMidi());
		volume.addChangeListener(e -> {metro.setVolume(volume.getValue() / 100f);});
		bpm.addChangeListener(e -> {metro.setTempo(bpm.getValue() * 1f);});
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
		else {
			bpmText.setText("" + metro.getTempo());
			bpm.setValue(Math.round(metro.getTempo()));
			volume.setValue(Math.round(100f * metro.getGain()));
		}
	}
	
	
}
