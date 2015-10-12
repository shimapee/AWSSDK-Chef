# AWSSDK-Chef

#### EC2-KnifeSolo

EC2インスタンスを作りつつ、Knife-soloを実行します。  
mainメソッドの第一引数は作成するインスタンス数  
第二引数は作成するインスタンスの基底名  
  基底名＋第一引数の数のインスタンス名が出来ます。  
  
例）3 testが引数  
作成されるインスタンス名：test1,test2,test3  
  
  
  
  
---
#### AwsCredentials.properties  
accessKey = ***************************************  
secretKey = ***************************************  
  
---
#### awssdk.properties  
\# AWS Setting  
REAGION = *リージョン名(東京ならAP_NORTHEAST_1)*  
INSTANCEIMAGE = *インスタンスイメージ名(Amazon Linuxならami-9a2fb89a)*  
SECGROUP = *自分のセキュリティグループ*  
KEY = *自分のキー*  
INSTANCETYPE = *インスタンスタイプ名(T2Microなどなど)*  

\# Knife solo Setting  
CHEFREPO = *自身のローカルのchefリポジトリ*  
SSHKEY = *EC2への接続キー*  
SSHUSER = *EC2への接続ユーザ(デフォルトはe2-user)*  
