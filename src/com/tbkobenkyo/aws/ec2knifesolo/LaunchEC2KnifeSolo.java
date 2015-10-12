package com.tbkobenkyo.aws.ec2knifesolo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.tbkobenkyo.aws.ec2knifesolo.common.Utilities;

/**
 * @author shimapee
 *	EC2インスタンスを作成して、Knife-Solo実行
 */
public class LaunchEC2KnifeSolo {

	/**
	 * @param args
	 * 	メインメソッド
	 * 	第１引数：インスタンス数
	 *  第２引数：インスタンスのベース名(ベース名＋インスタンス数が付与されます。)
	 */
	public static void main(String[] args) {
		
		int launchInstanceNumber = 0;
		String launchInstanceCoreName = null;
		
		// 引数が一つも無ければエラー
		if(args.length != 2) System.exit(1);

		// 引数から、作成インスタン個数、インスタンスのベース名を取得
		launchInstanceNumber = Integer.parseInt(args[0]);
		launchInstanceCoreName = args[1];

		
		/* 認証処理 */
		AmazonEC2 ec2 = new AmazonEC2Client(new ClasspathPropertiesFileCredentialsProvider());
		
		/* リージョン設定 */
		ec2.setRegion(Region.getRegion(Regions.valueOf(Utilities.getValue("REAGION"))));
		
		/* EC2インスタンス情報設定 */
		RunInstancesRequest request = new RunInstancesRequest()

		.withImageId(Utilities.getValue("INSTANCEIMAGE"))				// インスタンスイメージ
		.withMinCount(launchInstanceNumber)								// インスタンスの最小数
		.withMaxCount(launchInstanceNumber)								// インスタンスの最大数
		.withSecurityGroupIds(Utilities.getValue("SECGROUP"))			// SecurityGroupの設定	
		.withKeyName(Utilities.getValue("KEY"))							// 秘密鍵の設定
		.withInstanceType(InstanceType.valueOf(Utilities.getValue("INSTANCETYPE")));	// インスタンスタイプ選択

		/* インスタンス作成 */
		RunInstancesResult result = ec2.runInstances(request);
		
		/* 
		 * インスタンスにタグ付け 
		 *  ここではインスタンス名をつけます。
		 *  インスタンスのListを作成（複数の場合があるので…）
		 *  Listをループで回してタグ付け〜１、〜２…みたいな形
		 */
		List<Instance> instList = result.getReservation().getInstances();
		List<String> instIdList = new ArrayList<String>();
		int idx = 1;
		for(Instance list : instList) {
			String instname = launchInstanceCoreName+idx;
			CreateTagsRequest tag = new CreateTagsRequest()
				.withResources(list.getInstanceId())
				.withTags(new Tag("Name", instname));
			ec2.createTags(tag);
			instIdList.add(list.getInstanceId());
			
			idx++;

		}
		
		/* 作成したインスタンスの情報確認 */
		for(String idList : instIdList) {
			/* ステータスの状態確認 */
			boolean chk = true;
			while(chk) {
				// インスタンス情報取得
				DescribeInstancesResult descInst = ec2.describeInstances(
						new DescribeInstancesRequest().withInstanceIds(idList)
						);
				Instance instance = descInst.getReservations().get(0).getInstances().get(0);	//特定済み
				String stateInst = instance.getState().getName();	// ステータス名取得
				
				// インスタンスがRUNNING状態
				if(InstanceStateName.Running.toString().equalsIgnoreCase(stateInst)) {
					// インスタンスのステータス取得	
					DescribeInstanceStatusResult statusResult = ec2.describeInstanceStatus(
							new DescribeInstanceStatusRequest().withInstanceIds(idList)
							);
					InstanceStatus instStatus = statusResult.getInstanceStatuses().get(0);	// 特定しますた
					String instCheck = instStatus.getInstanceStatus().getStatus();	// インスタンス状態
					String sysCheck = instStatus.getSystemStatus().getStatus();		// システム状態
					
					// インスタンス・システム両方OKなら
					if(instCheck.equals("ok") && sysCheck.equals("ok")) {
						System.out.print(instance.getTags().get(0).getValue());
						System.out.print(":");
						System.out.println(instance.getPublicIpAddress());
						
						/* knife solo実行 */
						String chefrepo = Utilities.getValue("CHEFREPO");
						String key = Utilities.getValue("SSHKEY");
						String user = Utilities.getValue("SSHUSER");
						
						String host = instance.getPublicIpAddress();
						Runtime runtime = Runtime.getRuntime();
						try {
							
							// かなり強引ｗ　jsonファイルを強制作成ｗｗｗ
							String br = System.getProperty("line.separator");
							
							File file = new File(chefrepo + "/nodes/"+ host+".json");
							file.createNewFile();
							FileWriter fileWriter = new FileWriter(file);
							fileWriter.write("{"+br);
							fileWriter.write("  \"run_list\": ["+br);
							// TODO: 複数のrecipeに対応　
							// TODO: 引数とするか…設定ファイルにするか…それが問題だ！
							fileWriter.write("    \"recipe[httpd]\""+br);
							fileWriter.write("  ],"+br);
							fileWriter.write("  \"automatic\": {"+br);
							fileWriter.write("    \"ipaddress\": \""+host+"\""+br);
							fileWriter.write("  }"+br);
							fileWriter.write("}"+br);
							fileWriter.close();
							
							// knife solo
							Process p = runtime.exec("knife solo bootstrap -i "+ key + " " +user+"@"+host, null, new File(chefrepo));
							
							// 結果出力
							InputStream is = p.getInputStream();
							BufferedReader buf = new BufferedReader(new InputStreamReader(is));
							StringBuilder sb = new StringBuilder();
							String line;
							while((line = buf.readLine()) != null) {
								sb.append(line).append(br);
							}
							System.out.println(sb.toString());
							buf.close();
							
						} catch (IOException e) {
							e.printStackTrace();
						}
						chk = false;
						break;
					}
				}
			}
			
			try {
				// ５秒待つ。
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
