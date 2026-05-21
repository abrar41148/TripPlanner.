package com.example.tripplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "TripPlannerPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";

    TextInputEditText etEmail, etPassword;
    MaterialButton btnLogin;
    TextView tvGoToSignup;

    SharedPreferences sharedPreferences;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Auto-login if already logged in with Firebase
        if (mAuth.getCurrentUser() != null) {
            goToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });
    }

    void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                btnLogin.setEnabled(true);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String username = "Traveler";
                    if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                        username = user.getDisplayName();
                    }

                    // Save session details to sharedPreferences for compatibility
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    editor.putString(KEY_EMAIL, email);
                    editor.putString(KEY_USERNAME, username);
                    editor.apply();

                    Toast.makeText(this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
                    goToDashboard();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                    Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }

    void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
