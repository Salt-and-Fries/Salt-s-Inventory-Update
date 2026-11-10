package com.siu_release_remote;

import java.io.IOException;
import java.nio.file.*;

/** Recorder I/O only: Windows readers may briefly prevent replacing an old reply. */
public final class AtomicObservation {
    private AtomicObservation(){}
    public static void write(Path destination,String text)throws IOException,InterruptedException {
        Files.createDirectories(destination.getParent());
        Path temporary=destination.resolveSibling(destination.getFileName()+".tmp");
        Files.writeString(temporary,text);
        long deadline=System.nanoTime()+1_000_000_000L;
        while(true)try{
            Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);return;
        }catch(FileSystemException busy){
            if(System.nanoTime()>=deadline)throw busy;
            // Retry publication of the already serialized observation only.
            // The input action is never invoked again and its result is unchanged.
            Thread.sleep(5);
        }
    }
}
