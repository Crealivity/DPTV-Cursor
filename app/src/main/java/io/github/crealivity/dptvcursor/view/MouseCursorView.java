package io.github.crealivity.dptvcursor.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.io.File;

import io.github.crealivity.dptvcursor.R;
import io.github.crealivity.dptvcursor.assets.PointerAssets;
import io.github.crealivity.dptvcursor.gui.IconStyleSpinnerAdapter;
import io.github.crealivity.dptvcursor.helper.Helper;

/**
 * Draw a Mouse Cursor on screen
 */
public class MouseCursorView extends View {
    private static final int DEFAULT_ALPHA= 255;
    private final PointF mPointerLocation;
    private final Paint mPaintBox;
    private Bitmap mPointerBitmap;
    private int pointerDrawableReference;
    private int pointerSizeReference;

    private int pointerOffsetX;
    private int pointerOffsetY;

    private int dstWidth = 15; //50
    private int dstHeight = 15; // 50

    public MouseCursorView(Context context) {
        super(context);
        setWillNotDraw(false);
        mPointerLocation = new PointF();
        mPaintBox = new Paint();
        updateFromPreferences();
        setBitmap(context);
    }

    private void setBitmap(Context context) {
        if (pointerDrawableReference == 0) {
            File customFile = new File(context.getFilesDir(), "custom_pointer.png");
            if (customFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(customFile.getAbsolutePath());
                if (bmp != null) {
                    mPointerBitmap = Bitmap.createScaledBitmap(
                            bmp,
                            dstWidth * pointerSizeReference,
                            dstHeight * pointerSizeReference,
                            true
                    );
                    return;
                }
            }

            pointerDrawableReference = R.drawable.pointer;
        }


        BitmapDrawable bp = (BitmapDrawable) ContextCompat.getDrawable(context, pointerDrawableReference);
        Bitmap originalBitmap = bp.getBitmap();
        BitmapDrawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(originalBitmap, dstWidth * pointerSizeReference, dstHeight * pointerSizeReference, true));
        mPointerBitmap = d.getBitmap();
    }

    public void updateFromPreferences() {
        Context ctx = getContext();
        String iconStr = Helper.getMouseIconPref(ctx);

        pointerDrawableReference = PointerAssets.getResId(iconStr);
        pointerOffsetX = PointerAssets.getOffsetX(iconStr);
        pointerOffsetY = PointerAssets.getOffsetY(iconStr);
        pointerSizeReference = Helper.getMouseSizePref(ctx) + 1;

        setBitmap(getContext());
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaintBox.setAlpha(DEFAULT_ALPHA);
        canvas.drawBitmap(mPointerBitmap, mPointerLocation.x - dstWidth * pointerSizeReference * pointerOffsetX / 209, mPointerLocation.y - dstHeight * pointerSizeReference * pointerOffsetY / 209, mPaintBox);
    }

    public void updatePosition(PointF p) {
        mPointerLocation.x = p.x;
        mPointerLocation.y = p.y;
        this.postInvalidate();
    }
}