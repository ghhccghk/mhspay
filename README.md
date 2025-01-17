![](https://socialify.git.ci/ghhccghk/mhspay/image?description=1&descriptionEditable=%E9%80%9A%E8%BF%87Hook%E7%B1%B3%E7%94%BB%E5%B8%88%E7%9A%84%E5%AF%86%E7%A0%81%E8%BE%93%E5%85%A5%E6%8E%A5%E5%8F%A3%E5%B9%B6%E5%9C%A8%E6%8C%87%E7%BA%B9%E8%AF%86%E5%88%AB%E6%AD%A3%E7%A1%AE%E5%90%8E%E5%A1%AB%E5%85%A5%E5%AF%86%E7%A0%81%E6%9D%A5%E5%AE%9E%E7%8E%B0%E6%8C%87%E7%BA%B9%E8%AF%86%E5%88%AB&language=1&name=1&owner=1&theme=Auto)
---
![Release Download](https://img.shields.io/github/downloads/ghhccghk/mhspay/total?style=flat-square)
[![Release Version](https://img.shields.io/github/v/release/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/releases/latest)  
[![GitHub license](https://img.shields.io/github/license/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/LICENSE.md)  
[![GitHub Star](https://img.shields.io/github/stars/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/stargazers)  
[![GitHub Fork](https://img.shields.io/github/forks/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/network/members)
![GitHub Star](https://img.shields.io/github/stars/ghhccghk/mhspay.svg?style=social)

# 不要更新新版本，新版本有反hook，目前支持到 7.21.0
## 这是什么东西？

#### 这是一个Xposed模块（目前仅支持LSPosed），通过Hook米画师的密码输入接口并在指纹识别正确后填入密码来实现指纹识别

## 如何使用？

#### 模块成功Hook后会在设置里显示指纹设置选项，在里面打开总开关并设置好密码即可使用。

## 输入的密码会不会被窃取？

#### 密码使用AES加密，在有tee的设备会在tee加解密来保证安全，所有和密码有关的界面做了防录屏截图处理，防止第三方APP截取密码。本模块不会保留密码，密码只能在开启Debug编译的情况下在系统日志里会出现。

## 致谢
#### [Shellwen](https://github.com/ShellWen) 感谢其帮忙脱壳
#### [EzXHelper](https://github.com/KyuubiRan/EzXHelper) 使用了其项目
#### [XKT](https://github.com/xiaowine/XKT) 使用了其项目
#### [Lyric-Getter](https://github.com/xiaowine/Lyric-Getter) 参考了界面写法
#### [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler) 参考了焦点通知实现
#### [MIUINativeNotifyIcon](https://github.com/fankes/MIUINativeNotifyIcon) 参考了小米系统信息获取

[<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.png" width="200"/>](https://www.jetbrains.com)
