import processing.core.PApplet;
import processing.sound.*;

public class Main extends PApplet {

    FFT fft; // fast fourier transform object
    AudioIn in; // audio input source
    int bands = 16384; // the number of frequency bands to decompose signal into
    float[] spectrum = new float[bands]; // bins for each frequency band recording amplitude
    static final int SAMPLE_RATE = 44100; // standard microphone sample rate in Hz
    static final int MAX_FREQUENCY = SAMPLE_RATE / 2; // most precise frequency detectable is half sample rate
    double resolution = (double) MAX_FREQUENCY / bands; // width of each frequency band in Hz

    static final int HARMONICS = 3; // number of harmonics to consider in product
    float[] hpSpectrum = new float[bands]; // to store product of harmonics of each frequency band
    static final float LOW_FREQ_NOISE_CUTOFF = 50f; // below this value, frequencies considered noise

    public void settings() {
        size(1800, 360);
        System.out.println(resolution);
    }

    public void setup() {
        background(255);

        /* START CODE FROM PROCESSING WEBSITE */

        // Create an Input stream which is routed into the Amplitude analyzer
        fft = new FFT(this, bands);
        in = new AudioIn(this, 0);

        // start the Audio Input
        in.start();

        // patch the AudioIn
        fft.input(in);

        /* END CODE FROM PROCESSING WEBSITE */
    }

    public void draw() {
        background(255);
        fft.analyze(spectrum);

        // fft output
        for(int i = 0; i < bands; i++){
            // The result of the FFT is normalized
            // draw the line for frequency band i scaling it up by 5 to get more amplitude.
            line(i, height, i, height - spectrum[i]*height*5 );
        }

        // start search above 50 hz bin
        int startBin = (int) (LOW_FREQ_NOISE_CUTOFF / resolution);

        // update harmonic product spectrum
        for (int i = startBin; i < spectrum.length; i++) {
            // compute product of amplitudes of each bin's harmonics
            float product = 1.0f;
            for (int j = 1; j <= HARMONICS && j * i < spectrum.length; j++) {
                product *= spectrum[j * i];
            }
            hpSpectrum[i] = product;
        }

        // find maximum value
        int maxIndex = 0;
        for (int i = 1; i < hpSpectrum.length; i++) {
            if (hpSpectrum[maxIndex] < hpSpectrum[i]) {
                maxIndex = i;
            }
        }
        System.out.println(maxIndex * resolution);
        stroke(255, 0, 0);
        line(maxIndex, height, maxIndex, height - hpSpectrum[maxIndex]*height*5000);
    }
    public static void main(String[] args) {
        PApplet.main("Main");
    }
}