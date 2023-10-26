package com.example.myapplication.sample.module1

import android.content.Context
import android.view.View

/**
 *  created by Ashish Saini at 4th Oct 2023
 *
 **/
fun View?.visible(){
    this?.visibility = View.VISIBLE
}

fun View?.gone(){
    this?.visibility = View.GONE
}

fun View?.invisible(){
    this?.visibility = View.INVISIBLE
}

fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

fun Int.toDp(context: Context): Int = (this / context.resources.displayMetrics.density).toInt()