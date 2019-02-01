package de.defector.masque;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.openimaj.image.processing.face.detection.DetectedFace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.defector.masque.models.FaceDetectionResult;
import de.defector.masque.models.Layer;
import de.defector.masque.widget.LayerView;

public class MainActivity extends AppCompatActivity {

    private static int SELECT_IMAGE = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher_foreground);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            try {
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (imageUri != null) {
                    ProgressBar progressBar = findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.VISIBLE);
                    FaceDetectionTask faceDetectionTask = new FaceDetectionTask();
                    faceDetectionTask.execute(originalBitmap);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Could not import image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LayerView layerView = findViewById(R.id.layerView);

        switch (item.getItemId()) {
            case R.id.loadAction:
                layerView.finishActionMode();
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
                return true;
            case R.id.shareAction:
                try {
                    Bitmap tempBitmap = renderMaskedImage(layerView.getOriginalBackground(), layerView.getLayers());
                    File file = new File(this.getExternalCacheDir(), "temp.png");
                    FileOutputStream fOut = new FileOutputStream(file);
                    tempBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                    file.setReadable(true, false);
                    Intent myShareIntent = new Intent(Intent.ACTION_SEND);
                    myShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    myShareIntent.setType("image/*");
                    myShareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    startActivity(Intent.createChooser(myShareIntent, "Share"));
                } catch (Exception e) {
                    Toast.makeText(this, "Could not share file", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Bitmap renderMaskedImage(Bitmap originalBitmap, List<Layer> layers) {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.raw.face2);
        Canvas canvas = new Canvas(originalBitmap);

        for (Layer layer : layers) {
            if (layer.getType() == Layer.TYPE.BLACK) {
                canvas.drawRect(layer.getBounds().left,
                        layer.getBounds().top ,
                        layer.getBounds().right,
                        layer.getBounds().bottom,
                        paint);
                /*
                Log.d(TAG, String.format("Layer: left %s top %s right %s actionmenu %s",
                        (transformX + layer.getBounds().left)*scaleX,
                        transformY + layer.getBounds().top*scaleY,
                        (transformX + layer.getBounds().right)*scaleX,
                        transformY + layer.getBounds().bottom*scaleY));
                        */
            } else if (layer.getType() == Layer.TYPE.EMOJI) {
                Rect transformedLayerRect = new Rect(
                        layer.getBounds().left,
                        layer.getBounds().top,
                        layer.getBounds().right,
                        layer.getBounds().bottom
                );
                canvas.drawBitmap(bitmap,
                        null,
                        transformedLayerRect,
                        paint);
            }
        }
        return originalBitmap;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        LayerView layerView = findViewById(R.id.layerView);
        MenuItem item = menu.findItem(R.id.shareAction);
        item.setVisible(layerView.hasBackground());
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    try {
                        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        ProgressBar progressBar = findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.VISIBLE);
                        FaceDetectionTask faceDetectionTask = new FaceDetectionTask();
                        faceDetectionTask.execute(originalBitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Could not get image from gallery", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onDetection(FaceDetectionResult result) {
        LayerView layerView = this.findViewById(R.id.layerView);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        List<Layer> layers = new ArrayList<>();
        for (DetectedFace face : result.getFaces()) {
            Layer layer = new Layer(Layer.TYPE.BLACK, FaceDetection.convertToRect(face));
            layers.add(layer);
        }
        layerView.setBackground(result.getBitmap());
        layerView.setLayers(layers);
        progressBar.setVisibility(View.GONE);
        if (result.getFaces().size() == 0) {
            Toast.makeText(this, "No faces detected. Long press to mask faces manually.", Toast.LENGTH_SHORT).show();
        } else if (result.getFaces().size() == 1) {
            Toast.makeText(this, "1 face detected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, String.format("%s faces detected", result.getFaces().size()), Toast.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();
    }

    private class FaceDetectionTask extends AsyncTask<Bitmap, Void, FaceDetectionResult> {

        @Override
        protected FaceDetectionResult doInBackground(Bitmap... bitmaps) {
            return FaceDetection.detect(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(FaceDetectionResult result) {
            onDetection(result);
        }
    }

    @Override
    public void onBackPressed() {
        LayerView layerView = findViewById(R.id.layerView);
        if (layerView.hasBackground()) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
        }
    }
}
