const MonitorPage = {
    template: `
        <div>
            <div class="page-header">
                <h2>实时监控</h2>
                <div>
                    <el-select v-model="selectedAgent" placeholder="选择 Agent" style="width: 200px; margin-right: 12px;">
                        <el-option
                            v-for="agent in onlineAgents"
                            :key="agent.id"
                            :label="agent.appName + ' (' + agent.ip + ')'"
                            :value="agent.ip + ':' + agent.port" />
                    </el-select>
                    <el-button v-if="!streaming" type="primary" @click="startStream" :disabled="!selectedAgent">
                        开始监控
                    </el-button>
                    <el-button v-else type="danger" @click="stopStream">
                        停止
                    </el-button>
                    <el-button @click="clearLogs">清空</el-button>
                    <el-checkbox v-model="autoScroll" style="margin-left: 12px;">自动滚动</el-checkbox>
                </div>
            </div>

            <el-row :gutter="16" style="margin-bottom: 16px;">
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number">{{ stats.total }}</div>
                        <div class="label">总事件</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number" style="color: #67c23a;">{{ stats.matched }}</div>
                        <div class="label">匹配成功</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number" style="color: #e6a23c;">{{ stats.passthrough }}</div>
                        <div class="label">透传</div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card">
                        <div class="number" style="color: #f56c6c;">{{ stats.errors }}</div>
                        <div class="label">错误</div>
                    </el-card>
                </el-col>
            </el-row>

            <div class="filter-bar">
                <el-checkbox v-model="showMatched" @change="applyFilter">匹配成功</el-checkbox>
                <el-checkbox v-model="showPassthrough" @change="applyFilter">透传</el-checkbox>
                <el-input v-model="filterKeyword" placeholder="关键词过滤" clearable style="width: 200px;" @input="applyFilter" />
            </div>

            <div ref="logContainer" class="monitor-log">
                <div v-for="(entry, idx) in filteredLogs" :key="idx"
                     class="log-entry" :class="entry.matched ? 'match' : 'passthrough'">
                    <span class="log-time">{{ entry.time }}</span>
                    <span class="log-method">{{ entry.method }}</span>
                    <span>{{ entry.path }}</span>
                    <span class="log-status" :class="entry.matched ? 'matched' : 'missed'">
                        {{ entry.matched ? 'MATCHED' : 'PASS' }}
                    </span>
                    <span v-if="entry.caseId" style="color: #b5cea8; margin-left: 8px;">
                        case={{ entry.caseId }}
                    </span>
                    <span v-if="entry.failReason" style="color: #ce9178; margin-left: 8px;">
                        reason={{ entry.failReason }}
                    </span>
                </div>
                <div v-if="filteredLogs.length === 0" style="color: #666; text-align: center; padding: 40px;">
                    {{ streaming ? '等待事件...' : '选择 Agent 并开始监控' }}
                </div>
            </div>
        </div>
    `,
    data() {
        return {
            selectedAgent: '',
            streaming: false,
            eventSource: null,
            logs: [],
            filteredLogs: [],
            autoScroll: true,
            showMatched: true,
            showPassthrough: true,
            filterKeyword: '',
            onlineAgents: [],
            stats: { total: 0, matched: 0, passthrough: 0, errors: 0 },
            reconnectNotified: false
        };
    },
    mounted() {
        this.loadAgents();
    },
    beforeUnmount() {
        this.stopStream();
    },
    methods: {
        async loadAgents() {
            try {
                const agents = await api.getAgents();
                this.onlineAgents = (agents || []).filter(a => {
                    return a.lastHeartbeat && (Date.now() - new Date(a.lastHeartbeat).getTime() < 120000);
                });
            } catch (e) {
                // ignore
            }
        },
        startStream() {
            if (!this.selectedAgent || this.streaming) return;
            const url = 'http://' + this.selectedAgent + '/mock/events/stream';
            const es = new EventSource(url);
            this.eventSource = es;
            this.streaming = true;
            this.reconnectNotified = false;

            es.onmessage = (event) => {
                // Data arrived, so the connection is healthy.
                this.reconnectNotified = false;
                try {
                    const data = JSON.parse(event.data);
                    this.logs.push({
                        time: new Date(data.timestamp).toLocaleTimeString('zh-CN'),
                        method: data.method,
                        path: data.path,
                        matched: data.matched,
                        caseId: data.caseId,
                        failReason: data.failReason
                    });
                    this.stats.total++;
                    if (data.matched) {
                        this.stats.matched++;
                    } else {
                        this.stats.passthrough++;
                    }
                    this.applyFilter();
                    if (this.autoScroll) {
                        this.$nextTick(() => {
                            const el = this.$refs.logContainer;
                            if (el) el.scrollTop = el.scrollHeight;
                        });
                    }
                } catch (e) {
                    // parse error
                }
            };

            es.onerror = () => {
                // Ignore events from a stream we've already closed.
                if (this.eventSource !== es) return;
                if (es.readyState === EventSource.CLOSED) {
                    // Fatal error (e.g. agent unreachable): stop and let the user retry.
                    this.stopStream();
                    this.$message.warning('连接断开，请重新开始监控');
                } else if (!this.reconnectNotified) {
                    // EventSource auto-reconnects on transient errors; just tell the user once.
                    this.reconnectNotified = true;
                    this.$message.warning('连接异常，正在自动重连...');
                }
            };
        },
        stopStream() {
            if (this.eventSource) {
                this.eventSource.close();
                this.eventSource = null;
            }
            this.streaming = false;
            this.reconnectNotified = false;
        },
        clearLogs() {
            this.logs = [];
            this.filteredLogs = [];
            this.stats = { total: 0, matched: 0, passthrough: 0, errors: 0 };
        },
        applyFilter() {
            this.filteredLogs = this.logs.filter(entry => {
                if (!this.showMatched && entry.matched) return false;
                if (!this.showPassthrough && !entry.matched) return false;
                if (this.filterKeyword) {
                    const kw = this.filterKeyword.toLowerCase();
                    const text = (entry.method + entry.path + (entry.caseId || '')).toLowerCase();
                    if (!text.includes(kw)) return false;
                }
                return true;
            });
        }
    }
};
