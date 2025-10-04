package io.github.crealivity.dptvcursor.gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.crealivity.dptvcursor.assets.PointerAssets;
import io.github.crealivity.dptvcursor.helper.Helper;
import io.github.crealivity.dptvcursor.R;

public class CustomCursorActivity extends AppCompatActivity {

    private static final String CUSTOM_FILE = "custom_pointer.png";
    private static final int CURSOR_SIZE = 206;
    //private TextView tvInfo;
    private ImageView ivPreview;
    private Button btPick, btReset;

    private ActivityResultLauncher<String[]> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_cursor);
        Helper.limitWidthIfTvPercent(this, R.id.main, 0.6f);

        ivPreview = findViewById(R.id.iv_preview);
        //tvInfo    = findViewById(R.id.tv_info);
        btPick    = findViewById(R.id.bt_pick);
        btReset   = findViewById(R.id.bt_reset);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handlePickedUri
        );

        btPick.setOnClickListener(v -> {
            // for now i wanna allow all images in general
            pickImageLauncher.launch(new String[]{"image/*"});
        });

        btReset.setOnClickListener(v -> {
            File f = new File(getFilesDir(), CUSTOM_FILE);
            if (f.exists()) f.delete();
            Helper.setMouseIconPref(this, PointerAssets.STYLE_DEFAULT);
            sendBroadcast(new Intent(Helper.ACTION_REFRESH_CURSOR));
            Toast.makeText(this, "Reverted to default cursor.", Toast.LENGTH_SHORT).show();
            updateUi();
        });

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void handlePickedUri(@androidx.annotation.Nullable Uri uri) {
        if (uri == null) { toast("No image selected."); return; }

        int[] size = readImageSize(uri);
        if (size == null || size[0] <= 0 || size[1] <= 0) {
            toast("Invalid image."); return;
        }
        if (size[0] < CURSOR_SIZE || size[1] < CURSOR_SIZE) {
            toast("Image is too small. Minimum 206×206."); return;
        }

        Bitmap bmp = decodeSampledBitmap(uri, size[0], size[1], CURSOR_SIZE, CURSOR_SIZE);
        if (bmp == null) { toast("Couldn't decode image."); return; }

        Bitmap out = (bmp.getWidth() == CURSOR_SIZE && bmp.getHeight() == CURSOR_SIZE)
                ? bmp
                : Bitmap.createScaledBitmap(bmp, CURSOR_SIZE, CURSOR_SIZE, true);

        if (!saveAsPng(out, CUSTOM_FILE)) {
            toast("Failed to save custom cursor."); return;
        }

        Helper.setMouseIconPref(this, PointerAssets.STYLE_CUSTOM);
        sendBroadcast(new Intent(Helper.ACTION_REFRESH_CURSOR));

        toast("Custom cursor updated.");
        updateUi();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private @androidx.annotation.Nullable int[] readImageSize(@androidx.annotation.NonNull Uri uri) {

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                android.graphics.ImageDecoder.Source src =
                        android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
                final int[] out = new int[2];
                android.graphics.ImageDecoder.decodeDrawable(src, (decoder, info, s) -> {
                    android.util.Size sz = info.getSize();
                    out[0] = sz.getWidth();
                    out[1] = sz.getHeight();
                    decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                    decoder.setTargetSampleSize(16);
                });
                return (out[0] > 0 && out[1] > 0) ? out : null;
            } catch (Exception ignored) { }
        }

        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            return new int[]{ o.outWidth, o.outHeight };
        } catch (Exception e) {
            return null;
        }
    }

    private @androidx.annotation.Nullable Bitmap decodeSampledBitmap(
            @androidx.annotation.NonNull Uri uri,
            int srcW, int srcH, int reqW, int reqH
    ) {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                android.graphics.ImageDecoder.Source src =
                        android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
                return android.graphics.ImageDecoder.decodeBitmap(src, (decoder, info, s) -> {

                    decoder.setTargetSize(reqW, reqH);
                    decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } catch (Exception ignored) { }
        }

        int sample = computeInSampleSize(srcW, srcH, reqW, reqH);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = sample;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private int computeInSampleSize(int srcW, int srcH, int reqW, int reqH) {
        int inSampleSize = 1;
        if (srcH > reqH || srcW > reqW) {
            final int halfH = srcH / 2;
            final int halfW = srcW / 2;
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private boolean saveAsPng(@androidx.annotation.NonNull Bitmap bmp, @androidx.annotation.NonNull String fileName) {
        try (OutputStream out = openFileOutput(fileName, Context.MODE_PRIVATE)) {
            return bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUi() {
        File f = new File(getFilesDir(), CUSTOM_FILE);
        if (f.exists()) {
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b != null) {
                ivPreview.setImageBitmap(b);
                /**tvInfo.setText(String.format(Locale.US,
                        "%s • %dx%d • %d bytes",
                        CUSTOM_FILE, b.getWidth(), b.getHeight(), f.length()));**/
                btReset.setEnabled(true);
            } else {
                ivPreview.setImageResource(R.drawable.pointer);
                //tvInfo.setText("Could not load image. Required size: 206×206 PNG");
                btReset.setEnabled(true);
            }
        } else {
            ivPreview.setImageResource(R.drawable.pointer);
            //tvInfo.setText("No custom image. PNG with transparency required (206×206).");
            btReset.setEnabled(false);
        }
    }
}