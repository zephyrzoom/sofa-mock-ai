const CasesPage = {
    template: `
        <div>
            <div class="page-header">
                <h2>案例管理</h2>
                <div>
                    <el-upload
                        action="#"
                        :auto-upload="false"
                        :on-change="handleFileUpload"
                        :show-file-list="false"
                        accept=".json"
                        style="display: inline-block; margin-right: 12px;">
                        <el-button type="success">上传案例文件</el-button>
                    </el-upload>
                    <el-button @click="exportCases">导出</el-button>
                    <el-button type="primary" @click="showAddDialog">新增案例</el-button>
                </div>
            </div>

            <div class="filter-bar">
                <el-input v-model="filter.appName" placeholder="应用名" clearable style="width: 160px;" @change="loadCases" />
                <el-select v-model="filter.method" placeholder="请求方法" clearable style="width: 120px;" @change="loadCases">
                    <el-option label="GET" value="GET" />
                    <el-option label="POST" value="POST" />
                    <el-option label="PUT" value="PUT" />
                    <el-option label="DELETE" value="DELETE" />
                </el-select>
                <el-input v-model="filter.path" placeholder="路径关键词" clearable style="width: 200px;" @change="loadCases" />
                <el-button @click="loadCases">搜索</el-button>
            </div>

            <el-table :data="cases" v-loading="loading" border stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="appName" label="应用" width="120" />
                <el-table-column prop="method" label="方法" width="80" />
                <el-table-column prop="path" label="路径" show-overflow-tooltip />
                <el-table-column prop="status" label="状态码" width="80" />
                <el-table-column prop="matchType" label="匹配类型" width="100">
                    <template #default="{ row }">
                        {{ row.matchType || 'EXACT' }}
                    </template>
                </el-table-column>
                <el-table-column prop="enabled" label="启用" width="80">
                    <template #default="{ row }">
                        <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
                    </template>
                </el-table-column>
                <el-table-column prop="priority" label="优先级" width="80" />
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
                        <el-button size="small" type="danger" @click="deleteCase(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- Add/Edit Dialog -->
            <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑案例' : '新增案例'" width="700px" destroy-on-close>
                <el-tabs v-model="editMode">
                    <el-tab-pane label="表单模式" name="form">
                        <el-form :model="form" label-width="100px" class="case-detail">
                            <el-form-item label="应用名">
                                <el-input v-model="form.appName" placeholder="如 user-service" />
                            </el-form-item>
                            <el-form-item label="请求方法">
                                <el-select v-model="form.method">
                                    <el-option label="GET" value="GET" />
                                    <el-option label="POST" value="POST" />
                                    <el-option label="PUT" value="PUT" />
                                    <el-option label="DELETE" value="DELETE" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="路径">
                                <el-input v-model="form.path" placeholder="/api/users" />
                            </el-form-item>
                            <el-form-item label="路径模式">
                                <el-input v-model="form.pathPattern" placeholder="可选，如 /api/users/{id}" />
                            </el-form-item>
                            <el-form-item label="匹配类型">
                                <el-select v-model="form.matchType">
                                    <el-option label="精确匹配" value="EXACT" />
                                    <el-option label="正则匹配" value="REGEX" />
                                    <el-option label="Ant风格" value="ANT" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="请求体条件">
                                <el-input v-model="form.requestBody" type="textarea" :rows="3" placeholder='可选，如 {"userId": "123"}' />
                            </el-form-item>
                            <el-form-item label="条件表达式">
                                <el-input v-model="form.condition" placeholder='可选，如 headers.X-Token == "abc"' />
                            </el-form-item>
                            <el-form-item label="响应状态码">
                                <el-input-number v-model="form.status" :min="100" :max="599" />
                            </el-form-item>
                            <el-form-item label="响应体">
                                <el-input v-model="form.body" type="textarea" :rows="6" class="json-editor" />
                            </el-form-item>
                            <el-form-item label="响应头">
                                <el-input v-model="form.responseHeadersStr" type="textarea" :rows="2" placeholder='JSON格式，如 {"Content-Type": "application/json"}' />
                            </el-form-item>
                            <el-form-item label="延迟(ms)">
                                <el-input-number v-model="form.delayMs" :min="0" />
                            </el-form-item>
                            <el-form-item label="优先级">
                                <el-input-number v-model="form.priority" :min="0" />
                            </el-form-item>
                            <el-form-item label="描述">
                                <el-input v-model="form.description" />
                            </el-form-item>
                            <el-form-item label="启用">
                                <el-switch v-model="form.enabled" />
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                    <el-tab-pane label="JSON模式" name="json">
                        <el-input v-model="jsonText" type="textarea" :rows="20" class="json-editor"
                            placeholder="输入完整的 JSON 案例" />
                        <div v-if="jsonError" style="color: #f56c6c; margin-top: 8px;">{{ jsonError }}</div>
                    </el-tab-pane>
                </el-tabs>
                <template #footer>
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="saveCase" :loading="saving">保存</el-button>
                </template>
            </el-dialog>
        </div>
    `,
    data() {
        return {
            loading: false,
            saving: false,
            cases: [],
            filter: { appName: '', method: '', path: '' },
            dialogVisible: false,
            isEdit: false,
            editMode: 'form',
            editingId: null,
            jsonText: '',
            jsonError: '',
            form: this.emptyForm()
        };
    },
    mounted() {
        this.loadCases();
    },
    methods: {
        emptyForm() {
            return {
                appName: '',
                method: 'GET',
                path: '',
                pathPattern: '',
                matchType: 'EXACT',
                requestBody: '',
                condition: '',
                status: 200,
                body: '{}',
                responseHeadersStr: '',
                delayMs: 0,
                priority: 0,
                description: '',
                enabled: true
            };
        },
        async loadCases() {
            this.loading = true;
            try {
                const params = {};
                if (this.filter.appName) params.appName = this.filter.appName;
                if (this.filter.method) params.method = this.filter.method;
                this.cases = await api.getCases(params);
                if (this.filter.path) {
                    this.cases = this.cases.filter(c => c.path && c.path.includes(this.filter.path));
                }
            } catch (e) {
                this.$message.error('加载失败: ' + e.message);
            } finally {
                this.loading = false;
            }
        },
        showAddDialog() {
            this.isEdit = false;
            this.editingId = null;
            this.form = this.emptyForm();
            this.jsonText = '';
            this.jsonError = '';
            this.editMode = 'form';
            this.dialogVisible = true;
        },
        showEditDialog(row) {
            this.isEdit = true;
            this.editingId = row.id;
            this.form = {
                appName: row.appName || '',
                method: row.method || 'GET',
                path: row.path || '',
                pathPattern: row.pathPattern || '',
                matchType: row.matchType || 'EXACT',
                requestBody: row.requestBody || '',
                condition: row.condition || '',
                status: row.status || 200,
                body: row.body || '{}',
                responseHeadersStr: row.responseHeaders ? JSON.stringify(row.responseHeaders) : '',
                delayMs: row.delayMs || 0,
                priority: row.priority || 0,
                description: row.description || '',
                enabled: row.enabled !== false
            };
            this.jsonText = JSON.stringify(this.formToJson(), null, 2);
            this.jsonError = '';
            this.editMode = 'form';
            this.dialogVisible = true;
        },
        formToJson() {
            const data = {
                appName: this.form.appName,
                method: this.form.method,
                path: this.form.path,
                status: this.form.status,
                body: this.form.body,
                enabled: this.form.enabled,
                priority: this.form.priority
            };
            if (this.form.pathPattern) data.pathPattern = this.form.pathPattern;
            if (this.form.matchType && this.form.matchType !== 'EXACT') data.matchType = this.form.matchType;
            if (this.form.requestBody) data.requestBody = this.form.requestBody;
            if (this.form.condition) data.condition = this.form.condition;
            if (this.form.responseHeadersStr) {
                try { data.responseHeaders = JSON.parse(this.form.responseHeadersStr); } catch (e) {}
            }
            if (this.form.delayMs > 0) data.delayMs = this.form.delayMs;
            if (this.form.description) data.description = this.form.description;
            return data;
        },
        async saveCase() {
            this.saving = true;
            try {
                let data;
                if (this.editMode === 'json') {
                    try {
                        data = JSON.parse(this.jsonText);
                        this.jsonError = '';
                    } catch (e) {
                        this.jsonError = 'JSON 格式错误: ' + e.message;
                        return;
                    }
                } else {
                    data = this.formToJson();
                }

                if (this.isEdit) {
                    await api.updateCase(this.editingId, data);
                    this.$message.success('更新成功');
                } else {
                    await api.createCase(data);
                    this.$message.success('创建成功');
                }
                this.dialogVisible = false;
                this.loadCases();
            } catch (e) {
                this.$message.error('保存失败: ' + e.message);
            } finally {
                this.saving = false;
            }
        },
        async toggleEnabled(row) {
            try {
                await api.updateCase(row.id, { enabled: row.enabled });
                this.$message.success(row.enabled ? '已启用' : '已禁用');
            } catch (e) {
                row.enabled = !row.enabled;
                this.$message.error('操作失败: ' + e.message);
            }
        },
        async deleteCase(row) {
            try {
                await this.$confirm('确定删除该案例?', '提示', { type: 'warning' });
                await api.deleteCase(row.id);
                this.$message.success('已删除');
                this.loadCases();
            } catch (e) {
                if (e !== 'cancel') {
                    this.$message.error('删除失败: ' + e.message);
                }
            }
        },
        async handleFileUpload(uploadFile) {
            try {
                const result = await api.uploadCases(uploadFile.raw);
                if (result && result.error) {
                    this.$message.error('上传失败: ' + result.error);
                } else {
                    this.$message.success('上传成功: ' + (result.imported || 0) + ' 条案例');
                }
                this.loadCases();
            } catch (e) {
                this.$message.error('上传失败: ' + e.message);
            }
        },
        async exportCases() {
            try {
                const data = await api.exportCases();
                const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'mock-cases-export.json';
                a.click();
                URL.revokeObjectURL(url);
            } catch (e) {
                this.$message.error('导出失败: ' + e.message);
            }
        }
    }
};
