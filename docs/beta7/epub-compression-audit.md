# EPUB 压缩和图片质量核查（2026-09-13）

## 结论

最终本轮结果：新优化包`521be94e...a423d6`已安装。真实44章原文EPUB在MuMu生成并用Librera打开正文/插图；58项ZIP/CRC、44章、6正文图+独立封面完整。与09-10同书原图包比较，7张图的大小和SHA256全部一致，文件6,166,238→5,853,374bytes（减少312,864bytes，约5.07%）。新包50个文本条目解压628,145bytes、打包314,481bytes；mimetype保持20bytes未压缩。此实书只有PNG/JPEG，不冒充GIF/WebP动画播放验证。报告`20260913-lossless-comparison.json`和`20260913-lossless-360214-report.json`。

清理：明确本次测试EPUB在设备和主机各一份，清理命令被工具策略拒绝，未绕行。两份仍保留（各5,853,374bytes），准确路径见`20260913-lossless-metadata-runtime.json`；没有删除其他用户文件。

- 目前没有“图片画质百分比/有损压缩”开关。本轮不擅自改变用户确认的默认原图要求。
- 已安装候选`bfeddab2...911af`的原生下载writer把文本和图片都写为ZIP STORED。图片没有缩放、重新编码或抽取动画首帧；不是偷偷降低画质。
- 本轮源码把章节XHTML、目录、OPF、CSS等文本条目改为ZIP Deflate无损压缩。`mimetype`仍是第一个、未压缩条目；原始图片仍流式写为STORED。
- `EpubWriter`（编辑器纯文字导出）原本已经压缩文本；本次修的是`NativeEpubArchiveWriter`原生书籍下载路径。
- ZIP压缩文本不会改变解压后正文，不属于JPEG/WebP画质重编码。原始二进制图片不经Bitmap解码/压缩，透明层、GIF/WebP动画数据随原始字节保留。

## 为什么图书仍可能很大

JPEG/WebP/PNG/GIF本身已有编码。这里保留原图字节，文本压缩对图片占绝大多数的EPUB收益有限；它发生在本机打包阶段，不减少从服务器获取原图的网络流量。旧23GB→约12.2GB大书变化是修正源导出额外重复插图，不是降低画质。只有经阅读器源数据确认的额外重复才删除，合法重复出现仍保留。

不要将R8后APK只有约3.26MB误认为图片被压小：APK是应用程序，EPUB是用户下载作品，两者不同。

## 验证

- 先修改原“全部条目STORED”的历史实现锁定测试，改成真实ZIP读取验证：文本应使用Deflate并确实变小，mimetype首项保持STORED，正文字符完整，三份PNG/GIF/WebP二进制样例出包后逐字节相同。
- RED29133：无损压缩测试失败（文本仍STORED）；两项离线未知进度及两项管理草稿回归也各自失败，明确区分。
- GREEN7386：writer、下载检查点、reader进度共39tests，0失败/错误。重复中文样例文本105,370bytes→2,170bytes，三份图片合计418bytes完全相同。这是合成高重复样例，不是实际小说压缩率，更不是有效GIF动画解码播放测试。
- 旧完整有效EPUB检查点允许恢复原包：已完成包文本是否压缩不影响内容正确性，不为这次优化强迫重下/重扣授权。
- 后续已通过完整1225tests/173suites、Beta编译、R8/resources shrink、lint0错误5警告，并在521be94e新优化包完成上文实书验收。旧bfeddab2不含本次优化，保留作为历史。

## 标准依据

[W3C EPUB 3.3 OCF ZIP要求](https://www.w3.org/TR/epub-33/#sec-zip-container)允许STORED/Deflate条目；mimetype必须首项、未压缩。实现不引入阅读器不兼容的压缩算法。
