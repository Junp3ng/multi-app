package com.junp3ng.myapplication.flutter.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.flutter.FlutterInjector;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.FlutterEngineGroup;
import io.flutter.embedding.engine.dart.DartExecutor;

public abstract class BasicFlutterActivity extends FlutterActivity {

    BasicFlutterActivity(){
        super();
    }

    final List<AbstractMethodChannel> channels = new ArrayList<>();
    /**
     * 需要绑定的<code>AbstractMethodChannel</code>列表，在<code>onCreate</code>中调用
     * @return 需要绑定的通道列表
     */
    abstract List<AbstractMethodChannel> channelNeeded();

    /**
     * 为FlutterEngine提供入口函数名
     * @return dart入口函数名
     */
    abstract String provideEntryPointFunctionName();

    /**
     * 在<code>provideFlutterEngine</code>中使用，建议使用外部的FlutterEngineGroup
     * @return FlutterEngineGroup用于创建FlutterEngine
     */
    abstract FlutterEngineGroup engineCreator();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        channels.addAll(channelNeeded());
        for (AbstractMethodChannel channel: channels) {
            channel.attach();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (AbstractMethodChannel channel: channels) {
            channel.detach();
        }
    }

    Boolean shouldCacheFlutterEngine() {
        return false;
    }

    @Override
    public boolean shouldDestroyEngineWithHost() {
        return !shouldCacheFlutterEngine();
    }

    final String provideEngineId() {
        return provideEntryPointFunctionName();
    }

    @Nullable
    @Override
    final public FlutterEngine provideFlutterEngine(@NonNull Context context) {
        if (shouldCacheFlutterEngine() && FlutterEngineCache.getInstance().contains(provideEngineId())) {
            return FlutterEngineCache.getInstance().get(provideEngineId());
        }
        DartExecutor.DartEntrypoint entrypoint = new DartExecutor.DartEntrypoint(FlutterInjector.instance().flutterLoader().findAppBundlePath(), provideEntryPointFunctionName());
        FlutterEngine engine;
        if (shouldCacheFlutterEngine()) {
            engine = engineCreator().createAndRunEngine(context.getApplicationContext(), entrypoint);
            FlutterEngineCache.getInstance().put(provideEngineId(), engine);
        } else {
            engine =  engineCreator().createAndRunEngine(context, entrypoint);
        }
        return engine;
    }
}
