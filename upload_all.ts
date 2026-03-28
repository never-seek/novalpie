import { S3Storage } from "coze-coding-dev-sdk";
import * as fs from "fs";

async function uploadAll() {
  const storage = new S3Storage({
    endpointUrl: process.env.COZE_BUCKET_ENDPOINT_URL,
    accessKey: "",
    secretKey: "",
    bucketName: process.env.COZE_BUCKET_NAME,
    region: "cn-beijing",
  });

  // 上传 Country.mmdb
  const mmdbPath = "/root/clash/Country.mmdb";
  if (fs.existsSync(mmdbPath)) {
    console.log("上传 Country.mmdb...");
    const mmdbContent = fs.readFileSync(mmdbPath);
    const mmdbKey = await storage.uploadFile({
      fileContent: mmdbContent,
      fileName: "tools/clash/Country.mmdb",
      contentType: "application/octet-stream",
    });
    const mmdbUrl = await storage.generatePresignedUrl({
      key: mmdbKey,
      expireTime: 315360000,
    });
    console.log(`✓ Country.mmdb: ${mmdbUrl}\n`);
  }

  // 创建工具清单文档
  const readme = `# 开发环境工具清单

## 一键安装

\`\`\`bash
wget "https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/tools/install_tools_b9e1818c.sh?sign=2090073661-14ad3aa8dc-0-04d84609af88c41986868b2eae9aec20ab441b32423b2566836e3e120322b9ff" -O install_tools.sh
chmod +x install_tools.sh
./install_tools.sh
\`\`\`

## 单独下载

### Clash 代理
\`\`\`bash
mkdir -p ~/clash && cd ~/clash
wget "https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/tools/clash/clash-linux-amd64_ef65dfcd?sign=2090073587-53d8e9fdbb-0-6e68d68600c0efafc3c854394b915b6b0f697eabb25ded199e0cb27efde47a17" -O clash
wget "https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/tools/clash/config_0420c809.yaml?sign=2090073588-882c8e86d0-0-8dd66a8e5c6d62c972bf13d5c1da7668615d680ad5a06e8b9d47d5e5b12269e5" -O config.yaml
chmod +x clash
./clash -d .
\`\`\`

### Android SDK Command-line Tools
\`\`\`bash
mkdir -p ~/android-sdk/cmdline-tools
cd /tmp
wget "https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067/tools/android/commandlinetools-linux-11076708_8606417d.zip?sign=2090073598-ba373b415a-0-93ad166ef5a023b80450c1ab67d1e1eb4d90e67c380f57ab4f7c78a44e485e47" -O cmdline-tools.zip
unzip cmdline-tools.zip
mv cmdline-tools ~/android-sdk/cmdline-tools/latest
\`\`\`

## 环境变量设置

安装后需要设置的环境变量：

\`\`\`bash
# 代理
export http_proxy="http://127.0.0.1:7890"
export https_proxy="http://127.0.0.1:7890"
export all_proxy="socks5://127.0.0.1:7891"

# Java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Android SDK
export ANDROID_HOME=/root/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
\`\`\`

## 文件清单

| 文件 | 大小 | 说明 |
|------|------|------|
| tools/clash/clash-linux-amd64 | ~19MB | Clash 代理二进制文件 |
| tools/clash/config.yaml | ~0.5MB | Clash 配置文件 |
| tools/clash/Country.mmdb | ~9MB | GeoIP 数据库 |
| tools/android/commandlinetools-linux-11076708.zip | ~146MB | Android SDK Command-line Tools |
| tools/install_tools.sh | ~3KB | 一键安装脚本 |
`;

  const readmePath = "/workspace/projects/novelapp/TOOLS_README.md";
  fs.writeFileSync(readmePath, readme);
  
  console.log("上传工具清单文档...");
  const readmeContent = fs.readFileSync(readmePath);
  const readmeKey = await storage.uploadFile({
    fileContent: readmeContent,
    fileName: "tools/README.md",
    contentType: "text/markdown",
  });
  const readmeUrl = await storage.generatePresignedUrl({
    key: readmeKey,
    expireTime: 315360000,
  });
  console.log(`✓ README.md: ${readmeUrl}\n`);

  console.log("========================================");
  console.log("所有工具已上传完成！");
  console.log("========================================");
}

uploadAll().catch(console.error);
