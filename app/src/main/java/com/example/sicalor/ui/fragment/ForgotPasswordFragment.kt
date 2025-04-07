package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.sicalor.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : DialogFragment() {
    private lateinit var binding: FragmentForgotPasswordBinding
    private lateinit var auth: FirebaseAuth
    private var listener: OnDialogForgotPasswordListener? = null

    fun setListener(listener: OnDialogForgotPasswordListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSend.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Email cannot be empty!", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Email sent to ${email}!", Toast.LENGTH_SHORT).show()
                        listener?.onSendEmailForgotPassword(email)
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Email not sent!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    interface OnDialogForgotPasswordListener {
        fun onSendEmailForgotPassword(email: String)
    }
}