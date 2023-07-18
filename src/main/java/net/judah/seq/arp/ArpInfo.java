package net.judah.seq.arp;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
public class ArpInfo {

	Mode algo = Mode.Off;
	int range = 12;

	public ArpInfo(ArpInfo clone) {
		this.algo = clone.algo;
		this.range = clone.range;
	}

	
}
