package com.prateekj.snooper

class SnooperShakeListener(
    private val shakeAction: SnooperShakeAction
) : OnShakeListener {

    @Volatile
    private var isSnooperFlowStarted =
        false

    override fun onShake() {

        if (!isSnooperFlowStarted) {

            shakeAction.startSnooperFlow()

            isSnooperFlowStarted = true

            return
        }

        shakeAction.endSnooperFlow()

        isSnooperFlowStarted = false
    }
}
