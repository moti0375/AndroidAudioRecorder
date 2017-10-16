package com.bartovapps.audiorecorder.launcher

/**
 * Created by motibartov on 08/10/2017.
 */

import com.bartovapps.audiorecorder.BasePresenter
import com.bartovapps.audiorecorder.BaseView

interface MainContract{

    interface View : BaseView<BasePresenter>{
        fun showPlaybackStopped()
        fun showRecordingStopped()
        fun initButtons()
        fun showFileInfo(path: String, size: String)
        fun showAudioDuration(duration: Long)
        fun updateProgress(progress: Int)
    }

    interface Presenter : BasePresenter {
        fun startRecordClicked()
        fun stopRecordClicked()
        fun startPlayClicked()
        fun stopPlaybackClicked()
        fun onAmrRdChecked()
        fun onMp3RdChecked()
        fun onWavRdChecked()
    }
}