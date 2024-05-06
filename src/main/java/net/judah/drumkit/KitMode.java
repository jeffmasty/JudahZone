package net.judah.drumkit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum KitMode {
	Drum1(10), Drum2(11), Hats(12), Fills(13);
	
	private final int ch;
}
