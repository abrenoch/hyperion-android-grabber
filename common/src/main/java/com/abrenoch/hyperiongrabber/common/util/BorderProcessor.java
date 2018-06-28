package com.abrenoch.hyperiongrabber.common.util;

import java.nio.ByteBuffer;

public class BorderProcessor {

    private final int MAX_INCONSISTENT_FRAME_COUNT = 10;
    private final int MAX_UNKNOWN_FRAME_COUNT = 600;
    private final int BORDER_CHANGE_FRAME_COUNT = 50;

    private final int BLACK_THRESHOLD; // unit to detect how black a pixel is [0..255]
    private BorderObject mPreviousBorder;
    private BorderObject mCurrentBorder;
    private int mConsistentFrames = 0;
    private int mInconsistentFrames = 0;

    public BorderProcessor(int blackThreshold) {
        BLACK_THRESHOLD = blackThreshold;
    }

    public BorderObject getCurrentBorder() { return mCurrentBorder; }

    private void checkNewBorder(BorderObject newBorder) {
        if (mPreviousBorder != null && mPreviousBorder.equals(newBorder)) {
            ++mConsistentFrames;
            mInconsistentFrames = 0;
        } else {
            ++mInconsistentFrames;

            if (mInconsistentFrames <= MAX_INCONSISTENT_FRAME_COUNT) {
                return;
            }

            mPreviousBorder = newBorder;
            mConsistentFrames = 0;
        }

        if (mCurrentBorder != null && mCurrentBorder.equals(newBorder)) {
            mInconsistentFrames = 0;
            return;
        }

        if (!newBorder.isKnown()) {
            if (mConsistentFrames == MAX_UNKNOWN_FRAME_COUNT) {
                mCurrentBorder = newBorder;
            }
        } else {
            if (mCurrentBorder == null || !mCurrentBorder.isKnown() ||
                    mConsistentFrames == BORDER_CHANGE_FRAME_COUNT) {
                        mCurrentBorder = newBorder;
            }
        }
    }

    public void parseBorder(ByteBuffer buffer, int width, int height, int rowStride,
                            int pixelStride) {
        checkNewBorder( findBorder(buffer, width, height, rowStride, pixelStride) );
    }

    private BorderObject findBorder(ByteBuffer buffer, int width, int height, int rowStride,
                                    int pixelStride) {

        int width33percent = width / 3;
        int width66percent = width33percent * 2;
        int height33percent = height / 3;
        int height66percent = height33percent * 2;
        int yCenter = height / 2;
        int xCenter = width / 2;
        int firstNonBlackYPixelIndex = -1;
        int firstNonBlackXPixelIndex = -1;

        int c1R, c1G, c1B;
        int c2R, c2G, c2B;
        int c3R, c3G, c3B;
        int pos33p, pos66p;

        buffer.position(0).mark();

        for (int x = 0, posYCLp; x < width33percent; x++) {

            // RGB values at 33% height - to left of image
            pos33p = (height33percent * rowStride) + (x * pixelStride);
            c1R = buffer.get(pos33p) & 0xff;
            c1G = buffer.get(pos33p + 1) & 0xff;
            c1B = buffer.get(pos33p + 2) & 0xff;

            // RGB values at 66% height - to left of image
            pos66p = (height66percent * rowStride) + (x * pixelStride);
            c2R = buffer.get(pos66p) & 0xff;
            c2G = buffer.get(pos66p + 1) & 0xff;
            c2B = buffer.get(pos66p + 2) & 0xff;

            // RGB values at center Y - to right of image
            posYCLp = (yCenter * rowStride) + ((width - x - 1) * pixelStride);
            c3R = buffer.get(posYCLp) & 0xff;
            c3G = buffer.get(posYCLp + 1) & 0xff;
            c3B = buffer.get(posYCLp + 2) & 0xff;

            // check if any of our RGB values DO NOT evaluate as black
            if (!isBlack(c1R,c1G,c1B) || !isBlack(c2R,c2G,c2B) || !isBlack(c3R,c3G,c3B)) {
                firstNonBlackXPixelIndex = x;
                break;
            }
        }

        buffer.reset();
        for (int y = 0, posBCLp; y < height33percent; y++) {

            // RGB values at 33% width - top of image
            pos33p = (width33percent * pixelStride) + (y * rowStride);
            c1R = buffer.get(pos33p) & 0xff;
            c1G = buffer.get(pos33p + 1) & 0xff;
            c1B = buffer.get(pos33p + 2) & 0xff;

            // RGB values at 66% width - top of image
            pos66p = (width66percent * pixelStride) + (y * rowStride);
            c2R = buffer.get(pos66p) & 0xff;
            c2G = buffer.get(pos66p + 1) & 0xff;
            c2B = buffer.get(pos66p + 2) & 0xff;

            // RGB values at center X - bottom of image
            posBCLp = (xCenter * pixelStride) + ((height - y - 1) * rowStride);
            c3R = buffer.get(posBCLp) & 0xff;
            c3G = buffer.get(posBCLp + 1) & 0xff;
            c3B = buffer.get(posBCLp + 2) & 0xff;

            // check if any of our RGB values DO NOT evaluate as black
            if (!isBlack(c1R,c1G,c1B) || !isBlack(c2R,c2G,c2B) || !isBlack(c3R,c3G,c3B)) {
                firstNonBlackYPixelIndex = y;
                break;
            }
        }

        return new BorderObject(firstNonBlackXPixelIndex, firstNonBlackYPixelIndex);
    }

    private boolean isBlack(int red, int green, int blue) {
        return red < BLACK_THRESHOLD && green < BLACK_THRESHOLD && blue < BLACK_THRESHOLD;
    }

    public static class BorderObject {
        private boolean isKnown;
        private int horizontalBorderIndex;
        private int verticalBorderIndex;

        BorderObject(int horizontalBorderIndex, int verticalBorderIndex) {
            this.horizontalBorderIndex = horizontalBorderIndex;
            this.verticalBorderIndex = verticalBorderIndex;
            this.isKnown = this.horizontalBorderIndex != -1 && this.verticalBorderIndex != -1;
        }

        public boolean isKnown() { return isKnown; }
        public int getHorizontalBorderIndex() { return horizontalBorderIndex; }
        public int getVerticalBorderIndex() { return verticalBorderIndex; }

        public String toString() {
            if (this.isKnown) {
                return "Border information: firstX:" + String.valueOf(this.horizontalBorderIndex) +
                        " firstY: " + String.valueOf(this.verticalBorderIndex);
            }
            return "Border information: unknown";
        }

        @Override
        public int hashCode() {
            if (!this.isKnown) return 0;
            String hashString = String.valueOf(this.horizontalBorderIndex) +
                    String.valueOf(this.horizontalBorderIndex);
            return hashString.hashCode();
        }

        @Override
        public boolean equals(Object border) {
            return this.getClass() == border.getClass() && this.hashCode() == border.hashCode();
        }
    }
}
