package com.codingblocks.cbonlineapp.dashboard

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.caverock.androidsvg.SVG
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.admin.AdminActivity
import com.codingblocks.cbonlineapp.auth.LoginActivity
import com.codingblocks.cbonlineapp.commons.FragmentChangeListener
import com.codingblocks.cbonlineapp.commons.TabLayoutAdapter
import com.codingblocks.cbonlineapp.course.checkout.CheckoutActivity
import com.codingblocks.cbonlineapp.dashboard.doubts.DashboardDoubtsFragment
import com.codingblocks.cbonlineapp.dashboard.explore.DashboardExploreFragment
import com.codingblocks.cbonlineapp.dashboard.home.DashboardHomeFragment
import com.codingblocks.cbonlineapp.dashboard.library.DashboardLibraryFragment
import com.codingblocks.cbonlineapp.dashboard.mycourses.DashboardMyCoursesFragment
import com.codingblocks.cbonlineapp.jobs.JobsActivity
import com.codingblocks.cbonlineapp.mycourse.MyCourseActivity
import com.codingblocks.cbonlineapp.notifications.NotificationsActivity
import com.codingblocks.cbonlineapp.profile.ReferralActivity
import com.codingblocks.cbonlineapp.purchases.PurchasesActivity
import com.codingblocks.cbonlineapp.settings.AboutActivity
import com.codingblocks.cbonlineapp.settings.SettingsActivity
import com.codingblocks.cbonlineapp.tracks.LearningTracksActivity
import com.codingblocks.cbonlineapp.util.COURSE_ID
import com.codingblocks.cbonlineapp.util.COURSE_NAME
import com.codingblocks.cbonlineapp.util.MediaUtils
import com.codingblocks.cbonlineapp.util.NetworkUtils.okHttpClient
import com.codingblocks.cbonlineapp.util.PreferenceHelper
import com.codingblocks.cbonlineapp.util.RUN_ATTEMPT_ID
import com.codingblocks.cbonlineapp.util.RUN_ID
import com.codingblocks.cbonlineapp.util.extensions.colouriseToolbar
import com.codingblocks.cbonlineapp.util.extensions.loadImage
import com.codingblocks.cbonlineapp.util.extensions.observeOnce
import com.codingblocks.cbonlineapp.util.extensions.observer
import com.codingblocks.cbonlineapp.util.extensions.setToolbar
import com.codingblocks.cbonlineapp.util.extensions.slideDown
import com.codingblocks.cbonlineapp.util.extensions.slideUp
import com.codingblocks.fabnavigation.FabNavigation
import com.codingblocks.fabnavigation.FabNavigationAdapter
import com.codingblocks.onlineapi.Clients
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.app_bar_dashboard.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.singleTop
import org.jetbrains.anko.startActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FragmentChangeListener, FabNavigation.OnTabSelectedListener {

    private val pagerAdapter by lazy { TabLayoutAdapter(supportFragmentManager) }
    private val navigationAdapter: FabNavigationAdapter by lazy { FabNavigationAdapter(this, R.menu.bottom_nav_dashboard) }
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val viewModel by viewModel<DashboardViewModel>()
    private var doubleBackToExitPressedOnce = false
    private val prefs by inject<PreferenceHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        Clients.authJwt = prefs.SP_JWT_TOKEN_KEY
        Clients.refreshToken = prefs.SP_JWT_REFRESH_TOKEN
        viewModel.isLoggedIn.postValue(prefs.SP_JWT_TOKEN_KEY.isNotEmpty())
        initializeUI(prefs.SP_JWT_TOKEN_KEY.isNotEmpty())
    }

    private fun setUser() {
        referralContainer.isVisible = true
        viewModel.user.observer(this) {
            val navMenu = dashboardNavigation.menu
            navMenu.findItem(R.id.nav_inbox).isVisible = true
            navMenu.findItem(R.id.nav_admin).isVisible = prefs.SP_ADMIN

            dashboardNavigation.getHeaderView(0).apply {
                findViewById<CircleImageView>(R.id.navHeaderImageView).loadImage(prefs.SP_USER_IMAGE, true)
                findViewById<TextView>(R.id.navUsernameTv).text = ("Hello ${prefs.SP_USER_NAME}")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkForUpdates()
        val data = this.intent.data
        if (data != null && data.isHierarchical) {
            if (data.getQueryParameter("code") != null) {
                fetchToken(data)
            }
        }
    }

    private fun fetchToken(data: Uri) {
        val grantCode = data.getQueryParameter("code") as String
        viewModel.fetchToken(grantCode)
        initializeUI(true)
    }

    private fun initializeUI(loggedIn: Boolean) {
        setToolbar(dashboardToolbar, hasUpEnabled = false, homeButtonEnabled = false, title = "Dashboard")
        val toggle = ActionBarDrawerToggle(
            this,
            dashboardDrawer,
            dashboardToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        dashboardDrawer.addDrawerListener(toggle)
        toggle.syncState()
        dashboardNavigation.setNavigationItemSelectedListener(this)
        navigationAdapter.setupWithBottomNavigation(dashboardBottomNav)
        setupViewPager()

        dashboardBottomNav.apply {
            setTitleTypeface(Typeface.createFromAsset(assets, "fonts/gilroy_medium.ttf"))
            defaultBackgroundColor = getColor(R.color.dark)
            titleState = (FabNavigation.TitleState.ALWAYS_SHOW)
            setOnTabSelectedListener(this@DashboardActivity)
            accentColor = getColor(R.color.bottomNavSelected)
        }
        if (loggedIn) {
            setUser()
            createShortcut()
            dashboardBottomNav.setCurrentItem(1)
        } else {
            dashboardToolbar.colouriseToolbar(this@DashboardActivity, R.drawable.toolbar_bg_dark, getColor(R.color.white))
            dashboardNavigation.getHeaderView(0).apply {
                findViewById<TextView>(R.id.navUsernameTv).text = "Login/Signup"
            }
            dashboardBottomNav.setCurrentItem(0)
        }
        dashboardAppBarLayout.bringToFront()
    }

    @TargetApi(25)
    fun createShortcut() {

        val sM = getSystemService(ShortcutManager::class.java)
        val shortcutList: MutableList<ShortcutInfo> = ArrayList()

        viewModel.courses.observeOnce {

            doAsync {
                it.take(2).forEachIndexed { index, courseRun ->

                    val intent = Intent(this@DashboardActivity, MyCourseActivity::class.java)
                    intent.action = Intent.ACTION_VIEW
                    intent.putExtra(COURSE_ID, courseRun.courseRun.course.cid)
                    intent.putExtra(RUN_ID, courseRun.courseRun.run.crUid)
                    intent.putExtra(RUN_ATTEMPT_ID, courseRun.courseRun.runAttempt.attemptId)
                    intent.putExtra(COURSE_NAME, courseRun.courseRun.course.title)

                    val shortcut = ShortcutInfo.Builder(this@DashboardActivity, "topcourse$index")
                    shortcut.setIntent(intent)
                    shortcut.setLongLabel(courseRun.courseRun.course.subtitle)
                    shortcut.setShortLabel(courseRun.courseRun.course.title)
                    shortcut.setDisabledMessage("Login to open this")

                    okHttpClient.newCall(Request.Builder().url(courseRun.courseRun.course.logo).build())
                                    .execute().body?.let {
                            with(SVG.getFromInputStream(it.byteStream())) {
                                val picDrawable = PictureDrawable(
                                    this.renderToPicture(
                                        400, 400
                                    )
                                )
                                val bitmap = MediaUtils.getBitmapFromPictureDrawable(picDrawable)
                                val circularBitmap = MediaUtils.getCircularBitmap(bitmap)
                                shortcut.setIcon(Icon.createWithBitmap(circularBitmap))
                                shortcutList.add(index, shortcut.build())
                            }
                        }
                }
                sM?.apply {
                    dynamicShortcuts.clear()
                    dynamicShortcuts = shortcutList
                }
            }
        }
    }

    private fun setupViewPager() {
        pagerAdapter.apply {
            add(DashboardExploreFragment())
            add(DashboardMyCoursesFragment())
            add(DashboardHomeFragment())
            add(DashboardDoubtsFragment())
            add(DashboardLibraryFragment())
        }
        dashboardPager.apply {
            setPagingEnabled(false)
            adapter = pagerAdapter
            offscreenPageLimit = 4
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.dashboard_notification -> {
            startActivity(intentFor<NotificationsActivity>())
            true
        }
        R.id.dashboard_cart -> {
            startActivity(intentFor<CheckoutActivity>())
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_contatus -> {
                startActivity(intentFor<AboutActivity>())
            }
            R.id.nav_admin -> {
                startActivity(intentFor<AdminActivity>().singleTop())
            }
            R.id.nav_settings -> {
                startActivity(intentFor<SettingsActivity>().singleTop())
            }
            R.id.nav_tracks -> {
                startActivity(intentFor<LearningTracksActivity>().singleTop())
            }
            R.id.nav_purchases -> {
                startActivity(intentFor<PurchasesActivity>().singleTop())
            }
            R.id.nav_hiring -> {
                startActivity(intentFor<JobsActivity>().singleTop())
            }
            R.id.nav_inbox -> {
                startActivity(intentFor<ChatActivity>().singleTop())
            }
        }
        dashboardDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
    }

    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    1001
                )
            }
        }
    }

    override fun onBackPressed() {
        if (dashboardDrawer.isDrawerOpen(GravityCompat.START)) {
            dashboardDrawer.closeDrawer(GravityCompat.START)
        } else {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
                return
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
            GlobalScope.launch {
                delay(2000)
                doubleBackToExitPressedOnce = false
            }
        }
    }

    override fun openInbox(conversationId: String) {
    }

    override fun openClassroom() {
        dashboardBottomNav.setCurrentItem(1)
    }

    override fun openExplore() {
        dashboardBottomNav.setCurrentItem(0)
    }

    override fun onTabSelected(position: Int, wasSelected: Boolean): Boolean {
        when (position) {
            0 -> {
                supportActionBar?.title = getString(R.string.welcome)
                searchBtn.setOnClickListener {
                    startActivity(intentFor<LearningTracksActivity>().singleTop())
                }
                dashboardToolbar.colouriseToolbar(this@DashboardActivity, R.drawable.toolbar_bg_dark, getColor(R.color.white))
                dashboardToolbarSecondary.post {
                    dashboardToolbarSearch.slideDown()
                    dashboardToolbarSecondary.slideUp()
                }
            }
            2 -> {
                supportActionBar?.title = getString(R.string.dashboard)
                dashboardToolbar.colouriseToolbar(this@DashboardActivity, R.drawable.toolbar_bg_dark, getColor(R.color.white))

                if (viewModel.isLoggedIn.value == true) {
                    dashboardToolbarSearch.slideUp()
                    dashboardToolbarSecondary.slideDown()
//                    dashboardToolbarSecondary.crossfade(dashboardToolbarSearch, null)
                }
            }
            else -> {
                when (position) {
                    3 -> supportActionBar?.title = getString(R.string.my_doubs)
                    1 -> supportActionBar?.title = getString(R.string.my_courses)
                    else -> supportActionBar?.title = getString(R.string.my_library)
                }
                dashboardToolbar.colouriseToolbar(this@DashboardActivity, R.drawable.toolbar_bg, getColor(R.color.black))
                dashboardToolbarSecondary.post {
                    dashboardToolbarSearch.slideUp()
                    dashboardToolbarSecondary.slideUp()
                }
            }
        }
        dashboardPager.setCurrentItem(position, true)
        return true
    }

    fun openProfile(view: View) {
        if (prefs.SP_JWT_TOKEN_KEY.isEmpty()) {
            startActivity<LoginActivity>()
        }
    }

    fun openReferral(view: View) {
        startActivity<ReferralActivity>()
    }
}
