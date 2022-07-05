package com.google.android.gms.wearable

import com.google.android.gms.tasks.Task


abstract class Wearable {


    class getNodeClient(any: Any?) : NodeClient() {
        override val localNode: Task<Node> = null!!
        override val connectedNodes: Task<List<Node>> = null!!
    }

    class getMessageClient(any: Any) {
        fun sendMessage(var1: Any?, var2: Any?, var3: Any?) {}
    }


}
