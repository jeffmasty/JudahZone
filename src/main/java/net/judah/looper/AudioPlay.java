package net.judah.looper;

import static net.judah.util.Constants.Param.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import lombok.extern.log4j.Log4j;
import net.judah.api.Loadable;
import net.judah.util.JudahException;

@Log4j
public class AudioPlay implements Loadable {
	
	Clip clip;
	
	public static HashMap<String, Class<?>> template() {
		HashMap<String, Class<?>> result = activeTemplate();
		result.put("file", String.class);
		return result;
	}

	
	public static void main(String[] args) {
		HashMap<String, Object> props = new HashMap<>();
		props.put("file", "resources/samples/FeelGoodInc.wav");
		try {
			
			AudioPlay test = new AudioPlay();
			test.load(props);
			test.execute(props, 1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(File file) throws IOException {
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
	        clip = AudioSystem.getClip();
	        clip.open(stream); 
	        
	        log.debug(file.getName() + " loaded. " + stream.getFormat().toString());
		} catch(Exception e) {
			throw (e instanceof IOException) ? (IOException)e : new IOException(e);
		}
		
	}
	
	@Override
	public void load(HashMap<String, Object> props) throws IOException {
		File file = new File("" + props.get("file"));
		if (!file.isFile()) throw new IOException(file.getAbsolutePath());
		load(file);
	}

	@Override
	public void close() {
		if (clip != null && clip.isOpen())
			clip.close();
	}

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
			
			log.info("clip started.");
			Thread.sleep(1000);
		} else
			clip.stop();
	}


}
