package com.ottapp.moviestream.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ottapp.moviestream.R

class OnboardAdapter(private val slides: List<OnboardSlide>) :
    RecyclerView.Adapter<OnboardAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivOnboard: ImageView = view.findViewById(R.id.iv_onboard)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboard_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.ivOnboard.setImageResource(slide.imageRes)
        holder.tvTitle.text = slide.title
        holder.tvSubtitle.text = slide.subtitle
    }

    override fun getItemCount() = slides.size
}
