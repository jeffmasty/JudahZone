package net.judah.omni;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Threads {
	private static final ExecutorService threads = Executors.newFixedThreadPool(2 ^ 7);

	private static final ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();

	public static void sleep(long millis) {
	    try {
	        Thread.sleep(millis);
	    } catch(Throwable t) {System.err.println(t.getMessage());}
	}

	public static void timer(long msec, final Runnable r) {
	    threads.execute(()->{
    		sleep(msec);
    		r.run();
	    });
	}

	public static void execute(Runnable r) {
		threads.execute(r);
	}

	public static void virtual(Runnable r) {
		virtual.execute(r);
	}

    public static void writeToFile(File file, String content) {
    	execute(() -> {
            try { Files.write(Paths.get(file.toURI()), content.getBytes());
            } catch(IOException e) {e.printStackTrace();}
        });
    }



}
