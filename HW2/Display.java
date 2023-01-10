import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import javax.swing.*;

public class Display {
    JFrame frame;
    BufferedImage img;
    int width = 640, height = 480;
    String foregroundPath, backgroundPath;
    String[] foregroundFileNames, backgroundFileNames;

    int[] bytesPreImg;
    int[][] preImages = new int[6][];

    float foregroundBackgroundDiff = 2f;

    int mode;


    private float[] RGBToHSV(int r, int g, int b) {
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        hsv[0] *= 360;
        hsv[1] *= 100;
        hsv[2] *= 100;
        return hsv;
    }
    private static boolean isGreen(float[] hsv) {
        if (hsv[0] < 65 || hsv[0] > 175 || hsv[1] < 25 || hsv[2] < 25)
            return false;
        return true;
    }

//    private boolean isBoundary(int h, int w) {
//        int[][] dir = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
//        int x, y;
//        for (int[]pair : dir) {
//            x = w + pair[0];
//            y = h + pair[1];
//            if (x < 0 || y < 0 || y == height || x == width) continue;
//
//
//        }
//        return false;
//    }

    private int[] readImgToUnsignedInt(String imgPath) {
        try {
            int frameLength = width*height*3;

            File rgbFile = new File(imgPath);
            RandomAccessFile rafForeground = new RandomAccessFile(rgbFile, "r");
            rafForeground.seek(0);
            byte[] bytes = new byte[frameLength];
            rafForeground.read(bytes);
            int[] UnsignedInt = new int[frameLength];
            for (int i=0; i < frameLength; i++) {
                UnsignedInt[i] = Byte.toUnsignedInt(bytes[i]);
            }
            return UnsignedInt;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private BufferedImage readAImg(String imgPath) {
        int[] bytesImg = readImgToUnsignedInt(imgPath);
        if (bytesImg == null)
            return null;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int idx = 0;
        int r, g, b, pix;
        for(int y = 0; y < height; y++)
        {
            for(int x = 0; x < width; x++) {
                r = bytesImg[idx];
                g = bytesImg[idx+height*width];
                b = bytesImg[idx+height*width*2];
                pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                bi.setRGB(x,y,pix);
                idx++;
            }
        }
        return bi;
    }


//    private BufferedImage readImageWithoutGreenScreen(String foregroundPath, String backgroundPath, int[] bytePreImg) {
//        int[] bytesForeground = readImgToUnsignedInt(foregroundPath);
//        int[] bytesBackground = readImgToUnsignedInt(backgroundPath);
//        if (bytesForeground == null || bytesBackground == null || bytePreImg == null)
//            return null;
//
//        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//
//        int idx = 0;
//        int r, g, b, rr, gg, bb, pix;
//        float[] preHsv, curHsv;
//        for(int y = 0; y < height; y++)
//        {
//            for(int x = 0; x < width; x++)
//            {
//                r = bytesForeground[idx];
//                g = bytesForeground[idx+height*width];
//                b = bytesForeground[idx+height*width*2];
//                preHsv = RGBToHSV(r, g, b);
//                rr = bytePreImg[idx];
//                gg = bytePreImg[idx+height*width];
//                bb = bytePreImg[idx+height*width*2];
//                curHsv = RGBToHSV(rr, gg, bb);
//                bytePreImg[idx] = r;
//                bytePreImg[idx+height*width] = g;
//                bytePreImg[idx+height*width*2] = b;
//                if (Math.abs(preHsv[0] - curHsv[0]) < 2) {
//                    r = bytesBackground[idx];
//                    g = bytesBackground[idx+height*width];
//                    b = bytesBackground[idx+height*width*2];
////                    r = 0;
////                    g = 0;
////                    b = 0;
//                }
//                pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//                bi.setRGB(x,y,pix);
//                idx++;
//            }
//        }
//        return bi;
//    }

    private BufferedImage readImageWithGreenScreen(String foregroundPath, String backgroundPath) {
        int[] bytesForeground = readImgToUnsignedInt(foregroundPath);
        int[] bytesBackground = readImgToUnsignedInt(backgroundPath);

        if (bytesForeground == null || bytesBackground == null)
            return null;

        float[] hsv;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int idx = 0;
        int r, g, b, pix;
        for(int y = 0; y < height; y++)
        {
            for(int x = 0; x < width; x++)
            {
                r = bytesForeground[idx];
                g = bytesForeground[idx+height*width];
                b = bytesForeground[idx+height*width*2];

//                    hsv = RGBtoHSV(r, g, b);
                hsv = RGBToHSV(r, g, b);

//                    System.out.println(Arrays.toString(hsv));
                if (isGreen(hsv)) {
                    r = bytesBackground[idx];
                    g = bytesBackground[idx+height*width];
                    b = bytesBackground[idx+height*width*2];
                    pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    bi.setRGB(x,y,pix);
                }
                else {
                    pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    bi.setRGB(x,y,pix);
//                        if (x - 1 >= 0) {
//                            r = Byte.toUnsignedInt(bytesForeground[idx - 1]);
//                            g = Byte.toUnsignedInt(bytesForeground[idx + height * width - 1]);
//                            b = Byte.toUnsignedInt(bytesForeground[idx + height * width * 2 - 1]);
//                            Color.RGBtoHSB(r, g, b, hsv);
//                            hsv[0] *= 360;
//                            hsv[1] *= 100;
//                            hsv[2] *= 100;
//                            if (isGreen(hsv)) {
//                                r = Byte.toUnsignedInt(bytesBackground[idx-1]);
//                                g = Byte.toUnsignedInt(bytesBackground[idx+height*width-1]);
//                                b = Byte.toUnsignedInt(bytesBackground[idx+height*width*2-1]);
//                                pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//                                bi.setRGB(x - 1, y, pix);
//                            }
//                        }
//                        if (y - 1 >= 0) {
//                            r = Byte.toUnsignedInt(bytesForeground[idx - width]);
//                            g = Byte.toUnsignedInt(bytesForeground[idx + height * width - width]);
//                            b = Byte.toUnsignedInt(bytesForeground[idx + height * width * 2 - width]);
//                            Color.RGBtoHSB(r, g, b, hsv);
//                            hsv[0] *= 360;
//                            hsv[1] *= 100;
//                            hsv[2] *= 100;
//                            if (isGreen(hsv)) {
//                                r = Byte.toUnsignedInt(bytesBackground[idx - width]);
//                                g = Byte.toUnsignedInt(bytesBackground[idx + height * width - width]);
//                                b = Byte.toUnsignedInt(bytesBackground[idx + height * width * 2 - width]);
//                                pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
//                                bi.setRGB(x, y - 1, pix);
//                            }
//                        }
                }

                idx++;
            }
        }
        return bi;
    }

    private String[] readFileNames(String dir) {
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();
        int length = Objects.requireNonNull(listOfFiles).length;
        String[] fileNames = new String[length];
        String separator = File.separator;
        if (!dir.endsWith(separator))
            dir += separator;

        for (int i = 0; i < length; i++) {
            if (listOfFiles[i].isFile()) {
//                System.out.println(dir + listOfFiles[i].getName());
                fileNames[i] = dir + listOfFiles[i].getName();
            }
        }
        return fileNames;
    }

    private BufferedImage readImageWithoutGreenScreen(String foregroundPath, String backgroundPath) {
        int[] bytesForeground = readImgToUnsignedInt(foregroundPath);
        int[] bytesBackground = readImgToUnsignedInt(backgroundPath);
        if (bytesForeground == null || bytesBackground == null || preImages == null)
            return null;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int idx = 0;
        int r, g, b, rr, gg, bb, pix;
        float[] preHsv, curHsv;
        int[][] arr = new int[preImages.length][];
//        int frameChange, changeThreshold = (int) (preImages.length * 0.75);
        boolean isStatic;
        for(int y = 0; y < height; y++)
        {
            for(int x = 0; x < width; x++)
            {
                isStatic = true;
//                frameChange = 0;
                r = bytesForeground[idx];
                g = bytesForeground[idx+height*width];
                b = bytesForeground[idx+height*width*2];
                curHsv = RGBToHSV(r, g, b);
                for (int j = 1; j < preImages.length; j++) {
                    rr = preImages[j-1][idx];
                    gg = preImages[j-1][idx + height * width];
                    bb = preImages[j-1][idx + height * width * 2];
                    preHsv = RGBToHSV(rr, gg, bb);
//                    System.out.println(Arrays.toString(curHsv));
//                    System.out.println(Arrays.toString(preHsv));
                    arr[j-1] = preImages[j];
//                    preImages[j-1] = preImages[j];
                    // frame changed
                    if (Math.abs(preHsv[0] - curHsv[0]) > 1.5f && Math.abs(preHsv[1] - curHsv[1]) > 3f && Math.abs(preHsv[2] - curHsv[2]) > 5f) {
                        isStatic = false;
//                        frameChange +=1;
                    }
                }
                arr[arr.length-1] = bytesForeground;
//                preImages[preImages.length-1] = bytesForeground;
                if (isStatic /*frameChange < changeThreshold*/) {
                    r = bytesBackground[idx];
                    g = bytesBackground[idx + height * width];
                    b = bytesBackground[idx + height * width * 2];
                }
                pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                bi.setRGB(x,y,pix);
                idx++;
            }
        }
        preImages = arr;
        return bi;
    }

    public void showImg(String[] args) {
        foregroundPath = args[0];
        backgroundPath = args[1];
        mode = Integer.parseInt(args[2]);
        frame = new JFrame("CSCI576_HW2");
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        foregroundFileNames = readFileNames(foregroundPath);
        backgroundFileNames = readFileNames(backgroundPath);
        Arrays.sort(foregroundFileNames);
        Arrays.sort(backgroundFileNames);

        BufferedImage[] storedImages = new BufferedImage[foregroundFileNames.length];

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
        System.out.println("/////////////////////////////////////////////");
        System.out.println("Preprocessing images, may take 20 seconds");
        System.out.println("/////////////////////////////////////////////");
        if (mode == 1) {
//            img = readImageWithGreenScreen(foregroundFileNames[0], backgroundFileNames[0]);
//            if (img == null) {
//                System.out.println("Error reading img");
//                return;
//            }
//            JLabel lbl = new JLabel(new ImageIcon(img));
//            frame.getContentPane().add(lbl, c);
//            frame.pack();
//            frame.setVisible(true);
//            for (int i=1; i < foregroundFileNames.length; i++) {
//                img = readImageWithGreenScreen(foregroundFileNames[i], backgroundFileNames[i]);
//                assert img != null;
//                lbl.setIcon(new ImageIcon(img));
//            }
            for (int i=0; i<foregroundFileNames.length; i++) {
                storedImages[i] = readImageWithGreenScreen(foregroundFileNames[i], backgroundFileNames[i]);
            }
            JLabel lbl = new JLabel(new ImageIcon(storedImages[0]));
            frame.getContentPane().add(lbl, c);
            frame.pack();
            frame.setVisible(true);
            try {
                for (int i=1; i<foregroundFileNames.length; i++) {
                    Thread.sleep(40);
                    lbl.setIcon(new ImageIcon(storedImages[i]));
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            for (int i=0; i<preImages.length; i++) {
                preImages[i] = readImgToUnsignedInt(foregroundFileNames[i]);
            }
////            System.out.println(Arrays.toString(preImages[preImages.length - 1]));
//            img = readImageWithoutGreenScreen(foregroundFileNames[preImages.length], backgroundFileNames[preImages.length]);
//            JLabel lbl = new JLabel(new ImageIcon(img));
//            frame.getContentPane().add(lbl, c);
//            frame.pack();
//            frame.setVisible(true);
//            for (int i=preImages.length+1; i < foregroundFileNames.length; i++) {
//                img = readImageWithoutGreenScreen(foregroundFileNames[i], backgroundFileNames[i]);
//                assert img != null;
//                lbl.setIcon(new ImageIcon(img));
//            }
            for (int i=preImages.length; i<foregroundFileNames.length; i++) {
                storedImages[i] = readImageWithoutGreenScreen(foregroundFileNames[i], backgroundFileNames[i]);
            }
            JLabel lbl = new JLabel(new ImageIcon(storedImages[preImages.length]));
            frame.getContentPane().add(lbl, c);
            frame.pack();
            frame.setVisible(true);
            try {
                for (int i=preImages.length+1; i<foregroundFileNames.length; i++) {
                    Thread.sleep(40);
                    lbl.setIcon(new ImageIcon(storedImages[i]));
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }




    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Wrong number of sys args\n");
            return;
        }
        Display display = new Display();
        display.showImg(args);
    }
}
