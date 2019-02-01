package de.defector.masque;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;

import java.util.List;

import de.defector.masque.models.FaceDetectionResult;

public class FaceDetection {

    private static final int SCALE_BITMAP_TO_MAX_SIZE = 1080;
    private static final int MINIMUM_FACE_SIZE = 100;

    public static FaceDetectionResult detect(Bitmap original) {
        Bitmap bitmap = scaleBitmap(original, SCALE_BITMAP_TO_MAX_SIZE);
        MBFImage image = createMBFImage(bitmap, false);
        FaceDetector<DetectedFace, FImage> fd = new HaarCascadeDetector(MINIMUM_FACE_SIZE);
        List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(image));
        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(bitmap, 0, 0, null);
        return new FaceDetectionResult(tempBitmap, faces);
    }

    public static Rect convertToRect(DetectedFace face) {
        Rectangle rect = face.getBounds();
        return new Rect((int) rect.x,
                (int) rect.y,
                (int) (rect.x + rect.width),
                (int) (rect.y + rect.height));
    }

    private static MBFImage createMBFImage(Bitmap image, boolean alpha) {
        final int[] data = new int[image.getHeight() * image.getWidth()];
        image.getPixels(data, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        return new MBFImage(data, image.getWidth(), image.getHeight(), alpha);
    }

    private static Bitmap scaleBitmap(Bitmap original, float maxSize) {
        float ratio = Math.min(
                (float) maxSize / original.getWidth(),
                (float) maxSize / original.getHeight());
        int width = Math.round((float) ratio * original.getWidth());
        int height = Math.round((float) ratio * original.getHeight());

        return Bitmap.createScaledBitmap(original, width, height, false);
    }

}
