package com.codingblocks.cbonlineapp.tracks

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.course.CourseActivity
import com.codingblocks.cbonlineapp.course.CourseListAdapter
import com.codingblocks.cbonlineapp.course.ItemClickListener
import com.codingblocks.cbonlineapp.util.COURSE_ID
import com.codingblocks.cbonlineapp.util.COURSE_LOGO
import com.codingblocks.cbonlineapp.util.LOGO_TRANSITION_NAME
import com.codingblocks.cbonlineapp.util.extensions.loadImage
import com.codingblocks.cbonlineapp.util.extensions.observer
import com.codingblocks.cbonlineapp.util.extensions.setRv
import com.codingblocks.cbonlineapp.util.extensions.setToolbar
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_course.*
import kotlinx.android.synthetic.main.activity_course.courseToolbar
import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.activity_track.appbar
import kotlinx.android.synthetic.main.activity_track.courseBackdrop
import kotlinx.android.synthetic.main.activity_track.courseLogo
import kotlinx.android.synthetic.main.activity_track.shortTv
import org.koin.androidx.viewmodel.ext.android.viewModel

class TrackActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    private val courseId by lazy {
        intent.getStringExtra(COURSE_ID)
    }
    private val courseLogoImage by lazy {
        intent.getStringExtra(LOGO_TRANSITION_NAME)
    }

    private val courseLogoUrl by lazy {
        intent.getStringExtra(COURSE_LOGO)
    }
    private val viewModel by viewModel<TrackViewModel>()

    private val courseCardListAdapter = CourseListAdapter("TRACKS")

    private val itemClickListener: ItemClickListener by lazy {
        object : ItemClickListener {
            override fun onClick(id: String, name: String, logo: ImageView) {
                val intent = Intent(this@TrackActivity, CourseActivity::class.java)
                intent.putExtra(COURSE_ID, id)
                intent.putExtra(COURSE_LOGO, name)
                intent.putExtra(LOGO_TRANSITION_NAME, ViewCompat.getTransitionName(logo))
                val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@TrackActivity,
                    logo,
                    ViewCompat.getTransitionName(logo)!!)
                startActivity(intent, options.toBundle())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track)
        supportPostponeEnterTransition()
        setToolbar(courseToolbar)
        viewModel.id = courseId
        viewModel.fetchCurrentTrack()
        trackCourseRv.setRv(this@TrackActivity, courseCardListAdapter, setDivider = true, type = "THICK")
        if (!courseLogoImage.isNullOrEmpty()) {
            courseLogo.transitionName = courseLogoImage
            courseLogo.loadImage(courseLogoUrl) {
                supportStartPostponedEnterTransition()
            }
        }

        viewModel.currentTrack.observer(this) { track ->
            title = track.name
//            shortTv.text = course.subtitle

            if (courseLogoUrl.isNullOrEmpty()) courseLogo.loadImage(track.logo)
            courseBackdrop.loadImage(track.background)
            shortTv.text = track.description
            trackCourseNumTv.text = "${track.courses?.size} Courses"
        }

        viewModel.courses.observer(this) { courses ->
            courseCardListAdapter.submitList(courses)
        }
        courseCardListAdapter.onItemClick = itemClickListener
        appbar.addOnOffsetChangedListener(this)
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val alpha = (appBarLayout.totalScrollRange + verticalOffset).toFloat() / appBarLayout.totalScrollRange
        courseLogo.alpha = alpha
        shortTv.alpha = alpha
        trackCourseNumTv.alpha = alpha
    }
}
