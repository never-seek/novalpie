# 原生登录安全验证审计（进行中）

来源：App1871新评论4645及回复2424，用户新装一直“正在加载源站安全验证”，旧版登录后覆盖才能使用。当前Nuxt登录`DJsS0DJ-.js` + `DqTZyg2_.js`确认网站有Turnstile/reCAPTCHA/hCaptcha三方式、verify事件收token再POST原生认证协议；不新增/绕过服务器验证。

## 已复现及修订

- 79111三单测全部红：主文档错误仍Loading、页面已开始显示仍被原生全屏层覆盖、验证码guard/recovery清除共享Cookie/Storage。
- 改非阻挡加载行，onPageCommitVisible/onPageFinished结束加载，主文档网络/HTTP/TLS错误可见，20秒提示超时+手动重载。TLS失败不忽略，不自动重发登录。
- 删除清auth_token Cookie/Storage行为，只在验证码文档realm屏蔽该键读取/修改，不影响共享存储。`captcha-guard.test.cjs`执行真实guard脚本于合成存储，确认外部jar/存储原值保留，1项通过。
- 回填改WebMessageListener只收novalpie.cc主框架，20–16384长度及单次；不把接口暴露给任意外站iframe。过旧System WebView给出更新提示，无不安全后门。
- `93816` AuthCaptchaLifecycle+AuthPresentation 7项通过。必要的网页验证码仍保留原网站三选项，不把整个登录业务退回网页。

## 待验证

当前已登录Edge没有可附着Playwright会话，未创建新浏览器或重登录。Cloudflare技能要求先确认可见挑战后才执行光标脚本；当前没有确认挑战，不运行脚本。接着用App现存会话的Debug专用CaptchaAuditActivity查看真实控件，回填不提交登录且不导出短token，读取前后账号ID仅断言身份不变。

新登录修订尚未安装；`AuthCaptchaLiveDeviceTest`待运行。完整未登录新装/实际提交、账号切换和低系统/旧WebView矩阵仍未闭环，不计Beta7发布通过。

## 2026-09-07 真实源验证回填

55628完整1049unit/Debug/AndroidTest通过，`E3C83B7B331788F75A664F66D562642239226F6255B71A3ECD63653100BED314`已无损安装。87763自动化初验超时：现代Turnstile在封闭Shadow DOM中，顶层querySelector iframe为0，不代表未加载。

只附着现有App WebView的Playwright Android会话核对：/login、ready=complete、turnstile.render存在、三provider按钮可见，真实Cloudflare子frame已加载。未导出Cookie/Storage/验证码。滚动到控件后截图`20260907-captcha-widget-visible.png`确认真人复选控件；普通ADB点击后原生`安全验证已完成`显示，截图`20260907-captcha-token-returned.png`。未调用登录POST。返回时`/profile`未映射到个人页而落回收藏（截图核对后纠正），真实65本收藏可用；`20260907-captcha-profile-retained.xml`文件名虽为profile，不能当UID身份核验证据。前后currentUser身份相等仍由下一自动化断言完成。

Cloudflare桌面光标脚本未运行：目标为MuMu内WebView不是可可靠定位的桌面浏览器，使用已见控件的普通模拟器触控；未绕过人机验证。测试增加封闭控件伴随输入字段判据和固定竖屏测试Activity，待自动化重跑；这不替代真实新账号密码提交/各provider及旧WebView矩阵。

18785仅测试竖屏/观测修订重打包，`a6a571e4...bdc23d32`已无损安装。26103 `AuthCaptchaLiveDeviceTest`通过，报告`20260907-captcha-live-report.json`：/login、WebView高度586、真实控件1、Turnstile/reCAPTCHA/hCaptcha三按钮、existingAccountRetained=true、loginSubmitted=false。本次自动化停在真实控件已显示后取消；前次e3c83b7b人工正常点击已证verify回填，两份证据边界分开记录。
