package top.wanxiang.app.iteration.engine

import android.content.Context
import java.io.File

/**
 * CustomIterationBootstrap — 万象自定义迭代环境自举引擎。
 *
 * 职责：
 * 1. 准备沙盒内隔离的源码工作区路径 `~/custom_wanxiang`；
 * 2. 将预置的 `wanxiang-custom-iteration` Skill 部署至 Agent 的技能目录；
 * 3. 部署 GitHub Actions CI 工作流模板；
 * 4. 生成引导 Agent 执行自迭代开发的规范提示词。
 */
object CustomIterationBootstrap {

    const val WORKSPACE_NAME = "custom_wanxiang"
    const val OFFICIAL_REPO = "https://github.com/peakSee/Wanxiang"

    /** 模块 assets 中预置资产的相对路径（与 assets/skills、assets/templates 一一对应）。 */
    private const val ASSET_SKILL = "skills/wanxiang-custom-iteration/SKILL.md"
    private const val ASSET_WORKFLOW = "templates/workflows/wanxiangdev-build.yml"

    const val BOOTSTRAP_PROMPT = """我准备在万象（WanXiang）的手机 Linux 虚拟沙盒中进行 WanXiang 自定义迭代。

请按以下步骤引导我：
1. 检查本地开发环境：
   - 确认当前命令在 Linux PRoot 沙盒中执行；
   - 确认独立工作区为 ~/custom_wanxiang；
   - 优先使用 GitHub Actions 构建 WanXiangDev APK，无需在手机本地安装庞大的 Android SDK/NDK。

2. 检查并配置 GitHub 认证：
   - 检查 gh CLI 与 Git 配置；
   - 引导我使用 gh auth login（设备码流程）或 SSH Key 完成登录验证；
   - 不要把 Token、私钥写入命令历史或日志中。

3. 验证与克隆仓库：
   - 为官方仓库 $OFFICIAL_REPO 点星；
   - Fork main 分支到我自己的 GitHub 账户；
   - 将 Fork 后的仓库克隆到 ~/custom_wanxiang。

4. 遵循 wanxiang-custom-iteration Skill 开发规范：
   - 按照万象的 Jetpack Compose、Hilt 和多模块规范进行修改；
   - 编写或调整功能后运行单元测试验证；
   - 提交修改并推送到 Fork 仓库的特性分支。

5. 通过 GitHub Actions 构建独立的 WanXiangDev APK：
   - 触发 .github/workflows/wanxiangdev-build.yml 编译；
   - 实时监控构建进度并在成功后下载 APK 至手机；
   - 校验包名 top.wanxiang.app.dev 和应用名 WanXiangDev，与正式版独立共存。

6. 若体验满意，协助我生成标准 PR 提交到 $OFFICIAL_REPO。"""

    /**
     * 初始化自定义迭代环境与工作区。
     *
     * 除创建目录骨架外，还会把模块 assets 内预置的 Skill 与 CI 工作流模板
     * 真正落盘：否则 Agent 在会话中读不到规范文件，模板也无从复用。
     */
    fun bootstrap(context: Context, rootfsHomeDir: File): BootstrapResult {
        try {
            // 1. 创建隔离工作区目录
            val workspaceDir = File(rootfsHomeDir, WORKSPACE_NAME)
            if (!workspaceDir.exists()) {
                workspaceDir.mkdirs()
            }

            // 2. 部署 Agent Skill（目录 + 文件内容）
            val skillDir = File(rootfsHomeDir, ".wanxiang/skills/wanxiang-custom-iteration")
            copyAsset(context, ASSET_SKILL, File(skillDir, "SKILL.md"))

            // 3. 部署工作流模板缓存，供 Agent 克隆后复制进 .github/workflows
            val templatesDir = File(rootfsHomeDir, ".wanxiang/templates/workflows")
            copyAsset(context, ASSET_WORKFLOW, File(templatesDir, "wanxiangdev-build.yml"))

            return BootstrapResult(
                success = true,
                workspacePath = workspaceDir.absolutePath,
                prompt = BOOTSTRAP_PROMPT
            )
        } catch (e: Exception) {
            return BootstrapResult(
                success = false,
                workspacePath = "",
                prompt = "",
                errorMessage = e.message ?: "Bootstrap failed"
            )
        }
    }

    /**
     * 把 assets 中的单个文件流式拷贝到目标路径（自动建父目录、覆盖旧内容）。
     */
    private fun copyAsset(context: Context, assetPath: String, target: File) {
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    data class BootstrapResult(
        val success: Boolean,
        val workspacePath: String,
        val prompt: String,
        val errorMessage: String? = null
    )
}
