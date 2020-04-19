package net.judah.jack;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.Tab;
import net.judah.midi.JackReceiver;
import net.judah.midi.MidiPlayer;
import net.judah.plugin.Carla;
import net.judah.settings.Service;

@Log4j
public class JackUI extends Tab {
	private static final long serialVersionUID = 8233690415021867378L;

	boolean firsttime = true;

	@Override
	public String getTabName() {
		return "Jack";
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String text = super.parseInputBox(event);
		String[] split = text.split(" ");
		
		if (text.equalsIgnoreCase("help")) {
			addText("xrun");
			addText("volume carla_plugin_index value");
			addText("active carla_plugin_index 1_or_0");
			addText("parameter carla_plugin_index parameter_index value");
			return;
		}

		if (text.startsWith("xruns") || text.equalsIgnoreCase("xrun")) {
			for (Service s : JudahZone.getServices())
				if (s instanceof BasicClient) {
					addText(((BasicClient)s).xrunsToString());
				}
			return;
		}
		
		//  set_active	set_parameter_value set_volume 
		if (text.startsWith("volume ") && split.length == 3) {
			getCarla().setVolume(Integer.parseInt(split[1]), Float.parseFloat(split[2]));
		}
		else if (text.startsWith("active ") && split.length == 3) {
			getCarla().setActive(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
		}
		else if (text.startsWith("parameter ") && split.length == 4) {
			getCarla().setParameterValue(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Float.parseFloat(split[3]));
		}
		else if (text.startsWith("midi")) {
			try {
				File midiFile = null;
				if (split.length == 2) {
					try {
						midiFile = new File(split[1]);
						if (!midiFile.isFile()) {
							throw new FileNotFoundException(split[1]);
						}
					} catch (Throwable t) {
						addText(t.getMessage());
						log.info(t.getMessage(), t);
					}
				}
				if (midiFile == null) {
					addText("uh-oh, no midi file.");
					midiFile = new File("/home/judah/Tracks/midi/dance/dance21.mid");
				}
				
				MidiPlayer playa = new MidiPlayer(midiFile, 80, 0, 
						new JackReceiver(JudahZone.getServices().getMidiClient()));
				addText(playa.getSequencer().getDeviceInfo() + " / " + playa.getSequencer().getMasterSyncMode());
				playa.start();
			} catch (Throwable t) {
				addText(t.getMessage());
				log.error(t.getMessage(), t);
			}
		}

	}

	private Carla getCarla() {
		return (Carla)JudahZone.getServices().byClass(Carla.class);
	}
	
}
