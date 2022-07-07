package com.google.android.gms.wearable

abstract class NodeClient : Any() {

    abstract val localNode: com.google.android.gms.tasks.Task<Node>
    abstract val connectedNodes: com.google.android.gms.tasks.Task<List<Node>>
}