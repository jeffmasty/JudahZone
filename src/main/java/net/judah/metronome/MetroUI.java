package net.judah.metronome;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lombok.extern.log4j.Log4j;
import net.judah.util.Tab;

@Log4j @Deprecated
public class MetroUI extends Tab implements  ChangeListener {
	final MetroService metro;
	private final JToggleButton playBtn;
    private final JToggleButton stopBtn;
    JTextField bpb;
    JTextField bpmText;
    JSlider bpm;
    private final JSlider volume;

	public MetroUI(MetroService metro) {
		super(true); // custom ui
		this.metro = metro;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel banner = new JPanel();
		playBtn = new JToggleButton("Play");
		playBtn.addActionListener(this);
		stopBtn = new JToggleButton("Stop");
		stopBtn.addActionListener(this);
		stopBtn.setSelected(true);
		JPanel indicatorPanel = new JPanel();
		indicatorPanel.add(new JLabel("   "));
		banner.add(playBtn);
		banner.add(stopBtn);
		banner.add(indicatorPanel);
		add(banner);

		JPanel beatsPanel = new JPanel();
		beatsPanel.add(new JLabel("BPM:"));
		bpmText = new JTextField("70", 4);
		bpmText.addActionListener(this);
		beatsPanel.add(bpmText);
		beatsPanel.add(new JLabel("Beats per Bar:"));
		bpb = new JTextField("4", 3);
		bpb.addActionListener(this);
		beatsPanel.add(bpb);
		add(beatsPanel);

		JPanel tempoPanel = new JPanel();
		tempoPanel.add(new JLabel("Tempo:"));
		bpm = new JSlider(JSlider.HORIZONTAL, 50, 150, 70);

		bpm.addChangeListener(this);
		bpm.setMajorTickSpacing(20);
		bpm.setPaintTicks(true);
		bpm.setPaintLabels(true);
		tempoPanel.add(bpm);
		add(tempoPanel);

		JPanel volumePanel = new JPanel();
		volumePanel.add(new JLabel("Volume:"));
		volume = new JSlider(JSlider.HORIZONTAL,
                0, 100, 70);
		volume.addChangeListener(this);
		volume.setMajorTickSpacing(20);
		volume.setPaintTicks(true);
		volume.setPaintLabels(true);
		volumePanel.add(volume);
		add(volumePanel);

		update();
//		Properties props = new Properties();
//		props.put("bpb", 4);
//		props.put("bpm", 70f);
//		props.put("volume", 0.7f);
//		setProperties(props);

	}

	@Override
	public String getTabName() {
		return metro.getServiceName();
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

	@Override
	public void setProperties(Properties p) {
		update();
	}


}
