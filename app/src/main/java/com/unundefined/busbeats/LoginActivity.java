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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    private CallbackManager callbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupGoogleSignIn();
        setupFacebookLoginButton();

        Button signInButton = findViewById(R.id.login_button);
        Button registerInButton = findViewById(R.id.register_screen_button);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();

                EditText emailField = findViewById(R.id.email_field);
                EditText passwordField = findViewById(R.id.password_field);

                String email = emailField.getText().toString();
                String password = passwordField.getText().toString();

                if (!isEmail(email)) {
                    Log.w(TAG, "logInButtonOnClick: " + email
                            + " is not a valid email format");
                    Toast.makeText(getApplicationContext(), "Please enter a valid email",
                            Toast.LENGTH_SHORT).show();
                    hideLoading();
                    return;
                }

                if (password.isEmpty()) {
                    Log.w(TAG, "logInButtonOnClick: Password field is empty");
                    Toast.makeText(getApplicationContext(), "Password field cannot be empty",
                            Toast.LENGTH_SHORT).show();
                    hideLoading();
                    return;
                }

                signIn(email, password);
            }
        });

        registerInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegisterActivity();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        database = FirebaseFirestore.getInstance();
    }

    private void signIn(String email, String password) {
        Auth.getFirebaseAuth()
                .signInWithEmailAndPassword(email, password).addOnCompleteListener(
                LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "signInWithEmail:success");
                            User.setUpUserFromFirebase();
                            hideLoading();
                            goToMainActivity();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed: " +
                                    Objects.requireNonNull(task.getException())
                                            .getMessage(), Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    }
                });
    }

    private void setupFacebookLoginButton() {
        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = findViewById(R.id.facebook_login_button);
        loginButton.setPermissions(Arrays
                .asList("public_profile", "email", "user_birthday", "user_gender"));

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
            }
        });

        new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    hideLoading();
                }
            }
        };

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                Log.d(TAG, "registerCallback: onSuccess: Access token: " +
                        loginResult.getAccessToken());
                handleFacebookAccessToken(accessToken);
            }

            @Override
            public void onCancel() {
                hideLoading();
                Toast.makeText(getApplicationContext(),
                        "Facebook login was canceled, please try again",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(getApplicationContext(),
                        "Error occurred, please try again", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "registerCallback: onError: " + exception.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    assert account != null;
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException exception) {
                    Log.w(TAG, "Google sign in failed", exception);
                    hideLoading();
                    Toast.makeText(getApplicationContext(), "Sign in failed: "
                            + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "Google sign in failed: Task unsuccessful", task.getException());
                Toast.makeText(getApplicationContext(), "Google sign in failed",
                        Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        Auth.getFirebaseAuth().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential: Success");
                            storeGoogleUserData();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential: Failure", task.getException());
                            hideLoading();
                            Toast.makeText(LoginActivity.this,
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void storeGoogleUserData() {
        GoogleSignInAccount googleAccount =
                GoogleSignIn.getLastSignedInAccount(getApplicationContext());

        if (googleAccount == null) {
            Log.w(TAG, "storeGoogleUserData: lastSignedAccount is null");
            Toast.makeText(getApplicationContext(), "An error occurred. Please try again later"
                    , Toast.LENGTH_SHORT).show();
            mGoogleSignInClient.signOut();
            return;
        }

        // TODO: get birthday and gender
        String firstName = googleAccount.getGivenName();
        String lastName = googleAccount.getFamilyName();
        String email = googleAccount.getEmail();
        String id = googleAccount.getId();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("first_name", firstName);
        userMap.put("last_name", lastName);
        userMap.put("email", email);
        userMap.put("is_facebook_connected", false);
        userMap.put("is_google_connected", true);
        userMap.put("google_account_id", id);

        final FirebaseUser firebaseUser = Auth.getFirebaseAuth().getCurrentUser();

        assert firebaseUser != null;

        database.collection("users")
                .document(firebaseUser.getUid())
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        updateUser(firebaseUser);
                        hideLoading();
                        goToMainActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        database.collection("users")
                                .document(firebaseUser.getUid()).delete();
                        firebaseUser.delete();
                        mGoogleSignInClient.signOut();
                        hideLoading();
                        Toast.makeText(getApplicationContext(),
                                "An unknown error occurred." +
                                        " Please try again later",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken accessToken) {
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        Auth.getFirebaseAuth().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            storeFacebookUserData();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            LoginManager.getInstance().logOut();
                            hideLoading();
                            Toast.makeText(LoginActivity.this,
                                    "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void storeFacebookUserData() {
        final GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("LoginActivity", response.toString());
                        Log.d(TAG, "onCompleted: JSON object: " + object.toString());
                        String facebookId;
                        String firstName;
                        String lastName;
                        String email;
                        String gender;
                        Date birthday;

                        try {
                            facebookId = object.getString("id");
                        } catch (JSONException exception) {
                            facebookId = null;
                        }

                        try {
                            firstName = object.getString("first_name");
                        } catch (JSONException exception) {
                            firstName = null;
                        }

                        try {
                            email = object.getString("email");
                        } catch (JSONException exception) {
                            email = null;
                        }

                        try {
                            gender = object.getString("gender");
                        } catch (JSONException exception) {
                            gender = null;
                        }

                        try {
                            String dateString = object.getString("birthday")
                                    .replace("/", "-");
                            Log.d(TAG, "onCompleted: " + dateString);
                            birthday = Date.toDate(dateString);
                            if (birthday == null) {
                                birthday = new Date(1990, 1, 1);
                            }
                        } catch (JSONException exception) {
                            birthday = new Date(1990, 1, 1);
                        }

                        try {
                            lastName = object.getString("last_name");
                        } catch (JSONException exception) {
                            lastName = null;
                        }

                        Log.d(TAG, "createUserOnDatabase: Creating database info");
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("first_name", firstName);
                        userMap.put("last_name", lastName);
                        userMap.put("email", email);
                        userMap.put("gender", gender);
                        userMap.put("birthday", birthday.toString());
                        userMap.put("is_facebook_connected", true);
                        userMap.put("is_google_connected", false);
                        userMap.put("facebook_account_id", facebookId);

                        final FirebaseUser firebaseUser = Auth.getFirebaseAuth().getCurrentUser();

                        assert firebaseUser != null;

                        database.collection("users")
                                .document(firebaseUser.getUid())
                                .set(userMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "DocumentSnapshot successfully written!");
                                        updateUser(firebaseUser);
                                        hideLoading();
                                        goToMainActivity();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                        database.collection("users")
                                                .document(firebaseUser.getUid()).delete();
                                        firebaseUser.delete();
                                        LoginManager.getInstance().logOut();
                                        hideLoading();
                                        Toast.makeText(getApplicationContext(),
                                                "An unknown error occurred." +
                                                        " Please try again later",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,gender,birthday");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.
                Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(getString(R.string.server_client_id))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), googleSignInOptions);
        findViewById(R.id.google_sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void updateUser(FirebaseUser firebaseUser) {
        User.setCurrentUser(new User(firebaseUser));
    }

    private boolean isEmail(String email) {
        return email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
    }

    private void showLoading() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                disableEnableLayout(false, (ViewGroup) findViewById(R.id.login_layout));
                setLoadingVisibility(true);
            }
        });
    }

    private void hideLoading() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                setLoadingVisibility(false);
                disableEnableLayout(true, (ViewGroup) findViewById(R.id.login_layout));
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

    private void goToMainActivity() {
        startActivity(new Intent(getApplicationContext(), MapActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void goToRegisterActivity() {
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}