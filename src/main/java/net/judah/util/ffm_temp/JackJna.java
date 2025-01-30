package net.judah.util.ffm_temp;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class JackJna {
    public interface JackLibrary extends Library {
        JackLibrary INSTANCE = Native.load("jack", JackLibrary.class);

        Pointer jack_client_open(String client_name, int options, Pointer status);
        int jack_activate(Pointer client);
        void jack_client_close(Pointer client);
    }

    public static void main(String[] args) {
        long startTime, endTime;

        // Measure jack_client_open
        startTime = System.nanoTime();
        Pointer client = JackLibrary.INSTANCE.jack_client_open("JavaClient", 0, null);
        endTime = System.nanoTime();
        System.out.println("JNA jack_client_open time: " + (endTime - startTime) + " ns");

        // Measure jack_activate
        startTime = System.nanoTime();
        JackLibrary.INSTANCE.jack_activate(client);
        endTime = System.nanoTime();
        System.out.println("JNA jack_activate time: " + (endTime - startTime) + " ns");

        // Measure jack_client_close
        startTime = System.nanoTime();
        JackLibrary.INSTANCE.jack_client_close(client);
        endTime = System.nanoTime();
        System.out.println("JNA jack_client_close time: " + (endTime - startTime) + " ns");
    }
}