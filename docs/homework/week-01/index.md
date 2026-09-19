## 环境检查
- java --version: 
- openjdk 26.0.2 2026-07-21
  OpenJDK Runtime Environment (build 26.0.2+10-2-26.04.2-Ubuntu)
  OpenJDK 64-Bit Server VM (build 26.0.2+10-2-26.04.2-Ubuntu, mixed mode, sharing)
- mvn --version:
- Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
  Maven home: /opt/maven
  Java version: 26.0.2, vendor: Ubuntu, runtime: /usr/lib/jvm/java-26-openjdk-amd64
  Default locale: en, platform encoding: UTF-8
  OS name: "linux", version: "6.18.33.2-microsoft-standard-wsl2", arch: "amd64", family: "unix"
- git --version:
- git version 2.53.0
- docker --version:
- Docker version 29.8.0, build 88096ef
- docker compose version:
- Docker Compose version v5.5.1

## 概念回答
1. **什么是微服务架构？**
   微服务架构是一种将单一应用程序划分成一组小型服务的方法，每个服务运行在其独立的进程中，服务间采用轻量级通信机制（如HTTP/REST）进行通信。
2. **微服务和单体架构的主要区别是什么？**
   单体架构所有功能打包在一个应用中，代码耦合度高，扩展性差；微服务则是按业务拆分成独立部署的细小服务，每个服务可独立开发、部署和扩展，维护起来更灵活但运维复杂度更高。
3. **为什么本课程先实现单体系统，再逐步拆分为微服务？**
   直接上手微服务容易陷入分布式系统的复杂性，如网络延迟、分布式事务等。先从单体入手，可以快速熟悉业务逻辑，之后再逐步拆分，能够更好地理解微服务解决的具体痛点。
4. **为什么作业需要提供可重复运行的测试或验证脚本？**
   软件环境各异，确保作业结果可重复验证，是为了保证代码质量的稳定性。

## 问题记录
  安装IDEA插件时，遇到了网络问题导致的下载错误，通过网络代理解决了该问题