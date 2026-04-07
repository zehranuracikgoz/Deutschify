package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private LoginViewModel viewModel;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        TextView tvShowPassword = findViewById(R.id.tv_show_password);
        tvShowPassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                tvShowPassword.setText("gizle");
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                tvShowPassword.setText("göster");
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        viewModel.loginSuccess.observe(this, loginResponse -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        viewModel.errorMessage.observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        viewModel.isLoading.observe(this, loading ->
                btnLogin.setEnabled(!loading));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email ve şifre zorunlu", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.login(email, password);
    }
}