package com.example.sicalor.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private var filteredFoodList: List<FoodData> = emptyList()
    private var isFiltering = false
    private var _binding: FragmentFoodBinding? = null
    private val binding get() = _binding!!
    private var isLoading = false
    private val itemsPerPage = 5
    private var currentPage = 1

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
                    adapter.updateData(allFoodList.take(itemsPerPage))
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
            isFiltering = false
            currentPage = 1
            adapter.updateData(allFoodList.take(itemsPerPage))
            false
        }
    }

    private fun filterFoods(query: String) {
        if (query.isEmpty()) {
            isFiltering = false
            currentPage = 1
            adapter.updateData(allFoodList.take(itemsPerPage))
        } else {
            isFiltering = true
            filteredFoodList = allFoodList.filter { it.name.contains(query, ignoreCase = true) }
            currentPage = 1
            adapter.updateData(filteredFoodList.take(itemsPerPage))
        }

        Log.d("FoodFragment", "Filtered foods: ${filteredFoodList.size} for query: $query")
    }

    private fun initRecyclerView() {
        adapter = FoodAdapter(requireContext(), mutableListOf())
        recyclerView = binding.verticalRecyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter.setOnItemClickCallback(object : FoodAdapter.OnItemClickCallback {
            override fun onItemClicked(data: FoodData) {
                showSelectedFood(data)
            }
        })

        getFoodData()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && !isFiltering && lastVisibleItem >= totalItemCount - 3) {
                    loadMoreData()
                }
            }
        })
    }

    private fun loadMoreData() {
        if (isLoading) return
        isLoading = true

        Handler(Looper.getMainLooper()).postDelayed({
            val sourceList = if (isFiltering) filteredFoodList else allFoodList
            val start = currentPage * itemsPerPage
            val end = minOf(start + itemsPerPage, sourceList.size)

            if (start < end) {
                adapter.loadMoreData(sourceList.subList(start, end))
                currentPage++
            }

            isLoading = false
        }, 1000)
    }

    private fun showSelectedFood(food: FoodData) {
        Toast.makeText(context, "You choose ${food.name}", Toast.LENGTH_SHORT).show()
    }
}