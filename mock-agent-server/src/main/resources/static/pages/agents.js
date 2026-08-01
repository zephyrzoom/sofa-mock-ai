const AgentsPage = {
    template: `
        <div>
            <div class="page-header">
                <h2>Agent 监控</h2>
                <el-button @click="loadAgents" :loading="loading">刷新</el-button>
            </div>

            <el-table :data="agents" v-loading="loading" border stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="appName" label="应用名" width="150" />
                <el-table-column prop="instanceId" label="实例ID" show-overflow-tooltip />
                <el-table-column prop="ip" label="IP 地址" width="140" />
                <el-table-column prop="port" label="管理端口" width="100" />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="isOnline(row) ? 'success' : 'danger'" size="small">
                            {{ isOnline(row) ? '在线' : '离线' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="最后心跳" width="180">
                    <template #default="{ row }">
                        {{ formatTime(row.lastHeartbeat) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180">
                    <template #default="{ row }">
                        <el-button size="small" @click="viewAgentDetail(row)" :disabled="!isOnline(row)">
                            详情
                        </el-button>
                        <el-button size="small" type="danger" @click="deleteAgent(row)">
                            移除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- Agent Detail Dialog -->
            <el-dialog v-model="detailVisible" title="Agent 详情" width="700px">
                <div v-if="agentDetail">
                    <el-descriptions border :column="2">
                        <el-descriptions-item label="应用名">{{ agentDetail.appName }}</el-descriptions-item>
                        <el-descriptions-item label="实例ID">{{ agentDetail.instanceId }}</el-descriptions-item>
                        <el-descriptions-item label="IP">{{ agentDetail.ip }}</el-descriptions-item>
                        <el-descriptions-item label="管理端口">{{ agentDetail.port }}</el-descriptions-item>
                    </el-descriptions>

                    <div v-if="agentStats" style="margin-top: 20px;">
                        <h4 style="margin-bottom: 12px;">匹配统计</h4>
                        <el-row :gutter="16">
                            <el-col :span="8">
                                <el-card class="stat-card">
                                    <div class="number">{{ agentStats.totalRequests || 0 }}</div>
                                    <div class="label">总请求数</div>
                                </el-card>
                            </el-col>
                            <el-col :span="8">
                                <el-card class="stat-card">
                                    <div class="number">{{ agentStats.matchedRequests || 0 }}</div>
                                    <div class="label">匹配成功</div>
                                </el-card>
                            </el-col>
                            <el-col :span="8">
                                <el-card class="stat-card">
                                    <div class="number">{{ agentStats.passthroughRequests || 0 }}</div>
                                    <div class="label">透传请求</div>
                                </el-card>
                            </el-col>
                        </el-row>
                    </div>
                </div>
            </el-dialog>
        </div>
    `,
    data() {
        return {
            loading: false,
            agents: [],
            detailVisible: false,
            agentDetail: null,
            agentStats: null
        };
    },
    mounted() {
        this.loadAgents();
        this.refreshTimer = setInterval(this.loadAgents, 30000);
    },
    beforeUnmount() {
        if (this.refreshTimer) clearInterval(this.refreshTimer);
    },
    methods: {
        async loadAgents() {
            this.loading = true;
            try {
                this.agents = await api.getAgents();
            } catch (e) {
                this.$message.error('加载失败: ' + e.message);
            } finally {
                this.loading = false;
            }
        },
        isOnline(agent) {
            if (!agent.lastHeartbeat) return false;
            return Date.now() - new Date(agent.lastHeartbeat).getTime() < 120000;
        },
        formatTime(ts) {
            if (!ts) return '-';
            return new Date(ts).toLocaleString('zh-CN');
        },
        async viewAgentDetail(agent) {
            this.agentDetail = agent;
            this.agentStats = null;
            this.detailVisible = true;
            if (agent.port && agent.ip) {
                try {
                    const url = 'http://' + agent.ip + ':' + agent.port;
                    this.agentStats = await api.getAgentStats(url);
                } catch (e) {
                    // stats unavailable
                }
            }
        },
        async deleteAgent(agent) {
            try {
                await this.$confirm('确定移除该 Agent 记录?', '提示', { type: 'warning' });
                await api.request('DELETE', '/agents/' + agent.id);
                this.$message.success('已移除');
                this.loadAgents();
            } catch (e) {
                if (e !== 'cancel') {
                    this.$message.error('操作失败: ' + e.message);
                }
            }
        }
    }
};
