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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
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
import java.util.HashMap;
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


    public void registerDb(String name, Recognition rec, Context context) {
        registered.put(name, rec);

        try {
            File localFile = saveRecognitionToFile(context);
            uploadFileToFirebase(localFile, context);
        } catch (Exception e) {
            Log.d("Clique AQUI", "Clique AQUI file created: " + e.toString());
//            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File saveRecognitionToFile(Context context) throws IOException {
        Gson gson = new Gson();
        File localFile = new File(context.getFilesDir(), FileName);
        FileOutputStream fileOutputStream = new FileOutputStream(localFile);
        Type type = new TypeToken<HashMap<String, Recognition>>() {}.getType();
        String toStoreObject = gson.toJson(registered, type);
        ObjectOutputStream o = new ObjectOutputStream(fileOutputStream);
        o.writeObject(toStoreObject);
        Log.d("REGISTER", "Embedding name has been registered");
        o.close();
        fileOutputStream.close();
//        Toast.makeText(context, "Save file completed.", Toast.LENGTH_LONG).show();
        return localFile;
    }


    private void uploadFileToFirebase(File localFile, Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference test2 = storageRef.child(FileName);
        Uri file = Uri.fromFile(localFile);
        test2.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        Toast.makeText(context, "Upload Completed.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(context, "Upload Failure.", Toast.LENGTH_LONG).show();
                    }
                });
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


    public static TFLiteFaceRecognition create(
            final AssetManager assetManager,
            final String modelFilename,
            final int inputSize,
            final boolean isQuantized,
            Context context) throws IOException {

        TFLiteFaceRecognition d = initializeFaceRecognition(inputSize, isQuantized, assetManager, modelFilename);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        downloadFileFromFirebase(storage.getReference(), FileName, d, context);
        return d;
    }


    private Pair<String, Float> findNearestDb(float[] emb) {
        Pair<String, Float> ret = null;

        for (Map.Entry<String, Recognition> entry : registered.entrySet()) {
            String name = entry.getKey();
            float distance = calculateDistance(emb, entry.getValue());

            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }

        return ret;
    }

    private float[] deserializeEmbedding(String embeddingJson) {
        try {
            Gson gson = new Gson();
            float[][] knownEmb2d = gson.fromJson(embeddingJson, float[][].class);
            return knownEmb2d[0];
        } catch (Exception e) {
            Log.e("deserializeEmbedding", e.getMessage());
            return null;
        }
    }

    private float calculateDistance(float[] emb, Recognition recognition) {
        float distance = 0;
        try {
            float[] knownEmb = deserializeEmbedding(recognition.getEmbedding().toString());
            if (knownEmb != null) {
                for (int i = 0; i < emb.length; i++) {
                    float diff = emb[i] - knownEmb[i];
                    distance += diff * diff;
                }
                distance = (float) Math.sqrt(distance);
            }
        } catch (Exception e) {
            Log.e("calculateDistance", e.getMessage());
        }
        return distance;
    }


    private static TFLiteFaceRecognition initializeFaceRecognition(int inputSize, boolean isQuantized, AssetManager assetManager, String modelFilename) throws IOException {
        TFLiteFaceRecognition d = new TFLiteFaceRecognition();
        d.inputSize = inputSize;
        d.isModelQuantized = isQuantized;
        d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
        allocateImageBuffers(d);
        return d;
    }


    private static void allocateImageBuffers(TFLiteFaceRecognition d) {
        int numBytesPerChannel = d.isModelQuantized ? 1 : 4; // Quantized or floating point
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];
    }

    private static void downloadFileFromFirebase(StorageReference storageRef, String FileName, TFLiteFaceRecognition d, Context context) {
        StorageReference test2 = storageRef.child(FileName);
        test2.getMetadata().addOnSuccessListener(storageMetadata -> {
            try {
                final File localFile = File.createTempFile("Student", "txt");
                test2.getFile(localFile).addOnSuccessListener(taskSnapshot -> processDownloadedFile(localFile, d, context))
                        .addOnFailureListener(exception -> handleDownloadFailure(exception, context));
            } catch (IOException e) {
                Log.d("FILE_CREATION_ERROR", "Error creating temp file: " + e.toString());
                Toast.makeText(context, "Error creating temp file", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(exception -> handleFileNotExist(exception, context));
    }

    private static void processDownloadedFile(File file, TFLiteFaceRecognition d, Context context) {
        try {
            Gson gson = new Gson();
            ObjectInputStream i = new ObjectInputStream(new FileInputStream(file));
            Type type = new TypeToken<HashMap<String, Recognition>>(){}.getType();
            HashMap<String, Recognition> registeredDb = gson.fromJson((String) i.readObject(), type);
            i.close();
            if (registeredDb != null && !registeredDb.isEmpty()) {
                d.registered = registeredDb;
                registeredDb.forEach((key, value) -> Log.d("REGISTERED_DB", "Key: " + key + ", Value: " + value.toString()));
            } else {
                Log.d("REGISTERED_DB", "The registered database is empty or null.");
            }
//            Toast.makeText(context, "Content embeddings read", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.d("REGISTERED_DB_EXCEPTION", "Exception when reading and processing file: " + e.toString());
            Toast.makeText(context, "Exception 1: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void handleDownloadFailure(Exception exception, Context context) {
        Log.d("DOWNLOAD_FAILURE", "Error downloading file: " + exception.toString());
        Toast.makeText(context, "Exception 2: " + exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    private static void handleFileNotExist(Exception exception, Context context) {
        Log.d("FILE_NOT_FOUND", "File does not exist: " + exception.toString());
        Toast.makeText(context, "File not found.", Toast.LENGTH_LONG).show();
    }


    @Override
    public Recognition recognizeImage(Context context, final Bitmap bitmap, boolean storeExtra) {
        processBitmap(bitmap);
        runModel();
        Pair<String, Float> nearest = performRecognition();
        return createRecognition(nearest, storeExtra);
    }

    private void processBitmap(final Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else {
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
    }

    private void runModel() {
        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        embeddings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeddings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Log.d("TFLiteFaceRec", "Embedding content " + outputMap + ", " + embeddings[0]);
    }

    private Pair<String, Float> performRecognition() {
        if (registered.size() > 0) {
            Log.d("FROM DB", "dataset SIZE: " + registered.size());
            return findNearestDb(embeddings[0]);
        }
        return null;
    }

    private Recognition createRecognition(Pair<String, Float> nearest, boolean storeExtra) {
        float distance = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        if (nearest != null) {
            label = nearest.first;
            distance = nearest.second;
            Log.d("FROM DB", "nearest: " + label + " - distance: " + distance);
        }

        Recognition rec = new Recognition(id, label, distance, new RectF());

        if (storeExtra) {
            rec.setEmbedding(embeddings);
        }

        return rec;
    }

}
