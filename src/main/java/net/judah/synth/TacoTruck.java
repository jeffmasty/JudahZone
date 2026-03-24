package net.judah.synth;

public class TacoTruck { // extends SynthEngine {

//	@Getter private final Vector<WavSynth> tracks = new Vector<WavSynth>();
//
//	private static final float[] work = new float[Constants.bufSize()];
//
//    public TacoTruck(String name, ImageIcon picture) {
//    	super(name, Constants.MONO);
//    	icon = picture;
//    	getGain().setPreamp(WavConstants.TO_LINE);
//    }
//
//	public TacoTruck(String name) {
//		this(name, Icons.get("Waveform.png"));
//	}
//
//	@Override public WavSynth getTrack() {
//		if (tracks.isEmpty())
//			return null;
//		return tracks.getFirst();
//	}
//
//	@Override
//	public PianoTrack addTrack(String name, MidiUpdate gui, Chords c) throws InvalidMidiDataException {
//		TacoSynth result = new TacoSynth(name, this, new Notes(this, gui), c);
//		tracks.add(result);
//		return result;
//	}
//
//	@Override public String[] getPatches() {
//		List<String> result = OldSynthDB.keys();
//		return result.toArray(new String[result.size()]);
//	}
//
//	@Override public String progChange(int data2, int ch) {
//		return tracks.get(ch).progChange(data2);
//	}
//
//	@Override public void send(MidiMessage message, long timeStamp) {
//		if (message instanceof ShortMessage s) {
//			int ch = s.getChannel();
//			if (ch >= tracks.size()) {
//				RTLogger.warn(this, "Wrong channel: " + ch);
//				ch = 0;
//			}
//			tracks.get(ch).send(message, timeStamp);
//		}
//
//	}
//
//	@Override public void close() {
//		tracks.clear();
//	}
//
//	@Override public void processImpl() {
//		if (onMute)
//			return;
//
//		AudioTools.silence(work);
//		for (TacoSynth t : tracks) { // gather tracks
//			t.process();
//			AudioTools.mix(t.mono, work);
//		}
//		if (effects.contains(IR))
//			((Convolution.Mono)IR).process(work);
//		gain.monoToStereo(work, left, right);
//		hotSwap(); // activate FX
//		for( int i = 0, n = active.size(); i < n; i++)
//			active.get(i).process(left, right);
//	}
//
//	@Override
//	public int progChange(String preset, int ch) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public String getProgram(int ch) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getProgChange(int ch) {
//		// TODO Auto-generated method stub
//		return 0;
//	}

}