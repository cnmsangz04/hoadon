<template>
  <div class="container-fluid py-3 form-invoice-fields">
    <div class="d-flex flex-wrap align-items-center justify-content-between mb-3">
      <h4 class="mb-0 font-weight-bold">Phần tử mẫu hóa đơn</h4>
      <div class="page-actions">
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="reload"
          ><i class="fas fa-sync-alt mr-1"></i>Làm mới</b-button
        >
        <b-button size="sm" variant="outline-primary" class="mr-2" @click="openGroup()"
          >Nhóm dữ liệu</b-button
        >
        <b-button size="sm" variant="success" @click="openField()"
          ><i class="fas fa-plus mr-1"></i>Thêm phần tử</b-button
        >
      </div>
    </div>
    <b-alert v-if="error" show dismissible variant="danger" @dismissed="error = ''">{{ error }}</b-alert>
    <b-card class="mb-3 shadow-sm">
      <b-row>
        <b-col md="5" class="mb-2"
          ><b-form-input
            v-model.trim="filters.q"
            placeholder="Tìm mã, tên hoặc đường dẫn dữ liệu"
            @keyup.enter="applyFilters"
        /></b-col>
        <b-col md="2" class="mb-2"
          ><b-form-select
            v-model="filters.group"
            :options="[{ value: null, text: 'Tất cả nhóm dữ liệu' }, ...groupOptions]"
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
        <b-col md="12" class="mb-2 text-right"
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
          ><div>{{ groupLabel(item.group) }}</div></template
        >
        <template #cell(sourcePath)="{ item }"
          ><small class="text-muted">XML hóa đơn</small
          ><code class="d-block">{{ fieldXmlPath(item) }}</code></template
        >
        <template #cell(valueType)="{ item }">{{ typeLabel(item.valueType) }}</template>
        <template #cell(invoiceTypes)="{ item }">
          <div class="invoice-type-badges">
            <span
              v-for="option in invoiceTypeItems(item.applicableInvoiceTypes)"
              :key="option.value"
              class="invoice-type-badge"
              :class="`invoice-type-badge--${option.tone}`"
            >{{ option.shortText }}</span>
            <span v-if="!invoiceTypeItems(item.applicableInvoiceTypes).length" class="text-muted">Chưa thiết lập</span>
          </div>
        </template>
        <template #cell(active)="{ item }"
          ><b-badge :variant="fieldAvailable(item) ? 'success' : 'secondary'">{{
            fieldAvailable(item) ? 'Đang dùng' : 'Đã ẩn'
          }}</b-badge
          ><small v-if="item.requiredDisplay" class="d-block text-muted mt-1"
            >Bắt buộc trên mẫu</small
          ></template
        >
        <template #cell(actions)="{ item }"
          ><b-dropdown size="sm" right variant="link" no-caret boundary="window"
            ><template #button-content><i class="fas fa-ellipsis-h"></i></template
            ><b-dropdown-item class="text-center" @click="openField(item)">Cập nhật</b-dropdown-item
            ><b-dropdown-divider /><b-dropdown-item
              class="text-center text-danger"
              @click="deleteField(item)"
              >Xóa</b-dropdown-item
            ></b-dropdown
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
            ><b-form-group :label="form.id ? 'Tên hiển thị' : 'Tên hiển thị'"
              ><b-form-input
                v-model.trim="form.label"
                required
                :readonly="!!form.id"
                maxlength="500"
                placeholder="Ví dụ: Mã tra cứu" /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group :label="form.id ? 'Mã phần tử' : 'Mã phần tử'"
              ><b-form-input
                v-model.trim="form.key"
                required
                readonly
                placeholder="common.PBan"
                maxlength="160" /></b-form-group
          ></b-col>
          <b-col md="12"
          ><b-form-group label="Nguồn dữ liệu"
              ><div class="static-source">Thẻ XML hóa đơn</div></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group :label="form.id ? 'Nhóm dữ liệu' : 'Nhóm dữ liệu'" label-for="field-group"
              ><b-form-select id="field-group" v-model="form.group" :options="fieldGroupOptions" :disabled="!!form.id" required @change="syncGeneratedKey" /></b-form-group
          ></b-col>
          <b-col md="6"
            ><b-form-group :label="form.id ? 'Đường dẫn XML tương đối' : 'Đường dẫn XML tương đối'" label-for="field-xml-path"
              ><b-form-input
                id="field-xml-path"
                v-model.trim="form.sourcePath"
                required
                :readonly="!!form.id"
                placeholder="PBan"
                maxlength="700"
                @input="syncGeneratedKey" /></b-form-group
          ></b-col>
        </b-row>
        <b-form-group label="Đường dẫn XML đầy đủ" label-for="field-full-path">
          <b-form-input id="field-full-path" :value="fieldXmlPath(form)" readonly />
        </b-form-group>
        <b-form-group label="Loại hóa đơn, chứng từ áp dụng">
          <v-select
            v-model="form.invoiceTypeSelections"
            class="invoice-type-select"
            :options="invoiceTypeOptions"
            :reduce="(option) => option.value"
            label="shortText"
            multiple
            :close-on-select="false"
            :clearable="false"
            :searchable="false"
            placeholder="Chọn loại hóa đơn, chứng từ"
          >
            <template #option="option">{{ option.text }}</template>
            <template #no-options>Không có loại phù hợp</template>
          </v-select>
        </b-form-group>
        <div class="d-flex flex-wrap field-flags mb-3">
          <b-form-checkbox v-model="form.requiredDisplay" @change="requiredChanged"
            >Bắt buộc hiển thị trên mẫu</b-form-checkbox
          ><b-form-checkbox v-model="form.active" :disabled="form.requiredDisplay"
            >Cho phép sử dụng</b-form-checkbox
          >
        </div>
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
        <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
        <div class="modal-actions">
          <b-button variant="light" class="mr-2" :disabled="saving" @click="$refs.fieldModal.hide()"
            >Hủy</b-button
          ><b-button type="submit" variant="primary" :disabled="saving"
            ><b-spinner v-if="saving" small class="mr-1" />Lưu phần tử</b-button
          >
        </div>
      </b-form>
    </b-modal>
    <b-modal ref="groupModal" :title="groupForm.id ? 'Cập nhật nhóm dữ liệu' : 'Thêm nhóm dữ liệu'" hide-footer>
      <b-form @submit.prevent="saveGroup">
        <b-form-group label="Nhóm dữ liệu hiện có">
          <b-form-select
            :value="groupForm.id"
            :options="[
              { value: null, text: 'Thêm nhóm dữ liệu mới' },
              ...metadata.groups
                .filter((g) => g.code !== 'document')
                .map((g) => ({ value: g.id, text: g.label }))
            ]"
            @change="selectGroup"
          />
        </b-form-group>
        <b-form-group label="Tên nhóm dữ liệu"
          ><b-form-input v-model.trim="groupForm.label" required maxlength="255"
        /></b-form-group>
        <b-form-group label="Mã kỹ thuật"
          ><b-form-input
            v-model.trim="groupForm.code"
            required
            :disabled="!!groupForm.id"
            maxlength="80"
        /></b-form-group>
        <b-form-group label="Thứ tự hiển thị"
          ><b-form-input v-model.number="groupForm.sortOrder" type="number" min="0"
        /></b-form-group>
        <b-form-group label="Đường dẫn XML của nhóm">
          <b-form-input v-model.trim="groupForm.rootPath" required maxlength="700" :readonly="!!groupForm.id" />
        </b-form-group>
        <b-form-checkbox v-model="groupForm.active" class="mb-3">Cho phép sử dụng</b-form-checkbox>
        <b-alert v-if="error" show variant="danger">{{ error }}</b-alert>
        <div class="modal-actions">
          <b-button
            v-if="groupForm.id"
            variant="outline-danger"
            class="mr-auto"
            :disabled="saving"
            @click="deleteGroup(groupForm)"
            >Xóa nhóm</b-button
          ><b-button variant="light" class="mr-2" @click="$refs.groupModal.hide()">Đóng</b-button
          ><b-button type="submit" variant="primary" :disabled="saving">Lưu nhóm dữ liệu</b-button>
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
      metadata: { groups: [], groupUsage: {}, nextSortOrder: 10 },
      filters: { q: '', group: null, active: null },
      form: this.emptyForm(),
      groupForm: {},
      columns: [
        { key: 'index', label: 'STT', thStyle: { width: '4%' }, class: 'text-center' },
        { key: 'field', label: 'Phần tử', thStyle: { width: '15%' } },
        { key: 'group', label: 'Nhóm dữ liệu', thStyle: { width: '10%' } },
        { key: 'sourcePath', label: 'Dữ liệu', thStyle: { width: '20%' } },
        { key: 'valueType', label: 'Kiểu', thStyle: { width: '10%' } },
        { key: 'invoiceTypes', label: 'Áp dụng', thStyle: { width: '12%' } },
        { key: 'active', label: 'Trạng thái', thStyle: { width: '9%' }, class: 'text-center' },
        {
          key: 'actions',
          label: '',
          thStyle: { width: '4%' },
          class: 'text-center table-action-cell'
        }
      ]
    }
  },
  computed: {
    groupOptions() {
      return this.metadata.groups
        .filter((g) => g.code !== 'document')
        .map((g) => ({
        value: g.code,
        text: g.label
        }))
    },
    fieldGroupOptions() {
      return this.metadata.groups
        .filter((g) => g.code !== 'document' && (g.active || g.code === this.form.group))
        .map((g) => ({
          value: g.code,
          text: g.label
        }))
    },
    typeOptions() {
      return Object.entries(types).map(([value, text]) => ({ value, text }))
    },
    invoiceTypeOptions() {
      return [
        { value: '1', text: 'Hóa đơn giá trị gia tăng', shortText: 'GTGT', tone: 'vat' },
        { value: '2', text: 'Hóa đơn bán hàng', shortText: 'Bán hàng', tone: 'sales' },
        { value: '6:1', text: 'Phiếu xuất kho kiêm vận chuyển nội bộ', shortText: 'VC nội bộ', tone: 'internal' },
        { value: '6:2', text: 'Phiếu xuất kho hàng gửi bán đại lý', shortText: 'Gửi bán ĐL', tone: 'consignment' }
      ]
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
        sourceType: 'XML',
        sourcePath: '',
        valueType: 'STRING',
        maxLength: null,
        defaultFormatter: 'plainText',
        applicableInvoiceTypes: '1,2',
        invoiceTypeSelections: ['1', '2'],
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
    groupUsage(code) {
      return this.metadata.groupUsage[code] || { fieldCount: 0, groups: [], rowScope: code }
    },
    fieldXmlPath(field) {
      const root = this.groupUsage(field.group).xmlRoot || ''
      return field.sourcePath ? `${root}/${field.sourcePath}` : root
    },
    fieldAvailable(field) {
      return field.active && this.groupUsage(field.group).active &&
        this.metadata.groups.some((g) => g.code === field.group && g.active)
    },
    groupLabel(code) {
      return this.metadata.groups.find((g) => g.code === code)?.label || code
    },
    typeLabel(code) {
      return types[code] || code
    },
    invoiceTypeSelections(value) {
      return (value || '1,2,6').split(',').flatMap((item) => {
        const code = item.trim()
        return code === '6' ? ['6:1', '6:2'] : [code]
      }).filter((item, index, values) => item && values.indexOf(item) === index)
    },
    invoiceTypeLabel(value) {
      return this.invoiceTypeItems(value).map((option) => option.text).join(', ')
    },
    invoiceTypeItems(value) {
      const selected = this.invoiceTypeSelections(value)
      return this.invoiceTypeOptions.filter((option) => selected.includes(option.value))
    },
    openField(item) {
      this.error = ''
      this.form = item ? { ...item, expectedLockVersion: item.lockVersion } : this.emptyForm()
      this.form.invoiceTypeSelections = this.invoiceTypeSelections(this.form.applicableInvoiceTypes)
      delete this.form.dataset
      if (!item) {
        this.form.sortOrder = this.metadata.nextSortOrder
        this.form.group = this.filters.group || this.fieldGroupOptions[0]?.value || ''
        this.syncGeneratedKey()
      }
      this.$refs.fieldModal.show()
    },
    syncGeneratedKey() {
      if (this.form.id) return
      const normalize = (value) => (value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/gi, 'd')
      const group = normalize(this.form.group || 'field')
        .split('.')
        .map((part) => part.replace(/[^A-Za-z0-9]/g, ''))
        .filter(Boolean)
        .map((part) => part.charAt(0).toLowerCase() + part.slice(1))
        .join('.') || 'field'
      const path = normalize(this.form.sourcePath)
        .split('/')
        .map((segment) => {
          const base = segment.match(/^([^\[]+)/)?.[1] || ''
          const values = [...segment.matchAll(/\[[^=\]]+=(?:'([^']*)'|"([^"]*)")\]/g)]
            .map((match) => match[1] || match[2] || '')
          return [base, ...values]
            .join('')
            .replace(/[^A-Za-z0-9]/g, '')
        })
        .filter(Boolean)
      this.form.key = path.length ? `${group}.${path.join('.')}` : `${group}.field`
    },
    requiredChanged(value) {
      if (value) this.form.active = true
    },
    async saveField() {
      await this.perform(async () => {
        const body = { ...this.form, maxLength: this.form.maxLength || null }
        body.applicableInvoiceTypes = body.invoiceTypeSelections.join(',')
        delete body.invoiceTypeSelections
        if (body.id) await axios.put(`${endpoint}/${body.id}`, body)
        else await axios.post(endpoint, body)
        this.$refs.fieldModal.hide()
        await this.reload()
      })
    },
    async deleteField(item) {
      if (!window.confirm(`Xóa phần tử “${item.label}” khỏi danh mục?`)) return
      await this.perform(
        async () => {
          await axios.delete(`${endpoint}/${item.id}`, {
            params: { expectedLockVersion: item.lockVersion }
          })
          await this.reload()
        },
        'Đã xóa phần tử'
      )
    },
    selectGroup(id) {
      const item = this.metadata.groups.find((g) => g.id === id)
      this.groupForm = item
        ? { ...item, expectedLockVersion: item.lockVersion }
        : { id: null, code: '', label: '', cardinality: 'ONE', rootPath: '/HDon', sortOrder: 0, active: true }
      this.error = ''
    },
    openGroup(item) {
      this.selectGroup(item ? item.id : null)
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
    async deleteGroup(item) {
      if (!window.confirm(`Xóa nhóm “${item.label}”?`)) return
      await this.perform(
        async () => {
          await axios.delete(`${endpoint}/groups/${item.id}`, {
            params: { expectedLockVersion: item.expectedLockVersion ?? item.lockVersion }
          })
          this.$refs.groupModal.hide()
          await this.reload()
        },
        'Đã xóa nhóm'
      )
    },
    async perform(action, successMessage = 'Đã lưu thay đổi') {
      this.error = ''
      this.saving = true
      try {
        await action()
        this.$bvToast.toast(successMessage, { variant: 'success', solid: true })
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
.form-invoice-fields .table thead th {
  background: #f7f9fc;
  border-bottom: 1px solid #ecf0f6;
  color: #4a5568;
  font-weight: 700;
}
.page-actions {
  display: flex;
  flex-wrap: wrap;
}
.field-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
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
.modal-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.static-source {
  min-height: 38px;
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 0.875rem;
}
.invoice-type-select :deep(.vs__dropdown-toggle) {
  min-height: 38px;
  padding: 0 4px 3px;
  border-color: #ced4da;
  border-radius: 6px;
  background: #fff;
}
.invoice-type-select :deep(.vs__selected) {
  margin: 4px 2px 0;
  padding: 2px 6px;
  border: 0;
  border-radius: 4px;
  background: #eef2f7;
  color: #344054;
  font-size: 0.8125rem;
  line-height: 1.35;
}
.invoice-type-select :deep(.vs__search) {
  margin-top: 4px;
  font-size: 0.875rem;
}
.invoice-type-select :deep(.vs__actions) {
  padding-top: 4px;
}
.invoice-type-select :deep(.vs__dropdown-option) {
  padding: 8px 12px;
  white-space: normal;
}
.invoice-type-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.invoice-type-badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 2px 6px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 600;
  line-height: 1.25;
}
.invoice-type-badge--vat {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}
.invoice-type-badge--sales {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}
.invoice-type-badge--internal {
  border-color: #c4b5fd;
  background: #f5f3ff;
  color: #6d28d9;
}
.invoice-type-badge--consignment {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}
.fields-table :deep(.badge) {
  white-space: normal;
  line-height: 1.4;
}
.fields-table :deep(.dropdown-item) {
  text-align: center;
}
</style>
