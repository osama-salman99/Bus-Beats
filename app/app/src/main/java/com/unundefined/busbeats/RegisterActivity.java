package com.unundefined.busbeats;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class RegisterActivity extends AppCompatActivity {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String passwordConfirmation;
    private String gender;
    private Date birthday;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button registerButton = findViewById(R.id.register_button);
        TextView signInClickableText = findViewById(R.id.sign_in_clickable_text);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        signInClickableText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        database = FirebaseFirestore.getInstance();
    }

    private void register() {
        // TODO: Start loading popup

        firstName = ((EditText) findViewById(R.id.first_name_field)).getText()
                .toString();
        lastName = ((EditText) findViewById(R.id.last_name_field)).getText()
                .toString();
        switch (((RadioGroup) findViewById(R.id.gender_radio_group)).getCheckedRadioButtonId()) {
            case R.id.radio_button_male:
                gender = User.GENDER_MALE;
                break;
            case R.id.radio_button_female:
                gender = User.GENDER_FEMALE;
                break;
            case R.id.radio_button_not_specified:
            default:
                gender = User.GENDER_NOT_SPECIFIED;
        }
        email = ((EditText) findViewById(R.id.email_field)).getText().toString();
        password = ((EditText) findViewById(R.id.password_field)).getText().toString();
        passwordConfirmation = ((EditText) findViewById(R.id.password_confirmation_field))
                .getText().toString();
        birthday = new Date(1990, 1, 1);

        boolean debug = true;
        if (debug) {
            firstName = "Jake";
            lastName = "Scott";
            email = "hello11@gmail.com";
            password = "Qwerty1234";
            passwordConfirmation = password;
        }

        if (!validateData()) {
            return;
        }

        Auth.getFirebaseAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "createUserWithEmailAndPassword: " +
                                    "User successfully created");
                            uploadData();
                        } else {
                            Log.w(TAG, "createUserWithEmailAndPassword: Failure");
                            Log.w(TAG, "createUserWithEmailAndPassword: " +
                                    Objects.requireNonNull(task.getException()).getMessage());
                            Toast.makeText(getApplicationContext(),
                                    Objects.requireNonNull(task.getException()).getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            // TODO: Finish loading screen
                        }
                    }
                });
    }

    private void uploadData() {
        Log.w(TAG, "createUserOnDatabase: Creating database info");
        final Map<String, Object> userMap = new HashMap<>();
        userMap.put("first_name", firstName);
        userMap.put("last_name", lastName);
        userMap.put("email", email);
        userMap.put("gender", gender);
        userMap.put("birthday", birthday.toString());
        userMap.put("is_facebook_connected", false);
        userMap.put("is_google_connected", false);

        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(getApplicationContext(),
                    "An unknown error occurred. Please try again later.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        database.collection("users")
                .document(firebaseUser.getUid())
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "DocumentSnapshot successfully written!");
                        updateUser(firebaseUser);
                        Toast.makeText(getApplicationContext(), "Registration successful",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), MapActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        database.collection("users").document(firebaseUser.getUid()).delete();
                        firebaseUser.delete();
                        Toast.makeText(getApplicationContext(),
                                "An unknown error occurred. Please try again later or reach " +
                                        "out to the developers",
                                Toast.LENGTH_LONG).show();
                    }
                });
        // TODO: Finish loading screen
    }

    private void updateUser(FirebaseUser firebaseUser) {
        User.setCurrentUser(new User(firebaseUser));
    }

    private boolean validateData() {
        if (!isAlpha(firstName + lastName)) {
            Log.i(TAG, "register: First name and/or last name are not alpha");
            Toast.makeText(getApplicationContext(),
                    "Name must only contain characters or dashes", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isEmail(email)) {
            Log.i(TAG, "register: Invalid email");
            Toast.makeText(getApplicationContext(),
                    "Email is invalid", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(passwordConfirmation)) {
            Log.i(TAG, "register: Passwords do not match");
            Toast.makeText(getApplicationContext(),
                    "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!passwordIsValid(password)) {
            Log.i(TAG, "register: Password is invalid");
            Toast.makeText(getApplicationContext(),
                    "Password is invalid", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isAlpha(String name) {
        return name.matches("[a-zA-Z]+|(-)");
    }

    private boolean isEmail(String email) {
        return email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    }

    private boolean passwordIsValid(String password) {
        return password.length() >= 8
                && password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
    }

    private void showLoading() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                disableEnableLayout(false, (ViewGroup) findViewById(R.id.register_layout));
                setLoadingVisibility(true);
            }
        });
    }

    private void hideLoading() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                setLoadingVisibility(false);
                disableEnableLayout(true, (ViewGroup) findViewById(R.id.register_layout));
            }
        });
    }

    private void setLoadingVisibility(boolean visible) {
        int visibility;
        if (visible) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        findViewById(R.id.progress_screen).setVisibility(visibility);
    }

    private void disableEnableLayout(boolean enable, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                disableEnableLayout(enable, (ViewGroup) child);
            }
        }
    }
}