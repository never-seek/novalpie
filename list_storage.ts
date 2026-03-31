import { S3Storage } from "coze-coding-dev-sdk";
import { writeFileSync, existsSync, mkdirSync } from "fs";
import { execSync } from "child_process";

async function main() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 列出对象存储中的文件
  const result = await storage.listFiles({ prefix: "", maxKeys: 100 });
  console.log("📦 对象存储中的文件：");
  result.keys.forEach(key => {
    console.log(`  - ${key}`);
  });
}

main().catch(console.error);
