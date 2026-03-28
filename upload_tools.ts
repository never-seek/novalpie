import { S3Storage } from "coze-coding-dev-sdk";
import * as fs from "fs";

async function uploadTools() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  const files = [
    {
      path: "/root/clash/clash",
      name: "tools/clash/clash-linux-amd64",
      contentType: "application/octet-stream",
      desc: "Clash 代理二进制文件"
    },
    {
      path: "/root/clash/config.yaml",
      name: "tools/clash/config.yaml",
      contentType: "application/yaml",
      desc: "Clash 配置文件"
    },
    {
      path: "/tmp/cmdline-tools.zip",
      name: "tools/android/commandlinetools-linux-11076708.zip",
      contentType: "application/zip",
      desc: "Android SDK Command-line Tools"
    }
  ];

  console.log("========================================");
  console.log("开始上传工具文件到对象存储");
  console.log("========================================\n");

  const results: { name: string; key: string; url: string }[] = [];

  for (const file of files) {
    if (!fs.existsSync(file.path)) {
      console.log(`跳过 ${file.name}: 文件不存在`);
      continue;
    }

    const fileContent = fs.readFileSync(file.path);
    const size = (fileContent.length / 1024 / 1024).toFixed(2);

    console.log(`上传: ${file.name} (${size} MB)`);

    const key = await storage.uploadFile({
      fileContent,
      fileName: file.name,
      contentType: file.contentType,
    });

    // 生成 10 年有效期的链接
    const url = await storage.generatePresignedUrl({
      key,
      expireTime: 315360000, // 10年
    });

    console.log(`  ✓ ${file.desc}`);
    console.log(`  ✓ Key: ${key}`);
    console.log(`  ✓ URL: ${url}\n`);

    results.push({ name: file.name, key, url });
  }

  console.log("========================================");
  console.log("上传完成！文件清单:");
  console.log("========================================\n");

  for (const r of results) {
    console.log(`${r.name}:`);
    console.log(`  wget "${r.url}" -O ${r.name.split('/').pop()}`);
    console.log("");
  }

  return results;
}

uploadTools().catch(console.error);
