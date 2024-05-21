package com.example.attendanceproject.face_rec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;

/** Generic interface for interacting with different recognition engines. */
public interface FaceClassifier {

    void registerMul(String name, Recognition recognition);

    void register(String name, Recognition recognition);

    void registerDb(String name, Recognition recognition, Context context);


    void finalizeEmbeddings();

    Recognition recognizeImage(Bitmap bitmap, boolean getExtra);

    Recognition recognizeImageRec(Bitmap bitmap, boolean getExtra);

    public class Recognition {
        private final String id;

        /** Display name for the recognition. */
        private final String title;
        // A sortable score for how good the recognition is relative to others. Lower should be better.
        private final Float distance;
        private Object embedding;
        /** Optional location within the source image for the location of the recognized face. */
        private RectF location;
        private Bitmap crop;

        public Recognition(
                final String id, final String title, final Float distance, final RectF location) {
            this.id = id;
            this.title = title;
            this.distance = distance;
            this.location = location;
            this.embedding = null;
            this.crop = null;
        }

        public void setEmbedding(Object extra) {
            this.embedding = extra;
        }
        public Object getEmbedding() {
            return this.embedding;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getDistance() {
            return distance;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }

        public void setCrop(Bitmap crop) {
            this.crop = crop;
        }

        public Bitmap getCrop() {
            return this.crop;
        }
    }
}
