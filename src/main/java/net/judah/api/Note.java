package net.judah.api;

public record Note(Key key, int octave, float diff) {

	public Note(Key key, int octave) {
		this(key, octave, 0);
	}

	@Override public String toString() {
		return (key.alt == null ? key.name() : key.alt) + "" + octave;}

	public String full() {
		StringBuilder sb = new StringBuilder("(").append(toString());
		sb.append(diff == 0 ? "" : diff > 0 ? " +" + String.format("%.1f", diff) : " " + String.format("%.1f", diff));
		return sb.append(")").toString();
	}

}