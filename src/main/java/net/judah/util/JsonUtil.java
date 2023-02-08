package net.judah.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

	public static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static void writeJson(Object o, File file) throws IOException {
		saveString(MAPPER.writeValueAsString(o), file);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object readJson(File file, Class clazz) throws IOException {
        InputStream fis = new FileInputStream(file);
		return MAPPER.readValue(fis, clazz);
	}
	
	public static void saveString(String json, File toFile) throws IOException {
		if (!toFile.isFile()) 
			toFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(toFile);
        fos.write(json.getBytes());
        fos.close();
	}
	
	public static void threadedSave(String json, File file) {
		Constants.execute(() -> {
            try { Files.write(Paths.get(file.toURI()), json.getBytes());
            } catch(IOException e) {RTLogger.warn("Constants.writeToFile", e);}
        });
    }

}
