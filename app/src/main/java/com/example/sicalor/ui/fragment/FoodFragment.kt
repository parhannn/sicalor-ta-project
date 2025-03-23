package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sicalor.adapter.FoodAdapter
import com.example.sicalor.databinding.FragmentFoodBinding
import com.example.sicalor.ui.data.FoodData
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class FoodFragment : Fragment() {
    private lateinit var adapter: FoodAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var database: DatabaseReference
    private var allFoodList: List<FoodData> = emptyList()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.verticalRecyclerView

        initRecyclerView()

        setupSearchView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getFoodData() {
        database = Firebase.database.reference.child("Food")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val foodList = mutableListOf<FoodData>()

                    for (foodSnapshot in snapshot.children) {
                        val food = foodSnapshot.getValue(FoodData::class.java)
                        if (food != null) {
                            foodList.add(food)
                        }
                    }

                    allFoodList = foodList
                    adapter.updateData(foodList)
                } else {
                    Log.d("TAG", "No data available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    private fun setupSearchView() {
        searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterFoods(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterFoods(it) }
                return false
            }
        })
        searchView.setOnCloseListener {
            adapter.updateData(allFoodList)
            false
        }
    }

    private fun filterFoods(query: String) {
        val filteredFoods = if (query.isEmpty()) {
            allFoodList
        } else {
            allFoodList.filter { it.name.contains(query, ignoreCase = true) }
        }

        Log.d("FoodFragment", "Filtered foods: $filteredFoods for query: $query")
        adapter.updateData(filteredFoods)
    }

    private fun initRecyclerView() {
        adapter = FoodAdapter(requireContext(), mutableListOf())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        getFoodData()
    }
}