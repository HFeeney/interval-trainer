import processing.core.PApplet;
import processing.sound.*;

public class Main extends PApplet {

    FFT fft;
    AudioIn in;
    int bands = 16384;
    float[] spectrum = new float[bands];
    float[] correlation = new float[bands];
    int numHarmonics = 4;
    float lowFreqNoiseCutoff = 50f;
    static final int SAMPLE_RATE = 48000;
    double resolution = (double) SAMPLE_RATE / bands;

    public void settings() {
        size(1800, 360);
    }

    public void setup() {
        background(255);

        // Create an Input stream which is routed into the Amplitude analyzer
        fft = new FFT(this, bands);
        in = new AudioIn(this, 0);

        // start the Audio Input
        in.start();

        // patch the AudioIn
        fft.input(in);
    }

    public void draw() {
        background(255);
        fft.analyze(spectrum);

        stroke(0);

        for(int i = 0; i < bands; i++){
            // The result of the FFT is normalized
            // draw the line for frequency band i scaling it up by 5 to get more amplitude.
            line(i, height / 2f, i, height / 2f - spectrum[i]*height*5 );
        }

        for(int i = 0; i < bands; i++){
            // The result of the FFT is normalized
            // draw the line for frequency band i scaling it up by 5 to get more amplitude.
            line(i, height, i, height - correlation[i]*height*5000000);
        }

        // start search above 50 hz bin
        int startBin = (int) (lowFreqNoiseCutoff / resolution);

        // HPS
        for (int i = startBin; i < spectrum.length; i++) {
            float product = 1.0f;
            for (int j = 0; j < numHarmonics && j * i < spectrum.length; j++) {
                product *= spectrum[j * i];
            }
            correlation[i] = product;
        }

        int maxIndex = 0;
        for (int i = 1; i < correlation.length; i++) {
            if (correlation[maxIndex] < correlation[i]) {
                maxIndex = i;
            }
        }
        System.out.println(maxIndex * resolution);
        stroke(255, 0, 0);
        line(maxIndex, height, maxIndex, height - correlation[maxIndex]*height*5000000);
    }
    public static void main(String[] args) {
        PApplet.main("Main");
    }
}