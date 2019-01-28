package com.god.seep.memoryanalyzer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Heap;
import com.squareup.haha.perflib.HprofParser;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import com.squareup.haha.perflib.io.MemoryMappedFileBuffer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "memoryTag";
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.bitmap_list);

        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        ImageView image1 = new ImageView(this);
        image1.setImageBitmap(bitmap1);
        ImageView image2 = new ImageView(this);
        image2.setImageBitmap(bitmap1);
        list.addView(image1);
        list.addView(image2);

        findViewById(R.id.btn_analyzer).setOnClickListener(v -> {
            System.gc();
            System.runFinalization();
            dumpHprof();
        });
        findViewById(R.id.btn_analyzer).setBackground(getResources().getDrawable(R.drawable.background));
    }

    private void dumpHprof() {
        String path = getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + "_hprof";
        File file = new File(path);
        try {
            Debug.dumpHprofData(path);
            Log.e(TAG, "start parse and analyzer -->");
            parseAndAnalyzer(file);
            Log.e(TAG, "end parse and analyzer <--");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseAndAnalyzer(File file) {
        try {
            MemoryMappedFileBuffer fileBuffer = new MemoryMappedFileBuffer(file);
            HprofParser parser = new HprofParser(fileBuffer);
            Snapshot snapshot = parser.parse();
            snapshot.computeDominators();
            ClassObj bitmapClass = snapshot.findClass(Bitmap.class.getName());
            Collection<Heap> heaps = snapshot.getHeaps();
            List<Instance> heapInstances = new ArrayList<>();
            for (Heap heap : heaps) {
                if (!"zygote".equals(heap.getName()))
                    heapInstances.addAll(bitmapClass.getHeapInstances(heap.getId()));
            }
            Log.e(TAG, "bitmap heap size = " + heapInstances.size());

            HashMap<String, List<ReportInfo>> instanceMap = new HashMap<>();
            for (Instance bitmapInstance : heapInstances) {
                int width = 0;
                int height = 0;
                byte[] buffer = null;
                List<ClassInstance.FieldValue> fieldValues = ((ClassInstance) bitmapInstance).getValues();
                for (ClassInstance.FieldValue fieldValue : fieldValues) {
                    Field field = fieldValue.getField();
                    if ("mWidth".equals(field.getName())) {
                        width = (int) fieldValue.getValue();
                    }
                    if ("mHeight".equals(field.getName())) {
                        height = (int) fieldValue.getValue();
                    }
                    if ("mBuffer".equals(field.getName())) {
                        ArrayInstance mBuffer = (ArrayInstance) fieldValue.getValue();
                        Method byteArray = mBuffer.getClass().getDeclaredMethod("asRawByteArray", int.class, int.class);
                        byteArray.setAccessible(true);
                        buffer = (byte[]) byteArray.invoke(mBuffer, 0, mBuffer.getValues().length);
                    }
                }
                String hash = String.valueOf(Arrays.hashCode(buffer));

                ReportInfo info = new ReportInfo();
                info.setWidth(width);
                info.setHeight(height);
                info.setHash(hash);
                if (buffer != null)
                    info.setBufferSize(buffer.length);
                info.addInstance(bitmapInstance);
                if (!instanceMap.containsKey(hash)) {
                    ArrayList<ReportInfo> list = new ArrayList<>();
                    list.add(info);
                    instanceMap.put(hash, list);
                } else {
                    List<ReportInfo> list = instanceMap.get(hash);
                    if (list != null)
                        list.add(info);
                }
            }

            for (String s : instanceMap.keySet()) {
                List<ReportInfo> list = instanceMap.get(s);
                if (list != null) {
                    Log.e(TAG, "bitmap size --> " + list.size());
                    for (ReportInfo info : list) {
                        Log.e(TAG, "report-->" + info.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
