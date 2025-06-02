package com.kelompok4.eyewise.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.kelompok4.eyewise.R
import com.kelompok4.eyewise.ScreenTimeHistoryActivity
import com.kelompok4.eyewise.SessionManager
import com.kelompok4.eyewise.history.HistoryAdapter
import com.kelompok4.eyewise.history.HistoryItem
import com.kelompok4.eyewise.history.HistoryRepository
import com.kelompok4.eyewise.LoginActivity
import com.kelompok4.eyewise.ScreenTimeTracker
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuIcon: ImageView
    private lateinit var navigationView: NavigationView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = ArrayList<HistoryItem>()
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var screenTimeHandler: Handler
    private lateinit var screenTimeRunnable: Runnable
    private var screenTimeHours = 0
    private var screenTimeMinutes = 0
    private var hasUsagePermission = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(requireContext())

        drawerLayout = view.findViewById(R.id.drawerLayout)
        menuIcon = view.findViewById(R.id.menuIcon)
        navigationView = view.findViewById(R.id.navigationView)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    Toast.makeText(context, "Profile clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_language -> {
                    Toast.makeText(context, "Language clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_night_mode -> {
                    Toast.makeText(context, "Night mode clicked", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    logoutUser()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadHistoryData()
        checkAndRequestPermission(view)

        val seeDetailsText = view.findViewById<TextView>(R.id.seeDetailsText)
        seeDetailsText.setOnClickListener {
            val intent = Intent(requireContext(), ScreenTimeHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermission(view: View) {
        hasUsagePermission = ScreenTimeTracker.hasUsageStatsPermission(requireContext())
        if (!hasUsagePermission) {
            ScreenTimeTracker.requestUsageStatsPermission(requireContext())
            showPermissionNotice(view)
        } else {
            startScreenTimeTracking(view)
        }
    }

    private fun showPermissionNotice(view: View) {
        val screenTimeTextView = view.findViewById<TextView>(R.id.screenTimeText)
        screenTimeTextView.text = "Grant usage access in settings"

        // You might want to add a button or instruction to guide user
        Toast.makeText(
            requireContext(),
            "Please grant usage access permission to track screen time",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startScreenTimeTracking(view: View) {
        screenTimeHandler = Handler(Looper.getMainLooper())
        screenTimeRunnable = object : Runnable {
            override fun run() {
                updateScreenTime(view)
                screenTimeHandler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
            }
        }
        screenTimeHandler.post(screenTimeRunnable)
    }

    private fun updateScreenTime(view: View) {
        if (!ScreenTimeTracker.hasUsageStatsPermission(requireContext())) {
            hasUsagePermission = false
            showPermissionNotice(view)
            return
        }

        val (hours, minutes) = ScreenTimeTracker.getTodayScreenTime(requireContext())
        screenTimeHours = hours
        screenTimeMinutes = minutes

        val screenTimeTextView = view.findViewById<TextView>(R.id.screenTimeText)
        screenTimeTextView.text = "${screenTimeHours} h ${screenTimeMinutes} m"

        if (screenTimeHours >= 7) {
            showBreakAlert(view)
        } else {
            hideBreakAlert(view)
        }
    }

    private fun showBreakAlert(view: View) {
        val breakAlert = view.findViewById<View>(R.id.breakAlert)
        breakAlert.visibility = View.VISIBLE
    }

    private fun hideBreakAlert(view: View) {
        val breakAlert = view.findViewById<View>(R.id.breakAlert)
        breakAlert.visibility = View.GONE
    }

    private fun initViews(view: View) {
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList)
        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        historyList.clear()
        val repository = HistoryRepository()
        historyList.addAll(repository.getHistoryItems())
        historyAdapter.notifyDataSetChanged()
    }

    private fun logoutUser() {
        auth.signOut()
        sessionManager.clearSession()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        // Check permission again when returning from settings
        if (!hasUsagePermission) {
            checkAndRequestPermission(requireView())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::screenTimeHandler.isInitialized) {
            screenTimeHandler.removeCallbacks(screenTimeRunnable)
        }
    }
}