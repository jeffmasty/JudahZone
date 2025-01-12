package net.judah.seq.arp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Getter
public class ArpInfo {

	public Arp algo = Arp.Off;
	public int range = 12;

	public ArpInfo(ArpInfo clone) {
		this.algo = clone.algo;
		this.range = clone.range;
	}


}
