package com.google.android.gms.tasks

import com.google.android.gms.wearable.Node


abstract class Task<TResult> {

    abstract fun addOnFailureListener(var1: (List<Node>) -> Unit): Task<TResult>
    abstract fun addOnCanceledListener(var1: (List<Node>) -> Unit): Task<TResult>
    abstract fun addOnSuccessListener(var1: (TResult) -> Unit): Task<TResult>

}
