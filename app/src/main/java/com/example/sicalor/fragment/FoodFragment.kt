package com.example.sicalor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sicalor.databinding.FragmentFoodBinding

class FoodFragment : Fragment() {
    private var _binding: FragmentFoodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFoodBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }
}