package net.judah.seq.track;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class SynthInfo {

	Mode mode;
	int octaves;
	
}