import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import javax.swing.*;


public class DWTCompression {

    JFrame frame;
    BufferedImage originalImg;
    int height = 512;
    int width = 512;

    float [][] encodedArrR = new float[height][width];
    float [][] encodedArrG = new float[height][width];
    float [][] encodedArrB = new float[height][width];

    float [][] decodedR = new float[height][width];
    float [][] decodedG = new float[height][width];
    float [][] decodedB = new float[height][width];

    String imgPath;
    int decodeLevel;

    private void readImageRGB(int width, int height, String imgPath, BufferedImage OriginalImg) {
        try {
            int frameLength = width * height * 3;
            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            byte[] bytes = new byte[frameLength];
            raf.read(bytes);
            int idx = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = Byte.toUnsignedInt(bytes[idx]);
                    int g = Byte.toUnsignedInt(bytes[idx+height*width]);
                    int b = Byte.toUnsignedInt(bytes[idx+height*width*2]);
                    encodedArrR[y][x] = r;
                    encodedArrG[y][x] = g;
                    encodedArrB[y][x] = b;

//                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//                    originalImg.setRGB(x,y,pix);
                    idx++;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void DWTEncodeRow(float[] arr, int len) {
        float lowPass, highPass;
        int half = len / 2;
        float[] tmp = Arrays.copyOf(arr, arr.length);
        for (int i = 0; i < len; i+=2) {
            lowPass = (tmp[i] + tmp[i+1]) / 2;
            highPass = (tmp[i] - tmp[i+1]) / 2;
            arr[i/2] = lowPass;
            arr[i/2 + half] = highPass;
        }
    }

    private void DWTEncodeCol(float[][] arr, int idx, int len) {
        float lowPass, highPass;
        int half = len / 2;
        float[] tmp = new float[height];
        for (int i = 0; i < height; i++) {
            tmp[i] = arr[i][idx];
        }
        for (int i = 0; i < len; i+=2) {
            lowPass = (tmp[i] + tmp[i+1]) / 2;
            highPass = (tmp[i] - tmp[i+1]) / 2;
            arr[i/2][idx] = lowPass;
            arr[i/2 + half][idx] = highPass;
        }
    }

    private void DWTEncode() {
        int curWidth = width, curHeight = height;
        for (int level = 0; level < 9; level++) {
            for (int y = 0; y < height; y++) {
                DWTEncodeRow(encodedArrR[y], curWidth);
                DWTEncodeRow(encodedArrG[y], curWidth);
                DWTEncodeRow(encodedArrB[y], curWidth);
            }
            curWidth /= 2;
        }

        for (int level = 0; level < 9; level ++) {
            for (int x = 0; x < width; x++) {
                DWTEncodeCol(encodedArrR, x, curHeight);
                DWTEncodeCol(encodedArrG, x, curHeight);
                DWTEncodeCol(encodedArrB, x, curHeight);
            }
            curHeight /= 2;
        }
    }

    private void DWTDecodeRow(float[] eArr, float[] dArr, int len) {
        if (len < 2) {
            return;
        }
        float[] tmp = Arrays.copyOf(dArr, dArr.length);
        float coefficient;
        int half = len / 2;
        for (int i = 0; i < len; i+=2) {
            coefficient = eArr[i/2+half];
            dArr[i] = tmp[i/2] + coefficient;
            dArr[i+1] = tmp[i/2] - coefficient;
        }
    }

    private void DWTDecodeCol(float[][] eArr, float[][] dArr, int idx, int len) {
        if (len < 2) {
            return;
        }
        float[] tmp = new float[len];
        for (int i = 0; i < len; i++) {
            tmp[i] = dArr[i][idx];
        }
        float coefficient;
        int half = len / 2;
        for (int i = 0; i < len; i+=2) {
            coefficient = eArr[i/2+half][idx];
            dArr[i][idx] = tmp[i/2] + coefficient;
            dArr[i+1][idx] = tmp[i/2] - coefficient;
        }
    }

    private void DWTDecode(int level) {
        decodedR[0] = Arrays.copyOf(encodedArrR[0], encodedArrR.length);
        decodedG[0] = Arrays.copyOf(encodedArrG[0], encodedArrG.length);
        decodedB[0] = Arrays.copyOf(encodedArrB[0], encodedArrB.length);
//        decodedR[0][0] = encodedArrR[0][0];
//        decodedG[0][0] = encodedArrG[0][0];
//        decodedB[0][0] = encodedArrB[0][0];
        int curWidth = 2, curHeight = 2;

        // decode
        for (int l = 0; l < level; l++) {
            for (int x = 0; x < width; x++) {
                DWTDecodeCol(encodedArrR, decodedR, x, curHeight);
                DWTDecodeCol(encodedArrG, decodedG, x, curHeight);
                DWTDecodeCol(encodedArrB, decodedB, x, curHeight);
            }
            curHeight *= 2;
        }
        for (int l = 0; l < level; l++) {
            for (int y = 0; y < height; y++) {
                DWTDecodeRow(decodedR[y], decodedR[y], curWidth);
                DWTDecodeRow(decodedG[y], decodedG[y], curWidth);
                DWTDecodeRow(decodedB[y], decodedB[y], curWidth);
            }
            curWidth *= 2;
        }

        // zero out
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (i < curHeight/2 && j < curWidth/2)
                    continue;
                decodedR[i][j] = 0;
                decodedG[i][j] = 0;
                decodedB[i][j] = 0;
            }
        }

        // refill
        for (int l = level; l < 9; l++) {
            for (int x = 0; x < width; x++) {
                DWTDecodeCol(decodedR, decodedR, x, curHeight);
                DWTDecodeCol(decodedG, decodedG, x, curHeight);
                DWTDecodeCol(decodedB, decodedB, x, curHeight);
            }
            curHeight *= 2;
        }
        for (int l = level; l < 9; l++) {
            for (int y = 0; y < height; y++) {
                DWTDecodeRow(decodedR[y], decodedR[y], curWidth);
                DWTDecodeRow(decodedG[y], decodedG[y], curWidth);
                DWTDecodeRow(decodedB[y], decodedB[y], curWidth);
            }
            curWidth *= 2;
        }
    }

    private void loadImg(float[][] arrR, float[][] arrG, float[][] arrB, BufferedImage img) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = Math.round(arrR[y][x]);
                int g = Math.round(arrG[y][x]);
                int b = Math.round(arrB[y][x]);
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));
                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//                int pix = 0xff000000 | (((int)decodedR[y][x] & 0xff) << 16) | (((int)decodedG[y][x] & 0xff) << 8) | ((int)decodedB[y][x] & 0xff);
                img.setRGB(x,y,pix);
            }
        }
    }

    public void showImage(String[] args) {
        imgPath = args[0];
        decodeLevel = Integer.parseInt(args[1]);
        originalImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, imgPath, originalImg);
        DWTEncode();

//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int pix = 0xff000000 | (((int)encodedArrR[y][x] & 0xff) << 16) | (((int)encodedArrG[y][x] & 0xff) << 8) | ((int)encodedArrB[y][x] & 0xff);
//                originalImg.setRGB(x,y,pix);
//            }
//        }

        /////////////////////////////////////////
        frame = new JFrame("CSCI576_HW3");
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 1;
        KeyListener ESCKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        };
        frame.addKeyListener(ESCKey);
        JLabel lbIm1;
        if(decodeLevel >= 0 && decodeLevel <= 9) {
            DWTDecode(decodeLevel);
            loadImg(decodedR, decodedG, decodedB, originalImg);
            lbIm1 = new JLabel(new ImageIcon(originalImg));
            frame.getContentPane().add(lbIm1, c);
            frame.pack();
            frame.setVisible(true);
        }
        else if(decodeLevel == -1) {
            try {
                for (int i = 0; i < 10; i++) {
                    DWTDecode(i);
                    loadImg(decodedR, decodedG, decodedB, originalImg);
                    lbIm1 = new JLabel(new ImageIcon(originalImg));
                    JFrame curFrame = new JFrame("Low Pass Level " + i);
                    curFrame.getContentPane().setLayout(gLayout);
                    curFrame.getContentPane().add(lbIm1, c);
                    curFrame.pack();
                    curFrame.setVisible(true);
                    Thread.sleep(800);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Wrong low pass level\n");
        }
    }


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong number of sys args\n");
            return;
        }
        DWTCompression dwt = new DWTCompression();
        dwt.showImage(args);
    }
}
