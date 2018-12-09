package com.codingblocks.cbonlineapp.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.adapters.SectionDetailsAdapter
import com.codingblocks.cbonlineapp.database.AppDatabase
import com.codingblocks.cbonlineapp.database.CourseSection
import kotlinx.android.synthetic.main.fragment_course_content.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val ARG__ATTEMPT_ID = "attempt_id"

class CourseContentFragment : Fragment(), AnkoLogger {

    private lateinit var database: AppDatabase
    lateinit var attemptId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            attemptId = it.getString(ARG__ATTEMPT_ID)!!
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_course_content, container, false)
        database = AppDatabase.getInstance(context!!)
        val sectionDao = database.setionDao()
        val sectionsList = ArrayList<CourseSection>()
        val sectionAdapter = SectionDetailsAdapter(sectionsList, activity!!)
        view.rvExpendableView.layoutManager = LinearLayoutManager(context)
        view.rvExpendableView.adapter = sectionAdapter
        //to stop recyclerview from binding again again
        //fix for adding items on runtime
        view.rvExpendableView.setItemViewCacheSize(25)


        sectionDao.getCourseSection(attemptId).observe(this, Observer<List<CourseSection>> {
            info {
                "sections$it"
                sectionAdapter.setData(it as ArrayList<CourseSection>)
            }
        })

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String) =
                CourseContentFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG__ATTEMPT_ID, param1)
                    }
                }
    }


}