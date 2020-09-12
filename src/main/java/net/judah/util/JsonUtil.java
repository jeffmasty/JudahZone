package net.judah.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.log4j.Log4j;
import net.judah.midi.Midi;
import net.judah.song.Link;
import net.judah.song.Songlist;

@Log4j
public class JsonUtil {

	public static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static void saveSetList(ArrayList<String> list, File file) throws IOException {
		saveString(MAPPER.writeValueAsString(list), file);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object readJson(File file, Class clazz) throws IOException {
        InputStream fis = new FileInputStream(file);
		return MAPPER.readValue(fis, clazz);
	}
	
	public static ArrayList<String> loadSetlist(File file) throws IOException {
        InputStream fis = new FileInputStream(file);
        ArrayList<String> result = MAPPER.readValue(fis, ArrayList.class);
        // Songlist result = mapper.readValue(fis, Songlist.class);
        fis.close();
        // return result;
        return result;
	}
	
	
	public static String setlist2Json(Songlist list) throws IOException {
		
		String json = MAPPER.writeValueAsString(list.getSongs());
		log.info(json);
		return json;
	}
	
	public static void saveString(String json, File toFile) throws IOException {
		if (!toFile.isFile()) 
			toFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(toFile);
        fos.write(json.getBytes());
        fos.close();
	}
	
	public static void main(String[] args) throws IOException {
		
//		@Getter @Setter
//		class Temp {
//			private final Properties yoProps = new Properties();
//		}
//		
//		Temp t = new Temp();
//		t.yoProps.put("readings", "hot");
//		t.yoProps.put("Value", 0.75);
//		Properties props = new Properties();
//		props.put("folder", "JudahZone");
//		props.put("setlist", "/home/setlist1.songs");
//
//		log.info(MAPPER.writeValueAsString(t));
//		
//		HashMap<String,Object> result = new ObjectMapper().readValue(
//				"{ \"Value\" : 0.666, \"readings\" : \"lukewarm\" }", HashMap.class);
//		
//		for (Entry<String, Object> e : result.entrySet()) {
//			log.warn(e.getKey() + " = " + e.getValue());
//		}
//		
		
		Link l = new Link();
		
		HashMap<String, Object> props = new HashMap();
		props.put("Loop", 1);
		props.put("Active", true);
		// mappings.add(new Mapping(command, new Midi(176, 0, 97, 127), props));
		
		try {
			l.setMidi(new Midi(176, 0, 97, 127).getMessage());
		} catch (InvalidMidiDataException e) {
			log.error(e.getMessage(), e);
		}
		
		l.setProps(props);
//				);
		log.info(MAPPER.writeValueAsString(l));
		
        InputStream fis = new FileInputStream("/home/judah/temp/link.json");
        Link result = MAPPER.readValue(fis, Link.class);
        // Songlist result = mapper.readValue(fis, Songlist.class);
        
        fis.close();
        log.info("Link...");
        log.info(result);
        
        Midi midi = new Midi(result.getMidi());
        log.info(midi.getCommand() + " command on channel " + midi.getChannel() + " " + midi.getData1() + " " + midi.getData2());
		
	}
	 
	
}
