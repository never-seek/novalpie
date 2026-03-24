import os
import re
from pathlib import Path
from typing import Optional, Any, Dict, List, TypedDict, Iterable
from uuid import uuid4

import boto3
from botocore.exceptions import ClientError
from boto3.s3.transfer import TransferConfig
import logging
logger = logging.getLogger(__name__)

# 允许的文件名字符集（面向用户输入的约束）
FILE_NAME_ALLOWED_RE = re.compile(r"^[A-Za-z0-9._\-/]+$")


class ListFilesResult(TypedDict):
    # list_files 的返回结构类型
    keys: List[str]
    is_truncated: bool
    next_continuation_token: Optional[str]

class S3SyncStorage:
    """S3兼容存储实现"""

    def __init__(self, *, endpoint_url: Optional[str] = None, access_key: str, secret_key: str, bucket_name: str, region: str = "cn-beijing"):
        self.endpoint_url = os.environ.get("COZE_BUCKET_ENDPOINT_URL") or endpoint_url or ''
        self.access_key = access_key
        self.secret_key = secret_key
        self.bucket_name = bucket_name
        self.region = region
        self._client = None

    def _get_client(self):
        if self._client is None:
            endpoint = self.endpoint_url
            if endpoint is None or endpoint == "":
                try:
                    from coze_workload_identity import Client as CozeEnvClient
                    coze_env_client = CozeEnvClient()
                    env_vars = coze_env_client.get_project_env_vars()
                    coze_env_client.close()
                    for env_var in env_vars:
                        if env_var.key == "COZE_BUCKET_ENDPOINT_URL":
                            endpoint = env_var.value.replace("'", "'\\''")
                            self.endpoint_url = endpoint
                            break
                except Exception as e:
                    logger.error(f"Error loading COZE_BUCKET_ENDPOINT_URL: {e}")
                    # 保持向下校验逻辑，避免在此处中断
            if endpoint is None or endpoint == "":
                logger.error("未配置存储端点：请设置endpoint_url")
                raise ValueError("未配置存储端点：请设置endpoint_url")

            client = boto3.client(
                "s3",
                endpoint_url=endpoint,
                aws_access_key_id=self.access_key,
                aws_secret_access_key=self.secret_key,
                region_name=self.region,
            )

            # 注册 before-call 钩子，发送前注入 x-storage-token 头
            def _inject_header(**kwargs):
                try:
                    from coze_workload_identity import Client as CozeClient
                    coze_client = CozeClient()
                    try:
                        token = coze_client.get_access_token()
                    except Exception as e:
                        logger.error("Error loading COZE_WORKLOAD_IDENTITY_TOKEN: %s", e)
                        token = None
                        raise e
                    finally:
                        coze_client.close()
                    params = kwargs.get("params", {})
                    headers = params.setdefault("headers", {})
                    headers["x-storage-token"] = token
                except Exception as e:
                    logger.error("Error loading COZE_WORKLOAD_IDENTITY_TOKEN: %s", e)
                    pass
            client.meta.events.register("before-call.s3", _inject_header)
            self._client = client
        return self._client

    def _generate_object_key(self, *, original_name: str) -> str:
        suffix = Path(original_name).suffix.lower()
        stem = Path(original_name).stem
        uniq = uuid4().hex[:8]
        return f"{stem}_{uniq}{suffix}"

    def _extract_logid(self, e: Exception) -> Optional[str]:
        """从 ClientError 中提取 x-tt-logid"""
        if isinstance(e, ClientError):
            headers = (e.response or {}).get("ResponseMetadata", {}).get("HTTPHeaders", {})
            return headers.get("x-tt-logid")
        return None

    def _error_msg(self, msg: str, e: Exception) -> str:
        """构建带 logid 的错误信息"""
        logid = self._extract_logid(e)
        if logid:
            return f"{msg}: {e} (x-tt-logid: {logid})"
        return f"{msg}: {e}"

    def _resolve_bucket(self, bucket: Optional[str]) -> str:
        """统一解析 bucket 来源，确保得到有效桶名。"""
        target_bucket = bucket or os.environ.get("COZE_BUCKET_NAME") or self.bucket_name
        if not target_bucket:
            raise ValueError("未配置 bucket：请传入 bucket 或设置 COZE_BUCKET_NAME，或在实例化时提供 bucket_name")
        return target_bucket

    def _validate_file_name(self, name: str) -> None:
        """校验 S3 对象命名：长度≤1024；允许 [A-Za-z0-9._-/]；不以 / 起止且不含 //。"""
        msg = (
            "file name invalid: 文件名需满足以下 S3 对象命名规范："
            "1) 长度 1–1024 字节；"
            "2) 仅允许字母、数字、点(.)、下划线(_)、短横(-)、目录分隔符(/)；"
            "3) 不允许空格或以下特殊字符：? # & % { } ^ [ ] ` \\ < > ~ | \" ' + = : ;；"
            "4) 不以 / 开头或结尾，且不包含连续的 //；"
            "示例：report_2025-12-11.pdf、images/photo-01.png。"
        )

        if not name or not name.strip():
            raise ValueError(msg + "（原因：文件名为空）")

        # S3 限制对象 key 最大 1024 字节，这里沿用到输入文件名
        if len(name.encode("utf-8")) > 1024:
            raise ValueError(msg + "（原因：长度超过 1024 字节）")

        if name.startswith("/") or name.endswith("/"):
            raise ValueError(msg + "（原因：以 / 开头或结尾）")
        if "//" in name:
            raise ValueError(msg + "（原因：包含连续的 //）")

        # 允许字符集校验
        if not FILE_NAME_ALLOWED_RE.match(name):
            bad = re.findall(r"[^A-Za-z0-9._\-/]", name)
            example = bad[0] if bad else "非法字符"
            raise ValueError(msg + f"（原因：包含非法字符，例如：{example}）")

    def upload_file(self, *, file_content: bytes, file_name: str, content_type: str = "application/octet-stream", bucket: Optional[str] = None) -> str:
        # 先对输入文件名做规范校验，避免生成无效对象 key
        self._validate_file_name(file_name)
        try:
            client = self._get_client()
            object_key = self._generate_object_key(original_name=file_name)
            target_bucket = self._resolve_bucket(bucket)
            client.put_object(Bucket=target_bucket, Key=object_key, Body=file_content, ContentType=content_type)
            return object_key
        except Exception as e:
            logger.error(self._error_msg("Error uploading file to S3", e))
            raise e

    def delete_file(self, *, file_key: str, bucket: Optional[str] = None) -> bool:
        try:
            client = self._get_client()
            target_bucket = self._resolve_bucket(bucket)
            client.delete_object(Bucket=target_bucket, Key=file_key)
            return True
        except Exception as e:
            logger.error(self._error_msg("Error deleting file from S3", e))
            raise e

    def file_exists(self, *, file_key: str, bucket: Optional[str] = None) -> bool:
        try:
            client = self._get_client()
            target_bucket = self._resolve_bucket(bucket)
            client.head_object(Bucket=target_bucket, Key=file_key)
            return True
        except ClientError as e:
            code = (e.response or {}).get("Error", {}).get("Code", "")
            if code in {"404", "NoSuchKey", "NotFound"}:
                return False
            logger.error(self._error_msg("Error checking file existence in S3", e))
            return False
        except Exception as e:
            logger.error(self._error_msg("Error checking file existence in S3", e))
            return False

    def read_file(self, *, file_key: str, bucket: Optional[str] = None) -> bytes:
        try:
            client = self._get_client()
            target_bucket = self._resolve_bucket(bucket)
            resp = client.get_object(Bucket=target_bucket, Key=file_key)
            body = resp.get("Body")
            if body is None:
                raise RuntimeError("S3 get_object returned no Body")
            try:
                return body.read()
            finally:
                try:
                    body.close()
                except Exception as ce:
                    # 资源关闭失败不影响读取结果，仅记录以便排查
                    logger.debug("Failed to close S3 response body: %s", ce)
        except Exception as e:
            logger.error(self._error_msg("Error reading file from S3", e))
            raise e

    def list_files(self, *, prefix: Optional[str] = None, bucket: Optional[str] = None, max_keys: int = 1000, continuation_token: Optional[str] = None) -> ListFilesResult:
        """列出对象，支持前缀过滤与分页；返回 keys/is_truncated/next_continuation_token。"""
        try:
            client = self._get_client()
            target_bucket = self._resolve_bucket(bucket)
            if max_keys <= 0 or max_keys > 1000:
                raise ValueError("max_keys 必须在 1 到 1000 之间")

            kwargs: Dict[str, Any] = {
                "Bucket": target_bucket,
                "MaxKeys": max_keys,
                "Prefix": prefix,
                "ContinuationToken": continuation_token,
            }
            kwargs = {k: v for k, v in kwargs.items() if v is not None}

            resp = client.list_objects_v2(**kwargs)
            contents = resp.get("Contents", []) or []
            keys: List[str] = [item.get("Key") for item in contents if isinstance(item, dict) and item.get("Key")]
            return {
                "keys": keys,
                "is_truncated": bool(resp.get("IsTruncated")),
                "next_continuation_token": resp.get("NextContinuationToken"),
            }
        except ClientError as e:
            code = (e.response or {}).get("Error", {}).get("Code", "")
            logger.error(self._error_msg(f"Error listing files in S3 (code={code})", e))
            raise e
        except Exception as e:
            logger.error(self._error_msg("Error listing files in S3", e))
            raise e

    def generate_presigned_url(self, *, key: str, bucket: Optional[str] = None, expire_time: int = 1800) -> str:
        """通过 S3 Proxy 生成签名 URL。"""
        import json
        import urllib.request as urllib_request
        try:
            from coze_workload_identity import Client as CozeClient
            coze_client = CozeClient()
            try:
                token = coze_client.get_access_token()
            finally:
                try:
                    coze_client.close()
                except Exception:
                    # 资源释放失败不影响后续流程
                    pass
        except Exception as e:
            logger.error(f"Error loading x-storage-token: {e}")
            raise RuntimeError(f"获取 x-storage-token 失败: {e}")
        try:
            sign_base = os.environ.get("COZE_BUCKET_ENDPOINT_URL") or self.endpoint_url
            if not sign_base:
                raise ValueError("未配置签名端点：请设置 COZE_BUCKET_ENDPOINT_URL 或传入 endpoint_url")
            sign_url_endpoint = sign_base.rstrip("/") + "/sign-url"

            headers = {
                "Content-Type": "application/json",
                "x-storage-token": token,
            }

            target_bucket = self._resolve_bucket(bucket)
            payload = {"bucket_name": target_bucket, "path": key, "expire_time": expire_time}
            data = json.dumps(payload).encode("utf-8")
            request = urllib_request.Request(sign_url_endpoint, data=data, headers=headers, method="POST")
        except Exception as e:
            logger.error(f"Error creating request for sign-url: {e}")
            raise RuntimeError(f"创建 sign-url 请求失败: {e}")

        try:
            with urllib_request.urlopen(request) as resp:
                resp_bytes = resp.read()
                content_type = resp.headers.get("Content-Type", "")
                text = resp_bytes.decode("utf-8", errors="replace")
                if "application/json" in content_type or text.strip().startswith("{"):
                    try:
                        obj = json.loads(text)
                    except Exception:
                        return text
                    data = obj.get("data")
                    if isinstance(data, dict) and "url" in data:
                        return data["url"]
                    url_value = obj.get("url") or obj.get("signed_url") or obj.get("presigned_url")
                    if url_value:
                        return url_value
                    raise ValueError("签名服务返回缺少 data.url/url 字段")
                return text
        except Exception as e:
            raise RuntimeError(f"生成签名URL失败: {e}")

    def stream_upload_file(
            self,
            *,
            fileobj,
            file_name: str,
            content_type: str = "application/octet-stream",
            bucket: Optional[str] = None,
            multipart_chunksize: int = 5 * 1024 * 1024,
            multipart_threshold: int = 5 * 1024 * 1024,
            max_concurrency: int = 1,
            use_threads: bool = False,
    ) -> str:
        """流式上传（文件对象）
        - fileobj: 任何带有 read() 方法的文件对象（如 open(..., 'rb') 返回的对象、io.BytesIO 等）
        - file_name: 原始文件名，用于生成唯一 key
        - content_type: MIME 类型
        - bucket: 目标桶；为空时取环境变量或实例默认值
        - multipart_chunksize: 分片大小（默认 5MB，以适配代理层限制）
        - multipart_threshold: 触发分片上传的阈值（默认 5MB）
        - max_concurrency: 并发分片上传的并发数（默认 1，避免代理层节流影响）
        - use_threads: 是否启用线程并发（默认 False）
        返回：最终写入的对象 key
        """
        try:
            client = self._get_client()
            target_bucket = self._resolve_bucket(bucket)
            key = self._generate_object_key(original_name=file_name)

            extra_args = {"ContentType": content_type} if content_type else {}
            # 使用 boto3 的高阶方法执行多段上传（传入 TransferConfig 控制分片大小）

            config = TransferConfig(
                multipart_chunksize=multipart_chunksize,
                multipart_threshold=multipart_threshold,
                max_concurrency=max_concurrency,
                use_threads=use_threads,
            )
            client.upload_fileobj(Fileobj=fileobj, Bucket=target_bucket, Key=key, ExtraArgs=extra_args, Config=config)
            return key
        except Exception as e:
            logger.error(self._error_msg("Error streaming upload (fileobj) to S3", e))
            raise e

    def upload_from_url(
            self,
            *,
            url: str,
            bucket: Optional[str] = None,
            timeout: int = 30,
    ) -> str:
        """从 URL 流式下载并上传到 S3
        - url: 源文件 URL
        - bucket: 目标桶；为空时取环境变量或实例默认值
        - timeout: HTTP 请求超时时间（秒，默认 30）
        返回：最终写入的对象 key
        """
        import urllib.request as urllib_request
        from urllib.parse import urlparse, unquote
        try:
            request = urllib_request.Request(url)
            with urllib_request.urlopen(request, timeout=timeout) as resp:
                parsed = urlparse(url)
                file_name = Path(unquote(parsed.path)).name or "file"
                content_type = resp.headers.get("Content-Type", "application/octet-stream")
                return self.stream_upload_file(
                    fileobj=resp,
                    file_name=file_name,
                    content_type=content_type,
                    bucket=bucket,
                )
        except Exception as e:
            logger.error(self._error_msg("Error uploading from URL to S3", e))
            raise e

    def trunk_upload_file(self, *, chunk_iter: Iterable[bytes], file_name: str,
                           content_type: str = "application/octet-stream", bucket: Optional[str] = None,
                           part_size: int = 5 * 1024 * 1024) -> str:
        """流式上传（字节迭代器，显式分片 Multipart Upload）
        - chunk_iter: 可迭代对象，逐块产生 bytes；每块大小可变（内部累积到 part_size 再上传），最后一块可小于 5MB
        - file_name: 原始文件名，用于生成唯一 key
        - content_type: MIME 类型
        - bucket: 目标桶；为空时取环境或实例默认值
        - part_size: 每个 part 的最小大小（除最后一个）；默认 5MB
        返回：最终写入的对象 key
        """
        client = self._get_client()
        target_bucket = self._resolve_bucket(bucket)
        key = self._generate_object_key(original_name=file_name)

        # 初始化分片上传
        try:
            init_resp = client.create_multipart_upload(Bucket=target_bucket, Key=key, ContentType=content_type)
            upload_id = init_resp["UploadId"]
        except Exception as e:
            logger.error(self._error_msg("create_multipart_upload failed", e))
            raise e

        parts = []
        part_number = 1
        buffer = bytearray()
        try:
            for chunk in chunk_iter:
                if not chunk:
                    continue
                buffer.extend(chunk)
                while len(buffer) >= part_size:
                    data = bytes(buffer[:part_size])
                    buffer = buffer[part_size:]
                    resp = client.upload_part(Bucket=target_bucket, Key=key, UploadId=upload_id, PartNumber=part_number,
                                              Body=data)
                    parts.append({"PartNumber": part_number, "ETag": resp["ETag"]})
                    part_number += 1

            # 上传最后不足 part_size 的余量
            if len(buffer) > 0:
                resp = client.upload_part(Bucket=target_bucket, Key=key, UploadId=upload_id, PartNumber=part_number,
                                          Body=bytes(buffer))
                parts.append({"PartNumber": part_number, "ETag": resp["ETag"]})

            # 完成分片
            client.complete_multipart_upload(
                Bucket=target_bucket,
                Key=key,
                UploadId=upload_id,
                MultipartUpload={"Parts": parts},
            )
            return key
        except Exception as e:
            logger.error(self._error_msg("multipart upload failed", e))
            try:
                client.abort_multipart_upload(Bucket=target_bucket, Key=key, UploadId=upload_id)
            except Exception as ae:
                logger.error(self._error_msg("abort_multipart_upload failed", ae))
            raise e
