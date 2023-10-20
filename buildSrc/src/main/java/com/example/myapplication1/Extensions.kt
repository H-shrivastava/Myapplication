package com.example.myapplication1

import org.gradle.api.Project

val Project.versionCode: Int
    get() = intProperty("VERSION_CODE")

val Project.versionName: String
    get() = stringProperty("VERSION_NAME")

private fun Project.stringProperty(name: String): String {
    return property(name) as String
}

private fun Project.intProperty(name: String): Int {
    return (property(name) as String).toInt()
}
