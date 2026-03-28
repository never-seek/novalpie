import { S3Storage } from "coze-coding-dev-sdk";
import * as fs from "fs";

async function uploadApk() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 读取 APK 文件
  const apkPath = "novalpie-app/app-debug.apk";
  const fileContent = fs.readFileSync(apkPath);

  // 上传文件
  const key = await storage.uploadFile({
    fileContent,
    fileName: "novalpie-app-debug.apk",
    contentType: "application/vnd.android.package-archive",
  });

  console.log("Upload successful! Key:", key);

  // 生成下载链接（有效期 7 天）
  const downloadUrl = await storage.generatePresignedUrl({
    key,
    expireTime: 604800, // 7 天
  });

  console.log("Download URL:", downloadUrl);
}

uploadApk().catch(console.error);
