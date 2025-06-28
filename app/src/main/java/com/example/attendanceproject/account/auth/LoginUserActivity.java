package com.example.attendanceproject.account.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.attendanceproject.R;
import com.example.attendanceproject.account.admin.AdminActivity;
import com.example.attendanceproject.account.student.StudentActivity;
import com.example.attendanceproject.account.teacher.TeacherActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginUserActivity extends AppCompatActivity{
    private EditText emailEditText, passwordEditText;
    private Button loginButton, goToRegisterButton;
    private boolean valid;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_user);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.loginEmail);
        passwordEditText = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginBtn);
        goToRegisterButton = findViewById(R.id.gotoRegister);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                valid = checkField(emailEditText) && checkField(passwordEditText);

                if (valid) {
                    fAuth.signInWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            checkUserAccessLevel(authResult.getUser().getUid());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginUserActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });


        goToRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RoleSelectionActivity.class));
            }
        });


    }

    private void checkUserAccessLevel(String uid) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                fStore.collection("Users").document(uid).update("fcmToken", token);
            }
        });

        DocumentReference df = fStore.collection("Users").document(uid);
        df.get().addOnSuccessListener(documentSnapshot -> {
            if (Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"))) {
//                Toast.makeText(LoginUserActivity.this, "You are an admin", Toast.LENGTH_LONG).show();
                startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                finish();
            }
            if ((Objects.equals(documentSnapshot.getString("isApproved"), "true"))) {
                if (Boolean.TRUE.equals(documentSnapshot.getBoolean("isStudent"))) {
//                    Toast.makeText(LoginUserActivity.this, "You are a student", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getApplicationContext(), StudentActivity.class));
                }
                if (Boolean.TRUE.equals(documentSnapshot.getBoolean("isTeacher"))) {
//                    Toast.makeText(LoginUserActivity.this, "You are a teacher", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getApplicationContext(), TeacherActivity.class));
                }
                finish();
            }
            else if ((Objects.equals(documentSnapshot.getString("isApproved"), "pending"))){
                Toast.makeText(LoginUserActivity.this, "Account not yet approved by admin", Toast.LENGTH_LONG).show();
                sendPushNotificationToAdmin("Approval Request", "A new account needs your approval.");
            }

            else if (Objects.equals(documentSnapshot.getString("isApproved"), "false")) {
                Toast.makeText(LoginUserActivity.this, "Your account has been restricted. Please contact the admin.", Toast.LENGTH_LONG).show();
            }
        });
    }


    public boolean checkField(EditText textField){
        if(textField.getText().toString().isEmpty()){
            textField.setError("This field cannot be empty");
            return false;
        }else {
            return true;
        }
    }

    private void sendPushNotificationToAdmin(String title, String message) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .whereEqualTo("isAdmin", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String adminToken = queryDocumentSnapshots.getDocuments().get(0).getString("fcmToken");

                        if (adminToken != null && !adminToken.isEmpty()) {
                            // Now send push using FCM HTTP
                            String serverKey = "YOUR_SERVER_KEY_HERE"; // <-- From Firebase Console
                            String fcmUrl = "https://fcm.googleapis.com/fcm/send";

                            JSONObject notification = new JSONObject();
                            JSONObject notificationBody = new JSONObject();

                            try {
                                notificationBody.put("title", title);
                                notificationBody.put("body", message);

                                notification.put("to", adminToken);
                                notification.put("notification", notificationBody);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, fcmUrl, notification,
                                    response -> Log.d("FCM", "Notification sent!"),
                                    error -> Log.e("FCM", "Failed to send notification: " + error.toString())) {

                                @Override
                                public Map<String, String> getHeaders() {
                                    Map<String, String> headers = new HashMap<>();
                                    headers.put("Authorization", "key=" + serverKey);
                                    headers.put("Content-Type", "application/json");
                                    return headers;
                                }
                            };

                            // Add the request to the Volley queue
                            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                            queue.add(request);
                        }
                    }
                });
    }


//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
//            startActivity(new Intent(getApplicationContext(), MainActivity.class));
//            finish();
//        }
//    }
}
