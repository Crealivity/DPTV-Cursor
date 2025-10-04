package io.github.crealivity.dptvcursor.gui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.crealivity.dptvcursor.R;
import io.github.crealivity.dptvcursor.assets.PointerAssets;

// This isn't a generic re-usable adapter
// it should be refactored to become a generic adapter
// in case similar dropdowns are needed in future

public class IconStyleSpinnerAdapter extends ArrayAdapter<String> {
    private final List<String> objects;
    private Context context;
    public static Map<String, Integer> textToResourceIdMap = new HashMap<String, Integer>();
    public static Map<String, Integer> textToOffsetX = new HashMap<String, Integer>();
    public static Map<String, Integer> textToOffsetY = new HashMap<String, Integer>();

    public IconStyleSpinnerAdapter(@NonNull Context context, int resource, int textViewId) {
        super(context, resource, textViewId, PointerAssets.getAllStyles());
        this.context = context;
        this.objects = PointerAssets.getAllStyles();
    }

    public static List<String> getResourceList () {
        return new ArrayList<>(textToResourceIdMap.keySet());
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return objects.get(position);
    }

    @Override
    public int getPosition(@Nullable String item) {
        return objects.indexOf(item);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    private View getCustomView(int position, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.spinner_icon_text_gui, parent, false);

        TextView label = row.findViewById(R.id.textView);
        ImageView icon = row.findViewById(R.id.imageView);

        String selection = objects.get(position);
        label.setText(selection);

        int resId = PointerAssets.getResId(selection);
        if (resId != 0) {
            icon.setImageDrawable(ContextCompat.getDrawable(context, resId));
        } else {
            File f = new File(context.getFilesDir(), "custom_pointer.png");
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) { icon.setImageBitmap(b); return row; }
            }
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pointer)); // fallback
        }

        return row;
    }
}
