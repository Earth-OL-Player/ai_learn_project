import { ElAvatar } from 'element-plus/es/components/avatar/index.mjs';
import { ElButton } from 'element-plus/es/components/button/index.mjs';
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs';
import { ElDropdown } from 'element-plus/es/components/dropdown/index.mjs';
import { ElDropdownItem } from 'element-plus/es/components/dropdown/index.mjs';
import { ElDropdownMenu } from 'element-plus/es/components/dropdown/index.mjs';
import { ElForm } from 'element-plus/es/components/form/index.mjs';
import { ElFormItem } from 'element-plus/es/components/form/index.mjs';
import { ElInput } from 'element-plus/es/components/input/index.mjs';
import { ElMenu } from 'element-plus/es/components/menu/index.mjs';
import { ElMenuItem } from 'element-plus/es/components/menu/index.mjs';
import 'element-plus/theme-chalk/base.css';
import 'element-plus/es/components/avatar/style/css';
import 'element-plus/es/components/button/style/css';
import 'element-plus/es/components/dialog/style/css';
import 'element-plus/es/components/dropdown/style/css';
import 'element-plus/es/components/form/style/css';
import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/menu/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles/global.scss';

// 应用入口只装配基础插件，业务逻辑下沉到页面组件和状态仓库。
const app = createApp(App);
const pinia = createPinia();
const elementComponents = [
  ElAvatar,
  ElButton,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElMenu,
  ElMenuItem,
];

// 只注册项目实际使用的 Element Plus 组件，避免全量 UI 组件打包进首屏。
elementComponents.forEach((component) => {
  app.use(component);
});

app.use(pinia);
app.use(router);
app.mount('#app');
