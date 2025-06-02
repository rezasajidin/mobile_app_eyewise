package com.kelompok4.eyewise

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.kelompok4.eyewise.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        // Check if user is already logged in
        if (sessionManager.fetchAuthToken() != null) {
            startMainActivity()
            finish()
            return
        }

        // Set email+password dari intent jika ada
        val emailFromReg = intent.getStringExtra("EXTRA_EMAIL")
        val passwordFromReg = intent.getStringExtra("EXTRA_PASSWORD")
        if (!emailFromReg.isNullOrEmpty()) {
            binding.etEmail.setText(emailFromReg)
        }
        if (!passwordFromReg.isNullOrEmpty()) {
            binding.etPassword.setText(passwordFromReg)
        }

        binding.btnSignIn.setOnClickListener {
            loginUser()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Email harus diisi"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password harus diisi"
            binding.etPassword.requestFocus()
            return
        }

        binding.btnSignIn.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.btnSignIn.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
                }
            }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.btnSignIn.isEnabled = true
                if (task.isSuccessful) {
                    // Save session
                    task.result?.user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result?.token
                            if (token != null) {
                                sessionManager.saveAuthToken(token)
                                sessionManager.saveUserEmail(email)
                            }
                        }
                        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                        startMainActivity()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

}