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

Flutter模块目录结构如下：撰写

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

### FlutterEngineGroup如何使用

## FlutterFragment和FlutterActivity的使用和封装

## MethodChannel的使用和封装

### MethodChannel如何使用

### 封装MethodChannel

## 生命周期之谜

### 什么时候能拿到数据

### 怎么知道FlutterActivity打开了

## 写在最后
