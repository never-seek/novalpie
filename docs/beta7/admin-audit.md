# 管理员模块审计（未完成）

当前计划范围保留源站管理员功能，不实际操作其他用户资产。下面为代码与已有静态协议发现，不是实际危险写入验证。

## 已发现问题

- 原生`normalizeAdminAction`将源ack的success=false按值返回；root`runAdminMutation`只判断协程是否抛异常，未判断success，随后refresh并用默认成功文案。应将明确拒绝转为可显示的失败，不能显示“已通过/已删除”。
- root审核/keys/抓取配置加载仍等待多个接口全部完成才发布；新架构独立ViewModel/Repository尚待迁移。
- root刷新会清actionLoading，读写共用一个serial，切分区时旧写回执可能丢失且同一操作可重复发起；需独立操作身份/账号环境/失败草稿。
- 管理资料cookie/badge编辑器关闭后失败没有完整的继续编辑草稿入口；应按源权限补齐，不把关闭弹窗当成功保存。
- UI仅role显示不等服务授权，当前普通角色真实账号验证仍缺口；不能把管理员截图替代普通角色。

当前没有实际调用批量审核/删除/商城修改/密钥变更。后续用协议服务器及合成UI检验拒绝、分页、取消与身份；如真实写入，只使用明确拥有的受控测试项。

## 2026-09-09 当前实现片

- 84680两红：明确false和缺少ack均未中断调用，旧UI会默认成功。现normalizeAdminAction要求明确true，false为独立拒绝异常，空ack为未知；没有自动重复写入。
- 新AdminViewModel/Repository已接root，移出约200行载入/写入编排；每分区读serial、子接口分别发布、全局单个不可变写入身份。刷新/切分区不释放write锁、不强制跳回旧页，消息和失败草稿回到原分区可见；账号/角色变更清除并使旧结果失效。
- 保存cookie/rule/shop失败可继续编辑（秘密只在内存、诊断toString隐藏），取消确认回原草稿；调度日志补50/100/200/500/1000行选择。操作拒绝用错误色，不替换原主题布局。
- 16793仅新滚动控件缺import编译失败，补齐后37465定向仍执行。尚未管理员新包实机，不声称真实admin写入通过。
- 待续：review/shop源分页是否超100截断、素材文件真正上传/预览、真实普通角色、全部source控件和受控写入。已有API参数测试不等整模块完成。

00:55定向37465全部16项通过（3m18s）；11758全量1111中profile装备1红，发现normalizeAdminAction也供setCurrentUserEquipment使用，不能改变已验装扮协议期望。修为管理员12写入单用normalizeConfirmedAdminAction，原装备继续按返回ack判定；12824完整重建中。新增AdminFeatureDeviceTest只使用合成商品和拒绝/成功回执，不调用真实admin接口。源码当前网站DFnAz6dn/7CtD4vuJ同样只GET page1/page_size100，故100条并非单凭字符串就证明App阉割，但仍需实际分页字段/源控件核对。

00:59修后12824全量152suites/1111tests/0失败，Debug/AndroidTest通过4m20s，管理包`4A64B89D456E0189E2226E43C96CBF0E7CD1CDC15B10F6A5440ECB02CB440749`。尚未安装，MuMu正在launch。角色代次会清除仍打开的编辑/确认弹窗；cookie输入只内存保留且表单可滚动，取消保存确认不丢输入。

01:01包已install-r无损覆盖，41221 `AdminFeatureDeviceTest`在MuMu实际原生管理表单中完整通过：改名→保存→取消确认回原草稿（0写入）→确认→受控拒绝/错误文案→继续编辑保留新名称→再次确认→成功/草稿清除。只2个合成repo调用，**没有实际网站admin写入**。证据`20260909-admin-failed-draft.{json,log}`。普通角色和真实管理员端点响应/分页/素材行为仍未全验。
