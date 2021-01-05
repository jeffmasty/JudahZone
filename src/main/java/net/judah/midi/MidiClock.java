package net.judah.midi;

import net.judah.api.TimeProvider;

public interface MidiClock extends TimeProvider {
	// a2j:Komplete Audio 6 [24] (capture): Komplete Audio 6 MIDI 1
	// JudahMidi:midiIn
	
	void process(byte[] midi);
	
//	ArrayList<TimeListener> listeners = new ArrayList<>();
//			
//	int pulse;
//	int beat;
//	long ticker;
//	long delta;
//	int tempo;
//	
//	public void process(Midi midi) {
//		
//		if (ShortMessage.TIMING_CLOCK == midi.getStatus()) {
//			pulse++;
//			if (pulse == 1) {
//				ticker = System.currentTimeMillis();
//			}
//			if (pulse == 25) {
//				listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
//			}
//			if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
//				listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
//				delta = System.currentTimeMillis() - ticker;
//				float temptempo = Constants.toBPM(delta, 2);
//				assert temptempo > 0 : temptempo;
//				// RTLogger.log(this, "tempo: " + temptempo);
//				if (Math.round(temptempo) != tempo) {
//					tempo = Math.round(temptempo);
//					listeners.forEach(l -> {l.update(Property.TEMPO, 
//							tempo); });
//					// RTLogger.log(this, "TEMPO! : " + tempo);
//				}
//				pulse = 0;
//			}
//		}
//
//		else if (ShortMessage.START == midi.getStatus()) { 
//			Console.info("MIDI START!");
//			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
//					JackTransportState.JackTransportStarting); });
//			beat = 0;
//			pulse = 0;
//		}
//		
//		else if (ShortMessage.STOP == midi.getStatus()) {
//			Console.info("MIDI STOP");
//			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
//					JackTransportState.JackTransportStopped); });
//		}
//		
//		else if (ShortMessage.CONTINUE == midi.getStatus()) {
//			Console.info("MIDI CONTINUE");
//			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
//					JackTransportState.JackTransportRolling); });
//
//		}
//		
//		else 
//			Console.info("unknown beat buddy " + midi);
//	}
//
//	@Override
//	public void addListener(TimeListener l) {
//		if (!listeners.contains(l))
//			listeners.add(l);
//	}
//
//	@Override
//	public void removeListener(TimeListener l) {
//		listeners.remove(l);
//	}
//
//	@Override
//	public float getTempo() {
//		return tempo;
//	}
//
//	/** no-op, tempo is set by midi process() */
//	@Override
//	public boolean setTempo(float tempo) {
//		return false;
//	}
//
//	/** no-op */
//	@Override
//	public int getMeasure() {
//		return 0;
//	}
//
//	/** no-op */
//	@Override
//	public void setMeasure(int bpb) {
//	}
//
//	@Override
//	public long getLastPulse() {
//		return ticker;
//	}

}



//if (pulse == 25) {
//	beat++;
//	// calcTempo();
//	delta = System.currentTimeMillis() - ticker;
//	float temptempo = Constants.toBPM(delta);
//	assert temptempo > 0 : temptempo;
//	// RTLogger.log(this, "tempo: " + temptempo);
//	if (Math.round(temptempo) != tempo) {
//		tempo = Math.round(temptempo);
//		listeners.forEach(l -> {l.update(Property.TEMPO, 
//				tempo); });
//		RTLogger.log(this, "TEMPO! : " + tempo);
//	}
//	pulse = 0;
//}

