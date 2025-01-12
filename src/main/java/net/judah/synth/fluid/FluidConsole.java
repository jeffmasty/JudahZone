package net.judah.synth.fluid;

import static net.judah.util.Constants.NL;

import net.judah.util.RTLogger;

class FluidConsole implements RTLogger.Participant {
	public static final String PREFIX = "fluid";
	private final FluidSynth fluid;

	public FluidConsole(FluidSynth fluidSynth) {
		this.fluid = fluidSynth;
	}

	public String getPrefix() {
		return PREFIX;
	}

	@Override
	public void process(String[] input) {
		if (input == null || input.length < 2 || !PREFIX.equals(input[0])) return;
		String text = input [1];
		if (text.equals("help") && input.length == 2) {
			doHelp();
			return;
		}
		if (text.equals("sync")) {
			try {
				fluid.syncChannels();
				fluid.syncInstruments();
			} catch (Throwable e) {
				RTLogger.warn(this, e);
			}
		}

		if (text.equals("channels")) {
			RTLogger.addText("channels: " + fluid.getChannels().size() + NL);
			for (FluidChannel channel : fluid.getChannels()) {
				RTLogger.addText(channel.toString() + NL);
			}
		}
//		if (text.equals("inst") || text.equals("instruments")) {
//			Console.addText("instruments: " + FluidSynth.getInstruments().size() + NL);
//			for (MidiPatch instrument : FluidSynth.getInstruments()) {
//				Console.addText(instrument.toString() + NL);
//			}
//		}
//		if (text.equals("current")) {
//			Console.addText("current: " + FluidSynth.getInstruments().get(fluid.getChannels().getCurrentPreset(0)));
//		}
//		if (text.equals("mute")) {
//			fluid.mute();
//		}
		else {
			if (input.length > 2) text = text + " " + input[2];
			if (input.length > 3) text = text + " " + input[3];
			fluid.sendCommand(text);
		}

	}

	private void doHelp() {
		RTLogger.addText("Fluid help, commands use 'fluid' prefix.");
		fluid.sendCommand("help");
		try { Thread.sleep(20); } catch (InterruptedException e) { }
		RTLogger.addText("fluid sync");
		RTLogger.addText("fluid instruments");
		RTLogger.addText("fluid current");
		RTLogger.addText("fluid GM_NAMES");
	}




}
