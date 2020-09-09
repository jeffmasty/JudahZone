package net.judah.song;

import java.io.File;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class Songlist {
	
	@Getter @Setter
	private ArrayList<File> songs = new ArrayList<File>();
}

