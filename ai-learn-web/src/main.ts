import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles/global.scss';

// 应用入口只装配基础插件，业务逻辑下沉到页面组件。
const app = createApp(App);
app.use(router);
app.use(ElementPlus);
app.mount('#app');
