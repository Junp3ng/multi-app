package com.junp3ng.myapplication.flutter.activity;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;

public abstract class AbstractMethodChannel implements MethodChannel.MethodCallHandler {

    final MethodChannel channel;

    AbstractMethodChannel(BinaryMessenger messenger, String name){
        channel = new MethodChannel(messenger, name);
    }

    void attach() {
        channel.setMethodCallHandler(this);
    }

    void detach() {
        channel.setMethodCallHandler(null);
    }

}
