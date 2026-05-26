package com.richapps.cyphertext.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.richapps.cyphertext.R
import com.richapps.cyphertext.Utils
import com.richapps.cyphertext.adapters.SearchUserAdapter
import com.richapps.cyphertext.databinding.ActivitySearchUserBinding
import com.richapps.cyphertext.models.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SearchUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchUserBinding
    private lateinit var adapter: SearchUserAdapter
    private val serverUrl = Utils.SERVER_URL



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = SearchUserAdapter()
        binding.userList.layoutManager = LinearLayoutManager(this)
        binding.userList.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Initialize adapter once with empty list
//        adapter = SearchUserAdapter()
//        binding.userList.layoutManager = LinearLayoutManager(this)
//        binding.userList.adapter = adapter


        binding.searchBtn.setOnClickListener {
            val searchText = binding.etSearch.text.toString()

            if (searchText.isEmpty()) {
                binding.etSearch.error = "Please enter a username"
            }
            else {
                //setupSearchRecyclerView(searchText)
                searchUsers(searchText)
            }

        }

    }
//    private fun setupSearchRecyclerView(searchText: String) {
//        val query = FirebaseDatabase.getInstance().getReference("AllUsers")
//            .child("Users")
//            .orderByChild("userName")
//            .startAt(searchText)
//            .endAt(searchText + "\uf8ff")
//
//        val option = FirebaseRecyclerOptions.Builder<Users>()
//            .setQuery(query, Users::class.java)
//            .build()
//        adapter = SearchUserAdapter(option)
//        binding.userList.layoutManager = LinearLayoutManager(this)
//        binding.userList.adapter = adapter
//        adapter.startListening()
//    }
//
//    override fun onStart() {
//        super.onStart()
//        adapter.startListening()
//
//    }
//
//    override fun onStop() {
//        super.onStop()
//        adapter.stopListening()
//    }

    private fun searchUsers(searchText: String) {
        binding.searchBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedQuery = URLEncoder.encode(searchText, "UTF-8")
                val url = URL("$serverUrl/search?query=$encodedQuery")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Content-Type", "application/json")
                    // TODO: Add Authorization header once session management implemented
                    // setRequestProperty("Authorization", "Bearer <token>")
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val userList = parseUsers(response)

                    withContext(Dispatchers.Main) {
                        adapter.updateData(userList)
                        if (userList.isEmpty()) {
                            binding.etSearch.error = "No users found"
                        }
                    }
                } else {
                    Log.e("SearchUser", "Server error: $responseCode")
                    withContext(Dispatchers.Main) {
                        binding.etSearch.error = "Search failed, please try again"
                    }

                }
                connection.disconnect()

            } catch (e: Exception) {
                Log.e("SearchUser", "Search error: ${e.message}")
                Log.e("SearchUser", "Search error type: ${e.javaClass.simpleName}")
                Log.e("SearchUser", "Search error message: ${e.message}")
                Log.e("SearchUser", "Stack trace: ${e.stackTraceToString()}")
                withContext(Dispatchers.Main) {
                    binding.etSearch.error = "Network error, please try again"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.searchBtn.isEnabled = true
                }
            }
        }
    }

    private fun parseUsers(json: String): List<Users> {
        val userList = mutableListOf<Users>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            userList.add(
                Users(
                    userId = obj.getInt("id"),
                    userName = obj.getString("username"),
                    phoneNumber = obj.getString ("phone_number")
                )
            )
        }
        return userList
    }
}