package net.judah.midi.fluid;

class FluidChannel {
	public final int channel;
	public final int soundfont;
	public final int bank;
	public final int preset;
	public final String name;

	public FluidChannel(int channel, int soundfont, int bank, int preset, String name) {
		this.channel = channel;
		this. soundfont = soundfont;
		this.bank = bank;
		this.preset = preset;
		this.name = name;
	}

	public FluidChannel(String fluidString) {
		// example: chan 0, sfont 1, bank 0, preset 0, Yamaha Grand Piano
		String[] split = fluidString.split(",");
		if (split.length != 5) throw new IllegalArgumentException("for channel " + fluidString);
		channel = Integer.parseInt(split[0].trim().split(" ")[1]);
		soundfont = Integer.parseInt(split[1].trim().split(" ")[1]);
		bank = Integer.parseInt(split[2].trim().split(" ")[1]);
		preset = Integer.parseInt(split[3].trim().split(" ")[1]);
		name = split[4].trim();
	}

	@Override
	public String toString() {
		return "channel: " + channel + " name: " + name + " soundfont " + soundfont + " bank: " + bank + " preset " + preset;
	}

}
