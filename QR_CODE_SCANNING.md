# 二维码扫描添加镜像功能

## 功能说明

ADocker 现在支持通过扫描二维码来快速添加 Docker 镜像。这个功能基于 Google ML Kit 的 Barcode Scanning API 和 CameraX 实现。

## 使用方法

### 1. 打开扫码功能

有两种方式可以打开扫码界面：

1. **从 Images 页面**：点击右上角的下载图标 → 进入 Pull Image 页面 → 点击右上角的扫码图标
2. **直接从 Pull Image 页面**：点击右上角的扫码图标（二维码图标）

### 2. 扫描二维码

1. 应用会请求相机权限（首次使用时）
2. 将二维码对准屏幕中央的扫描框
3. 应用会自动识别二维码内容
4. 识别成功后会弹出确认对话框，显示识别到的镜像名称
5. 点击 "Pull Image" 按钮开始拉取镜像

### 3. 功能特性

- **自动对焦**：相机会自动对焦二维码
- **闪光灯控制**：右上角可以切换闪光灯开关（适用于暗光环境）
- **实时识别**：无需手动拍照，对准即可识别
- **扫描框引导**：屏幕中央有清晰的扫描框和引导提示

## 支持的二维码格式

扫码功能支持多种二维码格式：

### 1. 简单格式（推荐）

直接包含镜像名称：
```
alpine:latest
```
```
ubuntu:22.04
```
```
nginx:1.25
```

### 2. JSON 格式

包含更多元数据：
```json
{
  "type": "docker-image",
  "image": "ubuntu:22.04",
  "description": "Ubuntu 22.04 LTS"
}
```

### 3. URL 格式

使用自定义协议：
```
adocker://pull?image=ubuntu:22.04
```

## 生成测试二维码

### 在线工具

可以使用在线二维码生成器（如 qr-code-generator.com）生成包含镜像名称的二维码。

### 命令行工具

使用 qrencode（Linux/macOS）：

```bash
# 安装 qrencode
brew install qrencode  # macOS
sudo apt install qrencode  # Ubuntu/Debian

# 生成二维码
qrencode -o alpine.png "alpine:latest"
qrencode -o ubuntu.png "ubuntu:22.04"
qrencode -o nginx.png "nginx:1.25"

# JSON 格式
qrencode -o image.png '{"type":"docker-image","image":"alpine:latest"}'
```

### Python 脚本

```python
import qrcode

# 生成简单格式的二维码
img = qrcode.make("alpine:latest")
img.save("alpine_qr.png")

# 生成 JSON 格式的二维码
import json
data = {
    "type": "docker-image",
    "image": "ubuntu:22.04",
    "description": "Ubuntu 22.04 LTS"
}
img = qrcode.make(json.dumps(data))
img.save("ubuntu_qr.png")
```

## 测试示例

以下是一些常用镜像的二维码内容，可以直接生成二维码进行测试：

1. **Alpine Linux（最小）**
   ```
   alpine:latest
   ```

2. **Ubuntu 22.04**
   ```
   ubuntu:22.04
   ```

3. **Nginx**
   ```
   nginx:1.25
   ```

4. **Python**
   ```
   python:3.11-alpine
   ```

5. **Node.js**
   ```
   node:20-alpine
   ```

## 技术实现

### 使用的库

- **Google ML Kit Barcode Scanning** (v17.3.0): 离线条码/二维码识别
- **CameraX** (v1.4.1): 相机预览和图像分析
- **Jetpack Compose**: 现代化的 UI 实现

### 核心功能

1. **相机权限管理**：运行时动态请求相机权限
2. **实时预览**：使用 CameraX 实现流畅的相机预览
3. **图像分析**：使用 ML Kit 实时分析相机帧，识别二维码
4. **导航集成**：扫码结果通过 Navigation SavedStateHandle 传递给 Pull Image 页面

### 文件位置

- **扫码屏幕**: `app/src/main/java/com/adocker/runner/ui/screens/images/QRCodeScannerScreen.kt`
- **导航配置**: `app/src/main/java/com/adocker/runner/ui/navigation/Navigation.kt`
- **主屏幕路由**: `app/src/main/java/com/adocker/runner/ui/MainScreen.kt`
- **拉取页面**: `app/src/main/java/com/adocker/runner/ui/screens/images/PullImageScreen.kt`

## 权限要求

应用需要以下权限：

- `android.permission.CAMERA`: 用于访问设备相机进行二维码扫描

权限会在首次使用扫码功能时自动请求。

## 最佳实践

1. **光线充足**：在光线充足的环境下扫描效果最好
2. **二维码清晰**：确保二维码清晰、无污损
3. **保持距离**：手机与二维码保持 10-20cm 的距离
4. **稳定持握**：扫描时保持手机稳定，避免抖动
5. **使用闪光灯**：暗光环境下可以打开闪光灯辅助扫描

## 故障排除

### 无法打开相机
- 检查是否授予了相机权限
- 在系统设置中检查权限状态
- 重新启动应用

### 二维码识别失败
- 确保二维码内容是有效的镜像名称
- 尝试调整手机与二维码的距离
- 在光线充足的环境下重试
- 检查二维码是否清晰可见

### 扫码后无反应
- 确保二维码格式正确
- 查看应用日志确认错误信息
- 尝试使用简单格式的二维码（纯文本镜像名）

## 未来改进

可能的功能增强：

1. 支持批量扫码添加多个镜像
2. 二维码生成功能（分享镜像给其他用户）
3. 历史扫码记录
4. 支持其他类型的二维码（如仓库地址、配置文件等）
5. 图片库选择二维码（不仅限于相机扫描）

## 示例应用场景

1. **技术分享会**：讲师在演示时展示二维码，参与者扫码快速获取镜像
2. **文档集成**：在技术文档中嵌入二维码，方便快速部署
3. **团队协作**：通过二维码分享项目所需的镜像配置
4. **教育场景**：老师提供二维码，学生快速获取课程所需环境
