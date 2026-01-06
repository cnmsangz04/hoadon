<template>
    <div class="container-fluid py-3 form-invoices-admin">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h4 class="mb-0 font-weight-bold">Mẫu hóa đơn (Hệ thống)</h4>
            <div>
                <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload">
                    <i class="fas fa-sync-alt"></i>
                    Làm mới
                </b-button>
                <b-button size="sm" variant="success" @click="goCreate">
                    <i class="fas fa-file-alt"></i>
                    Tạo mẫu
                </b-button>
            </div>
        </div>

        <b-card class="mb-3 shadow-sm">
            <b-row>
                <b-col md="4" class="mb-2">
                    <b-form-input v-model.trim="filters.q" placeholder="Tìm theo tên" @keyup.enter="applyFilters" />
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
                <b-col md="2" class="mb-2 text-right">
                    <b-button size="sm" variant="primary" @click="applyFilters">Tìm</b-button>
                </b-col>
            </b-row>
            <b-row class="mb-2">
                <b-col md="4">
                    <v-select v-model="filters.companyId" :options="companyOptions" label="label" :reduce="c => c.value"
                        placeholder="Tất cả công ty" />
                </b-col>
                <b-col md="4">
                    <b-form-select v-model="filters.system" :options="systemOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả loại mẫu</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
                <b-col md="4">
                    <b-form-select v-model="filters.status" :options="statusOptions">
                        <template #first>
                            <b-form-select-option :value="null">Tất cả trạng thái</b-form-select-option>
                        </template>
                    </b-form-select>
                </b-col>
            </b-row>
        </b-card>

        <b-card class="shadow-sm">
            <b-table bordered hover responsive small :items="items" :fields="fields" :busy="isBusy"
                empty-text="Không có dữ liệu">
                <template #cell(photo)="{ item }">
                    <img v-if="item.photo" :src="item.photo" style="height:36px;" />
                </template>
                <template #cell(company)="{ item }">
                    <div>{{ item.companyName || '—' }}</div>
                </template>
                <template #cell(serial)="{ item }">
                    <code>{{ item.serial || '—' }}</code>
                </template>
                <template #cell(category)="{ item }">
                    <div>{{ getCategoryText(item.category) }}</div>
                </template>
                <template #cell(type)="{ item }">
                    <div>{{ getTypeText(item.type) }}</div>
                </template>
                <template #cell(system)="{ item }">
                    <b-badge :variant="item.system === 0 ? 'info' : 'secondary'">{{ item.system === 0 ? 'Template' :
                        'Thường' }}</b-badge>
                </template>
                <template #cell(status)="{ item }">
                    <b-badge :variant="item.status === 1 ? 'success' : 'secondary'">{{ item.status === 1 ? 'Kích hoạt' :
                        'Chưa kích hoạt' }}</b-badge>
                </template>
                <template #cell(option)="{ item }">
                    <b-dropdown size="sm" right variant="link" toggle-class="text-decoration-none" no-caret
                        boundary="window">
                        <template #button-content>
                            <i class="fas fa-ellipsis-h"></i>
                        </template>
                        <b-dropdown-item class="text-center" href="#" @click.prevent="edit(item)">Cập
                            nhật</b-dropdown-item>
                        <b-dropdown-item class="text-center" href="#"
                            @click.prevent="preview(item)">Xem</b-dropdown-item>
                        <b-dropdown-item class="text-center text-success" href="#"
                            @click.prevent="setStatus(item.id, 1)">Kích
                            hoạt</b-dropdown-item>
                        <b-dropdown-item class="text-center text-warning" href="#"
                            @click.prevent="setStatus(item.id, 0)">Tạm
                            ngưng</b-dropdown-item>
                        <b-dropdown-item class="text-center text-danger" href="#"
                            @click.prevent="onDelete(item)">Xóa</b-dropdown-item>
                    </b-dropdown>
                </template>
            </b-table>

            <b-row class="mt-2">
                <b-col cols="6">
                    <div class="pt-1 text-muted">
                        Tổng: <b>{{ total }}</b>
                    </div>
                </b-col>
                <b-col cols="6">
                    <b-pagination align="right" v-model.number="page" :per-page="size" :total-rows="total"
                        @input="fetch" />
                </b-col>
            </b-row>
        </b-card>
    </div>
</template>

<script>
import axios from '@/plugins/axios'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'

export default {
    name: 'AdminFormInvoiceList',
    components: { vSelect },
    data() {
        return {
            isBusy: false,
            items: [],
            page: 1,
            size: 10,
            total: 0,
            filters: { q: '', companyId: null, category: null, type: null, system: null, status: null },
            fields: [
                { key: 'id', label: '#', thStyle: { width: '60px' } },
                { key: 'company', label: 'Công ty' },
                { key: 'name', label: 'Tên mẫu' },
                { key: 'serial', label: 'Ký hiệu' },
                { key: 'category', label: 'Loại' },
                { key: 'type', label: 'Thuế suất' },
                { key: 'photo', label: 'Ảnh' },
                { key: 'system', label: 'Loại mẫu' },
                { key: 'status', label: 'Trạng thái' },
                { key: 'option', label: 'Chức năng', thStyle: { width: '220px' } }
            ],
            categoryOptions: [
                { value: 1, text: 'Hóa đơn giá trị gia tăng' },
                { value: 2, text: 'Hóa đơn bán hàng' }
            ],
            typeOptions: [
                { value: 1, text: 'Một thuế suất' },
                { value: 2, text: 'Nhiều thuế suất' }
            ],
            companyOptions: [],
            companyNameById: {},
            systemOptions: [
                { value: 0, text: 'Template' },
                { value: 1, text: 'Thường' }
            ],
            statusOptions: [
                { value: 1, text: 'Kích hoạt' },
                { value: 0, text: 'Chưa kích hoạt' }
            ]
        }
    },
    async mounted() { await this.loadCompanies(); this.fetch() },
    methods: {
        async loadCompanies() {
            try {
                const res = await axios.post('/administrator/company/list', {}, { params: { page: 0, size: 5000 } })
                const list = res.data?.content || []
                const map = {}
                this.companyOptions = list.map(c => {
                    const id = Number(c.id)
                    const name = c.name || c.companyName || c.domain || `#${id}`
                    map[id] = name
                    return { value: id, label: name }
                })
                this.companyNameById = map
            } catch (e) {
                this.companyOptions = []
                this.companyNameById = {}
            }
        },
        reload() { this.fetch() },
        async fetch() {
            this.isBusy = true
            try {
                const body = { q: this.filters.q || undefined, companyId: this.filters.companyId, category: this.filters.category, type: this.filters.type, status: this.filters.status, system: this.filters.system }
                const { data } = await axios.post('/administrator/form-invoices/list', body, { params: { page: Math.max(0, this.page - 1), size: this.size } })
                this.items = (data.items || []).map(it => ({
                    id: it.id,
                    name: it.name,
                    companyId: it.companyId || it.company_id || null,
                    companyName: it.companyName || this.companyNameById[(it.companyId || it.company_id) || 0] || '',
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
.form-invoices-admin .card.shadow-sm {
    border-radius: 10px;
}
</style>
