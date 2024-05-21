package com.example.attendanceproject.face_rec;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.example.attendanceproject.imagepicker.MainActivity2;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFLiteFaceRecognition
        implements FaceClassifier {

    private static final String FileName = "images";
    // CHANGE MODEL
    private static final int OUTPUT_SIZE = 512;
//    private static final int OUTPUT_SIZE = 192;

    // Only return this many results.
    private static final int NUM_DETECTIONS = 1;

    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    private boolean isModelQuantized;
    // Config values.
    private int inputSize;

    private int[] intValues;

    private float[][] embeddings;

    private ByteBuffer imgData;

    private Interpreter tfLite;
    private HashMap<String, Recognition> registered = new HashMap<>();



    public void register(String name, Recognition rec) {
//        Map<String, Recognition> recognitionMap = new HashMap<>();
//
//        for (Map.Entry<String, Recognition> entry : recognitionMap.entrySet()) {
//            if (entry.getKey().equals(name)) {
//                return;
//            }
//        }

        MainActivity2.registered.put(name, rec);
    }

    public void registerDb(String name, Recognition rec, Context context) {
        registered.put(name, rec);

        byte[] bytes=null;
        try {
            //write the bytes in file
            {
                Gson gson = new Gson();


                File localFile = new File(context.getFilesDir(), FileName);
                FileOutputStream fileOutputStream = new FileOutputStream(localFile);

                Type type = new TypeToken<HashMap<String, Recognition>>(){}.getType();
                String toStoreObject = gson.toJson(registered,type);

                ObjectOutputStream o = new ObjectOutputStream(fileOutputStream);
                o.writeObject(toStoreObject);
                Log.d("REGISTER", "Embedding name " + name + " has been registered");

                o.close();

                fileOutputStream.close();

                Toast.makeText(context, "save file completed.", Toast.LENGTH_LONG ).show();

            }

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference test2 = storageRef.child(FileName);

            Uri file = Uri.fromFile(new File(context.getFilesDir(),FileName));


            test2.putFile(file)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(context, "Upload Completed.", Toast.LENGTH_LONG ).show();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            // ...
                            Toast.makeText(context, "Upload Failure.", Toast.LENGTH_LONG ).show();
                        }
                    });


        }catch (Exception e){


            Log.d("Clique AQUI","Clique AQUI file created: " + e.toString());
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG ).show();

        }
    }

    public void registerMul(String name, Recognition rec) {
        if (MainActivity2.registered.containsKey(name)) {
            Recognition existingRec = MainActivity2.registered.get(name);
            // this part feels redundant - we already have a list from last else
            List<float[]> embeddings;
            if (existingRec.getEmbedding() instanceof List) {
                embeddings = (List<float[]>) existingRec.getEmbedding();
            } else {
                embeddings = new ArrayList<>();
                embeddings.add(((float[][]) existingRec.getEmbedding())[0]); // Cast and add the existing single embedding
            }
            embeddings.add(((float[][]) rec.getEmbedding())[0]); // Add the new embedding
            existingRec.setEmbedding(embeddings);
        } else {
            List<float[]> newEmbeddingList = new ArrayList<>();
            newEmbeddingList.add(((float[][]) rec.getEmbedding())[0]); // Start with a list even for a single new entry
            rec.setEmbedding(newEmbeddingList);
            MainActivity2.registered.put(name, rec);
        }
    }


    public void finalizeEmbeddings() {
        for (Map.Entry<String, Recognition> entry : MainActivity2.registered.entrySet()) {
            Object embedding = entry.getValue().getEmbedding();
            if (embedding instanceof List) {
                List<float[]> embeddings = (List<float[]>) embedding;
                if (!embeddings.isEmpty()) {
                    float[] averagedEmbedding = averageEmbeddings(embeddings);
                    // Check if averagedEmbedding is not null before setting
                    if (averagedEmbedding != null) {
                        entry.getValue().setEmbedding(averagedEmbedding);
                        Log.d("EMB_MUL", "For " + entry.getKey() + " value is " + Arrays.toString(averagedEmbedding));
                    }
                }
            }
        }
    }



    private float[] averageEmbeddings(List<float[]> embeddings) {
        if (embeddings == null || embeddings.isEmpty()) return null;
        int length = embeddings.get(0).length; // Assume all embeddings have the same length
        float[] sumEmbedding = new float[length];
        for (float[] emb : embeddings) {
            for (int i = 0; i < emb.length; i++) {
                sumEmbedding[i] += emb[i];
            }
        }
        for (int i = 0; i < length; i++) {
            sumEmbedding[i] /= embeddings.size();
        }
        return sumEmbedding;
    }






    private TFLiteFaceRecognition() {}

    //TODO loads the models into mapped byte buffer format
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    public static FaceClassifier createDb(
            final AssetManager assetManager,
            final String modelFilename,
            final int inputSize,
            final boolean isQuantized,
            Context context)
            throws IOException {

        final TFLiteFaceRecognition d = new TFLiteFaceRecognition();

        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference test2 = storageRef.child(FileName);

            File localFile = File.createTempFile("Student", "txt");
            test2.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    try {
                        Gson gson = new Gson();
                        ObjectInputStream i = new ObjectInputStream(new FileInputStream(localFile));

                        Type type = new TypeToken<HashMap<String, Recognition>>(){}.getType();
                        HashMap<String, Recognition> registeredDb = gson.fromJson((String)i.readObject(), type);
                        i.close();

                        // Check if the map is not empty
                        if (registeredDb != null && !registeredDb.isEmpty()){
                            d.registered = registeredDb;
                            // Logging each key-value pair
                            for (Map.Entry<String, Recognition> entry : registeredDb.entrySet()) {
                                Log.d("REGISTERED_DB", "Key: " + entry.getKey() + ", Value: " + entry.getValue().toString());
                            }
                        } else {
                            Log.d("REGISTERED_DB", "The registered database is empty or null.");
                        }

                        Toast.makeText(context, "Content embeddings read", Toast.LENGTH_LONG ).show();

                    } catch (Exception e) {
                        Log.d("REGISTERED_DB_EXCEPTION", "Exception when reading and processing file: " + e.toString());
                        Toast.makeText(context, "Exception 1" + e.getMessage(), Toast.LENGTH_LONG ).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("DOWNLOAD_FAILURE", "Error downloading file: " + exception.toString());
                    Toast.makeText(context, "Exception 2 " + exception.getMessage(), Toast.LENGTH_LONG ).show();
                }
            });


        } catch (Exception e) {

            Log.d("Clique AQUI", "Clique AQUI file created: " + e.toString());
        }


        d.inputSize = inputSize;

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];
        return d;
    }
    public static FaceClassifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final int inputSize,
            final boolean isQuantized)
            throws IOException {

        final TFLiteFaceRecognition d = new TFLiteFaceRecognition();
        d.inputSize = inputSize;

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];
        return d;
    }

    //looks for the nearest embedding in the dataset
    // and returns the pair <id, distance>
    private Pair<String, Float> findNearestMul(float[] emb) {
        Pair<String, Float> ret = null;

        for (Map.Entry<String, Recognition> entry : MainActivity2.registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = (float[]) entry.getValue().getEmbedding();

            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff*diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }
        return ret;
    }

    private Pair<String, Float> findNearest(float[] emb) {
        Pair<String, Float> ret = null;

        for (Map.Entry<String, Recognition> entry : MainActivity2.registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getEmbedding())[0];

            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff*diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }
        return ret;
    }

    private Pair<String, Float> findNearestDb(float[] emb) {
        Gson gson = new Gson();

        Pair<String, Float> ret = null;

        for (Map.Entry<String, Recognition> entry : registered.entrySet()) {
            String name = entry.getKey();

            float distance = 0;
            try {
                float[][] knownEmb2d = gson.fromJson(entry.getValue().getEmbedding().toString(), float[][].class);
                final float[] knownEmb = knownEmb2d[0];

                for (int i = 0; i < emb.length; i++) {
                    float diff = emb[i] - knownEmb[i];
                    distance += diff * diff;
                }
            } catch (Exception e) {
                //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG ).show();
                Log.e("findNearest", e.getMessage());
            }

            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }

        return ret;
    }


    //TAKE INPUT IMAGE AND RETURN RECOGNITIONS
    // bitmap = crop
    @Override
    public Recognition recognizeImageRec(final Bitmap bitmap, boolean storeExtra) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Object[] inputArray = {imgData};
        // Here outputMap is changed to fit the Face Mask detector
        Map<Integer, Object> outputMap = new HashMap<>();

        embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeddings);

        // Run the inference call.
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.d("TFLiteFaceRec", "Embedding content " + outputMap + ", " + embeddings[0]);


        float distance = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        if (registered.size() > 0) {
            Log.d("FROM DB", "dataset SIZE: " + registered.size());
            // looks for the nearest neighbour
            final Pair<String, Float> nearest = findNearestDb(embeddings[0]);
            if (nearest != null) {
                final String name = nearest.first;
                label = name;
                distance = nearest.second;

                Log.d("FROM DB", "nearest: " + name + " - distance: " + distance);
            }
        }


        final int numDetectionsOutput = 1;
        Recognition rec = new Recognition(
                id,
                label,
                distance,
                new RectF());


        // storeExtra bool true = new face to add
        if (storeExtra) {
            rec.setEmbedding(embeddings);
        }

        return rec;
    }

    @Override
    public Recognition recognizeImage(final Bitmap bitmap, boolean storeExtra) {
//        Log.d("BitmapInfo", "intValues length: " + intValues.length);
//        Log.d("BitmapInfo", "Bitmap dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Object[] inputArray = {imgData};
        // Here outputMap is changed to fit the Face Mask detector
        Map<Integer, Object> outputMap = new HashMap<>();

        embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeddings);

        // Run the inference call.
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.d("TFLiteFaceRec", "Embedding content " + outputMap + ", " + embeddings[0]);


        float distance = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        if (MainActivity2.registered.size() > 0) {
            final Pair<String, Float> nearest = findNearestMul(embeddings[0]);
            if (nearest != null) {
                final String name = nearest.first;
                label = name;
                distance = nearest.second;
            }
        }
        final int numDetectionsOutput = 1;
        Recognition rec = new Recognition(
                id,
                label,
                distance,
                new RectF());


        // storeExtra bool true = new face to add
        if (storeExtra) {
            rec.setEmbedding(embeddings);
        }

        return rec;
    }


}
