package net.judah.drums;

public record DrumUpdate(Drum drum, Mode mode, int paramIdx, int value /* 0 - 100 */) {

	public static enum Mode {BASE, FILTER, NOISE, PLUCK, FM }

}
