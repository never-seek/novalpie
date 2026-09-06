# 本人个人页分片审计

2026-09-07；原外观保留，数据和操作迁到`ProfileViewModel/WebsiteProfileRepository`，App外壳移除约360行旧load/mutation。

## 回归

- 78483三个合成测试先全红：慢hero覆盖输入草稿、装备使另一上传分区回包失效、切账号收旧保存回包。24130修后全绿。
- 85175确认失败装备同样会作废最初hero/inventory读取；改为成功之后独立刷新，Profile5项通过。
- 14988两个协议测试红：资料PATCH明确拒绝仍返回本地草稿当成功；装备/购买的外层success:false被data对象遮蔽。已修源明确拒绝与共享ack，22233全量133suites/1003tests/0失败、Debug/AndroidTest通过。
- 写入成功后读回失败使用“操作已提交成功，部分资料刷新失败，不要重复提交”；原用户资料保留，不伪报重新扣费操作失败。

## MuMu实机（不是最终优化包）

APK `495845b314a5acbe2064d9a5e758548e09241e8028f68fea8a178e2dc75b6d42`，Android15/360dp/font1.0；install-r、cold2.762s。

- 头像及外层头像框、已装备badge可見；截图`20260907-profile-feature.png`。
- 本人动态实际有新帖子/评论，筛选分区可达；`20260907-profile-activities.png`。
- 上传书封面/完整书名/作者与2/3/4列入口存在；`20260907-profile-books.png`。
- 仓库真实不同badge、头像框与已装备状态分别显示；`20260907-profile-inventory.png`。
- 签到实际215天、按日期记录含当日；没有主动点击签到/购买/装备/成年验证。
- 用户名输入框临时追加`_B7_QA`，**不保存**→上滑刷新→回编辑区，草稿仍`seeking_B7_QA`；截图`20260907-profile-draft-retained.png`。随后精确删6字符还原`seeking`并收起表单，账号原资料不变。
- 上述截图已逐张查看，没有凭截图推断普通账号管理员权限或动画全部帧通过。

## 待继续

- 普通账号/管理员权限矩阵、所有写入受控回验、500/401/取消/账号切换组合；读后刷新与快速连续不同动作的最终状态。
- 公开他人主页仍单独迁移，旧ProfilePresentation中的接口not-implemented空态降级需按当前v2数据再核对。
- 小宽/大系统字/键盘/返回矩阵和真实badge动图时序，最终优化包复验。
