package de.voize.flutterkmp.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FlutterModule(val name: String)
