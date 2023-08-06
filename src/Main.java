import processing.core.PApplet;
import processing.sound.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends PApplet {

    // audio objects
    FFT fft; // fast fourier transform object
    AudioIn in; // audio input source
    Amplitude volume;
    SinOsc sound;
    BeatDetector beatDetector;



    // HPS parameters and data
    static final int BANDS = 16384; // the number of frequency bands to decompose signal into
    float[] spectrum = new float[BANDS]; // bins for each frequency band recording amplitude
    static final int SAMPLE_RATE = 44100; // standard microphone sample rate in Hz
    static final int MAX_FREQUENCY = SAMPLE_RATE / 2; // most precise frequency detectable is half sample rate
    static final float RESOLUTION = (float) MAX_FREQUENCY / BANDS; // width of each frequency band in Hz
    static final int HARMONICS = 2; // number of harmonics to consider in product
    float[] hpSpectrum = new float[BANDS]; // to store product of harmonics of each frequency band
    static final float LOW_FREQ_NOISE_CUTOFF = 50f; // below this value, frequencies considered noise
    static final float HIGH_FREQ_NOISE_CUTOFF = 1200f; // above this value, frequencies considered noise
    static final int HPS_START_BIN = (int)(LOW_FREQ_NOISE_CUTOFF / RESOLUTION);
    static final int HPS_END_BIN = (int)(HIGH_FREQ_NOISE_CUTOFF / RESOLUTION);
    static final float MIN_VOLUME = 0.05f; // lowest volume for which pitch will be reported



    // other constants
    static final String[] TONES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    static final float STANDARD_PITCH = 440; // in Hz
    static final int STANDARD_PITCH_OCTAVE = 4; // octave the standard pitch is in
    static final int OCTAVE = 12; // # semitones in an octave
    static final int C_TO_A = 9; // # semitones separating C and A



    // program state
    private int currNoteOffset = 0; // stores offset of the current note from standard pitch, in semitones
    private int nextNoteOffset = getNextNoteOffset(); // stores offset from standard pitch of next note, in semitones
    private float currentAmp = 0.0f; // current amplitude of sound output
    private int activeNoteOffset = 0; // offset from standard pitch of note to be played
    private enum AUDIO_STATE {
        PLAYING,
        OFF
    }
    private AUDIO_STATE audioState = AUDIO_STATE.OFF;
    private enum STATE {
        IDLE,
        PLAY_1,
        PLAY_2,
        LISTEN_1,
        LISTEN_2,
        LISTEN_3,
        LISTEN_4,
        CHECK,
        PAUSE
    }
    private boolean stateChange = true;
    private STATE state = STATE.PLAY_1;
    private final Map<String, Integer> pitchCounts = new HashMap<>();
    private int samplesTaken = 0;
    private String mostFrequent = null;
    private final boolean[] userPlayedInterval = new boolean[2];



    // program parameters
    private static final int[] INTERVALS = {4, 5, 7, 12, -4, -5, -7, -12}; // intervals that will be tested
    private static final int NUM_DETECTION_SAMPLES = 50;
    private static final float VOLUME_RAMP_INCREMENT = 0.05f;
    private static final long NOTE_DURATION = 2000;



    public void settings() {
        size(500, 500);
    }

    public void setup() {
        // initialize audio objects
        fft = new FFT(this, BANDS);
        in = new AudioIn(this, 0);
        volume = new Amplitude(this);
        sound = new SinOsc(this);
        beatDetector = new BeatDetector(this);

        in.start();
        fft.input(in);
        volume.input(in);
        beatDetector.input(in);
    }

    // program loop
    public void draw() {
        update();
        audio();
        animate();
    }

    void animate() {
        background(255);
        noFill();
        strokeWeight(5);
        if (state == STATE.PLAY_1) {
            stroke(0, 255, 0);
            ellipse(width / 3.0f, height / 2.0f, 100, 100);
            stroke(0);
            ellipse(width * 2 / 3.0f, height / 2.0f, 100, 100);
        } else if (state == STATE.PLAY_2) {
            stroke(0);
            ellipse(width / 3.0f, height / 2.0f, 100, 100);
            stroke(0, 255, 0);
            ellipse(width * 2 / 3.0f, height / 2.0f, 100, 100);
        } else {
            stroke(0);
            ellipse(width / 3.0f, height / 2.0f, 100, 100);
            ellipse(width * 2 / 3.0f, height / 2.0f, 100, 100);
        }

        if (state == STATE.LISTEN_1) {
            stroke(0, 255, 0);
            ellipse(width / 3.0f, height / 2.0f, 80, 80);
        }
        if (state == STATE.LISTEN_2) {
            stroke(0, 255, 0);
            ellipse(width * 2 / 3.0f, height / 2.0f, 80, 80);
        }
        fill(0, 0, 255);
        if (beatDetector.isBeat())
            rect(0, 0, 50, 50);
    }

    void audio() {
        if (audioState == AUDIO_STATE.OFF && !sound.isPlaying()) return;
        sound.freq(offsetToFrequency(activeNoteOffset));
        if (audioState == AUDIO_STATE.PLAYING) {
            if (!sound.isPlaying()) sound.play();
            if (currentAmp < 1.0f - VOLUME_RAMP_INCREMENT) {
                currentAmp += VOLUME_RAMP_INCREMENT;
                sound.amp(currentAmp);
            } else {
                currentAmp = 1.0f;
                sound.amp(currentAmp);
            }
        } else if (audioState == AUDIO_STATE.OFF) {
            if (currentAmp > VOLUME_RAMP_INCREMENT) {
                currentAmp -= VOLUME_RAMP_INCREMENT;
                sound.amp(currentAmp);
            } else {
                currentAmp = 0.0f;
                sound.stop();
            }
        }
    }

    void update() {
        if (state == STATE.CHECK) {
            System.out.println(userPlayedInterval[0] + " " + userPlayedInterval[1]);
        }
        if (stateChange) {
            if (state == STATE.PLAY_1) {
                audioState = AUDIO_STATE.PLAYING;
                activeNoteOffset = currNoteOffset;
                (new Timer()).schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                changeState(STATE.PLAY_2);
                            }
                        },
                        NOTE_DURATION
                );
            } else if (state == STATE.PLAY_2) {
                activeNoteOffset = nextNoteOffset;
                (new Timer()).schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                audioState = AUDIO_STATE.OFF;
                                changeState(STATE.LISTEN_1);
                            }
                        },
                        NOTE_DURATION
                );
            } else if (state == STATE.PAUSE) {
                audioState = AUDIO_STATE.OFF;
                (new Timer()).schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                changeState(STATE.PLAY_1);
                            }
                        },
                        NOTE_DURATION
                );
            }
            stateChange = false;
        }
        if (!sound.isPlaying()) {
            if (state == STATE.LISTEN_1) {
                // wait for first note to play
                if (beatDetector.isBeat()) {
                    pitchCounts.clear();
                    mostFrequent = null;
                    samplesTaken = 0;
                    changeState(STATE.LISTEN_2);
                }
            } else if (state == STATE.LISTEN_2) {
                if (samplesTaken >= NUM_DETECTION_SAMPLES) {
                    userPlayedInterval[0] = mostFrequent.equals(getPitch(offsetToFrequency(currNoteOffset)));
                    changeState(STATE.LISTEN_3);
                    System.out.println(pitchCounts);
                }

                System.out.println(samplesTaken);
                // detected note is the most common pitch in a number of samples
                String detected = getPitch(detectPitch());
                if (detected != null) {
                    samplesTaken++;
                    if (!pitchCounts.containsKey(detected))
                        pitchCounts.put(detected, 0);
                    int newCount = pitchCounts.get(detected) + 1;
                    pitchCounts.put(detected, newCount);

                    if (mostFrequent == null || pitchCounts.get(mostFrequent) < newCount) {
                        mostFrequent = detected;
                    }
                }
            } else if (state == STATE.LISTEN_3) {
                if (beatDetector.isBeat()) {
                    pitchCounts.clear();
                    mostFrequent = null;
                    samplesTaken = 0;
                    changeState(STATE.LISTEN_4);
                }
            } else if (state == STATE.LISTEN_4) {
                if (samplesTaken >= NUM_DETECTION_SAMPLES) {
                    System.out.println(mostFrequent);
                    userPlayedInterval[1] = mostFrequent.equals(getPitch(offsetToFrequency(nextNoteOffset)));
                    changeState(STATE.CHECK);
                    System.out.println(pitchCounts);

                }

                // detected note is the most common pitch in a number of samples
                String detected = getPitch(detectPitch());
                if (detected != null) {
                    samplesTaken++;
                    if (!pitchCounts.containsKey(detected))
                        pitchCounts.put(detected, 0);
                    int newCount = pitchCounts.get(detected) + 1;
                    pitchCounts.put(detected, newCount);

                    if (mostFrequent == null || pitchCounts.get(mostFrequent) < newCount) {
                        mostFrequent = detected;
                    }
                }
            }
        }
    }

    void changeState(STATE newState) {
        System.out.println("new state: " + newState);
        stateChange = true;
        state = newState;
    }

    /**
     * Determines whether the user's input matches a target frequency
     * @param target the expected frequency in Hz
     * @return whether detected pitch matched the target
     */
    boolean listen(float target) {
        // record counts of detected pitches during sampling
        Map<String, Integer> pitchCounts = new HashMap<>();
        String mostFrequent = null;

        for (int i = 0; i < NUM_DETECTION_SAMPLES; i++) {
            // obtain a valid sample
            String detected = getPitch(detectPitch());
            while (detected == null) {
                detected = getPitch(detectPitch());
            }

            // record pitch in map
            if (!pitchCounts.containsKey(detected))
                pitchCounts.put(detected, 0);
            int newCount = pitchCounts.get(detected) + 1;
            pitchCounts.put(detected, newCount);

            if (mostFrequent == null || pitchCounts.get(mostFrequent) < newCount) {
                mostFrequent = detected;
            }
        }
        return mostFrequent.equals(getPitch(target));
    }

    /**
     * Determines the frequency of the input audio from the computer microphone, defaulting to 0.0 Hz
     * if the input volume is below a threshold value
     * @return the frequency of the detected pitch in Hz
     */
    float detectPitch() {
        fft.analyze(spectrum);

        if (volume.analyze() > MIN_VOLUME) {
            // update harmonic product spectrum, and find max bucket
            // start and end search at lowest and highest acceptable frequencies
            int maxBucket = 0;
            for (int i = HPS_START_BIN; i < HPS_END_BIN; i++) {
                // compute product of amplitudes of each bin's harmonics
                float product = 1.0f;
                for (int j = 1; j <= HARMONICS && j * i < spectrum.length; j++) {
                    product *= spectrum[j * i];
                }
                hpSpectrum[i] = product;
                if (hpSpectrum[maxBucket] < hpSpectrum[i]) {
                    maxBucket = i;
                }
            }
            return maxBucket * RESOLUTION;
        }

        return 0.0f;
    }

    /**
     * Randomly generates a next note offset whose interval with the current note  is one of the allowed intervals
     * @return the next note offset in semitones
     */
    int getNextNoteOffset() {
        int interval = INTERVALS[(int) (Math.random() * INTERVALS.length)];
        return currNoteOffset + interval;
    }

    /**
     * Plays a windowed pulse of pure tone at the given frequency
     * @param offset the offset of the note to be played
     */
    void playSound(int offset) {
        float freq = offsetToFrequency(offset);
        this.sound.freq(freq);
        this.sound.amp(0.0f);
        this.sound.play();
        // apply windowing
        for (int i = 0; i < 100; i++) {
            this.sound.amp(0.5f - 0.5f * (float) Math.cos(i * 2 * PI / 100));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.sound.stop();
    }

    /**
     * Converts a frequency into the equivalent musical pitch value
     * @param freq the frequency of the input pitch
     * @return musical scale value of pitch, in the form of the TONE concatenated with the OCTAVE
     * returns null when the frequency is outside a range of values a guitar might produce
     */
    static String getPitch(float freq) {
        if (freq < LOW_FREQ_NOISE_CUTOFF || freq > HIGH_FREQ_NOISE_CUTOFF) return null;

        // determine the number of semitones offset from C4 the offset is, using fundamental freq A4 = 440 Hz
        int offset = (int) Math.round(OCTAVE * Math.log(freq / STANDARD_PITCH) / Math.log(2)) + C_TO_A;
        // determine the octave number (offset in range [0, 11] is octave 4)
        int octave = offset / OCTAVE + STANDARD_PITCH_OCTAVE;
        int normalizedOffset = offset % OCTAVE;
        if (normalizedOffset < 0) {
            normalizedOffset += OCTAVE;
            octave--;
        }
        String pitch = TONES[normalizedOffset];
        return pitch + "" + octave;
    }

    /**
     * Converts a pitch offset into the equivalent frequency value
     * @param offset number of semitones offset from A4 of input pitch
     * @return the frequency of the input pitch in Hz
     */
    static float offsetToFrequency(int offset) {
        return STANDARD_PITCH * (float) Math.pow(2, (double) offset / OCTAVE);
    }

    public static void main(String[] args) {
        PApplet.main("Main");
    }
}