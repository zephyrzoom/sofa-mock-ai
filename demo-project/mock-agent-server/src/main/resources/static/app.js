const routes = [
    { path: '/', component: DashboardPage },
    { path: '/cases', component: CasesPage },
    { path: '/agents', component: AgentsPage },
    { path: '/monitor', component: MonitorPage }
];

const router = VueRouter.createRouter({
    history: VueRouter.createWebHashHistory(),
    routes
});

const app = Vue.createApp({
    data() {
        return {
            currentRoute: '/'
        };
    },
    watch: {
        '$route.path'(val) {
            this.currentRoute = val;
        }
    },
    methods: {
        handleMenuSelect(index) {
            this.$router.push(index);
        }
    }
});

// Register Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.use(ElementPlus);
app.use(router);
app.mount('#app');
