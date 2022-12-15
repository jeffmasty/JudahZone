package net.judah.api;

public class JudahException extends Exception {
	private static final long serialVersionUID = 4052236208283488917L;

	public JudahException(String s) {
		super(s);
	}

	public JudahException(Throwable t) {
		super(t);
	}

}
