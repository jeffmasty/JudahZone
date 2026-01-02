package net.judah.bridge;
/*
•  Introduce a small audio-sink abstraction (example AudioOutput) that JudahZone will call instead of writing to JACK ports.
•  Change JudahZone.process(...) to accept (or otherwise have) an AudioOutput and to write final left/right into that sink instead of AudioTools.copy(...).
•  Keep hot per-instance product buffers (no escaping). When something async needs a frame (scope/recording/UI), copy into a pool frame (e.g. Memory.STEREO.getFrame() or a small circular pool) before handing it off.
•  Provide a concrete sink implementation JavaxAudioSink that converts float[] to PCM16 and writes to a SourceDataLine (based on JavaxOut code).
•  Replace JACK callback scheduling with a small runner/adaptor that repeatedly calls JudahZone.process(...) at JACK buffer cadence (or integrate with existing real-time thread when available).

Brief notes on safety

•  Never hand left/right (hot buffers) directly to async paths. Always copy into a pooled frame for UI/recording.
•  Zero hot buffers at start of process.
•  Keep real-time work minimal and avoid allocations in process except pooled/taken frames.

Code: AudioOutput interface */

/*
//inside JudahZone (modify signature to accept AudioOutput or hold one as a field)
public boolean process(int nframes, net.judah.audio.AudioOutput output) {

if (!initialized) return true;

// zero hot product buffers
Arrays.fill(left, 0f);
Arrays.fill(right, 0f);

if (mains.isOnMute()) {
    if (clock.isActive()) looper.silently();
    // output silence via sink
    output.write(left, right, nframes);
    return true;
}

// ... run DSP workers and mixes (unchanged) ...

// instead of AudioTools.copy(...), write to sink
output.write(left, right, nframes);

// analysis: when passing frames to async code, copy into a pooled frame
boolean activeScope = scope.isActive();
boolean activeWaveform = MainFrame.getKnobMode() == KnobMode.Tuner;
if (activeScope || activeWaveform) {
    float[][] buf = Memory.STEREO.getFrame(); // pooled frame
    // fill pooled frame from selected channels
    selected.forEach(ch -> {
        AudioTools.mix(ch.getLeft(), buf[Constants.LEFT]);
        AudioTools.mix(ch.getRight(), buf[Constants.RIGHT]);
    });
    // safe to hand buf to knobs/realtime because it's a separate pooled frame
    if (activeWaveform && MainFrame.getKnobs() instanceof TunerKnobs knobs)
        knobs.process(buf);
    if (activeScope) {
        realtime.add(buf);
        // ...
    }
}

return true;
} */

/*
•  Add AudioOutput and implement JavaxAudioSink.
•  Change JudahZone to call the sink instead of JACK ports; remove outL/outR usage and ZoneJackClient dependency when ready.
•  Provide a real-time adapter that calls process(nframes, sink) at JACK cadence (or replace JACK processing loop with a scheduler for non-JACK playback).
•  Ensure any frame that leaves the audio thread is copied into a pooled frame (Memory.STEREO or a small circular pool).
•  Test for latency and underrun behavior in JavaxAudioSink and tune line buffer sizes. */

/* JavaxGraph implements the audio cadence.
•  Cadence definition: one cycle = WavConstants.JACK_BUFFER frames at WavConstants.S_RATE. JavaxGraph computes nanosPerCycle from those constants and runs a periodic loop to hit that cadence.
•  What JavaxGraph does: it zeroes reusable per-cycle buffers, calls registered AudioProducer.process(left, right, frames) for each producer, optionally calls MidiClock.pulse(), then calls registered AudioConsumer.consume(left, right, frames).
•  Guarantees/limits: it's a lightweight, best‑effort scheduler (block‑accurate at JACK_BUFFER). Not sample‑accurate like an audio‑driver callback; timing depends on thread scheduling and sleep/spin logic.
•  Interaction with JavaxOut: JavaxOut is a consumer/renderer with its own chunking logic (it uses WavConstants.FFT_SIZE for its play loop). That is compatible: JavaxGraph supplies blocks of JACK_BUFFER frames to consumers; consumers may buffer/convert as needed.
•  Migration note for JudahZone: register JudahZone (or an adapter) as an AudioProducer for JavaxGraph; keep hot per‑instance left/right buffers and never let them escape the audio thread — copy into pooled frames (Memory.STEREO or a circular pool) before handing to async code (scope, UI, recording).

Summary: use JavaxGraph as the cadence source (calls per JACK_BUFFER), register JudahZone as a producer and JavaxOut (or a sink adapter) as a consumer.	 */

public interface AudioOutput {
    /**
     * Write interleaved or non-interleaved stereo PCM floats into the sink.
     * Implementations must not block indefinitely on the calling thread.
     *
     * @param left  buffer of length nframes
     * @param right buffer of length nframes
     * @param nframes number of frames valid in each buffer
     */
    void write(float[] left, float[] right, int nframes);

    /** Optional lifecycle */
    default void start() {}
    default void stop() {}
    default void close() {}
}
