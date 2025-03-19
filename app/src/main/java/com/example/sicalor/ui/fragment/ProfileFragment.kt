package com.example.sicalor.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sicalor.databinding.FragmentProfileBinding
import com.example.sicalor.ui.data.UserData
import com.example.sicalor.ui.user.FormActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
        database = Firebase.database.reference.child("UserData")
            .child(userId)

        getUserData()

        binding.updateData.setOnClickListener {
            val intent = Intent(requireContext(), FormActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getUserData() {
        database.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val userData = userSnapshot.getValue(UserData::class.java)

                    if (userData != null) {
                        binding.tvName.text = if (userData.name.isNotEmpty()) userData.name else "Data Empty"
                        binding.tvGender.text = if (userData.gender.isNotEmpty()) userData.gender else "Data Empty"
                        binding.tvAge.text = if (userData.age.isNotEmpty()) "${userData.age} year" else "Data Empty"
                        binding.tvWeight.text = if (userData.weight.isNotEmpty()) "${userData.weight} kg" else "Data Empty"
                        binding.tvHeight.text = if (userData.height.isNotEmpty()) "${userData.height} cm" else "Data Empty"
                        binding.tvActivity.text = if (userData.activity.isNotEmpty()) userData.activity else "Data Empty"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
}