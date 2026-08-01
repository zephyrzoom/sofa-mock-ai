# Vendor Libraries

这些前端库是内网离线部署所需的本地副本，由 CDN 下载后放入本目录，避免页面依赖外网。
升级方式：从源地址下载新版本覆盖对应文件，并同步更新本说明。

| 文件 | 版本 | 源地址 |
|------|------|--------|
| `vue.global.prod.js` | Vue v3.5.40 | https://unpkg.com/vue@3/dist/vue.global.prod.js |
| `vue-router.global.prod.js` | vue-router v4.6.4 | https://unpkg.com/vue-router@4/dist/vue-router.global.prod.js |
| `element-plus/index.css` | Element Plus v2.14.3 | https://unpkg.com/element-plus/dist/index.css |
| `element-plus/index.full.min.js` | Element Plus v2.14.3 | https://unpkg.com/element-plus/dist/index.full.min.js |
| `element-plus-icons/index.iife.min.js` | Element Plus Icons Vue v2.3.2 | https://unpkg.com/@element-plus/icons-vue/dist/index.iife.min.js |

引用方式见 `../index.html`，均使用相对当前服务的本地路径（`/vendor/...`），
不发起任何外网请求。
