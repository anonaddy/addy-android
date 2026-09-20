# Keep Gson models so that reflection deserialization works under R8
-keep class host.stjin.anonaddy_shared.models.** { *; }
-keepclassmembers class host.stjin.anonaddy_shared.models.** { *; }

# Keep network results and sealed classes
-keep class host.stjin.anonaddy_shared.network.NetworkResult** { *; }
-keep class host.stjin.anonaddy_shared.repositories.LoginResult** { *; }
