### 简介
本項目提供了一个开箱即用的手写输入组件HandWritingView。 它本身并不提供手写识别的功能， 而是通过HWREngine调用实际进行手写数据识别的引擎。与此同时，本项目也提供了基于三个手写识别项目的四种引擎， 可以直接在项目中使用。
只要实现HandWritingRecognizer接口，就可向HWREngine提供自定义的识别引擎。

##### 依赖
在项目根目录下的build.gradle里面加入maven中央仓库, 如下所示

    allprojects {
        repositories {
            // 其它仓库, 如google()等
            mavenCentral()
        }
    }

在要使用该库的模块的build.gradle中加入如下依赖即可:

    dependencies {
        // 其它依赖
        implementation 'com.github.charleslzq:hw-view:1.0.0-RC5'
    }

因为本项目是用kotlin写的，在java中调用时需要加上kotlin的库

    org.jetbrains.kotlin:kotlin-stdlib:${versions.kotlin.base}

如果要使用现成的识别引擎，就需要加入相应引擎的依赖。比如使用灵云的识别引擎就需要加入如下依赖：

    com.github.charleslzq:hw-hcicloud-engine:1.0.0-RC5

#### 初始化
已定义的引擎需要向HWREngine注册才能使用， kotlin中注册灵云引擎的代码如下：

    HWREngine.register("hcicloud") {
                HciCloudRecognizer(
                        this,
                        你的灵云开发者key,
                        你的灵云app key
                ).also {
                    it.init()
                }
            }

java代码如下：

    HWREngine.register("hcicloud", new RecognizerBuilder() {
                @Override
                public HandWritingRecognizer build() {
                    hciCloudRecognizer = new HciCloudRecognizer(
                            HwrApplication.this,
                            你的灵云开发者key,
                            你的灵云app key
                    );
                    hciCloudRecognizer.init();
                    return hciCloudRecognizer;
                }
            });

接着就可以将HandWritingView的engine字段（layout中也有相应的属性）设为“hcicloud”来使用灵云引擎进行手写识别。这个字段也可以在运行时更改以使用不同的引擎。
在注册过程中， kotlin的block或者RecognizerBuilder的build方法并不会立刻被调用。它默认会在HandWritingView的engine字段被赋值时进行实际的引擎初始化操作。
因为灵云引擎的初始化较为耗时，所以可能需要提前初始化，在合适的时机调用HWREngine的prepare方法即可， 如下所示：

    HWREngine.prepare("hcicloud")

因为该过程包含联网验证，所以最好放在后台处理。 同时在应用退出时需要释放灵云引擎的资源，
在onTerminate方法中加入如下代码即可：

    hciCloudRecognizer.release();

#### 笔迹识别
在合适的位置使用com.github.charleslzq.hwr.view.HandWritingView即可。它本身是一个ImageView，会将
用户在它上面的触摸轨迹保留下来，并在每次用户手指离开时（写完一画时）将轨迹数据交给识别引擎处理，可以通过
如下方式获取识别结果：

    handWritingView.onCandidatesAvailable(new HandWritingView.ResultHandler() {
                @Override
                public void receive(@NotNull List<Candidate> candidates) {
                    //处理识别出的候选字列表
                }
            });

#### 选字
HandWritingView所返回的Candidate对象包含了选字所需要的所有信息和方法。它所对应的字存储在content字段里，
而当用户选取Candidate对应的字时，调用其select方法即可。用户所选择的字会通过HandWritingView的回调接口发送过来：

    handWritingView.onCandidateSelected(new HandWritingView.CandidateHandler() {
                @Override
                public void selected(@NotNull String content) {
                    //处理用户选择的词
                }
            });

Candidate还有一个bind方法，用来将其与一个Button绑定。该方法实际上将Button的text设为它的content，
并设置Button的OnClickListener，使其在被点击时调用它的select方法。最简单的用法如下所示：

    for (Candidate candidate: candidates) {
        Button button = new Button(candidatesBar.getContext());
        candidate.bind(button);
        candidatesBar.addView(button);
    }