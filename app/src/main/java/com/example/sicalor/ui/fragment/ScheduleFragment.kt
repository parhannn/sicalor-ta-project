package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sicalor.databinding.FragmentScheduleBinding
import com.example.sicalor.ui.data.MealPlanData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class ScheduleFragment : Fragment() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private lateinit var mealPlanData: MealPlanData
    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authUser()

        binding.addButton.setOnClickListener { showSheetDialog() }
    }

    private fun showSheetDialog() {
        val sheetDialog = AddMealFragment()

        sheetDialog.show(parentFragmentManager, "TAG")
    }

    private fun authUser() {
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser!!.uid
    }
}