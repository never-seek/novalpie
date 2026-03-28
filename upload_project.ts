import { S3Storage } from "coze-coding-dev-sdk";
import { execSync } from "child_process";
import { readFileSync } from "fs";
import { join } from "path";

async function main() {
  const workspacePath = process.env.COZE_WORKSPACE_PATH || "/workspace/projects";
  const projectPath = join(workspacePath, "novelapp/novalpie-app");
  const outputPath = "/tmp/novalpie-app.zip";

  // 打包项目
  console.log("📦 正在打包项目...");
  execSync(`cd ${projectPath} && zip -r ${outputPath} . -x "node_modules/*" -x ".git/*" -x "android/.gradle/*" -x "android/app/build/*" -x "*.apk"`, {
    stdio: "inherit"
  });
  console.log("✅ 打包完成");

  // 读取文件
  const fileContent = readFileSync(outputPath);

  // 初始化存储
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 上传文件
  console.log("📤 正在上传到对象存储...");
  const key = await storage.uploadFile({
    fileContent,
    fileName: "novalpie-app.zip",
    contentType: "application/zip",
  });
  console.log(`✅ 上传成功，key: ${key}`);

  // 生成签名 URL（7天有效期）
  const downloadUrl = await storage.generatePresignedUrl({
    key,
    expireTime: 7 * 24 * 60 * 60, // 7天
  });

  console.log("\n========================================");
  console.log("🎉 项目下载链接（7天内有效）：");
  console.log(downloadUrl);
  console.log("========================================\n");
}

main().catch(console.error);
