import { S3Storage } from "coze-coding-dev-sdk";
import * as fs from "fs";

async function uploadScript() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 上传安装脚本
  const scriptPath = "/workspace/projects/novelapp/install_tools.sh";
  const fileContent = fs.readFileSync(scriptPath);

  console.log("上传一键安装脚本...");

  const key = await storage.uploadFile({
    fileContent,
    fileName: "tools/install_tools.sh",
    contentType: "text/x-sh",
  });

  // 生成 10 年有效期的链接
  const url = await storage.generatePresignedUrl({
    key,
    expireTime: 315360000, // 10年
  });

  console.log("\n========================================");
  console.log("✓ 一键安装脚本已上传");
  console.log("========================================");
  console.log("\n使用方法:");
  console.log(`  wget "${url}" -O install_tools.sh`);
  console.log("  chmod +x install_tools.sh");
  console.log("  ./install_tools.sh");
  console.log("");

  return { key, url };
}

uploadScript().catch(console.error);
