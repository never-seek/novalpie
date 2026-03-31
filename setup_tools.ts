import { S3Storage } from "coze-coding-dev-sdk";
import { writeFileSync, existsSync, mkdirSync, unlinkSync } from "fs";
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

  // 1. 下载 Android SDK 命令行工具
  await downloadFile(
    storage,
    "tools/android/commandlinetools-linux-11076708_8606417d.zip",
    join(toolsDir, "commandlinetools.zip")
  );

  // 2. 下载 Clash 代理
  mkdirSync(join(toolsDir, "clash"), { recursive: true });
  await downloadFile(
    storage,
    "tools/clash/clash-linux-amd64_ef65dfcd",
    join(toolsDir, "clash/clash")
  );
  await downloadFile(
    storage,
    "tools/clash/config_0420c809.yaml",
    join(toolsDir, "clash/config.yaml")
  );
  await downloadFile(
    storage,
    "tools/clash/Country_75955ce2.mmdb",
    join(toolsDir, "clash/Country.mmdb")
  );

  console.log("\n✅ 所有工具下载完成！");
  
  // 设置权限
  execSync(`chmod +x ${toolsDir}/clash/clash`);
  
  console.log("\n启动 Clash 代理...");
  execSync(`cd ${toolsDir}/clash && (nohup ./clash -f config.yaml > /tmp/clash.log 2>&1 &)`);
  
  // 等待代理启动
  execSync("sleep 3");
  
  // 设置代理环境变量
  console.log("设置代理环境变量...");
  process.env.HTTP_PROXY = "http://127.0.0.1:7890";
  process.env.HTTPS_PROXY = "http://127.0.0.1:7890";
  
  // 检查代理
  console.log("检查代理状态...");
  try {
    execSync("curl -s --proxy http://127.0.0.1:7890 https://www.google.com > /dev/null", { stdio: "inherit" });
    console.log("✅ 代理工作正常！");
  } catch (e) {
    console.log("⚠️ 代理可能未正常工作");
  }
}

main().catch(console.error);
