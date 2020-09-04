# PackCompiler

Compiles "Packs" in .jar Format to load dynamically on a JVM.

On [plugins.gradle.org](https://plugins.gradle.org/plugin/com.jaqxues.pack-compiler)

## How it works
The Pack Compiler is a Gradle Plugin. It aims to create a \.jar file and automate part of the workflow when working with
such a Pack System, and hence, creates a number of different tasks. It creates these tasks for each buildVariant (e\. g\.
"debug" and "release" by default). In the enumeration below, only "debug" tasks will be listed, but release variants are
available.

In summary, this PackCompiler uses another Gradle Plugin as dependency: the official 'com.android.dynamic-feature'
Plugin. This allows to treat the pack as its own module, and handles dependencies, compilation etc. Each Task depends on
the previous one. 

##### Main Tasks:
1. `extractPackDexDebug` - The PackCompiler extracts the \.dex file(s) from the generated dynamic-feature APK file (This
    task depends on `assembleDebug` to make sure the APK is compiled correctly)
2. `bundlePackDebug` - It bundles the extracted \.dex file(s) into a \.jar file with Manifest values.
3. `signPackDebug` - It signs the generated Jar file appropriately (This task is usually only used with the `release` 
    buildVariant, since debug builds usually don't enforce Signature checks)
4. `adbPushPackDebug` - It allows pushing the Pack (\.jar file) directly to your phone via ADB. (It uses a Json Config
    file to allow for more complex configuration and control over storage location and multiple devices)

## How to use it
Examples and Projects that use this Plugin are:
* [SnipTools](https://github.com/jaqxues/SnipTools)
* [Instaprefs](https://github.com/marzika/Instaprefs/) (Private Project)

It generally is recommended to use these tasks not directly, but build a more full Automation layer on top of it. An
example of this can be seen in SnipTools, where it uses `adbPushPackDebug` as a dependency for a task that combines
multiple steps:
* Generate and push the Pack (`adbPushPackDebug`)
* Activate the Pack and update the Preferences to load the correct Pack immediately
* Force closes the target App (It hooks the Target app, so to apply changes, it needs to be fully restarted)
* Re-open the target App

This Script can be found in [SnipTools/packimpl/build.gradle](https://github.com/jaqxues/SnipTools/blob/master/packimpl/build.gradle)
