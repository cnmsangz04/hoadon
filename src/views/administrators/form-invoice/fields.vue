<template>
  <div class="container-fluid py-3 form-invoice-fields">
    <div class="d-flex flex-wrap align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Phần tử mẫu hóa đơn</h4>
      <div class="page-actions">
        <b-button size="sm" variant="outline-primary" @click="reload"
          ><i class="fas fa-sync-alt mr-1"></i>Làm mới</b-button
        >
        <b-button size="sm" variant="outline-primary" @click="openGroup()">Nhóm</b-button>
        <b-button size="sm" variant="outline-primary" @click="openDataset()"
          >Danh sách dữ liệu</b-button
        >
        <b-button size="sm" variant="success" @click="openField()"
          ><i class="fas fa-plus mr-1"></i>Thêm phần tử</b-button
        >
      </div>
    </div>
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="5" class="mb-2"
          ><b-form-input
            v-model.trim="filters.q"
            placeholder="Tìm mã, tên hoặc đường dẫn dữ liệu"
            @keyup.enter="applyFilters"
        /></b-col>
        <b-col md="3" class="mb-2"
          ><b-form-select
            v-model="filters.group"
            :options="[{ value: null, text: 'Tất cả nhóm' }, ...groupOptions]"
        /></b-col>
        <b-col md="2" class="mb-2"
          ><b-form-select
            v-model="filters.active"
            :options="[
              { value: null, text: 'Tất cả trạng thái' },
              { value: true, text: 'Đang dùng' },
              { value: false, text: 'Đã ẩn' }
            ]"
        /></b-col>
        <b-col md="2" class="mb-2 text-right"
          ><b-button size="sm" variant="primary" @click="applyFilters">Tìm kiếm</b-button></b-col
        >
      </b-row>
    </b-card>
    <b-card class="shadow-sm">
      <b-table
        bordered
        hover
        small
        show-empty
        class="mb-0 table-modern fields-table"
        :items="items"
        :fields="columns"
        :busy="busy"
        empty-text="Không có phần tử phù hợp"
      >
        <template #cell(index)="{ index }">{{ (page - 1) * size + index + 1 }}</template>
        <template #cell(field)="{ item }"
          ><div class="font-weight-bold">{{ item.label }}</div>
          <code>{{ item.key }}</code></template
        >
        <template #cell(group)="{ item }"
          ><div>{{ groupLabel(item.group) }}</div>
          <small v-if="item.repeated" class="text-muted"
            >{{ datasetLabel(item.dataset) }} (lặp)</small
          ></template
        >
        <template #cell(sourcePath)="{ item }"
          ><small class="text-muted">{{
            item.sourceType === 'XML' ? 'XML hóa đơn' : 'Thông tin hệ thống'
          }}</small
          ><code class="d-block">{{ item.sourcePath }}</code></template
        >
        <template #cell(valueType)="{ item }">{{ typeLabel(item.valueType) }}</template>
        <template #cell(active)="{ item }"
          ><b-badge :variant="item.active ? 'success' : 'secondary'">{{
            item.active ? 'Đang dùng' : 'Đã ẩn'
          }}</b-badge
          ><small v-if="item.requiredDisplay" class="d-block text-muted mt-1"
            >Bắt buộc trên mẫu</small
          ></template
        >
        <template #cell(actions)="{ item }"
          ><b-dropdown size="sm" right variant="link" no-caret boundary="window"
            ><template #button-content><i class="fas fa-ellipsis-h"></i></template
            ><b-dropdown-item @click="openField(item)">Cập nhật</b-dropdown-item></b-dropdown
          ></template
        >
      </b-table>
      <pagination-bar
        :current.sync="page"
        :size.sync="size"
        :total="total"
        :sizes="[10, 20, 50, 100]"
        @page-change="fetchItems"
        @size-change="applyFilters"
      />
    </b-card>
    <b-modal
      ref="fieldModal"
      size="lg"
      :title="form.id ? 'Cập nhật phần tử' : 'Thêm phần tử'"
      hide-footer
    >
      <b-form @submit.prevent="saveField">
        <b-row>
          <b-col md="6"
            ><b-form-group label="Tên hiển thị"
              ><b-form-input
                v-model.trim="form.label"
                required
                maxlength="500"
                placeholder="Ví dụ: Mã tra cứu" /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group label="Mã phần tử"
              ><b-form-input
                v-model.trim="form.key"
                required
                :disabled="!!form.id"
                placeholder="invoice.lookupCode"
                maxlength="160" /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group label="Nhóm thư viện"
              ><b-form-select v-model="form.group" :options="groupOptions" required /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group label="Phạm vi dữ liệu"
              ><b-form-select
                v-model="form.dataset"
                :options="datasetOptions"
                required /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group label="Lấy dữ liệu từ"
              ><b-form-select
                v-model="form.sourceType"
                :options="[
                  { value: 'XML', text: 'Thẻ XML hóa đơn' },
                  { value: 'CONTEXT', text: 'Thông tin hệ thống' }
                ]"
                @change="form.sourcePath = ''" /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group
              :label="form.sourceType === 'XML' ? 'Đường dẫn XML' : 'Thông tin hiển thị'"
              ><b-form-input
                v-if="form.sourceType === 'XML'"
                v-model.trim="form.sourcePath"
                required
                placeholder="DLHDon/TTChung/SHDon"
                maxlength="700" /><b-form-select
                v-else
                v-model="form.sourcePath"
                :options="contextOptions"
                required /></b-form-group
          ></b-col>
        </b-row>
        <p class="small text-muted">{{ sourceHelp }}</p>
        <details class="field-options mb-3">
          <summary>Thông tin bổ sung</summary>
          <b-row class="mt-3">
            <b-col md="4"
              ><b-form-group label="Kiểu dữ liệu"
                ><b-form-select v-model="form.valueType" :options="typeOptions" /></b-form-group
            ></b-col>
            <b-col md="4"
              ><b-form-group label="Độ dài tối đa"
                ><b-form-input
                  v-model.number="form.maxLength"
                  type="number"
                  min="1" /></b-form-group
            ></b-col>
            <b-col md="4"
              ><b-form-group label="Thứ tự"
                ><b-form-input
                  v-model.number="form.sortOrder"
                  type="number"
                  min="0"
                  max="1000000" /></b-form-group
            ></b-col>
          </b-row>
          <b-form-group label="Mô tả"
            ><b-form-textarea v-model.trim="form.description" rows="2" maxlength="2000"
          /></b-form-group>
        </details>
        <div class="d-flex flex-wrap field-flags mb-3">
          <b-form-checkbox v-model="form.requiredDisplay" @change="requiredChanged"
            >Bắt buộc hiển thị trên mẫu</b-form-checkbox
          ><b-form-checkbox v-model="form.active" :disabled="form.requiredDisplay"
            >Cho phép sử dụng</b-form-checkbox
          >
        </div>
        <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
        <div class="text-right">
          <b-button variant="light" class="mr-2" :disabled="saving" @click="$refs.fieldModal.hide()"
            >Hủy</b-button
          ><b-button type="submit" variant="primary" :disabled="saving"
            ><b-spinner v-if="saving" small class="mr-1" />Lưu phần tử</b-button
          >
        </div>
      </b-form>
    </b-modal>
    <b-modal ref="groupModal" title="Nhóm thư viện" hide-footer>
      <b-form @submit.prevent="saveGroup">
        <b-form-group label="Nhóm"
          ><b-form-select
            :value="groupForm.id"
            :options="[
              { value: null, text: 'Thêm nhóm mới' },
              ...metadata.groups.map((g) => ({ value: g.id, text: g.label }))
            ]"
            @change="selectGroup"
        /></b-form-group>
        <b-form-group label="Tên nhóm"
          ><b-form-input v-model.trim="groupForm.label" required maxlength="255"
        /></b-form-group>
        <b-form-group label="Mã nhóm"
          ><b-form-input
            v-model.trim="groupForm.code"
            required
            :disabled="!!groupForm.id"
            maxlength="80"
        /></b-form-group>
        <b-form-group label="Thứ tự"
          ><b-form-input v-model.number="groupForm.sortOrder" type="number" min="0"
        /></b-form-group>
        <b-form-checkbox v-model="groupForm.active" class="mb-3">Cho phép sử dụng</b-form-checkbox>
        <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
        <div class="text-right">
          <b-button variant="light" class="mr-2" @click="$refs.groupModal.hide()">Đóng</b-button
          ><b-button type="submit" variant="primary" :disabled="saving">Lưu nhóm</b-button>
        </div>
      </b-form>
    </b-modal>
    <b-modal ref="datasetModal" title="Danh sách dữ liệu" hide-footer>
      <b-form @submit.prevent="saveDataset">
        <b-form-group label="Danh sách"
          ><b-form-select
            :value="datasetForm.id"
            :options="[
              { value: null, text: 'Thêm danh sách mới' },
              ...metadata.datasets.map((d) => ({ value: d.id, text: d.label }))
            ]"
            @change="selectDataset"
        /></b-form-group>
        <b-form-group label="Tên hiển thị"
          ><b-form-input v-model.trim="datasetForm.label" required maxlength="255"
        /></b-form-group>
        <b-form-group label="Mã danh sách"
          ><b-form-input
            v-model.trim="datasetForm.code"
            required
            :disabled="!!datasetForm.id"
            maxlength="80"
        /></b-form-group>
        <b-form-group label="Dữ liệu cha"
          ><b-form-select
            v-model="datasetForm.parent"
            :options="[
              { value: null, text: 'Không có (root)' },
              ...datasetOptions.filter((d) => d.value !== datasetForm.code)
            ]"
        /></b-form-group>
        <b-form-group label="Loại"
          ><b-form-select
            v-model="datasetForm.cardinality"
            :options="[
              { value: 'ONE', text: 'Thông tin đơn' },
              { value: 'MANY', text: 'Danh sách lặp' }
            ]"
        /></b-form-group>
        <b-form-group label="Đường dẫn XML gốc"
          ><b-form-input v-model.trim="datasetForm.rootPath" required maxlength="700"
        /></b-form-group>
        <b-form-checkbox v-model="datasetForm.active" class="mb-3"
          >Cho phép sử dụng</b-form-checkbox
        >
        <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
        <div class="text-right">
          <b-button variant="light" class="mr-2" @click="$refs.datasetModal.hide()">Đóng</b-button
          ><b-button type="submit" variant="primary" :disabled="saving">Lưu danh sách</b-button>
        </div>
      </b-form>
    </b-modal>
  </div>
</template>

<script>
import axios from '@/plugins/axios'
import PaginationBar from '@/views/components/pagination_bar.vue'
const endpoint = '/administrator/invoice-template-fields'
const types = {
  STRING: 'Chuỗi ký tự',
  INTEGER: 'Số nguyên',
  DECIMAL: 'Số thập phân',
  DATE: 'Ngày',
  BOOLEAN: 'Đúng / Sai'
}
export default {
  name: 'AdminInvoiceTemplateFields',
  components: { PaginationBar },
  data() {
    return {
      busy: false,
      saving: false,
      error: '',
      items: [],
      page: 1,
      size: 20,
      total: 0,
      metadata: { groups: [], datasets: [], contextFields: [], nextSortOrder: 10 },
      filters: { q: '', group: null, active: null },
      form: this.emptyForm(),
      groupForm: {},
      datasetForm: {},
      columns: [
        { key: 'index', label: 'STT', thStyle: { width: '5%' } },
        { key: 'field', label: 'Phần tử', thStyle: { width: '24%' } },
        { key: 'group', label: 'Nhóm', thStyle: { width: '16%' } },
        { key: 'sourcePath', label: 'Dữ liệu', thStyle: { width: '26%' } },
        { key: 'valueType', label: 'Kiểu', thStyle: { width: '12%' } },
        { key: 'active', label: 'Trạng thái', thStyle: { width: '12%' } },
        { key: 'actions', label: '', thStyle: { width: '5%' } }
      ]
    }
  },
  computed: {
    groupOptions() {
      return this.metadata.groups.map((g) => ({ value: g.code, text: g.label }))
    },
    datasetOptions() {
      return this.metadata.datasets.map((d) => ({
        value: d.code,
        text: d.label + (d.cardinality === 'MANY' ? ' (lặp)' : '')
      }))
    },
    contextOptions() {
      return [
        { value: '', text: 'Chọn thông tin' },
        ...this.metadata.contextFields.map((f) => ({ value: f.key, text: f.label }))
      ]
    },
    typeOptions() {
      return Object.entries(types).map(([value, text]) => ({ value, text }))
    },
    sourceHelp() {
      const dataset = this.metadata.datasets.find((d) => d.code === this.form.dataset)
      return this.form.sourceType === 'CONTEXT'
        ? 'Chọn thông tin cần đưa lên mẫu, ví dụ mã cơ quan thuế hoặc mã tra cứu.'
        : `Đường dẫn tương đối với ${dataset?.rootPath || 'phạm vi dữ liệu đã chọn'}.`
    }
  },
  mounted() {
    this.reload()
  },
  methods: {
    emptyForm() {
      return {
        id: null,
        key: '',
        label: '',
        group: 'common',
        dataset: 'document',
        sourceType: 'XML',
        sourcePath: '',
        valueType: 'STRING',
        maxLength: null,
        defaultFormatter: 'plainText',
        description: '',
        requiredDisplay: false,
        active: true,
        sortOrder: null,
        expectedLockVersion: null
      }
    },
    async reload() {
      try {
        const { data } = await axios.get(`${endpoint}/metadata`)
        this.metadata = data
        await this.fetchItems()
      } catch (e) {
        this.notifyError(e)
      }
    },
    async fetchItems() {
      this.busy = true
      try {
        const { data } = await axios.get(endpoint, {
          params: { ...this.filters, page: this.page, size: this.size }
        })
        this.items = data.items
        this.total = data.total
        if (!this.items.length && this.page > 1) {
          this.page = Math.max(1, Math.ceil(this.total / this.size))
          await this.fetchItems()
        }
      } catch (e) {
        this.notifyError(e)
      } finally {
        this.busy = false
      }
    },
    applyFilters() {
      this.page = 1
      this.fetchItems()
    },
    groupLabel(code) {
      return this.metadata.groups.find((g) => g.code === code)?.label || code
    },
    datasetLabel(code) {
      return this.metadata.datasets.find((d) => d.code === code)?.label || code
    },
    typeLabel(code) {
      return types[code] || code
    },
    openField(item) {
      this.error = ''
      this.form = item ? { ...item, expectedLockVersion: item.lockVersion } : this.emptyForm()
      if (!item) this.form.sortOrder = this.metadata.nextSortOrder
      this.$refs.fieldModal.show()
    },
    requiredChanged(value) {
      if (value) this.form.active = true
    },
    async saveField() {
      await this.perform(async () => {
        const body = { ...this.form, maxLength: this.form.maxLength || null }
        if (body.id) await axios.put(`${endpoint}/${body.id}`, body)
        else await axios.post(endpoint, body)
        this.$refs.fieldModal.hide()
        await this.reload()
      })
    },
    selectGroup(id) {
      const item = this.metadata.groups.find((g) => g.id === id)
      this.groupForm = item
        ? { ...item, expectedLockVersion: item.lockVersion }
        : { id: null, code: '', label: '', sortOrder: 0, active: true }
      this.error = ''
    },
    openGroup() {
      this.selectGroup(null)
      this.$refs.groupModal.show()
    },
    async saveGroup() {
      await this.perform(async () => {
        const f = this.groupForm
        if (f.id) await axios.put(`${endpoint}/groups/${f.id}`, f)
        else await axios.post(`${endpoint}/groups`, f)
        await this.reload()
        this.$refs.groupModal.hide()
      })
    },
    selectDataset(id) {
      const item = this.metadata.datasets.find((d) => d.id === id)
      this.datasetForm = item
        ? {
            ...item,
            parent: this.metadata.datasets.find((d) => d.id === item.parentId)?.code || null,
            expectedLockVersion: item.lockVersion
          }
        : {
            id: null,
            code: '',
            label: '',
            parent: 'document',
            cardinality: 'MANY',
            rootPath: '',
            active: true
          }
      this.error = ''
    },
    openDataset() {
      this.selectDataset(null)
      this.$refs.datasetModal.show()
    },
    async saveDataset() {
      await this.perform(async () => {
        const f = this.datasetForm
        if (f.id) await axios.put(`${endpoint}/datasets/${f.id}`, f)
        else await axios.post(`${endpoint}/datasets`, f)
        await this.reload()
        this.$refs.datasetModal.hide()
      })
    },
    async perform(action) {
      this.error = ''
      this.saving = true
      try {
        await action()
        this.$bvToast.toast('Đã lưu thay đổi', { variant: 'success', solid: true })
      } catch (e) {
        this.error = this.errorMessage(e)
      } finally {
        this.saving = false
      }
    },
    errorMessage(e) {
      return e.response?.data?.message || 'Không thể lưu dữ liệu. Vui lòng thử lại.'
    },
    notifyError(e) {
      this.$bvToast.toast(this.errorMessage(e), { variant: 'danger', solid: true })
    }
  }
}
</script>
<style scoped>
.form-invoice-fields .card {
  border: 1px solid #e9ecef;
  border-radius: 10px;
}
.page-actions,
.field-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.fields-table {
  table-layout: fixed;
  width: 100%;
}
.fields-table :deep(th) {
  background: #f9fafb;
  border-bottom: 2px solid #e5e7eb;
  color: #4a5568;
}
.fields-table :deep(td),
.fields-table :deep(th) {
  padding: 0.5rem 0.75rem;
  vertical-align: middle;
  overflow-wrap: anywhere;
  white-space: normal;
}
.fields-table code {
  white-space: normal;
  overflow-wrap: anywhere;
}
.field-options {
  border: 1px solid #e5e7eb;
  padding: 12px;
  border-radius: 6px;
}
.field-flags {
  gap: 20px;
}
</style>
