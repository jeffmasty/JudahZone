package net.judah.metronome;
import static net.judah.Constants.Gui.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lombok.extern.log4j.Log4j;

@Log4j 
public class MetroPanel extends JPanel implements  ActionListener, ChangeListener {
	
	final Metronome metro;
	private final JToggleButton playBtn;
    private final JToggleButton stopBtn;
    private final JButton bpbBtn;
    private final JButton fileBtn;
    
    
    JTextField bpb;
    JTextField bpmText;
    JSlider bpm;
    private final JSlider volume;

	public MetroPanel(Metronome metro) {
		this.metro = metro;

		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel banner = new JPanel();
		playBtn = new JToggleButton("Play");
		playBtn.setFont(BOLD);
		playBtn.addActionListener(this);
		stopBtn = new JToggleButton("Stop");
		stopBtn.setFont(BOLD);
		stopBtn.addActionListener(this);
		stopBtn.setSelected(true);
		
		
		JPanel bpmPnl = new JPanel();
		bpmPnl.setLayout(new BoxLayout(bpmPnl, BoxLayout.Y_AXIS));
		JLabel bpmLbl = new JLabel("BPM");
		bpmLbl.setFont(FONT12);
		bpmText = new JTextField("70", 2);
		bpmText.setFont(FONT12);
		bpmText.addActionListener(this);
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

//		JPanel beatsPanel = new JPanel();
//		beatsPanel.add(new JLabel("BPB"));
//		bpb = new JTextField("4", 2);
//		bpb.addActionListener(this);
//		beatsPanel.add(bpb);
//		add(beatsPanel);

		JPanel tempoPanel = new JPanel();
		
		JLabel tempo = new JLabel("Tempo");
		tempo.setFont(FONT13);
		tempoPanel.add(tempo);
		
		bpm = new JSlider(JSlider.HORIZONTAL, 50, 170, 70);
		bpm.setFont(FONT9);
		bpm.setMajorTickSpacing(20);
		bpm.setPaintTicks(false);
		bpm.setPaintLabels(true);
		bpm.addChangeListener(this);
		tempoPanel.add(bpm);
		add(tempoPanel);

		JPanel volumePanel = new JPanel();
		JLabel volumeLbl = new JLabel("Volume");
		volumeLbl.setFont(FONT13);
		volumePanel.add(volumeLbl);
		
		volume = new JSlider(JSlider.HORIZONTAL, 0, 100, 70);
		volume.setFont(FONT9);
		volume.setMajorTickSpacing(20);
		volume.setPaintTicks(true);
		volume.setPaintLabels(false);
		volume.addChangeListener(this);
		volumePanel.add(volume);
		add(volumePanel);

//		Properties props = new Properties();
//		props.put("bpb", 4);
//		props.put("bpm", 70f);
//		props.put("volume", 0.7f);
//		setProperties(props);

	}

	public boolean start() {
		playBtn.setSelected(true);
		stopBtn.setSelected(false);
		return true;
	}

	public boolean stop() {
		playBtn.setSelected(false);
		stopBtn.setSelected(true);
		return true;
	}

	public void update() {
		Properties p = metro.getProps();
		// update UI
		bpb.setText("" + p.get("bpb"));
		bpmText.setText("" + p.get("bpm"));
		bpm.setValue(Math.round((Float)p.get("bpm")));
		volume.setValue(Math.round(100f * (Float)p.get("volume")));
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Properties props = metro.getProps();
		try {
			if (e.getSource() == volume) {

				props.put("volume", volume.getValue() / 100f);
				metro.execute(metro.settings, props);
			}
			if (e.getSource() == bpm) {
				props.put("bpm", bpm.getValue() * 1f);
				metro.execute(metro.settings, props);
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			if (e.getSource() == playBtn) {
				if (metro.isRunning()) {

				}
				metro.execute(metro.start, null);
				playBtn.setSelected(true);
			}
			if (e.getSource() == stopBtn) {
				metro.execute(metro.stop, null);
				stopBtn.setSelected(true);
			}
			if (e.getSource() == bpb) {
				try {
					metro.getProps().put("bpb", Integer.parseInt(bpb.getText()));
					metro.execute(metro.settings, metro.getProps());
				} catch (Throwable t) {
					log.info(t.getMessage());
					bpb.setText("" + metro.getProps().get("bpb"));
				}
			}
			if (e.getSource() == bpmText) {
				try {
					metro.getProps().put("bpm", Float.parseFloat(bpmText.getText()));
					metro.execute(metro.settings, metro.getProps());
				} catch (Throwable t) {
					log.info(t.getMessage());
					bpmText.setText("" + metro.getProps().get("bpb"));
				}
			}

		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

}
