package net.judah.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

	public static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static void writeJson(Object o, File file) throws IOException {
		String json = MAPPER.writeValueAsString(o);
		saveString(json, file);
	}

	public static <T> T readJson(File file, Class<T> clazz) throws IOException {
		return MAPPER.readValue(file, clazz);
	}

	public static void saveString(String json, File toFile) throws IOException {
		Path path = toFile.toPath();
		Files.createDirectories(path.getParent()); // if you need parent dirs
		Files.write(path, json.getBytes());
	}

	public static void threadedSave(String json, File file) {
		Threads.execute(() -> {
			try {
				saveString(json, file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}