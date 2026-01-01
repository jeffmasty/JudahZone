package net.judah.gui.fft_temp;

public class JIT {

}


//	/** put algorithms through their paces */
//	public void justInTimeCompiler() {
//
//		looper.onDeck(looper.getSoloTrack());
//		looper.getSoloTrack().solo(true);
//		mains.getReverb().setActive(true);
//		final float restore = mains.getGain().getGain();
//		mains.getGain().setGain(0.05f);
//		mains.setOnMute(false);
//		instance.mic.getReverb().setActive(true);
//		final Effect[] fx = { guitar.getReverb(), guitar.getDelay(), guitar.getChorus(), guitar.getLfo(),
//				guitar.getEq(), guitar.getHiCut(), guitar.getLoCut(), guitar.getCompression() };
//		for (Effect effect : fx)
//			effect.setActive(true);
//		looper.trigger(looper.getLoopC());
//		int timer = 777;
//		Threads.timer(timer, () -> {
//			looper.getLoopC().capture(false);
//			instance.mic.getLfo2().setActive(true);
//			midi.getJamstik().toggle();
//			for (Effect effect : fx)
//				effect.setActive(false);
//			guitar.getGain().set(Gain.PAN, 25);
//		});
//		Threads.timer(timer * 2 + 100, () -> {
//			if (midi.getJamstik().isActive())
//				midi.getJamstik().toggle();
//			looper.delete();
//			looper.getSoloTrack().solo(false);
//			// looper.get(0).load("Satoshi2", true); // load loop from disk
//			mains.getReverb().setActive(false);
//			guitar.getGain().set(Gain.PAN, 50);
//			instance.mic.getReverb().setActive(false);
//			instance.mic.getLfo2().setActive(false);
//			mains.getGain().setGain(restore);
//			// try { Tape.toDisk(sampler.get(7).getRecording(),
//			// new File("/home/judah/djShadow.wav"), sampler.get(7).getLength());
//			//	Threads.timer(timer * 4, () -> {
//			//		looper.delete(); }); // clear loop loaded from disk
//			// } catch (Throwable t) { RTLogger.warn("JudahZone.JIT", t); }
//		});
//	}
//

