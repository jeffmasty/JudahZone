package net.judah.looper;

import static net.judah.settings.Commands.MixerLbls.*;
import static net.judah.util.Constants.Param.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Loadable;
import net.judah.util.JudahException;

@Log4j
public class AudioPlay extends Command implements Loadable {
	
	Clip clip;
	
	public AudioPlay() {
		super(AUDIOPLAY.name, AUDIOPLAY.desc, template());
	}
	
	private static HashMap<String, Class<?>> template() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put("file", String.class);
		return result;
	}

	@Override
	public void load(HashMap<String, Object> props) throws IOException {
		
		try {
			File file = new File("" + props.get("file"));
	        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
	        clip = AudioSystem.getClip();
	        clip.open(stream); 
	        log.debug(file.getName() + " loaded. " + stream.getFormat().toString());
		} catch(Exception e) {
			throw (e instanceof IOException) ? (IOException)e : new IOException(e);
		}
	}

	@Override
	public void close() {
		if (clip != null && clip.isOpen())
			clip.close();
	}

	@Override
	public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
		boolean active = false;
		if (midiData2 < 0) 
			active = Boolean.parseBoolean("" + props.get(ACTIVE));
		else
			active = midiData2 > 0;
			
		if (clip == null || !clip.isOpen()) throw new JudahException("Oops, no clip");
		if (active) {
			clip.setFramePosition(0);
			clip.start();
		} else
			clip.stop();
	}


}
