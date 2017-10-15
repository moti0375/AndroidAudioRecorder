# AndroidAudioRecorder

This is a tiny project that comes to get experiance with Android audio recording using the Android AudioRecord API
The app is recording audio in the following formats:
1. AMR - Using the opencore-amr-android library - https://github.com/kevinho/opencore-amr-android
2. MP3 - Using Android LAME MP3 encoder library - https://github.com/naman14/TAndroidLame 
3. WAV - Based on code that can be found here: https://gist.github.com/kmark/d8b1b01fb0d2febf5770

The project is developed in new Android Kotlin language and with MVP architecture 

In the first commit - The developer should change the output format manually in code. (by comment and select manually the format output function)

The output files can be pulled and played in external audio player (iTunes or any other player which supporta these formats)
In emulator the files can be pulled using Android Device Monitor tool.



