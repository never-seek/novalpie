import { S3Storage } from "coze-coding-dev-sdk";
import { writeFileSync, existsSync, mkdirSync } from "fs";
import { execSync } from "child_process";
import { join } from "path";

async function downloadFile(storage: S3Storage, key: string, localPath: string) {
  console.log(`📥 下载: ${key}`);
  const buffer = await storage.readFile({ fileKey: key });
  writeFileSync(localPath, buffer);
  console.log(`✅ 已保存到: ${localPath}`);
}

async function main() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  const toolsDir = "/tmp/tools";
  mkdirSync(toolsDir, { recursive: true });

  // 下载 Android SDK 命令行工具
  await downloadFile(
    storage,
    "tools/android/commandlinetools-linux-11076708_8606417d.zip",
    join(toolsDir, "commandlinetools.zip")
  );

  // 下载 Clash 代理
  mkdirSync(join(toolsDir, "clash"), { recursive: true });
  await downloadFile(storage, "tools/clash/clash-linux-amd64_ef65dfcd", join(toolsDir, "clash/clash"));
  await downloadFile(storage, "tools/clash/config_0420c809.yaml", join(toolsDir, "clash/config.yaml"));
  await downloadFile(storage, "tools/clash/Country_75955ce2.mmdb", join(toolsDir, "clash/Country.mmdb"));

  // 设置权限并启动代理
  execSync(`chmod +x ${toolsDir}/clash/clash`);
  execSync(`cd ${toolsDir}/clash && (nohup ./clash -f config.yaml > /tmp/clash.log 2>&1 &)`);
  execSync("sleep 2");
  
  console.log("✅ 工具准备完成！");
}

main().catch(console.error);
