# Issue #41 保守的修正の実装・検証

実施日: 2026-10-03。対象: [PR #40](https://github.com/kusamaru/StanDroid/pull/40) の既存ブランチ `fix/issue-39-login-response-handling`。
変更前HEAD: `8d23ce62411d24ec67cd476c666a65605578c186`。
調査の出発点: `F:/data/LLMCache/StanDroid_Issue41_V4_Codex_Handoff.md` とローカルの `ISSUE_41_REVIEW.md`。元資料・既存の未追跡ファイルは変更していない。

## 修正範囲

- `NicoLiveProgram`: 旧 `embedded-data` は維持し、新 `DAT-csr-data` の4種類の一覧を既存モデルへ変換。提供者は `socialGroup` / `supplier` を区別。新トップ一覧の時刻だけ秒からミリ秒へ変換。
- `CommunityListFragment`: HTTP応答を閉じ、成功後だけリストを置き換え、失敗・キャンセルでも更新表示を解除する。section欠落・`hasError`・未知項目を正常な空一覧にしない。
- `NicoVideoHTML` / `NicoVideoWatchResponse`: 既存GETの旧形式はそのまま返す。V4の識別子がある場合だけ遅延情報を取得し、確認済みの一般投稿者・シリーズなしの形を既存JSONへ変換。nicohistory、配信候補の順序、課金情報、コメント情報を保持。
- 動画再生・バックグラウンド再生・メニュー・NG投稿者・キャッシュ取得/情報更新は、この取得関数へ統一。変換に失敗した情報はキャッシュ保存へ渡さない。オフライン読込には通信を追加しない。
- `NicoLogin` / `NicoLoginTwoFactorAuth`: Cookie名を完全一致で解析し、位置依存を撤去。空・削除済み・期限切れCookieをログイン成功にしない。LocationはHTTP URLの解決を使い、エンコード済みのクエリを手動で復号しない。通信失敗は既存の認証失敗経路へ返す。
- `NicoLiveViewModel`: 再ログイン後に取得したHTMLを返す。再認証は1回まで。認証失敗/MFA開始/HTTP失敗で解析を続けない。
- `TwoFactorAuthLoginActivity`: 成功時に終了し、その後の失敗表示へ進まない。
- `app/build.gradle`: `versionName` を **15.7.5 → 15.7.6**。依頼どおりversionCodeは112のまま。実行時依存の追加・更新はない。

## ponytailで絞った点と制約

画面構成、共有データモデル、Room、Media3、WebSocket、既存キャッシュ形式を変更しない。Watch V3 APIへのフォールバック、新規Repository層、HLSダウンローダーは追加しない。既存キャッシュ取得のHLS保存能力が今回拡張されたわけではない。

V4の非nullシリーズ、チャンネル投稿者、未観測の投稿者公開状態は推測で変換せず、明示的なエラーで保存前に止める。ログインviewerは既存のID・premium・like項目が提供される場合だけ扱い、欠損をfalseなどで補わない。この対応範囲を全V4動画の再生保証とは扱わない。

V4遅延APIが失敗するとV4の再生開始も止まる。期限切れキーの自動再試行は追加しない。大百科有無が未提供のタグでは専用ボタンを隠し、記事不存在を示す値をJSONへ追加しない。

## 検証結果

JDK 11（公式Correttoの一時展開）・既存Android SDK・Gradle 7.2で次を実行:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
BUILD SUCCESSFUL
```

- 単体テスト **17件成功、失敗0、skip0**。既存1件 + Issue #41用16件。
- 旧形式の無変更通過、V4一般動画と既存読取関数、viewer/課金保持、明示的nullと欠落の区別、対応外/不完全/暗号化/サービスエラーの拒否、新旧HTML selector、4種類の空一覧、時刻・公式判定・supplier、Cookie順序・類似名・削除・期限切れを検証。
- テストデータは合成であり、V4実応答の捕捉データではない。
- JVM用 `org.json` とAndroidの数値→文字列変換差を検出し、テスト依存を[Android由来のJSON実装](https://central.sonatype.com/artifact/com.vaadin.external.google/android-json)へ変更。Maven Central追加はこのテスト用moduleだけに限定した。
- 既存の日付読取関数はJDK/Androidで許容するoffset表記が異なる。既存読取関数のJVMテストにはRFC822表記を使用し、別テストでISO offsetの無変更保持を確認した。Androidでの日時描画確認の代わりとはしない。
- `git diff --check` 成功。debug APKのメタデータで `versionName=15.7.6 / versionCode=112` を確認。
- APK: `app/build/outputs/apk/debug/app-debug.apk`。既存のdeprecated/JVM-target等の警告は残る。ビルドツール・SDK・依存の一括更新は行わない。

## 未検証と引き継ぎ

今回の公開匿名 `sm9` は旧形式だったため、V4遅延APIの実応答とV4再生は現在の条件では未確認。ニコ生トップの新形式は公開HTMLで確認済みだが、認証付きフォロー中一覧は未確認。

実アカウントの通常ログイン/MFA、セッション切れ後のニコ生再認証、放送WebSocket接続、動画/生放送の実再生、実機日時表示、既存オフラインキャッシュの再生は未実施。PR #40との組み合わせで既知の一覧・Cookie解析・再認証制御の不具合を修正したが、Issue #41全体の解決確認やcloseは実機確認後に行う。
