package com.unundefined.busbeats;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.content.ContentValues.TAG;
import static com.unundefined.busbeats.MapActivity.SIGNED_IN_KEY;
import static com.unundefined.busbeats.MapActivity.sharedPreferences;

public class User {
    public static final String GENDER_MALE = "Male";
    public static final String GENDER_FEMALE = "Female";
    public static final String GENDER_NOT_SPECIFIED = "Not specified";
    private static User currentUser;
    private String firstName;
    private String lastName;
    private String email;
    private Date birthday;
    private String gender;
    private String facebookId;
    private String googleId;
    private FirebaseUser firebaseUser;
    private boolean isFacebookConnected;
    private boolean isGoogleConnected;

    public User(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        // TODO: Get user info from Firebase and create User
        // TODO: If user does not exist, sign out and relaunch the app
        // TODO: Block method until getting data is finished
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection("users").document(uid);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    assert document != null;
                    // TODO: Get data
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        synchronized (User.this) {
                            User.this.notify();
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

//        try {
//            synchronized (this) {
//                wait();
//            }
//        } catch (InterruptedException exception) {
//            throw new RuntimeException(exception);
//        }
    }

    public static void setUpUserFromFirebase() {
        FirebaseUser firebaseUser = Auth.getFirebaseAuth().getCurrentUser();
        assert firebaseUser != null;
        setCurrentUser(new User(firebaseUser));
    }

    public static void signOut() {
        Auth.getFirebaseAuth().signOut();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SIGNED_IN_KEY, false);
        editor.apply();
    }

    public static void setCurrentUser(User currentUser) {
        User.currentUser = currentUser;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SIGNED_IN_KEY, true);
        editor.apply();
    }
}
