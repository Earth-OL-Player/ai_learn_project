import { ElAlert } from 'element-plus/es/components/alert/index.mjs';
import { ElAvatar } from 'element-plus/es/components/avatar/index.mjs';
import { ElButton } from 'element-plus/es/components/button/index.mjs';
import { ElCard } from 'element-plus/es/components/card/index.mjs';
import { ElCollapse } from 'element-plus/es/components/collapse/index.mjs';
import { ElCollapseItem } from 'element-plus/es/components/collapse/index.mjs';
import { ElDescriptions } from 'element-plus/es/components/descriptions/index.mjs';
import { ElDescriptionsItem } from 'element-plus/es/components/descriptions/index.mjs';
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs';
import { ElDropdown } from 'element-plus/es/components/dropdown/index.mjs';
import { ElDropdownItem } from 'element-plus/es/components/dropdown/index.mjs';
import { ElDropdownMenu } from 'element-plus/es/components/dropdown/index.mjs';
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs';
import { ElForm } from 'element-plus/es/components/form/index.mjs';
import { ElFormItem } from 'element-plus/es/components/form/index.mjs';
import { ElInput } from 'element-plus/es/components/input/index.mjs';
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs';
import { ElMenu } from 'element-plus/es/components/menu/index.mjs';
import { ElMenuItem } from 'element-plus/es/components/menu/index.mjs';
import { ElOption } from 'element-plus/es/components/select/index.mjs';
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs';
import { ElProgress } from 'element-plus/es/components/progress/index.mjs';
import { ElSelect } from 'element-plus/es/components/select/index.mjs';
import { ElSkeleton } from 'element-plus/es/components/skeleton/index.mjs';
import { ElSwitch } from 'element-plus/es/components/switch/index.mjs';
import { ElTable } from 'element-plus/es/components/table/index.mjs';
import { ElTableColumn } from 'element-plus/es/components/table/index.mjs';
import { ElTag } from 'element-plus/es/components/tag/index.mjs';
import { ElTooltip } from 'element-plus/es/components/tooltip/index.mjs';
import { ElUpload } from 'element-plus/es/components/upload/index.mjs';
import 'element-plus/dist/index.css';
import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles/global.scss';

// 应用入口只装配基础插件，业务逻辑下沉到页面组件和状态仓库。
const app = createApp(App);
const pinia = createPinia();
const elementComponents = [
  ElAlert,
  ElAvatar,
  ElButton,
  ElCard,
  ElCollapse,
  ElCollapseItem,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElProgress,
  ElSelect,
  ElSkeleton,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTooltip,
  ElUpload,
];

// 只注册项目实际使用的 Element Plus 组件，避免全量 UI 组件打包进首屏。
elementComponents.forEach((component) => {
  app.use(component);
});

app.use(pinia);
app.use(router);
app.mount('#app');
