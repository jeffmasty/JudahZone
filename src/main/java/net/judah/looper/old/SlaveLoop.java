package net.judah.looper.old;

import org.jaudiolibs.jnajack.JackException;

import lombok.extern.log4j.Log4j;
import net.judah.jack.AudioTools;
import net.judah.jack.ClientConfig;

@Log4j
public class SlaveLoop extends GLoop implements Listener {

	public SlaveLoop(Looper looper, ClientConfig config, LoopSettings settings) throws JackException {
		super(looper, config, settings);
		start();
	}

	@Override // TODO allow for more ports
	protected void makeConnections() throws JackException { // TODO use patchbay
    	if (looper.getMasterLoop() != null)
    		looper.getMasterLoop().register(this);
		AudioTools.makeConnecction(jackclient, inputPorts[0], "system", "capture_2");
		AudioTools.makeConnecction(jackclient, inputPorts[1], "system", "capture_2");
		AudioTools.makeConnecction(jackclient, outputPorts[0], "system", "playback_1");
		AudioTools.makeConnecction(jackclient, outputPorts[1], "system", "playback_2");
    }

	@Override
	public void newFrame(int frame, long time) {
		// liveRecording.add(new Chunk(time, AudioTools.silence(outputPorts.length, buffersize)));
	}

	@Override
	public void sealTheDeal(int frame, long time) {
//		loopLength = liveRecording.size();
//		new Thread() {
//			@Override public void run() {
//				log.info("Slave participating. " + loopLength + " frames.");
//				new LoopAnalysis(SlaveLoop.this);
// 				if (playback.compareAndSet(Mode.ARMED, Mode.STARTING));
//			}
//		}.start();
	}

	@Override
	public void clear() {
		log.warn("no op");

	}

//	/** no op, I'm a Listener */
//	@Override
//	protected void recordingFinished(long time) {
//	}

}
