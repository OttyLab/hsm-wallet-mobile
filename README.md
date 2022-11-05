# Lite HSM wallet for Flow
## 概要

本プロジェクトはブロックチェーンにおける安全な秘密鍵管理を安価に提供することを目的とする。
ここではFlowブロックチェーンを対象とし、Android端末でウォレットを作成した。

## 背景

Bintcoinの誕生以来、秘密鍵の安全な管理が課題として存在する。
一般にはコールドウォレットの安全性が高く、ペーパーウォレットやハードウェアウォレットが使用される。
ただし、ペーパーウォレットは盗難、ハードウェアウォレットは故障のリスクがある。
また、ハードウェアウォレットは故障のリスク回避としてニーモニックのバックアップ機能が提供されるが、その場合はニーモニック盗難のリスクがある。

これらのリスクを回避するためにはHSM (Hardware Security Module)が有効である。
一般にHSMは秘密鍵をハードウェア内部に保存し、外部から直接アクセスする方法がない。
そのため、秘密鍵が盗まれることはなく、権限を持つユーザーのみが安全に署名を行うことができる。

しかし、Bitcoinに代表される多くのブロックチェーンでは公開鍵から直接導出されるアドレスを使用する。
この場合、秘密鍵のバックアップが取れないHSMを使用することは故障に対するリスクを負うこととなる（そのため、ハードウェアウォレットはニーモニックのバックアック機能を提供している）。
したがって、キーペアと独立したアドレスを使用し、キーペアが任意にキーローテーションできることがHSMを利用する必要条件となる。


## デバイス

HSMは一般に高価であるため、より安価なシステムが求められる。
そこで今回着目したのがAndroid端末が提供する[StrongBox Keymaster](https://source.android.com/docs/security/best-practices/hardware?hl=ja)である。
StrongBox Keymasterはハードウェア的に独立した暗号機能を提供しており、秘密鍵もそのハードウェア内部で管理される。
つまり、HSMと同様に秘密鍵に対して外部からアクセスする手段が存在しない。

## 対象チェーン

StrongBox Keymasterが提供する暗号アルゴリズムは制限が強く、楕円関数としては[P-256のみ](https://developer.android.com/training/articles/keystore?hl=ja#HardwareSecurityModule)である。
また、ハッシュ関数として[SHA-3も提供されない](https://developer.android.com/training/articles/keystore?hl=ja#SupportedSignatures)。
これらの暗号の条件を満たし、かつ上記のキーペアと独立したアドレス体系を使用するチェーンとしてFlowブロックチェーンが挙げられる。

## Flowのアカウント管理

Flowはアカウントをオンチェーンで管理している。ウォレットはキーペアを作成し、トランザクションを発行してコントラクトを呼び出すことでアカウントの作成およびアカウトで使用する公開の紐付けをオンチェーン上で行う。
アカウントに対して公開鍵を追加・削除することでキーローテーションを行うことができる。

トランザクション発行時にガス代を支払う必要があるが、ウォレット作成時にはアカウントが存在しないためガス代を支払うことができない。
そこで、Flowでは一般的にウォレット業者がガス代を肩代わりする。
本プロジェクトではテストネット向けにトランザクションを代理で発行する[バックエンドを提供](https://github.com/OttyLab/hsm-wallet-backend)している。

## 本ウォレットの挙動

本ウォレットはStrongBox KeymasterとFlowの機能を最大限利用する。
特に、作成したアカウントに対して複数台の端末で管理する公開鍵を紐付けることで、秘密鍵を漏洩することなくアカウントのバックアップが可能である。

1. 初回起動時はWelcome画面が表示
2. CREATE ACCOUNT（一台目）かSTART AS BACKUP（二台目以降）を選択

### CREATE ACCOUNT
1. ウォレットがキーペアを作成し、バックエンドに依頼してアカウントを作成
2. Home画面に遷移し、使用可能となる
3. Transferではトランザクション作成時に署名のための生体認証必要

### START AS BACKUP（二台目）
1. ユーザーが既存アカウントのアドレスを入力
2. ウォレットがキーペアを作成し、公開鍵を表示
3. ユーザーが一台目の設定画面で公開鍵を入力
4. 一台目のウォレットがトランザクションを発行し、公開鍵をアカウントに紐付け
5. 二台目でもホーム画面に遷移して使用可能

## 動画

[CREATE ACCOUNT](./img/1.create_s.mp4)
[送金](./img/2.transfer_s.mp4)
[START AS　BACKUP](./img/3.backup_s.mp4)
[公開鍵追加](./img/4.add_s.mp4)