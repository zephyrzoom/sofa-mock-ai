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
                            :value="agent.id" />
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
            pollTimer: null,
            seenEvents: new Set(),
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
            this.streaming = true;
            this.seenEvents = new Set();
            this.reconnectNotified = false;
            this.fetchEvents();
            this.pollTimer = setInterval(this.fetchEvents, 2000);
        },
        stopStream() {
            if (this.pollTimer) {
                clearInterval(this.pollTimer);
                this.pollTimer = null;
            }
            this.streaming = false;
            this.reconnectNotified = false;
        },
        // 通过管理端服务器中转拉取 agent 事件（避免浏览器直连 agent 的跨域/不可达问题）。
        async fetchEvents() {
            try {
                const events = await api.request('GET', '/agents/' + this.selectedAgent + '/events');
                if (!Array.isArray(events)) return;
                // 拉取成功即认为连接正常
                this.reconnectNotified = false;
                // agent 事件最新在前，转成时间正序后按需追加
                const chrono = events.slice().reverse();
                let appended = false;
                for (const data of chrono) {
                    const sig = [data.timestamp, data.method, data.path, data.matchTimeMs,
                                 data.responseStatus, data.matched ? 1 : 0,
                                 data.caseId || '', data.failReason || ''].join('|');
                    if (this.seenEvents.has(sig)) continue;
                    this.seenEvents.add(sig);
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
                    appended = true;
                }
                if (appended) {
                    this.reconnectNotified = false;
                    this.applyFilter();
                    if (this.autoScroll) {
                        this.$nextTick(() => {
                            const el = this.$refs.logContainer;
                            if (el) el.scrollTop = el.scrollHeight;
                        });
                    }
                }
            } catch (e) {
                if (!this.reconnectNotified) {
                    this.reconnectNotified = true;
                    this.$message.warning('获取监控数据失败，正在重试...');
                }
            }
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
