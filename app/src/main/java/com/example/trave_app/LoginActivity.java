package com.example.trave_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trave_app.database.TravelDatabase;
import com.example.trave_app.database.dao.UserDao;
import com.example.trave_app.database.entity.User;
import com.example.trave_app.firebase.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private UserDao userDao;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userDao = TravelDatabase.getDatabase(this).userDao();

        // Ensure default user exists
        TravelDatabase.databaseWriteExecutor.execute(() -> {
            User existing = userDao.getByUsername(" bipin");
            if (existing == null) {
                User def = new User();
                def.username = " bipin"; // note leading space as requested
                def.password = "123";
                def.phone = "0000000000";
                def.location = "default";
                userDao.insert(def);
            }
        });

        editUsername = findViewById(R.id.inputUsername);
        editPassword = findViewById(R.id.inputPassword);
        MaterialButton btnSignIn = findViewById(R.id.buttonSignIn);
        MaterialButton btnRegister = findViewById(R.id.buttonRegister);
        MaterialButton btnForgot = findViewById(R.id.buttonForgot);

        // Prefill default credentials
        editUsername.setText(" bipin");
        editPassword.setText("123");

        btnSignIn.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> showRegisterDialog());
        btnForgot.setOnClickListener(v -> showForgotDialog());
    }

    private void attemptLogin() {
        String username = editUsername.getText() == null ? "" : editUsername.getText().toString();
        String password = valueOf(editPassword);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            toast("Please enter username and password");
            return;
        }

        TravelDatabase.databaseWriteExecutor.execute(() -> {
            User u = userDao.getByUsername(username);
            mainHandler.post(() -> {
                if (u == null) {
                    toast("User not found");
                } else if (!password.equals(u.password)) {
                    toast("Incorrect password");
                } else {
                    String email = toEmail(username);
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result -> {
                                FirebaseRepository.getInstance().recordUserLogin(username);
                            })
                            .addOnFailureListener(e -> {
                                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener(res -> {
                                            UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(username)
                                                    .build();
                                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                                FirebaseAuth.getInstance().getCurrentUser().updateProfile(req);
                                            }
                                            FirebaseRepository.getInstance().recordUserLogin(username);
                                        });
                            });
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private void showRegisterDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_register, null, false);
        EditText eUser = view.findViewById(R.id.editRegUsername);
        EditText ePass = view.findViewById(R.id.editRegPassword);
        EditText eConfirm = view.findViewById(R.id.editRegConfirmPassword);
        EditText ePhone = view.findViewById(R.id.editRegPhone);
        EditText eLocation = view.findViewById(R.id.editRegLocation);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New Registration")
                .setView(view)
                .setPositiveButton("Register", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String u = textOf(eUser);
                String p = textOf(ePass);
                String c = textOf(eConfirm);
                String ph = textOf(ePhone);
                String loc = textOf(eLocation);

                if (u.isEmpty() || p.isEmpty() || c.isEmpty() || ph.isEmpty() || loc.isEmpty()) {
                    toast("All fields are required");
                    return;
                }
                if (!p.equals(c)) {
                    toast("Passwords do not match");
                    return;
                }

                TravelDatabase.databaseWriteExecutor.execute(() -> {
                    User existing = userDao.getByUsername(u);
                    if (existing != null) {
                        mainHandler.post(() -> toast("Username already exists"));
                        return;
                    }
                    User nu = new User();
                    nu.username = u;
                    nu.password = p;
                    nu.phone = ph;
                    nu.location = loc;
                    userDao.insert(nu);
                    String email = toEmail(u);
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, p)
                            .addOnSuccessListener(res -> {
                                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(u)
                                        .build();
                                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                    FirebaseAuth.getInstance().getCurrentUser().updateProfile(req);
                                }
                                FirebaseRepository.getInstance().syncUserRegistration(u, ph, loc, System.currentTimeMillis());
                            })
                            .addOnFailureListener(err -> {
                                FirebaseRepository.getInstance().syncUserRegistration(u, ph, loc, System.currentTimeMillis());
                            });
                    mainHandler.post(() -> {
                        toast("Registration successful. You can sign in now.");
                        dialog.dismiss();
                    });
                });
            });
        });

        dialog.show();
    }

    private void showForgotDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null, false);
        EditText eUser = view.findViewById(R.id.editFpUsername);
        EditText ePhone = view.findViewById(R.id.editFpPhone);
        EditText eLocation = view.findViewById(R.id.editFpLocation);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setView(view)
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String u = textOf(eUser);
                String ph = textOf(ePhone);
                String loc = textOf(eLocation);

                if (u.isEmpty() || ph.isEmpty() || loc.isEmpty()) {
                    toast("All fields are required");
                    return;
                }

                TravelDatabase.databaseWriteExecutor.execute(() -> {
                    User user = userDao.getByUsername(u);
                    if (user == null) {
                        mainHandler.post(() -> toast("User not found"));
                        return;
                    }
                    boolean match = ph.equals(user.phone) && loc.equalsIgnoreCase(user.location == null ? "" : user.location);
                    if (!match) {
                        mainHandler.post(() -> toast("Details do not match"));
                        return;
                    }
                    // Reset password to phone for this demo per requirements (no extra prompts)
                    userDao.updatePassword(u, ph);
                    mainHandler.post(() -> {
                        toast("Verification successful. Password reset to your contact number.");
                        dialog.dismiss();
                    });
                });
            });
        });

        dialog.show();
    }

    private String valueOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String textOf(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private String toEmail(String username) {
        String base = username == null ? "" : username.trim().toLowerCase().replaceAll("\\s+", "_");
        if (base.isEmpty()) base = "user";
        return base + "@travelapp.local";
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
