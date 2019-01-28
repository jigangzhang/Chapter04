package com.hprof.bitmap;

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

public class Main {

    public static void main(String[] args) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        String path = reader.readLine();
//        File file = new File(path);

        if (args.length > 0) {
            String path = args[0];
            File file = new File(path);
            if (file.exists()) {
                System.out.println("start parse and analyzer -->");
                parseAndAnalyzer(file);
                System.out.println("<-- end parse and analyzer");
            }
        } else {
            System.out.println("请输入文件路径");
        }
    }

    private static void parseAndAnalyzer(File file) {
        try {
            MemoryMappedFileBuffer fileBuffer = new MemoryMappedFileBuffer(file);
            HprofParser parser = new HprofParser(fileBuffer);
            Snapshot snapshot = parser.parse();
            snapshot.computeDominators();
            ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
            Collection<Heap> heaps = snapshot.getHeaps();
            List<Instance> heapInstances = new ArrayList<>();
            for (Heap heap : heaps) {
                if (!"zygote".equals(heap.getName()))
                    heapInstances.addAll(bitmapClass.getHeapInstances(heap.getId()));
            }
            System.out.println("bitmap heap size = " + heapInstances.size());

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
                        if (mBuffer != null) {
                            Method byteArray = mBuffer.getClass().getDeclaredMethod("asRawByteArray", int.class, int.class);
                            byteArray.setAccessible(true);
                            buffer = (byte[]) byteArray.invoke(mBuffer, 0, mBuffer.getValues().length);
                        }
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
                if (list != null && list.size() > 1) {
                    System.out.println("bitmap size --> " + list.size());
                    for (ReportInfo info : list) {
                        System.out.println("report-->" + info.toString());
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
