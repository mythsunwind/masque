package de.defector.masque.models;

import android.graphics.Bitmap;

import org.openimaj.image.processing.face.detection.DetectedFace;

import java.util.List;

public class FaceDetectionResult {
    private Bitmap bitmap;
    private List<DetectedFace> faces;

    public FaceDetectionResult(Bitmap bitmap, List<DetectedFace> faces) {
        this.bitmap = bitmap;
        this.faces = faces;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public List<DetectedFace> getFaces() {
        return faces;
    }
}
