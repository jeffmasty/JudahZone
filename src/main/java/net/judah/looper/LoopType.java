package net.judah.looper;

public enum LoopType {
	/** sync to clock measures */ SYNC,
	/** sync to clock but unknown number of measures at recording start */ BSYNC,
	/** not sync'ed to clock */ FREE
}
