# 将Flutter模块加入现有Android应用

## 写在开始

> 本文主要是这段时间进行Flutter开发时的经验总结。使用Flutter是看上了它的跨平台功能，指望能写一份代码，同时能在Android和iOS上运行。由于当前Android和iOS项目代码已经十分庞大，整个项目用Flutter重写太费时，我们选择用Flutter模块导入现有应用的方式来使用Flutter。
>
> 本文使用的Flutter版本是2.2.3

```bash
Flutter 2.2.3 • channel stable • https://github.com/flutter/flutter.git
Framework • revision f4abaa0735 (7 周前) • 2021-07-01 12:46:11 -0700
Engine • revision 241c87ad80
Tools • Dart 2.13.4
```

### 学习资料整理

* [Flutter官方中文网](https://flutter.cn/)
* [《Flutter实战》](https://book.flutterchina.club/)
* [B站上的Google中国](https://space.bilibili.com/64169458?from=search&seid=14407446110849580877)里的Flutter Widgets介绍合集

## Flutter模块的创建、关联及版本控制

首先创建新的Android项目，名为`multi_app_android`

目录结构如下：

```bash
multi_app_android
├── app
│   ├── build.gradle
│   ├── libs
│   ├── proguard-rules.pro
│   └── src
├── build.gradle
├── gradle
│   └── wrapper
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
└── settings.gradle
```



### 如何创建Flutter模块

> 创建Flutter模块有两种方式：
>
> 1. 通过`flutter create --project-name multi_app_flutter_module --template module ./multi_app_flutter_module`创建
> 2. Android Studio中Flie>New>New Module>Flutter Module可以创建

通过上述的1，我在`./multi_app_flutter_module`中创建了Flutter模块

Flutter模块目录结构如下：

```shell
multi_app_flutter_module
├── README.md
├── lib
│   └── main.dart
├── multi_app_flutter_module.iml
├── multi_app_flutter_module_android.iml
├── pubspec.lock
├── pubspec.yaml
└── test
    └── widget_test.dart
```

### 如何将Flutter模块关联到现有Android应用

> 通过Android Studio操作：File>New>New Module>import Flutter Module
>
> 然后选择刚刚创建的Flutter模块即可

关联完后有如下修改：

`multi_app_android/app/build.gradle`中自动添加了依赖`implementation project(path: ':flutter')`

`multi_app_android/setting.gradle`增加了如下代码:

```groovy
// ...
setBinding(new Binding([gradle: this]))
evaluate(new File(
  settingsDir,
  '../multi_app_flutter_module/.android/include_flutter.groovy'
))
// 下面这两行去掉也可以，加上下面这两行，就能在Android工程里看见flutter模块了
include ':multi_app_flutter_module'
project(':multi_app_flutter_module').projectDir = new File('../multi_app_flutter_module')

```

### 关于版本控制的一点思考

如果是自己开发，使用关联的代码通常不会有什么问题，但如果这个项目有多人开发，就需要所有人都把flutter模块放到与Android工程平级的位置。这种限制无可厚非，但我认为还有更好的做法。

我们可以观察到`evaluate()`方法传入的一个`File`路径是`multi_app_flutter_module/.android/include_flutter.groovy`,

我们可以在`setting.gradle`中自己写一个方法，来获取上述的`File`对象。修改后的`setting.gradle`如下：

```groovy
include ':app'

File multi_app_flutter_module_dir = getMultiAppFlutterModule()
setBinding(new Binding([gradle: this]))
evaluate(new File(
  multi_app_flutter_module_dir,
  '.android/include_flutter.groovy'
))
rootProject.name = "multi_app_android"

include ':multi_app_flutter_module'
project(':multi_app_flutter_module').projectDir = multi_app_flutter_module_dir

File getMultiAppFlutterModule() {
    Properties properties = new Properties()
    properties.load((new File(settingsDir, "local.properties")).newDataInputStream())
    String path = properties.get("multi_app_flutter_module.path");
    if (path == null || path.isEmpty()) {
        throw new IllegalArgumentException("请在local.properties中配置multi_app_flutter_module.path属性，multi_app_flutter_module.path=<flutter模块根目录地址>")
    }
    return new File(path)
}
```

然后再在Android工程根目录下`local.properties`中配置本地的flutter模块的位置：

`multi_app_flutter_module.path=../multi_app_flutter_module`

最后sync一下就可以了。

## 有关FlutterEngine

### FlutterEngine是干嘛的

> FlutterEngine是Flutter与原生平台之间的桥梁，负责Flutter页面渲染、Flutter与原生平台通信、提供启动Flutter页面的入口等。具体使用方法可以移步[官网](https://flutter.cn/docs/development/add-to-app/android/add-flutter-screen)查看。

`FlutterEngine`的使用分三步走：

1. 创建`FlutterEngine`实例。
2. 调用`FlutterEngine.getDartExecutor().executeDartEntryPoint`运行Flutter入口函数。此时main.dart中的入口函数（默认是`main()`）已经被调用了，但页面未显示出来，相当于渲染了一个宽高为0的页面。
3. 将`FlutterEngine`附属到`FlutterActivity`,`FlutterFragment`,`FlutterView`上，此时Flutter页面才有宽高。这一步如何做会在接下来的`FlutterFragment`和`FlutterActivity`的介绍中提及。

在第二步的时候，可以设置入口函数，比如你希望用`dev()`作为入口函数，具体操作如下：

Native:

1. java

```java
FlutterEngine flutterEngine = new FlutterEngine(context);
String appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
String functionName = "dev";
DartExecutor.DartEntrypoint entrypoint = new DartExecutor.DartEntrypoint(appBundlePath, functionName);
flutterEngine.getDartExecutor().executeDartEntrypoint(entrypoint);
```

2. kotlin

```kotlin
val flutterEngine = FlutterEngine(context)
val appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
val functionName = "dev"
val entrypoint = DartExecutor.DartEntrypoint(appBundlePath, functionName)
flutterEngine.dartExecutor.executeDartEntrypoint(entrypoint)
```

Flutter:main.dart

```dart
void main() => runApp(MyApp());

@pragma("entry-point")
void dev() => runApp(YourDevApp());
```

需要注意的是，`main.dart`中的入口函数需要增加`@pragma("entry-point")`注解，函数名需要和Native中设置的`functionName`一致。

### FlutterEngineGroup如何使用

> Flutter2.0新增了`FlutterEngineGroup`类，能大幅减少新建`FlutterEngine`的内存消耗，官网上说从 Android 上 **约 19MB**，iOS 上 **约 13MB**，降至 **约 180kB**

使用`FlutterEngineGroup`创建`FlutterEngine`的方式如下:

```java
FlutterEngineGroup engineGroup = new FlutterEngineGroup(context);
String appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
String entryPointFunctionName = "dev";
DartExecutor.DartEntrypoint entrypoint = new DartExecutor.DartEntrypoint(appBundlePath, entryPointFunctionName);
FlutterEngine engine = engineGroup.createAndRunEngine(context, entrypoint);
```

使用`FlutterEngineGroup`创建第一个`FlutterEngine`时与直接实例化`FlutterEngine`没有区别，但创建第二个`FlutterEngine`的内存消耗就大幅减少了。

`createAndRunEngine`方法源码：

```java
public FlutterEngine createAndRunEngine(
      @NonNull Context context, @Nullable DartEntrypoint dartEntrypoint) {
    FlutterEngine engine = null;

    if (dartEntrypoint == null) {
      dartEntrypoint = DartEntrypoint.createDefault();
    }
		// FlutterEngineGroup内部维护一个List<FlutterEngine>，来管理由此FlutterEngineGroup产生的FlutterEngine
    // 从这里看，当activeEngines的长度为0时，使用普通的方式创建FlutterEngine
    // 不然就使用第一个的FlutterEngine来生产一个
    if (activeEngines.size() == 0) {
      engine = createEngine(context);
      engine.getDartExecutor().executeDartEntrypoint(dartEntrypoint);
    } else {
      engine = activeEngines.get(0).spawn(context, dartEntrypoint);
    }

    activeEngines.add(engine);
    // 如果这个FlutterEngine调用了destroy方法，则从activeEngines中移除此引擎
    final FlutterEngine engineToCleanUpOnDestroy = engine;
    engine.addEngineLifecycleListener(
        new FlutterEngine.EngineLifecycleListener() {

          @Override
          public void onPreEngineRestart() {
            // No-op. Not interested.
          }

          @Override
          public void onEngineWillDestroy() {
            activeEngines.remove(engineToCleanUpOnDestroy);
          }
        });
    return engine;
  }
```

## FlutterEngine和FlutterView的绑定和解绑

> 这一小节是对FlutterEngine的绑定和解绑的源码分析，从源码中可以得出两个信息：
>
> 1. 重写`provideFlutterEngine`可以为`FlutterFragment`和`FlutterActivity`指定`FlutterEngine`;
> 2. 重写`shouldDestroyEngineWithHost`可以控制`FlutterEngine`在与`FlutterActivity`和`FlutterFragment`解绑的同时销毁;

### FlutterEngine和FlutterView的绑定过程

> 从[《将Flutter集成到现有应用》](https://flutter.cn/docs/development/add-to-app)可知，展示Flutter页面需要用到`FlutterActivity`,`FlutterFragment`以及`FlutterView`
>
> 实际上不论是`FlutterActivity`还是`FlutterFragment`，`FlutterEngine`实际上都与`FlutterView`绑定，也就是说，实际上负责展示Flutter页面内容的是`FlutterView`。
>
> `FlutterFragment`和`FlutterActivity`都实例化了`FlutterActivityAndFragmentDelegate`负责FlutterView创建和FlutterEngine的获取和绑定。

`FlutterActivity`和`FlutterFragment`分别在`onCreate`和`onCreateView`回调时直接或间接调用了`FlutterActivityAndFragmentDelegate.onCreateView`来创建`FlutterView`以及将其与`FlutterEngine`绑定。

```java
// FlutterActivityAndFragmentDelegate
View onCreateView(
  LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
  Log.v(TAG, "Creating FlutterView.");
  ensureAlive();  // 这里检查host是否为null，如果host为null，抛出异常
	
  // 根据设置的渲染模式，初始化FlutterView
  if (host.getRenderMode() == RenderMode.surface) {
    FlutterSurfaceView flutterSurfaceView =
        new FlutterSurfaceView(
            host.getActivity(), host.getTransparencyMode() == TransparencyMode.transparent);
    host.onFlutterSurfaceViewCreated(flutterSurfaceView);
    flutterView = new FlutterView(host.getActivity(), flutterSurfaceView);
  } else {
    FlutterTextureView flutterTextureView = new FlutterTextureView(host.getActivity());
    host.onFlutterTextureViewCreated(flutterTextureView);
    flutterView = new FlutterView(host.getActivity(), flutterTextureView);
  }

  // ......这些不重要......
  
  // FlutterSplashView实际是一个FrameLayout，在这里会将FlutterView添加到布局里
  flutterSplashView.displayFlutterViewWithSplash(flutterView, host.provideSplashScreen());

  Log.v(TAG, "Attaching FlutterEngine to FlutterView.");
  flutterView.attachToFlutterEngine(flutterEngine);  // 在这里将FlutterView与FlutteEngine绑定

  return flutterSplashView;
}
```

`FlutterEngine`实例的获取是在`FlutterActivityAndFragmentDelegate.onAttach`中调用`setupFlutterEngine`实现

```java
// FlutterActivityAndFragmentDelegate
void setupFlutterEngine() {
  Log.v(TAG, "Setting up FlutterEngine.");

  // ......这些不重要......

  flutterEngine = host.provideFlutterEngine(host.getContext());
  if (flutterEngine != null) {
    isFlutterEngineFromHost = true;
    return;
  }

  // ......这些不重要......
  
  flutterEngine =
      new FlutterEngine(
          host.getContext(),
          host.getFlutterShellArgs().toArray(),
          /*automaticallyRegisterPlugins=*/ false,
          /*willProvideRestorationData=*/ host.shouldRestoreAndSaveState());
  isFlutterEngineFromHost = false;
}
```

从上面的源码可以看出，只有`host.providFlutterEngine`返回`null`时才新建一个`FlutterEngine`。host是一个名曰`Host`的接口，而实现这个接口的只有`FlutterActivity`和`FlutterFragment`，在`FlutterActivityAndFragmentDelegate`初始化时传入。

所以我们可以通过重写`FlutterActivity`和`FlutterFragment`的`provideFlutterEngine`来指定`FlutterEngine`。

### FlutterEngine和FlutterView的解绑过程

> 解绑过程发生在`FlutterActivity`的`release`方法以及`FlutterFragment`的`onDetach`方法中，`release`方法在`Activity.onDestroy`中被调用，而最终都调用了`FlutterActivityAndFragmentDelegate.onDetach`方法。

```java
// FlutterActivityAndFragmentDelegate
void onDetach() {
  Log.v(TAG, "onDetach()");
  ensureAlive();

  // Give the host an opportunity to cleanup any references that were created in
  // configureFlutterEngine().
  host.cleanUpFlutterEngine(flutterEngine);

  // shouldAttachEngineToActivity在FlutterActivity中默认返回true，在FlutterFragment中默认返回false
  if (host.shouldAttachEngineToActivity()) {
    // Notify plugins that they are no longer attached to an Activity.
    Log.v(TAG, "Detaching FlutterEngine from the Activity that owns this Fragment.");
    if (host.getActivity().isChangingConfigurations()) {
      flutterEngine.getActivityControlSurface().detachFromActivityForConfigChanges();
    } else {
      flutterEngine.getActivityControlSurface().detachFromActivity();
    }
  }

  // ......这些不重要......

  if (host.shouldDestroyEngineWithHost()) {
    flutterEngine.destroy();

    if (host.getCachedEngineId() != null) {
      FlutterEngineCache.getInstance().remove(host.getCachedEngineId());
    }
    flutterEngine = null;
  }
}
```

从上面的源码可知，我们可以重写`cleanUpFlutterEngine`来做缓存清理，以及如果`shouldDestroyEngineWithHost`返回`true`则在解绑的同时销毁`FlutterEngine`。

## Native和Flutter的通信

> Native、Flutter之间的通信通过`MethodChannel`实现，详见[MethodChannel](https://api.flutter-io.cn/flutter/services/MethodChannel-class.html)以及[撰写双端平台代码（插件编写实现）](https://flutter.cn/docs/development/platform-integration/platform-channels#codec)
>
> 我们可以把Flutter和Native的通信用C/S模型来理解：Flutter相当于客户端，可以使用服务端(Native)的接口，并接收接口的响应，Native则相当于服务端，负责处理来自客户端(Flutter)的请求。
>
> 这一小节先介绍MethodChannel的基本用法，再介绍自己在开发时做的封装

### MethodChannel基本用法
实现Flutter和Native的通信分三步走：
#### 在Android Native实例化MethodChannel类
在Android中创建MethodChannel对象需要两个参数：`BinaryMessenger messenger`和 `String name`
前者要传FlutterEngine.getDartExecutor()与FlutterEngine做绑定
后者是Native与Flutter约定的通道名称
比如像下面👇这样：
```java
MethodChannel channel = new MethodChannel(flutterEngine.getDartExecutor(), "sample/channel")
```
#### 在Flutter实例化MethodChannel类

在Flutter中创建MethodChannel只需要一个`String name`，代表通道名称
这个名称**需要与Android 中创建的通道名称一致**。

```dart
MethodChannel channel = MethodChannel("sample/channel");
```
#### 给两端的MethodChannel对象分别设置监听

> 发送端通过调用`MethodChannel.invokeMethod`可以发送方法名和参数，接收端则通过设置监听来接收来自发送端的方法名和参数，并给予发送端反馈。

##### Android端

```java
MethodChannel channel = new MethodChannel(engine.getDartExecutor(),"sample/channel");
// 设置监听
channel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
		switch(call.method) {
      case "hello":
        System.out.println("HELLO WORLD");
        result.success(true);
        break;
      default:
        break;
    }
  }
});
// 发送say hello，参数为null
channel.invokeMethod("say hello", null);
        
```

Android的监听回调有两个参数`MethodCall call`和`MethodChannel.Result result`

call里包含了从Flutter传过来的方法名`String method`和参数`Object arguments`；

result用于为Flutter端提供反馈，调用成功则调用`result.success(obj)`失败则调用`reuslt.error(errorCode,errorMessage,errorDetail)`最好每一次接收到回调都通过result给Flutter提供反馈，不然Flutter可能认为这个方法不存在。

此外不论是`channel.invokeMethod`还是`result.success`抑或是`result.error`都**需要在主线程使用**，不然会导致应用崩溃。

##### Flutter/Dart端

```dart
MethodChannel channel = MethodChannel("sample/channel");
    channel.setMethodCallHandler((call) async {
    switch (call.method) {
      case "say hello":
    	print("HELLO I AM DART");
    	break;
  	}
});
channel.invokeMethod("hello", null);
```

Flutter中的监听回调只有一个参数`MethodCall call`，它包含两个参数代表方法名`String method`和参数`dynamic arguments`。

Flutter和Native的`arguments`类型对应关系如下表所示（此表来自官方文档）：

| Dart                       | Java                | Kotlin      | Obj-C                                          | Swift                                   |
| -------------------------- | ------------------- | ----------- | ---------------------------------------------- | --------------------------------------- |
| null                       | null                | null        | nil (NSNull when nested)                       | nil                                     |
| bool                       | java.lang.Boolean   | Boolean     | NSNumber numberWithBool:                       | NSNumber(value: Bool)                   |
| int                        | java.lang.Integer   | Int         | NSNumber numberWithInt:                        | NSNumber(value: Int32)                  |
| int, if 32 bits not enough | java.lang.Long      | Long        | NSNumber numberWithLong:                       | NSNumber(value: Int)                    |
| double                     | java.lang.Double    | Double      | NSNumber numberWithDouble:                     | NSNumber(value: Double)                 |
| String                     | java.lang.String    | String      | NSString                                       | String                                  |
| Uint8List                  | byte[]              | ByteArray   | FlutterStandardTypedData typedDataWithBytes:   | FlutterStandardTypedData(bytes: Data)   |
| Int32List                  | int[]               | IntArray    | FlutterStandardTypedData typedDataWithInt32:   | FlutterStandardTypedData(int32: Data)   |
| Int64List                  | long[]              | LongArray   | FlutterStandardTypedData typedDataWithInt64:   | FlutterStandardTypedData(int64: Data)   |
| Float32List                | float[]             | FloatArray  | FlutterStandardTypedData typedDataWithFloat32: | FlutterStandardTypedData(float32: Data) |
| Float64List                | double[]            | DoubleArray | FlutterStandardTypedData typedDataWithFloat64: | FlutterStandardTypedData(float64: Data) |
| List                       | java.util.ArrayList | List        | NSArray                                        | Array                                   |
| Map                        | java.util.HashMap   | HashMap     | NSDictionary                                   | Dictionary                              |

## 

###  模版模式封装MethodChannel









## 写在最后
