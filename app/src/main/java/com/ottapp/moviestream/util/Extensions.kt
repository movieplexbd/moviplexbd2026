package com.ottapp.moviestream.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.ottapp.moviestream.R
import java.util.concurrent.TimeUnit

fun View.show()      { visibility = View.VISIBLE }
fun View.hide()      { visibility = View.GONE    }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/**
 * Glide image loader with:
 * - placeholder & error drawable (blank screen সমস্যা fix)
 * - thumbnail() for faster perceived loading
 * - disk + memory cache
 */
fun ImageView.loadImage(url: String, placeholderRes: Int? = null) {
    val placeholder = placeholderRes ?: R.color.surface2
    val requestOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .skipMemoryCache(false)
        .centerCrop()
        .placeholder(placeholder)
        .error(placeholder)

    Glide.with(context)
        .load(url.ifBlank { null })
        .apply(requestOptions)
        .thumbnail(
            Glide.with(context)
                .load(url.ifBlank { null })
                .apply(RequestOptions().override(50, 50).diskCacheStrategy(DiskCacheStrategy.ALL))
        )
        .transition(DrawableTransitionOptions.withCrossFade(250))
        .into(this)
}

fun Long.toReadableSize(): String {
    if (this <= 0) return "0 B"
    val kb = this / 1024.0
    val mb = kb  / 1024.0
    val gb = mb  / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else    -> "$this B"
    }
}

fun Long.toFormattedTime(): String {
    val h = TimeUnit.MILLISECONDS.toHours(this)
    val m = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
           else       "%02d:%02d".format(m, s)
}

fun Long.toReadableDate(): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}
