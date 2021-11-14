/*************************************************** 
  This is an example for the Adafruit VS1053 Codec Breakout

  Designed specifically to work with the Adafruit VS1053 Codec Breakout 
  ----> https://www.adafruit.com/products/1381

  Adafruit invests time and resources providing this open source code, 
  please support Adafruit and open-source hardware by purchasing 
  products from Adafruit!

  Written by Limor Fried/Ladyada for Adafruit Industries.  
  BSD license, all text above must be included in any redistribution
 ****************************************************/

// define the pins used
#define VS1053_RX  2 // This is the pin that connects to the RX pin on VS1053

#define VS1053_RESET 9 // This is the pin that connects to the RESET pin on VS1053
// If you have the Music Maker shield, you don't need to connect the RESET pin!

// If you're using the VS1053 breakout:
// Don't forget to connect the GPIO #0 to GROUND and GPIO #1 pin to 3.3V
// If you're using the Music Maker shield:
// Don't forget to connect the GPIO #1 pin to 3.3V and the RX pin to digital #2

// See http://www.vlsi.fi/fileadmin/datasheets/vs1053.pdf Pg 31
#define VS1053_BANK_DEFAULT 0x00
#define VS1053_BANK_DRUMS1 0x78
#define VS1053_BANK_DRUMS2 0x7F
#define VS1053_BANK_MELODY 0x79

// See http://www.vlsi.fi/fileadmin/datasheets/vs1053.pdf Pg 32 for more!
#define VS1053_GM1_OCARINA 80

#define MIDI_NOTE_ON  0x90
#define MIDI_NOTE_OFF 0x80
#define MIDI_CHAN_MSG 0xB0
#define MIDI_CHAN_BANK 0x00
#define MIDI_CHAN_VOLUME 0x07
#define MIDI_CHAN_PROGRAM 0xC0
#define GM_HARP 47

#define WAIT 266
#define STEPS 16


//#if defined(__AVR_ATmega328__) || defined(__AVR_ATmega328P__)
  #include <SoftwareSerial.h>
  SoftwareSerial VS1053_MIDI(0, 2); // TX only, do not use the 'rx' side
//#else
  // on a Mega/Leonardo you may have to change the pin to one that 
  // software serial support uses OR use a hardware serial port!
//  #define VS1053_MIDI Serial1
//#endif


bool drums1 = true;
int step = 0;

void setup() {
  
  VS1053_MIDI.begin(31250); // MIDI uses a 'strange baud rate'
  
  pinMode(VS1053_RESET, OUTPUT);
  digitalWrite(VS1053_RESET, LOW);
  delay(10);
  digitalWrite(VS1053_RESET, HIGH);
  delay(10);
  
  midiSetChannelBank(0, VS1053_BANK_MELODY);
  midiSetInstrument(0, GM_HARP);

  midiSetChannelBank(9, VS1053_BANK_DRUMS2);
  
}

byte bass1[] = {0, 4, 7, 8, 12, 15};
byte snare1[] = {2, 6, 10};
byte hat1[] = {1, 3, 5, 7, 9, 11, 13, 15};
byte shake1[] = {13, 14};
byte tom1[] = {14};

void loop() {  
  dummySequence();

  
}

void dummySequence() {
  for (int i = 0; i < sizeof(bass1) / sizeof(byte); i++) {
    if (bass1[i] == step) midiNoteOn(9, 36, 127);
  }
  for (int i = 0; i < sizeof(snare1) / sizeof(byte); i++) {
    if (snare1[i] == step) midiNoteOn(9, 38, 127);
  }
  for (int i = 0; i < sizeof(hat1) / sizeof(byte); i++) {
    if (hat1[i] == step) midiNoteOn(9, 42, 127);
  }
  for (int i = 0; i < sizeof(shake1) / sizeof(byte); i++) {
    if (shake1[i] == step) midiNoteOn(9, 76, 127);
  } 
  for (int i = 0; i < sizeof(tom1) / sizeof(byte); i++) {
    if (tom1[i] == step) midiNoteOn(9, 75, 127);
  } 
  
  step++;
  if (step == STEPS) step = 0;
  delay(WAIT);
  
}

void midiSetInstrument(uint8_t chan, uint8_t inst) {
  if (chan > 15) return;
  inst --; // page 32 has instruments starting with 1 not 0 :(
  if (inst > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_PROGRAM | chan);  
  VS1053_MIDI.write(inst);
}


void midiSetChannelVolume(uint8_t chan, uint8_t vol) {
  if (chan > 15) return;
  if (vol > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_MSG | chan);
  VS1053_MIDI.write(MIDI_CHAN_VOLUME);
  VS1053_MIDI.write(vol);
}

void midiSetChannelBank(uint8_t chan, uint8_t bank) {
  if (chan > 15) return;
  if (bank > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_MSG | chan);
  VS1053_MIDI.write((uint8_t)MIDI_CHAN_BANK);
  VS1053_MIDI.write(bank);
}

void midiNoteOn(uint8_t chan, uint8_t n, uint8_t vel) {
  if (chan > 15) return;
  if (n > 127) return;
  if (vel > 127) return;
  
  VS1053_MIDI.write(MIDI_NOTE_ON | chan);
  VS1053_MIDI.write(n);
  VS1053_MIDI.write(vel);
}

void midiNoteOff(uint8_t chan, uint8_t n, uint8_t vel) {
  if (chan > 15) return;
  if (n > 127) return;
  if (vel > 127) return;
  
  VS1053_MIDI.write(MIDI_NOTE_OFF | chan);
  VS1053_MIDI.write(n);
  VS1053_MIDI.write(vel);
}
