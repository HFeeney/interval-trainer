import processing.core.PApplet;
import processing.sound.*;

public class Main extends PApplet {

    FFT fft; // fast fourier transform object
    AudioIn in; // audio input source
    Amplitude volume;
    static final int BANDS = 16384; // the number of frequency bands to decompose signal into
    float[] spectrum = new float[BANDS]; // bins for each frequency band recording amplitude
    static final int SAMPLE_RATE = 44100; // standard microphone sample rate in Hz
    static final int MAX_FREQUENCY = SAMPLE_RATE / 2; // most precise frequency detectable is half sample rate
    static final double RESOLUTION = (double) MAX_FREQUENCY / BANDS; // width of each frequency band in Hz

    static final int HARMONICS = 2; // number of harmonics to consider in product
    float[] hpSpectrum = new float[BANDS]; // to store product of harmonics of each frequency band
    static final float LOW_FREQ_NOISE_CUTOFF = 50f; // below this value, frequencies considered noise
    static final float HIGH_FREQ_NOISE_CUTOFF = 1200f; // above this value, frequencies considered noise
    static final int HPS_START_BIN = (int)(LOW_FREQ_NOISE_CUTOFF / RESOLUTION);
    static final int HPS_END_BIN = (int)(HIGH_FREQ_NOISE_CUTOFF / RESOLUTION);
    static final float MIN_VOLUME = 0.01f; // lowest volume for which pitch will be reported

    public void settings() {
        size(1800, 360);
        System.out.println(RESOLUTION);
    }

    public void setup() {
        background(255);

        /* START CODE FROM PROCESSING WEBSITE */

        // Create an Input stream which is routed into the Amplitude analyzer
        fft = new FFT(this, BANDS);
        in = new AudioIn(this, 0);
        volume = new Amplitude(this);

        // start the Audio Input
        in.start();

        // patch the AudioIn
        fft.input(in);
        volume.input(in);

        /* END CODE FROM PROCESSING WEBSITE */
    }

    public void draw() {
        background(255);
        fft.analyze(spectrum);

        stroke(0);

        // fft output
        for(int i = 0; i < BANDS; i++){
            // The result of the FFT is normalized
            // draw the line for frequency band i scaling it up by 5 to get more amplitude.
            line(i, height, i, height - spectrum[i]*height*5 );
        }


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

            System.out.println(maxBucket * RESOLUTION);
            stroke(100, 255, 250);
            line(maxBucket, height, maxBucket, 0);
        }
    }
    public static void main(String[] args) {
        PApplet.main("Main");
    }
}