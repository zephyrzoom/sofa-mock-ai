const DashboardPage = {
    template: `
        <div>
            <div class="page-header">
                <h2>仪表盘</h2>
                <el-button @click="refresh" :loading="loading">刷新</el-button>
            </div>

            <el-row :gutter="20" style="margin-bottom: 24px;">
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number">{{ stats.totalCases }}</div>
                        <div class="label">案例总数</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number">{{ stats.onlineAgents }}</div>
                        <div class="label">在线 Agent</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number">{{ stats.totalApps }}</div>
                        <div class="label">应用数</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number">{{ stats.enabledCases }}</div>
                        <div class="label">启用案例</div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20">
                <el-col :span="12">
                    <el-card>
                        <template #header>
                            <span>最近案例</span>
                        </template>
                        <el-table :data="recentCases" size="small" max-height="300">
                            <el-table-column prop="appName" label="应用" width="120" />
                            <el-table-column prop="method" label="方法" width="80" />
                            <el-table-column prop="path" label="路径" show-overflow-tooltip />
                            <el-table-column prop="enabled" label="状态" width="80">
                                <template #default="{ row }">
                                    <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                                        {{ row.enabled ? '启用' : '禁用' }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </el-col>
                <el-col :span="12">
                    <el-card>
                        <template #header>
                            <span>在线 Agent</span>
                        </template>
                        <el-table :data="agents" size="small" max-height="300">
                            <el-table-column prop="appName" label="应用" width="120" />
                            <el-table-column prop="instanceId" label="实例" show-overflow-tooltip />
                            <el-table-column prop="ip" label="IP" width="140" />
                            <el-table-column label="状态" width="80">
                                <template #default>
                                    <el-tag type="success" size="small">在线</el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </el-col>
            </el-row>
        </div>
    `,
    data() {
        return {
            loading: false,
            stats: { totalCases: 0, onlineAgents: 0, totalApps: 0, enabledCases: 0 },
            recentCases: [],
            agents: []
        };
    },
    mounted() {
        this.refresh();
        this._timer = setInterval(() => this.refresh(), 30000);
    },
    beforeUnmount() {
        if (this._timer) {
            clearInterval(this._timer);
        }
    },
    methods: {
        async refresh() {
            this.loading = true;
            try {
                const [cases, agents, onlineAgents, apps] = await Promise.all([
                    api.getCases().catch(() => []),
                    api.getAgents().catch(() => []),
                    api.getOnlineAgents().catch(() => []),
                    api.getApps().catch(() => [])
                ]);
                this.recentCases = (cases || []).slice(0, 10);
                this.agents = onlineAgents || [];
                this.stats.totalCases = (cases || []).length;
                this.stats.enabledCases = (cases || []).filter(c => c.enabled).length;
                this.stats.onlineAgents = (onlineAgents || []).length;
                this.stats.totalApps = (apps || []).length;
            } catch (e) {
                this.$message.error('加载失败: ' + e.message);
            } finally {
                this.loading = false;
            }
        }
    }
};
