package net.judah.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.log4j.Log4j;

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
        @SuppressWarnings("unchecked")
		ArrayList<String> result = MAPPER.readValue(fis, ArrayList.class);
        fis.close();
        return result;
	}
	
//	public static String setlist2Json(Songlist list) throws IOException {
//		String json = MAPPER.writeValueAsString(list.getSongs());
//		log.info(json);
//		return json;
//	}
	
	public static void saveString(String json, File toFile) throws IOException {
		if (!toFile.isFile()) 
			toFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(toFile);
        fos.write(json.getBytes());
        fos.close();
	}
	
}
