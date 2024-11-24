![](https://socialify.git.ci/ghhccghk/mhspay/image?description=1&descriptionEditable=%E9%80%9A%E8%BF%87Hook%E7%B1%B3%E7%94%BB%E5%B8%88%E7%9A%84%E5%AF%86%E7%A0%81%E8%BE%93%E5%85%A5%E6%8E%A5%E5%8F%A3%E6%9D%A5%E5%AE%9E%E7%8E%B0%E6%8C%87%E7%BA%B9%E8%AF%86%E5%88%AB&language=1&name=1&owner=1&theme=Auto)
---
![Release Download](https://img.shields.io/github/downloads/ghhccghk/mhspay/total?style=flat-square)
[![Release Version](https://img.shields.io/github/v/release/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/releases/latest)  
[![GitHub license](https://img.shields.io/github/license/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/LICENSE.md)  
[![GitHub Star](https://img.shields.io/github/stars/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/stargazers)  
[![GitHub Fork](https://img.shields.io/github/forks/ghhccghk/mhspay?style=flat-square)](https://github.com/ghhccghk/mhspay/network/members)
![GitHub Star](https://img.shields.io/github/stars/ghhccghk/mhspay.svg?style=social)


## 这是什么东西？

#### 这是一个Xposed模块（目前仅支持LSPosed），通过Hook米画师的密码输入接口来实现指纹识别

## 如何使用？

#### 模块成功Hook后会在设置里显示指纹设置选项，在里面打开总开关并设置好密码即可使用。

## 输入的密码会不会被窃取？

#### 密码使用AES加密，在有tee的设备会在tee加解密来保证安全，所有和密码有关的界面做了防录屏截图处理，防止第三方APP截取密码。本模块不会保留密码，密码只能在开启Debug编译的情况下在系统日志里会出现。