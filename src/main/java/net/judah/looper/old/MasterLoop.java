package net.judah.looper.old;

import static net.judah.looper.old.GLoop.Mode.*;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import lombok.extern.log4j.Log4j;
import net.judah.jack.AudioTools;
import net.judah.jack.ClientConfig;
import net.judah.jack.Status;
import net.judah.looper.ClipBoard;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Log4j
public class MasterLoop extends GLoop {

	private ClipBoard clipboard;

	public MasterLoop(Looper looper, ClientConfig config, LoopSettings settings) throws JackException {
		super(looper, config, settings);
		clipboard = new ClipBoard(Constants.CHANNELS, buffersize);
		start();
	}

	@Override
	protected void makeConnections() throws JackException { // TODO use patchbay
		AudioTools.makeConnecction(jackclient, outputPorts[0], "system", "playback_1");
		AudioTools.makeConnecction(jackclient, outputPorts[1], "system", "playback_2");
    }

	float[][] ins;
	float[][] tape;
	FloatBuffer buf;
	int i;
	long time;

	@Override
	public boolean process(JackClient client, int nframes) {
		tape = null;
        if (state.get() != Status.ACTIVE) {
            return false;
        }
		time = System.currentTimeMillis();

		// get data from Jack
		for (i = 0; i < inputPorts.length; i++) {
            inputs.set(i, inputPorts[i].getFloatBuffer());
        }
        for (i = 0; i < outputPorts.length; i++) {
            outputs.set(i, outputPorts[i].getFloatBuffer());
        }

    	if (playback.compareAndSet(STOPPING, STOPPED)) {
    		looper.UI.update();
    	}
    	else if (playback.compareAndSet(STARTING, RUNNING)) {
    		looper.UI.update();
    	}

    	if (playback.get() == RUNNING)
   			tape = clipboard.play(outputs, nframes);
    	else
    		AudioTools.processSilence(outputs);

        if (recording.compareAndSet(STOPPING, STOPPED)) {
        	clipboard.stopRecording(jackTime(client));
    		if (playback.compareAndSet(Mode.ARMED, Mode.STARTING));
        	looper.UI.update();
        }
        if (recording.compareAndSet(STARTING, RUNNING)) {
        	clipboard.startRecording(jackTime(client));
        	looper.UI.update();
        }

    	if (recording.get() == RUNNING) {
    		if (clipboard.isEmpty()) {
        		ins = memory.getArray();
        		for (i = 0; i < Math.min(inputs.size(), ins.length); i++) {
        			buf = inputs.get(i);
        			for (int j = 0; j < buf.capacity(); j++)
        				ins[i][j] = buf.get();
        		}
    			clipboard.record(ins);
    		}
    		else {
    			// wasn't overdubbing anyways
    			// clipboard.overdub(tape, inputs, memory.getArray());
    		}
//    		if (!clipboard.isEmpty()) {
//    			outs = memory.getArray();
//    			clipboard.mix(ins, outs);
//    		}
    	}


//    		if (settings.playLiveMic) { // primary loop plays the live mic
//    			if (clipboard.isEmpty())
//    				processEcho(inputs, outputs);
//    			else {// overdub
//    				if (outs == null) {
//    					outs = memory.getArray();
//clipboard.mix(ins, outs);
//    				}
//    				processEcho(outs, outputs);
//    			}
//    			return true;
//    		}
//   			clipboard.play(outputs, nframes);
//   			return true;
//    	}


//        // copy data (for now even if while we aren't recording)
//		ins = memory.getArray();
//		for (i = 0; i < Math.min(inputs.size(), ins.length); i++) {
//			buf = inputs.get(i);
//			for (int j = 0; j < buf.capacity(); j++)
//				ins[i][j] = buf.get();
//			buf.rewind();
//		}
//
//        if (recording.compareAndSet(STOPPING, STOPPED)) {
//
//        	clipboard.stopRecording(jackTime(client));
//    		if (playback.compareAndSet(Mode.ARMED, Mode.STARTING));
//        	looper.UI.update();
//        }
//        if (recording.compareAndSet(STARTING, RUNNING)) {
//        	clipboard.startRecording(jackTime(client));
//        	looper.UI.update();
//        }
//
//    	if (recording.get() == RUNNING) {
//    		clipboard.record(ins);
//    		if (!clipboard.isEmpty()) {
//    			outs = memory.getArray();
//    			clipboard.mix(ins, outs);
//    		}
////			for (Listener listener : listeners) {
////				listener.newFrame(clipboard.size(), time);
////			}
//    	}
//
//    	if (playback.compareAndSet(STOPPING, STOPPED)) {
//    		looper.UI.update();
//    	}
//    	if (playback.compareAndSet(STARTING, RUNNING)) {
//    		counter.set(0);
//    		looper.UI.update();
//    	}
//    	if (playback.get() == RUNNING) {
////    		if (settings.playLiveMic) { // primary loop plays the live mic
////    			if (clipboard.isEmpty())
////    				processEcho(inputs, outputs);
////    			else {// overdub
////    				if (outs == null) {
////    					outs = memory.getArray();
////clipboard.mix(ins, outs);
////    				}
////    				processEcho(outs, outputs);
////    			}
////    			return true;
////    		}
//   			clipboard.play(outputs, nframes);
//   			return true;
//    	}
//
////    	if (settings.playLiveMic) // primary loop plays the live mic
////    		processEcho(inputs, outputs);
////    	else
//
//    	processSilence(outputs);

    	return true;
	}

	private long jackTime(JackClient client) {
		try {
			return client.getLastFrameTime();
		} catch (JackException e) {
			RTLogger.warn(this, e);
			return -1l;
		}
	}

	@Override
	public void clear() {
		playback.set(STOPPING);
		recording.set(STOPPING);
		new Thread() {
			@Override public void run() {
				try {
					Thread.sleep(100); // for a frame or two
				} catch (InterruptedException e) {
					log.warn(e.getMessage(), e);
				}
				clipboard.clear();
				log.debug(jackclient.getName() + " at your command");
			}
		}.start();
	}

    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

//	/** put recording in the out buffer
//	 * Play https://github.com/kampfschlaefer/jackmix/blob/master/backend/jack_backend.cpp*/
//	public void play(List<FloatBuffer> outputs, int nframes) {
//		float[][] old = next().getInputs();
//		FloatBuffer b;
//		for (int i = 0; i < outputs.size(); i++) {
//			b = outputs.get(i);
//			b.rewind();
//			for (int j = 0; j < nframes; j++) {
//				b.put(old[i][j] * gain);
//			}
//		}
//	}

//	public void record(final long time, List<FloatBuffer> inputs, final int nframes) {
//		final float[][] ins = memory.getArray();

//		FloatBuffer buf;
//		for (int i = 0; i < Math.min(inputs.size(), ins.length); i++) {
//			buf = inputs.get(i);
//			for (int j = 0; j < buf.capacity(); j++)
//				ins[i][j] = buf.get();
//			buf.rewind();
//		}
//		new Thread() {
//			@Override public void run() {
//				if (liveRecording.isEmpty())
//					liveRecording.setStartTime(time);
//				liveRecording.add(new Chunk(time, ins));
//				for (Listener listener : listeners) {
//					listener.newFrame(liveRecording.size(), time);
//				}
//				}}.start();
//	}

//	@Override
//	/** in RT thread */
//	protected void recordingFinished(long time) {
//		if (loopLength == null) { // first recording
//			// this is it boys, let's start teh loop-di-loops...
//			loopLength = liveRecording.size();
//			clipboard.push(liveRecording);
////			liveRecording = new Chunks();
//			// listeners
//			for (Listener listener : listeners) {
//				listener.sealTheDeal(loopLength, time);
//			}
//
//			if (playback.compareAndSet(Mode.ARMED, Mode.STARTING));
//
//			new Thread() {
//				@Override public void run() {
//					log.info("First loop made. " + loopLength + " frames.");
////					new LoopAnalysis(MasterLoop.this);
//				}
//			}.start();
//		}
//	}

}
