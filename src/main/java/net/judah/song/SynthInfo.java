package net.judah.song;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class SynthInfo {

	Mode mode;
	int octaves;
	
}
