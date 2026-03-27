
# AttendApp

## Overview
*AttendApp* is an innovative mobile application designed to provide users with an innovative way of marking attendance. This project leverages Android Studio for app development and Firebase for backend services.

At its core, AttendApp uses **on-device face recognition** powered by TensorFlow Lite and the FaceNet deep learning model to automate attendance tracking. Teachers upload group photos; the app detects every face, matches each one against a registered student database, and saves the attendance record — all without any custom server.

---

## Table of Contents
- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [AI / ML Pipeline](#ai--ml-pipeline)
  - [Models](#models)
  - [Face Detection](#face-detection)
  - [Face Recognition](#face-recognition)
  - [Embedding Database](#embedding-database)
  - [Recognition Threshold](#recognition-threshold)
  - [Data Flow](#data-flow)
- [Firebase Backend](#firebase-backend)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)

---

## Features
| Role | Capabilities |
|------|-------------|
| **Student** | Register with a face photo, view enrolled courses, check weekly attendance records |
| **Teacher** | Upload group photos, auto-recognize students via face recognition, review & correct names, save attendance |
| **Admin** | Approve pending accounts, manage courses, assign students & teachers |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                         │
│                                                         │
│  ┌──────────────┐   ┌───────────────────────────────┐  │
│  │  UI / Views  │   │       AI / ML Engine           │  │
│  │  Activities  │──▶│  ML Kit Face Detection         │  │
│  │  Fragments   │   │  TFLite + FaceNet Embeddings   │  │
│  │  Adapters    │   │  Euclidean Distance Matching   │  │
│  └──────────────┘   └───────────────────────────────┘  │
│          │                        │                     │
└──────────┼────────────────────────┼─────────────────────┘
           │                        │
           ▼                        ▼
   ┌───────────────┐     ┌─────────────────────┐
   │  Firestore DB │     │   Firebase Storage   │
   │  Users        │     │   Profile images     │
   │  Courses      │     │   Embedding database │
   │  Attendance   │     └─────────────────────┘
   └───────────────┘
```

The app is fully **serverless** — all backend logic is handled by Firebase. Face recognition runs entirely **on-device**, meaning no face data is sent to an external server during the recognition step.

---

## AI / ML Pipeline

### Models

Two TFLite models are bundled in `app/src/main/assets/`:

| Model | File | Size | Input | Output | Use Case |
|-------|------|------|-------|--------|---------|
| **FaceNet** | `facenet.tflite` | ~23 MB | 160×160 px | 512-D float vector | Default — high accuracy |
| **MobileNet** | `mobile_face_net.tflite` | ~5.1 MB | 160×160 px | 512-D float vector | Lightweight alternative |

Both models generate **face embeddings**: fixed-length numerical representations of a face that capture its unique geometric features. Similar faces produce embeddings that are close together in vector space.

> The active model is loaded in `RecognizeActivity.java`:
> ```java
> faceClassifier = TFLiteFaceRecognition.create(
>     getAssets(), "facenet.tflite", 160, false, this);
> ```

---

### Face Detection

**Library**: [Google ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection) (`com.google.mlkit:face-detection:16.1.6`)

ML Kit is used as the **first stage** of the pipeline. Given an image (e.g., a group photo uploaded by a teacher), it:
1. Locates all faces and returns their bounding boxes (`RectF`)
2. Each detected bounding box is cropped from the original image
3. Each crop is resized to **160×160 pixels** before being passed to the TFLite model

---

### Face Recognition

**Implementation**: [`TFLiteFaceRecognition.java`](app/src/main/java/com/example/attendanceproject/face_rec/TFLiteFaceRecognition.java)
**Interface**: [`FaceClassifier.java`](app/src/main/java/com/example/attendanceproject/face_rec/FaceClassifier.java)

#### Embedding Extraction

```
Input image (160×160 px)
    ↓
Pixel normalization  (mean=128.0, std=128.0  for float models)
    ↓
TFLite model inference
    ↓
512-dimensional embedding vector
```

#### Nearest-Neighbour Matching

After generating the embedding for an unknown face, the app compares it against every stored embedding using **Euclidean distance**:

```
distance = √( Σ (embedding[i] − known_embedding[i])² )
```

The registered face with the **smallest distance** is selected as the candidate match.

#### `Recognition` Object

```java
public class Recognition {
    private final String id;        // Unique identifier
    private final String title;     // Matched person's name
    private final Float distance;   // Euclidean distance (lower = more similar)
    private Object embedding;       // Raw 512-D vector
    private RectF location;         // Bounding box in the original image
    private Bitmap crop;            // Cropped face image
}
```

---

### Embedding Database

When a student **registers**, their face photo is processed through the TFLite model and the resulting 512-D embedding is stored alongside their name.

- Embeddings are stored in a `HashMap<String, Recognition>`
- The map is serialized to JSON (via Gson) and uploaded to **Firebase Storage**
- At recognition time, the app downloads this database and matches against it

```
Student registers with photo
    ↓
TFLite generates 512-D embedding
    ↓
Saved as JSON → uploaded to Firebase Storage
    ↓
Teacher opens RecognizeActivity
    ↓
App downloads embedding database
    ↓
Ready for matching
```

---

### Recognition Threshold

```java
// RecognizeActivity.java
String recognizedName = recognition.getDistance() < 1.0
    ? recognition.getTitle()
    : "Unknown";
```

| Distance | Interpretation |
|----------|---------------|
| `< 1.0` | Recognized — name is displayed |
| `≥ 1.0` | Unrecognized — displayed as "Unknown" |

Teachers can manually correct any misidentified or unknown faces before saving the record.

---

### Data Flow

```
Teacher uploads group photo(s)
         │
         ▼
RecognizeActivity
         │
         ├─ For each photo:
         │       │
         │       ▼
         │  ML Kit FaceDetection
         │  → returns face bounding boxes
         │       │
         │       ▼
         │  Crop + Resize to 160×160
         │       │
         │       ▼
         │  TFLiteFaceRecognition.recognizeImage()
         │  → normalize pixels
         │  → run FaceNet inference → 512-D embedding
         │  → Euclidean distance vs. all registered embeddings
         │  → return best match (name + distance)
         │       │
         │       ▼
         │  distance < 1.0 ?
         │  ├─ YES → display student name
         │  └─ NO  → display "Unknown"
         │
         ▼
Teacher reviews & corrects names
         │
         ▼
Save to Firestore:
  ├── AttendanceRecords/{record}
  │     courseID, courseName, week, date, attendees[]
  └── Users/{uid}/CoursesEnrolled/{course}/Attendance
```

---

## Firebase Backend

The app uses a **fully serverless Firebase architecture** with no custom backend server.

### Firestore Collections

```
users/
  └── {uid}/
        FullName, email, isStudent, isTeacher, isAdmin
        isApproved (pending | true | false)
        └── CoursesEnrolled/
              └── {courseId}/
                    courseReference, Attendance[]

courses/
  └── {courseId}/
        courseName, courseDetail, teacherReference

attendanceRecords/
  └── {recordId}/
        courseID, courseName, week, date, attendees[]
```

### Firebase Storage

| Path | Contents |
|------|----------|
| Profile images | Student/teacher profile photos |
| `images` (embedding file) | Serialized face embedding database (JSON) |

### Firebase Services Used

| Service | Purpose |
|---------|---------|
| **Firebase Auth** | Email/password authentication |
| **Cloud Firestore** | User profiles, courses, attendance records |
| **Firebase Storage** | Profile images, embedding database |
| **Firebase UI Auth** | Pre-built auth flows |

---

## Project Structure

```
AttendanceProject/
├── app/src/main/
│   ├── assets/
│   │   ├── facenet.tflite              # Primary face recognition model (23 MB)
│   │   └── mobile_face_net.tflite     # Lightweight alternative (5.1 MB)
│   ├── java/com/example/attendanceproject/
│   │   ├── face_rec/
│   │   │   ├── FaceClassifier.java    # Recognition interface & Recognition data class
│   │   │   └── TFLiteFaceRecognition.java  # TFLite inference, embedding DB, matching
│   │   ├── FaceDetActivity.java       # Standalone face detection demo
│   │   └── account/
│   │       ├── auth/
│   │       │   ├── LoginUserActivity.java
│   │       │   ├── RegisterStudentActivity.java   # Face capture + embedding on signup
│   │       │   ├── RegisterTeacherActivity.java
│   │       │   └── RoleSelectionActivity.java
│   │       ├── student/
│   │       │   ├── StudentActivity.java
│   │       │   ├── ViewAttendanceStudentActivity.java
│   │       │   └── CourseStudentBottomSheetFragment.java
│   │       ├── teacher/
│   │       │   ├── TeacherActivity.java
│   │       │   ├── RecognizeActivity.java         # Core attendance recognition flow
│   │       │   ├── ViewAttendanceTeacherActivity.java
│   │       │   └── CourseTeacherBottomSheetFragment.java
│   │       ├── admin/
│   │       │   ├── AdminActivity.java
│   │       │   ├── AcceptAccFragment.java
│   │       │   ├── CoursesFragment.java
│   │       │   ├── StudentsFragment.java
│   │       │   └── TeachersFragment.java
│   │       └── adapters/
│   │           ├── FaceAdapter.java              # Displays recognized faces
│   │           ├── EntityAdapter.java
│   │           └── CheckableEntityAdapter.java
│   └── res/
│       ├── layout/                               # 28+ XML layouts
│       ├── drawable/
│       └── anim/
├── app/build.gradle                              # Dependencies & build config
├── build.gradle
└── README.md
```

### Key Dependencies

```gradle
// AI / ML
implementation 'com.google.mlkit:face-detection:16.1.6'
implementation 'org.tensorflow:tensorflow-lite:+'

// Firebase
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-storage:20.3.0'
implementation 'com.firebaseui:firebase-ui-auth:7.2.0'

// Image loading
implementation 'com.github.bumptech.glide:glide:4.12.0'
implementation 'com.github.chrisbanes:PhotoView:2.3.0'
```

---

## Getting Started

### Prerequisites
- **Android Studio**: Required for development. [Download Android Studio](https://developer.android.com/studio)
- **Firebase Account**: Necessary for accessing Firebase services. [Sign up or log in to Firebase](https://console.firebase.google.com/)

### Installation

1. **Clone the Repository**
   Clone the project to your local machine using the following command:
   ```bash
   git clone https://github.com/sorana01/AttendanceProject.git
   ```

2. **Set Up Firebase**
   - Navigate to the [Firebase Console](https://console.firebase.google.com/).
   - Create a new project or select an existing project.
   - Add an Android app to your Firebase project by following the instructions provided by Firebase.
   - Download the `google-services.json` file and place it in the `app/` directory of your cloned project.

3. **Open the Project in Android Studio**
   - Open Android Studio.
   - Select "Open an Existing Project" and navigate to the project directory.
   - Android Studio will automatically sync the project with Gradle files.

### Running the Application
- After setting up the project and Firebase configuration, build the project using `Build -> Rebuild Project`.
- Run the application on a physical device or an emulator by clicking `Run -> Run 'app'`.

> **Note**: Face recognition accuracy is best on a **physical device** with a real camera. The minimum supported Android version is **8.0 (API 26)**.
