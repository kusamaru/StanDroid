<p align="center">
    <img width="100" height="100" src="https://imgur.com/CXmeOTX.png"><br>
    <span style="font-size:20px;font-weight:500">TatimiDroid</span>
</p>


<p align="center" style="font-size:20px;font-weight:300">
(非公式の)Androidで動くニコニコ動画・生放送・実況クライアント<br>
ユーザー生放送では全部屋表示に対応。<br>
Android 5.1以上で利用可能<br>
</p>

<p align="center">
    <img width="200" src="https://imgur.com/Gg7ToYW.png">
    <img width="200" src="https://imgur.com/z9gEtwl.png">
    <img width="230" src="https://imgur.com/HhnLFIG.png">
    <img width="200" src="https://imgur.com/Tun8JNr.png">
</p>

<p align="center" style="font-weight:10">↖最後の立ち見部屋かもしれない</p>
<p align="center" style="font-weight:10">実はGMS無くても動く(GoogleCastは使えないけど)。のでガラホとかGapps焼いてないカスタムROM上でも動く（ただしインストール方法がPlayStoreしか無い）</p>

# 主な機能
- 共通
    - ダークモード
        - できる限り`#000000`なダークモードを作ってる
    - ポップアップ、バックグラウンド再生
    - 動画と生放送は端末内履歴機能が使えます
    - コメントのロックオン機能（指定したユーザーのコメントのみを表示する機能）
    - NG機能、コテハン機能
    - 好きなフォントに変更できる機能
- 動画
    - 動画IDを直接入力して再生
    - ランキング、マイリスト、視聴履歴、投稿動画の表示機能
    - キャッシュ取得（オフライン再生）
        - キャッシュ専用連続再生機能
        - 自分で持ってる動画+コメントファイルの再生機能[詳しくは](https://takusan23.github.io/Bibouroku/2020/04/08/たちみどろいどのキャッシュ機能について/)
    - **暗号化された動画は再生できません。あとコメント投稿機能はないです。**
- 生放送
    - ユーザー放送なら全部屋表示
        - 部屋別表示も可能
    - ギフト、ニコニ広告の表示
    - 参加中コミュ（フォロー中番組）、ランキング、ルーキー番組等の表示機能
    - **ニコ生ゲームは本家でどうぞ**
- 実況
    - ニコ生版ニコニコ実況に対応
    - ポップアップ再生では背景半透明
    - **なんかしらんけど、公式番組なのに流量制限コメントサーバー(ハブられたコメント、store鯖？)へ接続できる（公式番組はAPIが叩けないので本来無理なはず）**
        - ということはコメントが多くなっても全コメント拾える？(ユーザー番組は全部拾ってるはず)

## インストール
https://play.google.com/store/apps/details?id=io.github.takusan23.tatimidroid&hl=ja

## 使い方
初回起動時はログイン画面が表示されるので、ログインしてください。  
ログインで利用したメアド、パスワードは端末内にのみ保存され、ニコニコに再ログインが必要になったときのみ使われます。

# おすすめ機能？

## 生放送編
好きな番組をホーム画面にショートカットとして追加できます。例えばニコニコ実況のNHK総合(`ch2646436`)を登録しておけば何かあったときにすぐに確認できます。  
すべてのAndroidのバージョンで利用できます。  
Android 7以前はホーム画面に追加ダイアログが表示されず、押したら自動的に追加されるようになっています。  
(`CreateShortcutTool#createHomeScreenShortcut()`参照)

<img src="https://imgur.com/6rBlXg1.png" width="200">

登録方法
- 旧UI
    - 再生画面からメニュータブを選んで、ホーム画面に追加を選べば追加できます。

<p>
    <img src="https://imgur.com/UBlKtr9.png" width="200">
    <img src="https://imgur.com/6RkIbbX.png" width="200">
</p>
    
- 新UI
    - 再生画面からちょっとスクロールして、メニュータブを押して、ホーム画面に追加を選べば追加できます。

<p>
    <img src="https://imgur.com/YeOUhZY.png" width="200">
    <img src="https://imgur.com/1YhrcaB.png" width="200">
</p>


## 動画編
考え中

# 開発者向け？

## わたしのAndroid Studio

Canary版Android Studio。Jetpack Composeを使っているんで

Android Studio Arctic Fox | 2020.3.1 Beta 1

## ビルド方法

興味があればどうぞ。

https://tatimidroid.negitoro.dev/dev/build

ビルド方法のドキュメントのリポジトリはここです

https://github.com/takusan23/tatimidroid_docs

## たちみどろいどのIntent
Intentという仕組みを利用することでたちみどろいどを他アプリから起動できます。
MainActivityを指したIntentを飛ばしてください。

バージョン 13.0.4 から使えると思います

### 生放送ID / 動画ID を指定して起動

`Intent`に`putExtra()`で値を渡すことで生放送、動画の再生画面を開くことが可能です。

| name      | value                                              |
|-----------|----------------------------------------------------|
| `liveId`  | 生放送を開く場合は生放送IDかコミュIDかチャンネルID |
| `videoId` | 動画を開く場合は動画ID                             |

以下は最古のきしめんを開く例

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // たちみどろいどのMainActivityを起動させる
        val intent = Intent()
        intent.setClassName("io.github.takusan23.tatimidroid", "io.github.takusan23.tatimidroid.MainActivity")
        intent.putExtra("videoId", "sm157")
        startActivity(intent)
    }
}
```

生放送の場合は`liveId`にすればいいです

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // たちみどろいどのMainActivityを起動させる
        val intent = Intent()
        intent.setClassName("io.github.takusan23.tatimidroid", "io.github.takusan23.tatimidroid.MainActivity")
        intent.putExtra("liveId", "ch2646436") // ニコニコ実況。NHK総合
        startActivity(intent)
    }
}
```


## ビルドまで進めない

- `Invalid Gradle JDK configuration found. Open Gradle Settings`
    - `.idea`フォルダを消してみてください


まあ後でまた書く
