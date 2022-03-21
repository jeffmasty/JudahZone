package net.judah.clock;

/**
https://github.com/UlrikHjort/Unix-Cmdline-Midi-Master-Clock

void send_clock(int fd, uint32_t bpm) {

  uint8_t  time_sync_cmd[] = {0xF8};
  uint32_t tempo = 1000/(bpm/60.0);
  
  uint32_t interval = tempo/24.0; // 24 ppqn    
  uint32_t prev_time = 0;
  
  while (1) {
   
    uint32_t  current_time = current_time_ms();

    if (current_time - prev_time > interval) {
      prev_time = current_time;
      write(fd, time_sync_cmd, 1);	  
    }
  }
}
*/

public class Midi24 {

	
	
}
