<template>
    <div class="container-fluid py-3 form-invoices-admin">
        <!-- Title + actions -->
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">Mẫu hóa đơn</h4>
            <div>
                <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
                    <i class="fas fa-sync-alt"></i>
                    Làm mới
                </b-button>
                <b-button size="sm" variant="success" @click="goCreate">
                    <i class="fas fa-plus"></i>
                    Tạo mẫu
                </b-button>
            </div>
        </div>

        <!-- Filters Row -->
        <b-card class="mb-3 shadow-sm">
            <b-row>
                <b-col md="4" class="mb-2">
                    <b-input-group>
                        <b-input-group-prepend is-text>
                            <i class="fas fa-search text-muted"></i>
                        </b-input-group-prepend>
                        <b-form-input
                            v-model.trim="filters.q"
                            placeholder="Tìm theo tên mẫu"
                            @keyup.enter="applyFilters"
                        />
                    </b-input-group>
                </b-col>
                <b-col md="3" class="mb-2">
                    <b-form-select v-model="filters.category" :options="categoryOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả loại hóa đơn</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
                <b-col md="3" class="mb-2">
                    <b-form-select v-model="filters.type" :options="typeOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả loại thuế suất</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
                <b-col md="2" class="text-right mb-2">
                    <b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button>
                </b-col>
            </b-row>
            <b-row>
                <b-col md="6">
                    <b-form-select v-model="filters.status" :options="statusOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
            </b-row>
        </b-card>

        <!-- Table -->
        <b-card class="shadow-sm">
            <b-table
                bordered
                hover
                responsive
                small
                show-empty
                :items="items"
                :fields="fields"
                :busy="isBusy"
                empty-text="Không có dữ liệu"
                class="mb-0 table-modern table-compact"
            >
                <!-- STT -->
                <template #cell(id)="{ index }">
                    {{ (page - 1) * size + index + 1 }}
                </template>

                <template #cell(name)="{ item }">
                    <div class="font-weight-bold">{{ item.name }}</div>
                    <div class="text-muted small" v-if="item.serial">Ký hiệu: <code>{{ item.serial }}</code></div>
                </template>

                <template #cell(photo)="{ item }">
                    <img v-if="item.photo" :src="item.photo" style="height:36px; border-radius: 4px;" alt="Preview" />
                    <span v-else class="text-muted">—</span>
                </template>

                <template #cell(category)="{ item }">
                    <b-badge :variant="item.category === 1 ? 'primary' : 'info'">
                        {{ getCategoryText(item.category) }}
                    </b-badge>
                </template>

                <template #cell(type)="{ item }">
                    <div>{{ getTypeText(item.type) }}</div>
                </template>

                <template #cell(status)="{ item }">
                    <b-badge :variant="item.status === 1 ? 'success' : 'secondary'">
                        {{ item.status === 1 ? 'Kích hoạt' : 'Chưa kích hoạt' }}
                    </b-badge>
                </template>

                <template #cell(option)="{ item }">
                    <b-dropdown
                        size="sm"
                        right
                        variant="link"
                        toggle-class="text-decoration-none"
                        no-caret
                        boundary="window"
                    >
                        <template #button-content>
                            <i class="fas fa-ellipsis-h"></i>
                        </template>
                        <b-dropdown-item class="text-center" href="#" @click.prevent="edit(item)">
                            Cập nhật
                        </b-dropdown-item>
                        <b-dropdown-item class="text-center" href="#" @click.prevent="preview(item)">
                            Xem
                        </b-dropdown-item>
                        <b-dropdown-item class="text-center text-success" href="#" @click.prevent="setStatus(item.id, 1)">
                            Kích hoạt
                        </b-dropdown-item>
                        <b-dropdown-item class="text-center text-warning" href="#" @click.prevent="setStatus(item.id, 0)">
                            Tạm ngưng
                        </b-dropdown-item>
                        <b-dropdown-item class="text-center text-danger" href="#" @click.prevent="onDelete(item)">
                            Xóa
                        </b-dropdown-item>
                    </b-dropdown>
                </template>
            </b-table>

            <b-pagination
                v-if="total > size"
                v-model="page"
                :per-page="size"
                :total-rows="total"
                align="right"
                class="mt-2"
                @change="fetch"
            />
        </b-card>
    </div>
</template>

<script>
import axios from '@/plugins/axios'

export default {
    name: 'AdminFormInvoiceList',
    data() {
        return {
            isBusy: false,
            items: [],
            page: 1,
            size: 10,
            total: 0,
            filters: { q: '', category: null, type: null, status: null },
            fields: [
                { key: 'id', label: 'STT', thStyle: { width: '60px' } },
                { key: 'name', label: 'Tên mẫu' },
                { key: 'category', label: 'Loại hóa đơn', thStyle: { width: '150px' } },
                { key: 'type', label: 'Loại thuế suất', thStyle: { width: '160px' } },
                { key: 'photo', label: 'Ảnh', thStyle: { width: '100px' } },
                { key: 'status', label: 'Trạng thái', thStyle: { width: '120px' } },
                { key: 'option', label: 'Chức năng', thStyle: { width: '100px' } }
            ],
            categoryOptions: [
                { value: 1, text: 'Hóa đơn giá trị gia tăng' },
                { value: 2, text: 'Hóa đơn bán hàng' }
            ],
            typeOptions: [
                { value: 1, text: 'Một thuế suất' },
                { value: 2, text: 'Nhiều thuế suất' }
            ],
            statusOptions: [
                { value: 1, text: 'Kích hoạt' },
                { value: 0, text: 'Chưa kích hoạt' }
            ]
        }
    },
    mounted() { this.fetch() },
    methods: {
        reload() { this.fetch() },
        async fetch() {
            this.isBusy = true
            try {
                const body = { q: this.filters.q || undefined, category: this.filters.category, type: this.filters.type, status: this.filters.status }
                const { data } = await axios.post('/administrator/form-invoices/list', body, { params: { page: Math.max(0, this.page - 1), size: this.size } })
                this.items = (data.items || []).map(it => ({
                    id: it.id,
                    name: it.name,
                    companyId: it.companyId || it.company_id || null,
                    companyName: it.companyName || '',
                    serial: (it.formCode || '') + (it.serial || ''),
                    category: it.category,
                    type: it.type,
                    status: it.status,
                    photo: it.photo,
                    file: it.file,
                    system: it.system
                }))
                this.total = data.total || 0
            } catch (e) {
                // ignore
            } finally { this.isBusy = false }
        },
        getCategoryText(val) {
            const opt = this.categoryOptions.find(o => o.value === Number(val))
            return opt ? opt.text : (val || '—')
        },
        getTypeText(val) {
            const opt = this.typeOptions.find(o => o.value === Number(val))
            return opt ? opt.text : (val || '—')
        },
        applyFilters() { this.page = 1; this.fetch() },
        goCreate() { this.$router.push({ name: 'admin-form-invoice-create' }) },
        edit(item) { this.$router.push({ name: 'admin-form-invoice-edit', params: { id: item.id } }) },
        preview(item) {
            if (!item.id) return
            window.open(`/v1/file/${item.id}/view`, '_blank')
        },
        async setStatus(id, status) {
            if (!confirm(status === 1 ? 'Kích hoạt mẫu này?' : 'Tạm ngưng mẫu này?')) return
            try {
                await axios.post(`/administrator/form-invoices/${id}/status`, { status })
                this.$bvToast && this.$bvToast.toast(status === 1 ? 'Đã kích hoạt' : 'Đã tạm ngưng', { title: 'Thành công', variant: 'success', solid: true })
                this.fetch()
            } catch (e) {
                this.$bvToast && this.$bvToast.toast('Cập nhật trạng thái thất bại', { title: 'Lỗi', variant: 'danger', solid: true })
            }
        },
        async onDelete(item) {
            if (!confirm('Xóa mẫu này?')) return
            try {
                await axios.delete(`/administrator/form-invoices/${item.id}`)
                this.$bvToast && this.$bvToast.toast('Đã xóa', { title: 'Thành công', variant: 'success', solid: true })
                this.fetch()
            } catch (e) {
                this.$bvToast && this.$bvToast.toast('Xóa thất bại', { title: 'Lỗi', variant: 'danger', solid: true })
            }
        }
    }
}
</script>

<style scoped>
/* Card Styling */
.form-invoices-admin .card.shadow-sm {
    border-radius: 10px;
}

.form-invoices-admin .card {
    border: 1px solid #e9ecef;
}

/* Table Hover */
.form-invoices-admin .table-hover tbody tr:hover {
    background-color: #fafbfd;
}

/* Buttons */
.form-invoices-admin .btn-outline-primary {
    border-color: #dfe7ff;
}

.form-invoices-admin .btn-outline-primary:hover {
    background: #eef3ff;
}

/* Table Header */
.form-invoices-admin .table thead th {
    background: #f7f9fc;
    border-bottom: 1px solid #ecf0f6;
    color: #4a5568;
    font-weight: 700;
}

/* Modern Table Styles */
.table-modern thead th {
    background-color: #f9fafb;
    border-bottom: 2px solid #e5e7eb;
    position: sticky;
    top: 0;
    z-index: 1;
}

.table-compact td,
.table-compact th {
    padding: 0.5rem 0.75rem;
}

.table td {
    vertical-align: middle;
}

.table-modern tbody tr:hover {
    background-color: #f6f8fa;
}

.table-modern.table-striped tbody tr:nth-of-type(odd) {
    background-color: #fcfdff;
}

.table-modern {
    border-color: #e9ecef;
}

/* Badge Styling */
.badge {
    font-size: 13px;
    padding: 0.35em 0.65em;
}

/* Text Utilities */
.text-mono {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

/* Card Body */
.card-body {
    padding: 0.75rem 1rem;
}

/* Input Group */
.input-group-text {
    background-color: #f8f9fa;
    border-right: none;
}

.input-group .form-control:focus {
    border-color: #80bdff;
    box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.input-group .form-control {
    border-left: none;
}

/* Dropdown Items */
.dropdown-item {
    font-size: 0.9rem;
    padding: 0.5rem 1rem;
}

.dropdown-item i {
    width: 20px;
    text-align: center;
}

/* Image Styling */
img {
    object-fit: cover;
}
</style>
