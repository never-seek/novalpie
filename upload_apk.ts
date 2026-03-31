import { S3Storage } from "coze-coding-dev-sdk";
import { readFileSync } from "fs";

async function main() {
  const apkPath = "/workspace/projects/novelapp/novalpie-app/android/app/build/outputs/apk/debug/app-debug.apk";
  
  // 读取 APK 文件
  const fileContent = readFileSync(apkPath);
  console.log(`📦 APK 大小: ${(fileContent.length / 1024 / 1024).toFixed(2)} MB`);

  // 初始化存储
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 上传文件
  console.log("📤 正在上传 APK...");
  const key = await storage.uploadFile({
    fileContent,
    fileName: "novalpie-app-debug.apk",
    contentType: "application/vnd.android.package-archive",
  });
  console.log(`✅ 上传成功，key: ${key}`);

  // 生成签名 URL（7天有效期）
  const downloadUrl = await storage.generatePresignedUrl({
    key,
    expireTime: 7 * 24 * 60 * 60,
  });

  console.log("\n========================================");
  console.log("🎉 APK 下载链接（7天有效）：");
  console.log(downloadUrl);
  console.log("========================================\n");
}

main().catch(console.error);
