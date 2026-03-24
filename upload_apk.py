import os
from coze_coding_dev_sdk.s3 import S3SyncStorage

# 初始化存储客户端
storage = S3SyncStorage(
    endpoint_url=os.getenv("COZE_BUCKET_ENDPOINT_URL"),
    access_key="",
    secret_key="",
    bucket_name=os.getenv("COZE_BUCKET_NAME"),
    region="cn-beijing",
)

# APK 文件路径
apk_path = "/workspace/projects/novalpie-app/android/app/build/outputs/apk/debug/app-debug.apk"

# 读取 APK 文件内容
with open(apk_path, "rb") as f:
    apk_content = f.read()

# 上传 APK 文件
file_key = storage.upload_file(
    file_content=apk_content,
    file_name="NovalPie/NovalPie-debug.apk",
    content_type="application/vnd.android.package-archive",
)

# 生成下载链接（有效期 30 天）
download_url = storage.generate_presigned_url(
    key=file_key,
    expire_time=180 * 24 * 60 * 60  # 180 天
)

print(f"File uploaded successfully!")
print(f"Key: {file_key}")
print(f"Download URL: {download_url}")
