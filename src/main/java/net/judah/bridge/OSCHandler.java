package net.judah.bridge;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.udp.OSCPortOut;

import judahzone.util.RTLogger;

/**
 * OSCHandler — encapsulates OSC outbound communication.
 *
 * Responsibilities:
 * - Manage an OSCPortOut instance and its lifecycle (connect/disconnect).
 * - Provide safe send(...) entry points that handle connect-on-demand and
 *   catch/log exceptions so callers don't need to manage OSC errors.
 *
 * Notes:
 * - Methods are synchronized to make the handler thread-safe.
 * - send(...) will attempt to connect if not already connected.
 * - The class does not spawn background threads; callers can call send from any thread.
 */
public class OSCHandler implements Closeable {

	public static final int OSC_PORT = 4040;

	private final InetAddress host;
    private final int port;

    private OSCPortOut osc;

	// Clients: private final ArrayList<Object> oscData = new ArrayList<>();

    /**
     * Construct an OSCHandler bound to given host/port.
     *
     * @param host destination host (e.g. InetAddress.getLoopbackAddress())
     * @param port destination port
     */
    public OSCHandler(InetAddress host, int port) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
    }

    /**
     * Connect (open) the underlying OSCPortOut. Safe to call multiple times.
     *
     * @throws IOException if the socket cannot be opened
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) return;
        try {
            osc = new OSCPortOut(host, port);
            osc.connect();
        } catch (IOException | RuntimeException re) {
            if (osc != null) {
                try { osc.close(); } catch (Exception ignored) {}
                osc = null;
            }
            throw re;
        }
    }

    /**
     * Send an OSC message with a list of arguments.
     * If not connected, this attempts to connect once and then send.
     *
     * @param address OSC address pattern (e.g. "/tempo")
     * @param args    list of OSC arguments (may be empty)
     */
    public synchronized void send(String address, List<Object> args) {
        Objects.requireNonNull(address, "address");
        try {
            if (!isConnected()) {
                try { connect(); } catch (IOException ioe) {
                    RTLogger.warn(this, "OSC connect failed: " + ioe.getMessage());
                    return;
                }
            }
            OSCMessage msg = new OSCMessage(address, args);
            osc.send(msg);
        } catch (Exception e) {
            RTLogger.warn(this, "OSC send failed: " + e.getMessage());
        }
    }

    /**
     * Convenience varargs send.
     *
     * @param address OSC address pattern
     * @param args    zero or more arguments
     */
    public synchronized void send(String address, Object... args) {
        send(address, Arrays.asList(args));
    }

    /**
     * Disconnect/close the OSC port. Safe to call multiple times.
     */
    @Override
    public synchronized void close() {
        if (osc != null) {
            try {
                if (osc.isConnected()) osc.disconnect();
            } catch (Exception e) {
                RTLogger.warn(this, e);
            } finally {
                try { osc.close(); } catch (Exception ignored) {}
                osc = null;
            }
        }
    }

    /** true if OSC is connected */
    public synchronized boolean isConnected() {
        return osc != null && osc.isConnected();
    }

    /** host getter */
    public InetAddress getHost() { return host; }

    /** port getter */
    public int getPort() { return port; }

}